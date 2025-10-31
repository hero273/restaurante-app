package com.wayas.app.controller;

import com.wayas.app.model.Compra;
import com.wayas.app.model.Proveedor; 
import com.wayas.app.service.CompraService; 
import com.wayas.app.service.ProveedorService; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; 
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List; 
import java.util.stream.Collectors; 

@Controller
@RequestMapping("/calificacion") 
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;
    
    @Autowired
    private CompraService compraService;

    @GetMapping("/proveedores")
    public String mostrarGestionProveedores(
            @RequestParam(required = false) String buscarProveedor, 
            Model model) {
        
        List<Proveedor> listaProveedores;
            listaProveedores = proveedorService.listarTodos();
        model.addAttribute("proveedores", listaProveedores); 
        if (!model.containsAttribute("proveedor")) {
            model.addAttribute("proveedor", new Proveedor());
        }
        return "calificacion_registrar_proveedor";
    }

    @PostMapping("/proveedores/guardar")
    public String guardarProveedor(@ModelAttribute("proveedor") Proveedor proveedor, RedirectAttributes redirectAttrs) {
        if (proveedor.getRazonSocial() == null || proveedor.getRazonSocial().trim().isEmpty()) {
             redirectAttrs.addFlashAttribute("mensajeError", "El nombre (Razón Social) del proveedor es obligatorio.");
             redirectAttrs.addFlashAttribute("proveedor", proveedor); 
             return "redirect:/calificacion/proveedores";
        }
        try {
            Proveedor guardado = proveedorService.guardar(proveedor);
            redirectAttrs.addFlashAttribute("mensajeExito", "Proveedor '" + guardado.getRazonSocial() + "' guardado correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al guardar el proveedor: " + e.getMessage());
            redirectAttrs.addFlashAttribute("proveedor", proveedor);
        }
        return "redirect:/calificacion/proveedores";
    }

    @GetMapping("/proveedores/editar/{id}")
    public String mostrarEditarProveedor(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Proveedor proveedor = proveedorService.obtenerPorId(id);
        if (proveedor != null) {
            redirectAttrs.addFlashAttribute("proveedor", proveedor); 
        } else {
            redirectAttrs.addFlashAttribute("mensajeError", "Proveedor con ID " + id + " no encontrado.");
        }
        return "redirect:/calificacion/proveedores"; 
    }

    @GetMapping("/proveedores/eliminar/{id}") 
    public String eliminarProveedor(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Proveedor proveedor = proveedorService.obtenerPorId(id); 
         if (proveedor == null) {
            redirectAttrs.addFlashAttribute("mensajeError", "Proveedor con ID " + id + " no encontrado para eliminar.");
            return "redirect:/calificacion/proveedores";
        }
        try {
            proveedorService.eliminar(id);
            redirectAttrs.addFlashAttribute("mensajeExito", "Proveedor '" + proveedor.getRazonSocial() + "' eliminado.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al eliminar proveedor '" + proveedor.getRazonSocial() + "'. Podría estar asignado a insumos.");
        }
        return "redirect:/calificacion/proveedores";
    }
    @GetMapping("/evaluar")
    public String mostrarEvaluarProveedor(Model model) {
        List<Compra> comprasRegistradas = compraService.listarTodas().stream()
                .filter(c -> "REGISTRADA".equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.toList());
        
        model.addAttribute("compras", comprasRegistradas);
        return "calificacion_evaluar_proveedor";
    }
    @GetMapping("/consultar")
    public String mostrarConsultarDesempeno(Model model) {
        model.addAttribute("proveedores", proveedorService.listarTodos());
        return "calificacion_consultar_desempeno";
    }
}
