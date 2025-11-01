package com.wayas.app.controller;

import com.wayas.app.model.Usuario;
import com.wayas.app.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String mostrarGestionUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new Usuario());
        }
        return "gestion_usuarios"; 
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, RedirectAttributes redirectAttrs) {
        try {
            usuarioService.guardar(usuario);
            redirectAttrs.addFlashAttribute("mensajeExito", "Usuario '" + usuario.getUsername() + "' guardado.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al guardar: " + e.getMessage());
            redirectAttrs.addFlashAttribute("usuario", usuario);
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String mostrarEditarUsuario(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        if (usuario != null) {
            usuario.setPassword(null); 
            redirectAttrs.addFlashAttribute("usuario", usuario);
        } else {
            redirectAttrs.addFlashAttribute("mensajeError", "Usuario no encontrado.");
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        try {
            usuarioService.eliminar(id);
            redirectAttrs.addFlashAttribute("mensajeExito", "Usuario eliminado.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al eliminar.");
        }
        return "redirect:/usuarios";
    }
}