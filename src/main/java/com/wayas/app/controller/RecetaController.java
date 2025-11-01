package com.wayas.app.controller;

import com.wayas.app.model.Receta;
import com.wayas.app.service.InsumoService;
import com.wayas.app.service.RecetaService;
import com.wayas.app.service.StockInsuficienteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/recetas") 
public class RecetaController {

    @Autowired
    private RecetaService recetaService;

    @Autowired
    private InsumoService insumoService;

    @GetMapping("/registrar")
    public String mostrarRegistrarBorrador(Model model) {
        
        // 1. Añade una nueva receta vacía para el formulario
        if (!model.containsAttribute("receta")) {
            model.addAttribute("receta", new Receta());
        }
        
        // 2. Carga todos los INSUMOS para el dropdown de "Ingredientes"
        model.addAttribute("todosLosInsumos", insumoService.listarTodos());
        
        // 3. Carga los borradores existentes para la tabla inferior
        model.addAttribute("recetasBorrador", recetaService.listarBorradores());

        return "recetas_registrar_borrador"; 
    }

    @PostMapping("/guardarBorrador")
    public String guardarBorrador(@ModelAttribute("receta") Receta receta,
                                  @RequestParam(value = "insumoIds", required = false) List<Integer> insumoIds,
                                  @RequestParam(value = "cantidades", required = false) List<BigDecimal> cantidades,
                                  @RequestParam(value = "unidades", required = false) List<String> unidades,
                                  RedirectAttributes redirectAttrs) {
        
        try {
            recetaService.guardarReceta(receta, insumoIds, cantidades, unidades);
            redirectAttrs.addFlashAttribute("mensajeExito", "Receta '" + receta.getNombre() + "' guardada como borrador.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al guardar: " + e.getMessage());
            redirectAttrs.addFlashAttribute("receta", receta); // Devuelve el objeto para no perder datos
        }
        
        return "redirect:/recetas/registrar";
    }

