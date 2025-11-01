package com.wayas.app.controller;

import com.wayas.app.model.Compra;
import com.wayas.app.model.Requerimiento;
import com.wayas.app.repository.IProveedorRepository;
import com.wayas.app.service.CompraService;
import com.wayas.app.service.InsumoService;
import com.wayas.app.service.RequerimientoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/compras") 
public class CompraController {

    @Autowired private CompraService compraService;
    @Autowired private RequerimientoService reqService;
    @Autowired private IProveedorRepository repoProv;
    @Autowired private InsumoService insumoService;
    
    @GetMapping("/registrar")
    public String mostrarRegistrarCompra(Model model) {
        
        List<Requerimiento> reqsComprables = reqService.listarPorEstado("PENDIENTE");
        reqsComprables.addAll(reqService.listarPorEstado("ENVIADO"));
        
        
        List<Long> idsReqYaComprados = compraService.listarTodas().stream()
            .filter(c -> c.getRequerimiento() != null && !"ANULADA".equalsIgnoreCase(c.getEstado()))
            .map(c -> c.getRequerimiento().getId())
            .collect(Collectors.toList());
        reqsComprables.removeIf(r -> idsReqYaComprados.contains(r.getId()));

        model.addAttribute("requerimientos", reqsComprables);
        model.addAttribute("proveedores", repoProv.findAll());
        model.addAttribute("todosLosInsumos", insumoService.listarTodos()); // Para el JS

        if (!model.containsAttribute("compra")) {
            model.addAttribute("compra", new Compra());
        }
        return "compra_registrar_compra";
    }

    @PostMapping("/registrar/guardar")
    public String guardarCompra(@ModelAttribute Compra compra,
                                @RequestParam Long idRequerimiento,
                                @RequestParam Integer idProveedor,
                                @RequestParam(required = false) List<Integer> insumoIds,   
                                @RequestParam(required = false) List<BigDecimal> cantidades,
                                RedirectAttributes redirectAttrs) {

        if (idRequerimiento == null || idProveedor == null || compra.getFechaCompra() == null || compra.getMontoTotal() == null) {
            redirectAttrs.addFlashAttribute("mensajeError", "Faltan datos obligatorios (Requerimiento, Proveedor, Fecha, Monto).");
            redirectAttrs.addFlashAttribute("compra", compra);
            redirectAttrs.addFlashAttribute("selectedReqId", idRequerimiento);
            redirectAttrs.addFlashAttribute("selectedProvId", idProveedor);
            return "redirect:/compras/registrar";
        }

        try {
            
             compraService.registrarCompra(idRequerimiento, compra.getFechaCompra(), idProveedor,
                                           compra.getMontoTotal(), compra.getNroFactura(), compra.getDetalleInsumosComprados(),
                                           insumoIds, cantidades); 
            redirectAttrs.addFlashAttribute("mensajeExito", "Compra registrada y stock actualizado correctamente.");
            return "redirect:/compras/historial"; 
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error inesperado al registrar compra: " + e.getMessage());
            e.printStackTrace();
        }

        redirectAttrs.addFlashAttribute("compra", compra);
        redirectAttrs.addFlashAttribute("selectedReqId", idRequerimiento);
        redirectAttrs.addFlashAttribute("selectedProvId", idProveedor);
        return "redirect:/compras/registrar";
    }

    @GetMapping("/historial")
    public String mostrarHistorialCompras(Model model) {
         model.addAttribute("compras", compraService.listarTodas());
         return "compra_historial"; 
    }

    @PostMapping("/anular/{id}")
    public String anularCompra(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            Compra anulada = compraService.anularCompra(id);
            if (anulada != null) {
                redirectAttrs.addFlashAttribute("mensajeExito", "Compra " + anulada.getCodigoCompra() + " anulada.");
            } else {
                redirectAttrs.addFlashAttribute("mensajeError", "No se pudo anular la compra (ID: " + id + ").");
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al anular la compra: " + e.getMessage());
        }
        return "redirect:/compras/historial";
    }
    
    //test
}
