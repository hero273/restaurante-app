package com.wayas.app.service;
import com.wayas.app.model.Insumo;
import com.wayas.app.repository.IInsumoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InsumoService {
    @Autowired private IInsumoRepository repoInsumo;

    public List<Insumo> listarTodos() { return repoInsumo.findAll(); }
    public List<Insumo> buscarPorDescripcion(String desc) { return repoInsumo.findByDescripcionContainingIgnoreCase(desc); }
    public List<Insumo> listarInsumosBajoMinimo() { return repoInsumo.findInsumosBajoMinimo(); }
    public Insumo guardar(Insumo insumo) { return repoInsumo.save(insumo); }
    public Insumo obtenerPorId(Integer id) { return repoInsumo.findById(id).orElse(null); }
    public void eliminar(Integer id) { repoInsumo.deleteById(id); }
    public void actualizar(Insumo insumo) {repoInsumo.save(insumo); 
    }
}