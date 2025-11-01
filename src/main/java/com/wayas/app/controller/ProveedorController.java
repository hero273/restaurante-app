package com.wayas.app.controller;

import com.wayas.app.model.Calificacion; // NUEVO
import com.wayas.app.model.Compra;
import com.wayas.app.model.Proveedor;
import com.wayas.app.service.CalificacionService; // NUEVO
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

    @Autowired
    private CalificacionService calificacionService; // NUEVO SERVICIO

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

    @PostMapping("/proveedores/eliminar/{id}") 
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

    // --- CORRECCIÓN 1: EVALUAR PROVEEDOR ---

    @GetMapping("/evaluar")
    public String mostrarEvaluarProveedor(Model model) {
        // Obtenemos solo compras registradas
        List<Compra> comprasRegistradas = compraService.listarTodas().stream()
                .filter(c -> "REGISTRADA".equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.toList());
        
        model.addAttribute("compras", comprasRegistradas);
        // Añadimos un objeto vacío para el formulario
        model.addAttribute("calificacion", new Calificacion()); 
        return "calificacion_evaluar_proveedor";
    }

    // NUEVO ENDPOINT PARA GUARDAR LA CALIFICACIÓN
    @PostMapping("/guardar")
    public String guardarCalificacion(@RequestParam Long idCompra,
                                      @RequestParam Integer idProveedor,
                                      @RequestParam int calidad,
                                      @RequestParam int peso,
                                      @RequestParam int puntualidad,
                                      @RequestParam String observaciones,
                                      RedirectAttributes redirectAttrs) {
        try {
            calificacionService.guardarCalificacion(idCompra, idProveedor, calidad, peso, puntualidad, observaciones);
            redirectAttrs.addFlashAttribute("mensajeExito", "Calificación guardada exitosamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/calificacion/evaluar";
    }

    // --- CORRECCIÓN 2: CONSULTAR DESEMPEÑO ---
    
    @GetMapping("/consultar")
    public String mostrarConsultarDesempeno(
            @RequestParam(required = false) Integer idProveedor, // Buscamos por ID
            Model model) {
        
        model.addAttribute("proveedores", proveedorService.listarTodos()); // Para el <select>
        
        if (idProveedor != null) {
            // Si se seleccionó un proveedor, buscamos su historial
            List<Calificacion> historial = calificacionService.historialPorProveedor(idProveedor);
            model.addAttribute("historial", historial);
            model.addAttribute("selectedProvId", idProveedor); // Para mantener el valor en el <select>
        }
        
        return "calificacion_consultar_desempeno";
    }
}
