package com.wayas.app.service;

import com.wayas.app.model.Compra;
import com.wayas.app.model.DetalleCompra;
import com.wayas.app.model.Insumo;
import com.wayas.app.model.Proveedor;
import com.wayas.app.model.Requerimiento;
import com.wayas.app.repository.ICompraRepository;
import com.wayas.app.repository.IProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CompraService {

    @Autowired private ICompraRepository repoCompra;
    @Autowired private RequerimientoService reqService; 
    @Autowired private IProveedorRepository repoProv;
    
    @Autowired private InsumoService insumoService; 

    public List<Compra> listarTodas() {
        return repoCompra.findAll();
    }

    public Compra obtenerPorId(Long id) {
        return repoCompra.findById(id).orElse(null);
    }
    
    @Transactional
    public Compra registrarCompra(Long idRequerimiento, LocalDate fechaCompra, Integer idProveedor,
                                  BigDecimal montoTotal, String nroFactura, String detalleTexto,
                                  List<Integer> insumoIds, List<BigDecimal> cantidades) {
    	Requerimiento req = reqService.obtenerPorId(idRequerimiento);
        Optional<Proveedor> provOpt = repoProv.findById(idProveedor);

        if (req == null || provOpt.isEmpty()) {
            throw new IllegalArgumentException("Requerimiento o Proveedor no válido.");
        }
        if (!req.getEstado().equals("PENDIENTE") && !req.getEstado().equals("ENVIADO")) {
             throw new IllegalStateException("Solo se pueden registrar compras de requerimientos PENDIENTES o ENVIADOS.");
        }
        Compra compra = new Compra();
        compra.setRequerimiento(req);
        compra.setFechaCompra(fechaCompra);
        compra.setProveedor(provOpt.get());
        compra.setMontoTotal(montoTotal);
        compra.setNroFactura(nroFactura);
        compra.setDetalleInsumosComprados(detalleTexto);
        compra.setEstado("REGISTRADA");
        long count = repoCompra.count();
        compra.setCodigoCompra(String.format("COMP-%d-%d", LocalDate.now().getYear(), count + 1)); 

        if (insumoIds != null && cantidades != null && insumoIds.size() == cantidades.size()) {
            for (int i = 0; i < insumoIds.size(); i++) {
                Integer idInsumo = insumoIds.get(i);
                BigDecimal cantidad = cantidades.get(i);
                
                if (idInsumo == null || cantidad == null) continue; 

                Insumo insumo = insumoService.obtenerPorId(idInsumo);
                if (insumo != null) {
                    
                    compra.agregarDetalle(insumo, cantidad);

                    
                    BigDecimal nuevoStock = insumo.getStockActual().add(cantidad);
                    insumo.setStockActual(nuevoStock);
                    
                    insumoService.actualizar(insumo); 
                }
            }
        }

        reqService.actualizarEstado(idRequerimiento, "COMPRADO");

        return repoCompra.save(compra);
    }

    /**
     * CORRECCIÓN: Método 'anularCompra' actualizado.
     * Ahora revierte el stock de los insumos asociados a la compra
     * y regresa el requerimiento al estado 'PENDIENTE'.
     */
    @Transactional
    public Compra anularCompra(Long idCompra) {
        Compra compra = obtenerPorId(idCompra);
        
        // Solo anular si está REGISTRADA
        if (compra != null && "REGISTRADA".equalsIgnoreCase(compra.getEstado())) {

            // --- INICIO DE LA CORRECCIÓN ---
            // Revertir el stock de los insumos que se agregaron en esta compra
            if (compra.getDetalles() != null) {
                for (DetalleCompra detalle : compra.getDetalles()) {
                    Insumo insumo = detalle.getInsumo();
                    BigDecimal cantidadAnulada = detalle.getCantidad();
                    
                    if (insumo != null && cantidadAnulada != null) {
                        // Restar la cantidad del stock actual
                        BigDecimal nuevoStock = insumo.getStockActual().subtract(cantidadAnulada);
                        
                        // Seguridad: Evitar stock negativo si se hicieron ajustes manuales
                        if (nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
                            insumo.setStockActual(BigDecimal.ZERO);
                        } else {
                            insumo.setStockActual(nuevoStock);
                        }
                        // Usamos 'guardar' o 'actualizar', ambos deberían funcionar
                        insumoService.guardar(insumo); 
                    }
                }
            }
            // --- FIN DE LA CORRECCIÓN ---

            // Marcar la compra como anulada
            compra.setEstado("ANULADA");
            
            // Revertir el estado del requerimiento para que pueda ser comprado de nuevo
            if (compra.getRequerimiento() != null) {
                 reqService.actualizarEstado(compra.getRequerimiento().getId(), "PENDIENTE");
            }
            
            return repoCompra.save(compra);
        }
        // Si ya está anulada o no existe, no hacer nada
        return null;
    }
}
