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

    @Autowired
    private IProveedorRepository repoProveedor; 

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
        return repoCalificacion.save(cal);
    }

    // 5. Método para buscar el historial
    public List<Calificacion> historialPorProveedor(Integer idProveedor) {
        return repoCalificacion.findByProveedorIdProv(idProveedor);
    }
}
