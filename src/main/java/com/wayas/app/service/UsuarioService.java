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

    // --- INICIO DE LA CORRECCIÓN (BUG PASSWORD UPDATE) ---
    public Usuario guardar(Usuario usuario) {
        
        // 1. Verificar si es una ACTUALIZACIÓN (ya tiene ID)
        if (usuario.getId() != null) {
            
            // 2. Verificar si el admin dejó la contraseña en blanco (nula O vacía)
            if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
                
                // 3. Obtener la contraseña actual (hash) de la BD
                Usuario usuarioExistente = repoUsuario.findById(usuario.getId()).orElse(null);
                
                if (usuarioExistente != null) {
                    usuario.setPassword(usuarioExistente.getPassword()); // Asignamos la contraseña antigua sin cambios
                } else {
                    // Caso raro: editando un usuario que no existe.
                    // Mejor encriptar la contraseña vacía (comportamiento anterior).
                    usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
                }
            } else {
                // 4. Si el admin SÍ escribió una nueva contraseña, la encriptamos
                // (Mantenemos la validación para no re-encriptar un hash)
                 if (!usuario.getPassword().startsWith("$2a$")) {
                    usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
                 }
            }
        } else {
            // 5. Es un USUARIO NUEVO, siempre encriptar la contraseña
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        
        return repoUsuario.save(usuario);
    }
}