    @GetMapping("/editar/{id}")
    public String mostrarEditarBorrador(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {

        // 1. Busca la receta Y SUS DETALLES (Ingredientes)
        //    (Esto usa el RecetaService que ya corregimos para el error LazyInitializationException)
        Receta receta = recetaService.obtenerPorId(id);

        if (receta != null) {
            // 2. Pone la receta en un atributo temporal (Flash)
            redirectAttrs.addFlashAttribute("receta", receta);
        } else {
            redirectAttrs.addFlashAttribute("mensajeError", "Receta no encontrada.");
        }

        // 3. Redirige al controlador GET /registrar
        // (El método 'mostrarRegistrarBorrador' recibirá la receta y la mostrará en el formulario)
        return "redirect:/recetas/registrar";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarBorrador(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
        try {
            recetaService.eliminar(id);
            redirectAttrs.addFlashAttribute("mensajeExito", "Borrador eliminado.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/recetas/registrar";
    }
    
    @GetMapping("/enviar")
    public String mostrarEnviarValidacion(Model model) {
        // Cargamos los borradores para el dropdown <select>
        model.addAttribute("recetasBorrador", recetaService.listarBorradores());
        
        // Cargamos las que ya están "En Revisión" para la tabla de historial
        model.addAttribute("recetasEnRevision", recetaService.listarPorEstado("EN_REVISION"));
        
        return "recetas_enviar_validacion"; // Apunta al HTML
    }
    
    @PostMapping("/enviar")
    public String enviarARevision(@RequestParam("recetaId") Long idReceta,
                                  RedirectAttributes redirectAttrs) {
        
        try {
            Receta receta = recetaService.cambiarEstado(idReceta, "EN_REVISION");
            redirectAttrs.addFlashAttribute("mensajeExito", "Receta '" + receta.getNombre() + "' enviada a validación.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al enviar: " + e.getMessage());
        }

        return "redirect:/recetas/enviar";
    }
    
    @GetMapping("/validar")
    public String mostrarValidarReceta(Model model, 
                                     @RequestParam(value="id", required=false) Long idRecetaSeleccionada) {
        
        // 1. Cargar la lista para el dropdown "Recetas en revisión"
        model.addAttribute("recetasEnRevision", recetaService.listarPorEstado("EN_REVISION"));

        // 2. Cargar las listas para la tabla de "Historial"
        model.addAttribute("recetasAprobadas", recetaService.listarPorEstado("APROBADA"));
        model.addAttribute("recetasRechazadas", recetaService.listarPorEstado("RECHAZADA"));

        // 3. Si el usuario seleccionó una receta (hizo clic en "Ver detalles")
        if (idRecetaSeleccionada != null) {
            Receta seleccionada = recetaService.obtenerPorId(idRecetaSeleccionada);
            model.addAttribute("recetaSeleccionada", seleccionada);
        } else {
            model.addAttribute("recetaSeleccionada", null); // Para que el HTML sepa que no hay nada
        }

        return "recetas_validar"; // Apunta al HTML
    }
    
    @PostMapping("/validar/aprobar")
    public String aprobarReceta(@RequestParam("recetaId") Long idReceta,
                                RedirectAttributes redirectAttrs) {
        try {
            Receta receta = recetaService.cambiarEstado(idReceta, "APROBADA");
            redirectAttrs.addFlashAttribute("mensajeExito", "Receta '" + receta.getNombre() + "' APROBADA.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al aprobar: " + e.getMessage());
        }
        return "redirect:/recetas/validar";
    }
    
    @PostMapping("/validar/rechazar")
    public String rechazarReceta(@RequestParam("recetaId") Long idReceta,
                                 RedirectAttributes redirectAttrs) {
        try {
            Receta receta = recetaService.cambiarEstado(idReceta, "RECHAZADA");
            redirectAttrs.addFlashAttribute("mensajeExito", "Receta '" + receta.getNombre() + "' RECHAZADA.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al rechazar: " + e.getMessage());
        }
        return "redirect:/recetas/validar";
    }
    
    @GetMapping("/consultar")
    public String mostrarConsultarAprobada(Model model,
                                         @RequestParam(value="id", required=false) Long idRecetaSeleccionada) {

        // 1. Cargar la lista para el dropdown "Recetas Aprobadas"
        model.addAttribute("recetasAprobadas", recetaService.listarPorEstado("APROBADA"));

        // 2. Si el usuario seleccionó una receta (hizo clic en "Ver detalles")
        if (idRecetaSeleccionada != null) {
            Receta seleccionada = recetaService.obtenerPorId(idRecetaSeleccionada);
            model.addAttribute("recetaSeleccionada", seleccionada);
        } else {
            model.addAttribute("recetaSeleccionada", null);
        }
        
        return "recetas_consultar_aprobadas"; // Apunta al HTML
    }
    
    @GetMapping("/ajustar")
    public String mostrarAjustarReceta(Model model) {
        // 1. Cargar las recetas rechazadas para el dropdown
        model.addAttribute("recetasRechazadas", recetaService.listarPorEstado("RECHAZADA"));
        
        // 2. Cargar las que están en revisión para la tabla de historial
        model.addAttribute("recetasEnRevision", recetaService.listarPorEstado("EN_REVISION"));

        return "recetas_ajustar"; // Apunta al HTML "recetas_ajustar.html"
    }
    
    
    @PostMapping("/preparar")
    public String prepararReceta(@RequestParam("recetaId") Long idReceta,
                                 @RequestParam("cantidadPreparaciones") int cantidad, // Este es el nombre del input
                                 RedirectAttributes redirectAttrs) {
        
        try {
            recetaService.prepararReceta(idReceta, cantidad);
            redirectAttrs.addFlashAttribute("mensajeExito", "Receta preparada. Stock descontado del inventario.");
        
        } catch (StockInsuficienteException e) {
            // Error de negocio (Falta stock)
            redirectAttrs.addFlashAttribute("mensajeError", "Error al preparar: " + e.getMessage());
        } catch (Exception e) {
            // Otro error (inesperado)
            redirectAttrs.addFlashAttribute("mensajeError", "Error inesperado: " + e.getMessage());
        }
        
        // Regresamos a la misma página, manteniendo el ID seleccionado
        return "redirect:/recetas/consultar?id=" + idReceta;
    }
    
    //test
}