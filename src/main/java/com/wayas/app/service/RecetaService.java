package com.wayas.app.service;

import org.springframework.transaction.annotation.Transactional; 
import com.wayas.app.model.DetalleReceta;
import com.wayas.app.model.Insumo;
import com.wayas.app.model.Receta;
import com.wayas.app.service.StockInsuficienteException;
import com.wayas.app.repository.IRecetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class RecetaService {

    @Autowired
    private IRecetaRepository repoReceta;

    @Autowired
    private InsumoService insumoService;

    /**
     * Busca todas las recetas que están marcadas como BORRADOR
     */
    @Transactional(readOnly = true) 
    public List<Receta> listarBorradores() {
        return repoReceta.findByEstado("BORRADOR");
    }

    /**
     * Guarda una receta principal y todos sus detalles (ingredientes)
     */
    @Transactional
    public Receta guardarReceta(Receta recetaFormulario, 
                                List<Integer> insumoIds, 
                                List<BigDecimal> cantidades, 
                                List<String> unidades) {
        
        Receta recetaParaGuardar;

        if (recetaFormulario.getId() != null) {
            // Es una EDICIÓN: Carga la receta y sus detalles de la BD
            recetaParaGuardar = repoReceta.findById(recetaFormulario.getId())
                                        .orElse(recetaFormulario); 
            
            // Actualiza los campos simples con los datos del formulario
            recetaParaGuardar.setNombre(recetaFormulario.getNombre());
            recetaParaGuardar.setCategoria(recetaFormulario.getCategoria());
            recetaParaGuardar.setDescripcion(recetaFormulario.getDescripcion());
            recetaParaGuardar.setPorciones(recetaFormulario.getPorciones());
            recetaParaGuardar.setPasos(recetaFormulario.getPasos());
            
            // Limpia los detalles *antiguos*
            recetaParaGuardar.getDetalles().clear();
        } else {
            // Es NUEVA
            recetaParaGuardar = recetaFormulario;
        }

        // Itera sobre la lista de ingredientes (nuevos o actualizados)
        if (insumoIds != null && insumoIds.size() == cantidades.size()) {
            for (int i = 0; i < insumoIds.size(); i++) {
                
                Insumo insumo = insumoService.obtenerPorId(insumoIds.get(i));
                if (insumo == null) {
                    throw new IllegalArgumentException("Insumo no encontrado con ID: " + insumoIds.get(i));
                }

                DetalleReceta detalle = new DetalleReceta();
                detalle.setInsumo(insumo);
                detalle.setCantidad(cantidades.get(i));
                detalle.setUnidadMedida(unidades.get(i));
                
                // Añade el detalle a la receta principal
                recetaParaGuardar.addDetalle(detalle);
            }
        }
        
        recetaParaGuardar.setEstado("BORRADOR");
        return repoReceta.save(recetaParaGuardar);
    }
    
    @Transactional(readOnly = true) 
    public Receta obtenerPorId(Long id) {
        Receta receta = repoReceta.findById(id).orElse(null);
        if (receta != null) {
            
            receta.getDetalles().size(); 
        }
        return receta;
    }
   

    @Transactional
    public void eliminar(Long id) {
        Optional<Receta> recetaOpt = repoReceta.findById(id);
        if (recetaOpt.isPresent()) {
             repoReceta.delete(recetaOpt.get());
        } else {
            throw new IllegalArgumentException("No se puede eliminar. Receta no encontrada con ID: " + id);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Receta> listarPorEstado(String estado) {
        return repoReceta.findByEstado(estado);
    }
    
    @Transactional
    public Receta cambiarEstado(Long idReceta, String nuevoEstado) {

        Receta receta = obtenerPorId(idReceta); 
        
        if (receta == null) {
            throw new IllegalArgumentException("Receta no encontrada con ID: " + idReceta);
        }
        
        receta.setEstado(nuevoEstado);
        return repoReceta.save(receta);
    }
    
    @Transactional(rollbackFor = StockInsuficienteException.class) // Si se lanza esta excep, hace rollback
    public void prepararReceta(Long idReceta, int porcionesAPreparar) throws StockInsuficienteException {
        
        Receta receta = obtenerPorId(idReceta); // (Ya viene con los detalles cargados)
        
        if (receta == null || !receta.getEstado().equals("APROBADA")) {
            throw new IllegalArgumentException("Solo se pueden preparar recetas APROBADAS.");
        }
        
        if (porcionesAPreparar <= 0) {
            throw new IllegalArgumentException("La cantidad de porciones debe ser mayor a cero.");
        }

        // --- BUCLE 1: VERIFICACIÓN DE STOCK ---
        // Primero, revisamos si TENEMOS stock de TODO, antes de descontar nada.
        for (DetalleReceta detalle : receta.getDetalles()) {
            Insumo insumo = detalle.getInsumo();
            
            // Calcula cuánto necesitamos (ej. 0.5kg por porción * 4 porciones = 2kg)
            BigDecimal cantidadNecesaria = detalle.getCantidad().multiply(new BigDecimal(porcionesAPreparar));
            
            if (insumo.getStockActual().compareTo(cantidadNecesaria) < 0) {
                // No hay stock suficiente
                throw new StockInsuficienteException(
                    "Stock insuficiente de '" + insumo.getDescripcion() + "'. " +
                    "Se necesitan: " + cantidadNecesaria + " " + detalle.getUnidadMedida() + ". " +
                    "Solo hay: " + insumo.getStockActual() + " " + insumo.getUnidadMedida()
                );
            }
        }
        
        // --- BUCLE 2: DESCUENTO DE STOCK ---
        // Si pasamos la verificación, ahora sí descontamos (esto es seguro por el @Transactional)
        for (DetalleReceta detalle : receta.getDetalles()) {
            Insumo insumo = detalle.getInsumo();
            BigDecimal cantidadADescontar = detalle.getCantidad().multiply(new BigDecimal(porcionesAPreparar));
            
            BigDecimal nuevoStock = insumo.getStockActual().subtract(cantidadADescontar);
            insumo.setStockActual(nuevoStock);
            
            insumoService.guardar(insumo); // Guardamos el insumo con el stock actualizado
        }
    }
    
}
