package com.wayas.app.repository;

import com.wayas.app.model.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IRecetaRepository extends JpaRepository<Receta, Long> {
    List<Receta> findByEstado(String estado);
}