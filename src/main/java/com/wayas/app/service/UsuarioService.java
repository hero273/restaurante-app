package com.wayas.app.service;

import com.wayas.app.model.Usuario;
import com.wayas.app.repository.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private IUsuarioRepository repoUsuario;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> listarTodos() {
        return repoUsuario.findAll();
    }

    public Usuario obtenerPorId(Integer id) {
        return repoUsuario.findById(id).orElse(null);
    }

    public void eliminar(Integer id) {
        repoUsuario.deleteById(id);
    }

    public Usuario guardar(Usuario usuario) {
        if (usuario.getPassword() != null && !usuario.getPassword().startsWith("$2a$")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return repoUsuario.save(usuario);
    }
}