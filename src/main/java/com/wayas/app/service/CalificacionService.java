package com.wayas.app.service;

import com.wayas.app.model.Calificacion;
import com.wayas.app.model.Compra;
import com.wayas.app.model.Proveedor;
import com.wayas.app.repository.ICalificacionRepository;
import com.wayas.app.repository.ICompraRepository;
import com.wayas.app.repository.IProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CalificacionService {

    @Autowired
    private ICalificacionRepository repoCalificacion;

    @Autowired
    private ICompraRepository repoCompra; 

    // --- INICIO MEJORA (PROMEDIO) ---
    @Autowired
    private IProveedorRepository repoProveedor; 
    // --- FIN MEJORA ---

    @Transactional
    public Calificacion guardarCalificacion(Long idCompra, Integer idProveedor, int calidad, int peso, int puntualidad, String observaciones) {
        
        // 1. Validar si la compra ya fue calificada
        if (repoCalificacion.findByCompraId(idCompra).isPresent()) {
            throw new IllegalStateException("Esta compra ya fue calificada.");
        }

        // 2. Buscar las entidades
        Compra compra = repoCompra.findById(idCompra)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada con ID: " + idCompra));
        
        Proveedor proveedor = repoProveedor.findById(idProveedor)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado con ID: " + idProveedor));

        // 3. Crear la nueva calificación
        Calificacion cal = new Calificacion();
        cal.setCompra(compra);
        cal.setProveedor(proveedor);
        cal.setFecha(LocalDate.now());
        cal.setCalidad(calidad);
        cal.setPeso(peso);
        cal.setPuntualidad(puntualidad);
        cal.setObservaciones(observaciones);

        // 4. Guardar
        Calificacion calGuardada = repoCalificacion.save(cal);
        
        // --- INICIO MEJORA (PROMEDIO) ---
        // 5. Actualizar el puntaje promedio del proveedor
        actualizarPromedioProveedor(proveedor.getIdProv());
        // --- FIN MEJORA ---
        
        return calGuardada;
    }

    // 5. Método para buscar el historial
    public List<Calificacion> historialPorProveedor(Integer idProveedor) {
        return repoCalificacion.findByProveedorIdProv(idProveedor);
    }
    
    // --- INICIO MEJORA (PROMEDIO) ---
    /**
     * Calcula y actualiza el puntaje promedio de un proveedor basándose
     * en todo su historial de calificaciones.
     */
    @Transactional
    private void actualizarPromedioProveedor(Integer idProveedor) {
        Proveedor proveedor = repoProveedor.findById(idProveedor).orElse(null);
        if (proveedor == null) {
            return; // No se encontró el proveedor, salir
        }

        // 1. Obtener todo el historial de calificaciones
        List<Calificacion> historial = repoCalificacion.findByProveedorIdProv(idProveedor);
        
        if (historial.isEmpty()) {
            // Si no hay historial, resetear a los valores por defecto (ej. 3)
            proveedor.setPuntajeCalidad(3);
            proveedor.setPuntajePuntualidad(3);
            // (El campo 'disponibilidad' no se califica, se deja como esté o se resetea)
            // proveedor.setPuntajeDisponibilidad(3); 
        } else {
            // 2. Calcular los promedios
            // (Nota: 'peso' de Calificacion no tiene un campo 'puntajePeso' en Proveedor.java, se ignora)
            
            double avgCalidad = historial.stream()
                    .mapToInt(Calificacion::getCalidad)
                    .average()
                    .orElse(3.0); // Promedio por defecto si hay error

            double avgPuntualidad = historial.stream()
                    .mapToInt(Calificacion::getPuntualidad)
                    .average()
                    .orElse(3.0);
                    
            // 3. Guardamos los promedios redondeados como enteros (ya que el modelo Proveedor usa 'int')
            proveedor.setPuntajeCalidad((int) Math.round(avgCalidad));
            proveedor.setPuntajePuntualidad((int) Math.round(avgPuntualidad));
        }
        
        // 4. Guardar el proveedor actualizado
        repoProveedor.save(proveedor);
    }
    // --- FIN MEJORA ---
}