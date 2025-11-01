package com.wayas.app.repository;

import com.wayas.app.model.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICalificacionRepository extends JpaRepository<Calificacion, Long> {
    
    List<Calificacion> findByProveedorIdProv(Integer idProv);
    
    Optional<Calificacion> findByCompraId(Long idCompra);
}
