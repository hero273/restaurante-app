package com.wayas.app.repository;

import com.wayas.app.model.DetalleReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDetalleRecetaRepository extends JpaRepository<DetalleReceta, Long> {
}