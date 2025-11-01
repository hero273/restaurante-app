package com.wayas.app.controller;

import com.wayas.app.model.Insumo;
import com.wayas.app.model.Requerimiento;
import com.wayas.app.service.InventarioService;
import com.wayas.app.service.InsumoService;
import com.wayas.app.service.ProveedorService; // Añadido para proveedores
import com.wayas.app.service.RequerimientoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List; // Añadido

@Controller
@RequestMapping("/inventario") 
public class InventarioController {

    @Autowired private InventarioService inventarioService;
    @Autowired private RequerimientoService reqService;
    @Autowired private InsumoService insumoService;
    @Autowired private ProveedorService proveedorService; 

    @GetMapping("/ingresar/pendientes")
    public String mostrarIngresosPendientes(Model model) {
        List<Requerimiento> reqsComprados = reqService.listarPorEstado("COMPRADO");
        model.addAttribute("requerimientos", reqsComprados);
        
        return "inventario_ingresar_compras";
    }

    @PostMapping("/ingresar/procesar/{id}")
    public String procesarIngreso(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            boolean exito = inventarioService.ingresarRequerimientoAInventario(id);
            if (exito) {
                redirectAttrs.addFlashAttribute("mensajeExito", "Requerimiento ID " + id + " ingresado al inventario.");
            } else {
                redirectAttrs.addFlashAttribute("mensajeError", "No se pudo ingresar el requerimiento ID " + id + " (verificar estado o existencia).");
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al ingresar requerimiento ID " + id + ": " + e.getMessage());
        }
        
        return "redirect:/inventario/ingresar/pendientes";
    }

    @GetMapping("/contrastar")
    public String mostrarContrasteInventario(Model model) {
        model.addAttribute("insumos", insumoService.listarTodos());
        
        return "inventario_conteo_fisico";
    }

    
     @PostMapping("/contrastar/actualizar")
     public String actualizarStockIndividual(@RequestParam Integer idInsumo,
                                             @RequestParam(required = false) BigDecimal conteoFisico,
                                             RedirectAttributes redirectAttrs) {
         if (conteoFisico == null) {
              redirectAttrs.addFlashAttribute("mensajeError", "El conteo físico no puede estar vacío para el insumo ID: " + idInsumo);
              return "redirect:/inventario/contrastar";
         }

         try {
             Insumo actualizado = inventarioService.actualizarStockPorConteo(idInsumo, conteoFisico);
             if (actualizado != null) {
                 redirectAttrs.addFlashAttribute("mensajeExito", "Stock de '" + actualizado.getDescripcion() + "' actualizado a " + conteoFisico + " " + actualizado.getUnidadMedida());
             } else {
                 redirectAttrs.addFlashAttribute("mensajeError", "No se pudo actualizar el insumo ID: " + idInsumo + ". Verifique que exista y el conteo sea válido (>= 0).");
             }
         } catch (Exception e) {
             redirectAttrs.addFlashAttribute("mensajeError", "Error al actualizar stock para ID " + idInsumo + ": " + e.getMessage());
         }
         return "redirect:/inventario/contrastar";
     }


    @GetMapping("/insumos")
    public String mostrarGestionInsumos(
            @RequestParam(required = false) String buscarInsumo,
            Model model) {

        List<Insumo> listaInsumos;
        if (buscarInsumo != null && !buscarInsumo.trim().isEmpty()) { 
            listaInsumos = insumoService.buscarPorDescripcion(buscarInsumo);
        } else {
            listaInsumos = insumoService.listarTodos();
        }

        model.addAttribute("insumos", listaInsumos);
        model.addAttribute("proveedores", proveedorService.listarTodos()); 

        if (!model.containsAttribute("insumo")) {
            model.addAttribute("insumo", new Insumo());
        }
        return "gestion_insumos";
    }

    @PostMapping("/insumos/guardar")
    public String guardarInsumo(@ModelAttribute("insumo") Insumo insumo, RedirectAttributes redirectAttrs) {
        try {
            
            if (insumo.getProveedor() != null && insumo.getProveedor().getIdProv() == null) {
                insumo.setProveedor(null); 
            }
            Insumo guardado = insumoService.guardar(insumo);
            redirectAttrs.addFlashAttribute("mensajeExito", "Insumo '" + guardado.getDescripcion() + "' guardado correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al guardar el insumo: " + e.getMessage());
            
            redirectAttrs.addFlashAttribute("insumo", insumo);
        }
        return "redirect:/inventario/insumos"; 
    }

    @GetMapping("/insumos/editar/{id}")
    public String mostrarEditarInsumo(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Insumo insumo = insumoService.obtenerPorId(id);
        if (insumo != null) {
            
            redirectAttrs.addFlashAttribute("insumo", insumo);
        } else {
            redirectAttrs.addFlashAttribute("mensajeError", "Insumo con ID " + id + " no encontrado.");
        }
        return "redirect:/inventario/insumos"; 
    }

    @PostMapping("/insumos/eliminar/{id}")
    public String eliminarInsumo(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Insumo insumo = insumoService.obtenerPorId(id); 
        if (insumo == null) {
            redirectAttrs.addFlashAttribute("mensajeError", "Insumo con ID " + id + " no encontrado para eliminar.");
            return "redirect:/inventario/insumos";
        }

        try {
            insumoService.eliminar(id);
            redirectAttrs.addFlashAttribute("mensajeExito", "Insumo '" + insumo.getDescripcion() + "' eliminado correctamente.");
        } catch (Exception e) { 
            redirectAttrs.addFlashAttribute("mensajeError", "Error al eliminar el insumo '" + insumo.getDescripcion() + "'. Es posible que esté siendo utilizado en requerimientos o recetas.");
        }
        return "redirect:/inventario/insumos"; 
    }
}