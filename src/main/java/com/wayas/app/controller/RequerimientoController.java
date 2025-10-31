package com.wayas.app.controller;

import com.wayas.app.model.Insumo;
import com.wayas.app.model.Proveedor;
import com.wayas.app.model.Requerimiento;
import com.wayas.app.model.RequerimientoDetalle;
import com.wayas.app.repository.IProveedorRepository;
import com.wayas.app.service.InsumoService; 
import com.wayas.app.service.RequerimientoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/requerimientos")
public class RequerimientoController {

    @Autowired private InsumoService insumoService;
    @Autowired private RequerimientoService reqService;
    @Autowired private IProveedorRepository repoProv;

    private Requerimiento obtenerRequerimientoDeSesion(HttpSession session) {
        Requerimiento req = (Requerimiento) session.getAttribute("requerimientoEnProceso");
        if (req == null) {
            req = new Requerimiento();
            req.setDetalles(new ArrayList<>());
            session.setAttribute("requerimientoEnProceso", req);
        }
        if (req.getDetalles() == null) {
            req.setDetalles(new ArrayList<>());
        }
        return req;
    }
    private void guardarRequerimientoEnSesion(HttpSession session, Requerimiento req) {
        session.setAttribute("requerimientoEnProceso", req);
    }
    private void limpiarRequerimientoDeSesion(HttpSession session) {
        session.removeAttribute("requerimientoEnProceso");
    }
    @GetMapping("/generar")
    public String mostrarGenerarRequerimiento(Model model, HttpSession session) {
        List<Insumo> bajos = insumoService.listarInsumosBajoMinimo();
        model.addAttribute("insumosBajoStock", bajos);
        model.addAttribute("todosLosInsumos", insumoService.listarTodos());
        model.addAttribute("proveedores", repoProv.findAll());
        model.addAttribute("requerimientoActual", obtenerRequerimientoDeSesion(session));
        model.addAttribute("requerimientosHistorial", reqService.listarTodos());
        return "compra_lista_requerimientos";
    }

    @PostMapping("/agregarItem")
    public String agregarItemARequerimiento(@RequestParam Integer insumoId,
                                            @RequestParam(required = false) Integer proveedorId,
                                            @RequestParam(defaultValue = "1") BigDecimal cantidad,
                                            HttpSession session,
                                            RedirectAttributes redirectAttrs) {
        Requerimiento reqActual = obtenerRequerimientoDeSesion(session);
        Optional<Insumo> insumoOpt = Optional.ofNullable(insumoService.obtenerPorId(insumoId));
        Optional<Proveedor> provOpt = (proveedorId != null) ? Optional.ofNullable(repoProv.findById(proveedorId).orElse(null)) : Optional.empty();

        if (insumoOpt.isPresent()) {
            Insumo insumo = insumoOpt.get();
            RequerimientoDetalle detalle = new RequerimientoDetalle();
            detalle.setInsumo(insumo);
            detalle.setCantidad(cantidad.compareTo(BigDecimal.ZERO) > 0 ? cantidad : BigDecimal.ONE);
            provOpt.ifPresent(detalle::setProveedor);
            detalle.setRequerimiento(reqActual);
            reqActual.getDetalles().add(detalle);
            guardarRequerimientoEnSesion(session, reqActual);
            redirectAttrs.addFlashAttribute("mensajeExito", "Insumo '" + insumo.getDescripcion() + "' agregado.");
        } else {
            redirectAttrs.addFlashAttribute("mensajeError", "Insumo no encontrado.");
        }
        return "redirect:/requerimientos/generar";
    }

    @PostMapping("/eliminarItem")
    public String eliminarItemTemporal(@RequestParam int index,
                                        HttpSession session,
                                        RedirectAttributes redirectAttrs) {
        Requerimiento reqActual = obtenerRequerimientoDeSesion(session);
        try {
            if (index >= 0 && index < reqActual.getDetalles().size()) {
                RequerimientoDetalle eliminado = reqActual.getDetalles().remove(index);
                guardarRequerimientoEnSesion(session, reqActual);
                redirectAttrs.addFlashAttribute("mensajeExito", "Ítem '" + eliminado.getInsumo().getDescripcion() + "' eliminado.");
            } else {
                 redirectAttrs.addFlashAttribute("mensajeError", "Índice inválido.");
            }
        } catch (Exception e) {
             redirectAttrs.addFlashAttribute("mensajeError", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/requerimientos/generar";
    }

    @PostMapping("/guardar")
    public String guardarRequerimiento(HttpSession session,
                                       RedirectAttributes redirectAttrs) {
        Requerimiento reqParaGuardar = obtenerRequerimientoDeSesion(session);
        if (reqParaGuardar.getDetalles() == null || reqParaGuardar.getDetalles().isEmpty()) {
            redirectAttrs.addFlashAttribute("mensajeError", "No se puede generar un requerimiento vacío.");
            return "redirect:/requerimientos/generar";
        }
        try {
            List<Long> insumoIds = reqParaGuardar.getDetalles().stream().map(det -> det.getInsumo().getIdInsumo().longValue()).collect(Collectors.toList());
            List<BigDecimal> cantidades = reqParaGuardar.getDetalles().stream().map(RequerimientoDetalle::getCantidad).collect(Collectors.toList());
            List<Integer> proveedorIds = reqParaGuardar.getDetalles().stream().map(det -> (det.getProveedor() != null) ? det.getProveedor().getIdProv() : null).collect(Collectors.toList());
            
            reqService.crearRequerimiento(insumoIds, cantidades, proveedorIds);
            limpiarRequerimientoDeSesion(session);
            redirectAttrs.addFlashAttribute("mensajeExito", "Requerimiento generado correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al generar requerimiento: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/requerimientos/generar";
    }

     @PostMapping("/eliminar/{id}")
     public String eliminarRequerimiento(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        Requerimiento req = reqService.obtenerPorId(id);
        if (req == null) {
             redirectAttrs.addFlashAttribute("mensajeError", "Requerimiento no encontrado (ID: " + id + ")");
             return "redirect:/requerimientos/generar";
        }
        try {
             reqService.eliminar(id);
             redirectAttrs.addFlashAttribute("mensajeExito", "Requerimiento " + req.getCodigoRequerimiento() + " eliminado.");
        } catch (Exception e) {
             redirectAttrs.addFlashAttribute("mensajeError", "Error al eliminar requerimiento: " + e.getMessage());
        }
        return "redirect:/requerimientos/generar";
     }
}
