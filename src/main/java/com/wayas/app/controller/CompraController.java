package com.wayas.app.controller;
//hola
import com.wayas.app.model.Compra;
import com.wayas.app.model.Insumo;
import com.wayas.app.model.Proveedor;
import com.wayas.app.model.Requerimiento;
import com.wayas.app.model.RequerimientoDetalle;
import com.wayas.app.repository.IProveedorRepository;
import com.wayas.app.service.CompraService;
import com.wayas.app.service.InsumoService;
import com.wayas.app.service.RequerimientoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/compra")
public class CompraController {

    @Autowired private InsumoService insumoService;
    @Autowired private RequerimientoService reqService;
    @Autowired private CompraService compraService;
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

    @GetMapping("/requerimientos/generar")
    public String mostrarGenerarRequerimiento(Model model, HttpSession session) {
        List<Insumo> bajos = insumoService.listarInsumosBajoMinimo();
        model.addAttribute("insumosBajoStock", bajos);
        model.addAttribute("todosLosInsumos", insumoService.listarTodos());
        model.addAttribute("proveedores", repoProv.findAll());

        Requerimiento reqActual = obtenerRequerimientoDeSesion(session);
        model.addAttribute("requerimientoActual", reqActual);

        model.addAttribute("requerimientosHistorial", reqService.listarTodos());

        return "compra_lista_requerimientos";
    }

    @PostMapping("/requerimientos/agregarItem")
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

            redirectAttrs.addFlashAttribute("mensajeExito", "Insumo '" + insumo.getDescripcion() + "' agregado a la lista.");

        } else {
            redirectAttrs.addFlashAttribute("mensajeError", "Insumo con ID " + insumoId + " no encontrado.");
        }

        return "redirect:/compra/requerimientos/generar";
    }

    @PostMapping("/requerimientos/eliminarItem")
    public String eliminarItemTemporal(@RequestParam int index,
                                        HttpSession session,
                                        RedirectAttributes redirectAttrs) {
        Requerimiento reqActual = obtenerRequerimientoDeSesion(session);
        try {
            if (index >= 0 && index < reqActual.getDetalles().size()) {
                RequerimientoDetalle eliminado = reqActual.getDetalles().remove(index);
                guardarRequerimientoEnSesion(session, reqActual);
                redirectAttrs.addFlashAttribute("mensajeExito", "Ítem '" + eliminado.getInsumo().getDescripcion() + "' eliminado de la lista.");
            } else {
                 redirectAttrs.addFlashAttribute("mensajeError", "Índice inválido para eliminar.");
            }
        } catch (Exception e) {
             redirectAttrs.addFlashAttribute("mensajeError", "Error al eliminar ítem: " + e.getMessage());
        }
        return "redirect:/compra/requerimientos/generar";
    }


    @PostMapping("/requerimientos/guardar")
    public String guardarRequerimiento(HttpSession session,
                                       RedirectAttributes redirectAttrs) {

        Requerimiento reqParaGuardar = obtenerRequerimientoDeSesion(session);

        if (reqParaGuardar.getDetalles() == null || reqParaGuardar.getDetalles().isEmpty()) {
            redirectAttrs.addFlashAttribute("mensajeError", "No se puede generar un requerimiento vacío. Agregue al menos un insumo.");
            return "redirect:/compra/requerimientos/generar";
        }

        try {
            List<Long> insumoIds = reqParaGuardar.getDetalles().stream()
                                     .map(det -> det.getInsumo().getIdInsumo().longValue())
                                     .collect(Collectors.toList());
            List<BigDecimal> cantidades = reqParaGuardar.getDetalles().stream()
                                          .map(RequerimientoDetalle::getCantidad)
                                          .collect(Collectors.toList());
            List<Integer> proveedorIds = reqParaGuardar.getDetalles().stream()
                                         .map(det -> (det.getProveedor() != null) ? det.getProveedor().getIdProv() : null)
                                         .collect(Collectors.toList());

            reqService.crearRequerimiento(insumoIds, cantidades, proveedorIds);

            limpiarRequerimientoDeSesion(session);

            redirectAttrs.addFlashAttribute("mensajeExito", "Requerimiento generado correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al generar requerimiento: " + e.getMessage());
            e.printStackTrace();
             return "redirect:/compra/requerimientos/generar";
        }
        return "redirect:/compra/requerimientos/generar"; // Redirige de vuelta a generar
    }

     @PostMapping("/requerimientos/eliminar/{id}")
     public String eliminarRequerimiento(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        Requerimiento req = reqService.obtenerPorId(id);
        if (req == null) {
             redirectAttrs.addFlashAttribute("mensajeError", "Requerimiento no encontrado (ID: " + id + ")");
             return "redirect:/compra/requerimientos/generar"; // Ajustado para redirigir a generar
        }
        try {
             reqService.eliminar(id);
             redirectAttrs.addFlashAttribute("mensajeExito", "Requerimiento " + req.getCodigoRequerimiento() + " eliminado.");
        } catch (Exception e) {
             redirectAttrs.addFlashAttribute("mensajeError", "Error al eliminar requerimiento " + req.getCodigoRequerimiento() + ": " + e.getMessage());
        }
        return "redirect:/compra/requerimientos/generar"; // Ajustado para redirigir a generar
     }

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
        model.addAttribute("todosLosInsumos", insumoService.listarTodos());

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
            return "redirect:/compra/registrar";
        }

        try {
             compraService.registrarCompra(idRequerimiento, compra.getFechaCompra(), idProveedor,
                                           compra.getMontoTotal(), compra.getNroFactura(), compra.getDetalleInsumosComprados());
            redirectAttrs.addFlashAttribute("mensajeExito", "Compra registrada correctamente.");
            return "redirect:/compra/historial";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error inesperado al registrar compra: " + e.getMessage());
            e.printStackTrace();
        }

        redirectAttrs.addFlashAttribute("compra", compra);
        redirectAttrs.addFlashAttribute("selectedReqId", idRequerimiento);
        redirectAttrs.addFlashAttribute("selectedProvId", idProveedor);
        return "redirect:/compra/registrar";
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
                redirectAttrs.addFlashAttribute("mensajeExito", "Compra " + anulada.getCodigoCompra() + " anulada correctamente.");
            } else {
                redirectAttrs.addFlashAttribute("mensajeError", "No se pudo anular la compra con ID " + id + ". Puede que no exista o ya estuviera anulada.");
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al intentar anular la compra ID " + id + ": " + e.getMessage());
        }
        return "redirect:/compra/historial";
    }


    @GetMapping("/reporte/insumos")
    public String reporteInsumos(
            @RequestParam(required = false) String nombreInsumo,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String estadoStock,
            Model model) {

        List<Insumo> insumosFiltrados = insumoService.listarTodos();

        if (nombreInsumo != null && !nombreInsumo.trim().isEmpty()) {
            insumosFiltrados = insumosFiltrados.stream()
                .filter(ins -> ins.getDescripcion() != null && ins.getDescripcion().toLowerCase().contains(nombreInsumo.toLowerCase()))
                .collect(Collectors.toList());
        }
        if (categoria != null && !categoria.trim().isEmpty()) {
            insumosFiltrados = insumosFiltrados.stream()
                .filter(ins -> categoria.equalsIgnoreCase(ins.getCategoria()))
                .collect(Collectors.toList());
        }
        if (proveedor != null && !proveedor.trim().isEmpty()) {
            insumosFiltrados = insumosFiltrados.stream()
                .filter(ins -> ins.getProveedor() != null && ins.getProveedor().getRazonSocial() != null &&
                               ins.getProveedor().getRazonSocial().toLowerCase().contains(proveedor.toLowerCase()))
                .collect(Collectors.toList());
        }
        if (estadoStock != null && !estadoStock.trim().isEmpty()) {
            insumosFiltrados = insumosFiltrados.stream()
                .filter(ins -> {
                    if (ins.getStockActual() == null || ins.getStockMinimo() == null || !"activo".equalsIgnoreCase(ins.getEstado())) return false;
                    BigDecimal stock = ins.getStockActual();
                    BigDecimal minimo = ins.getStockMinimo();
                    BigDecimal moderadoLimite = minimo.multiply(new BigDecimal("1.5"));
                    if ("Bajo".equalsIgnoreCase(estadoStock)) return stock.compareTo(minimo) <= 0;
                    else if ("Moderado".equalsIgnoreCase(estadoStock)) return stock.compareTo(minimo) > 0 && stock.compareTo(moderadoLimite) <= 0;
                    else if ("Suficiente".equalsIgnoreCase(estadoStock)) return stock.compareTo(moderadoLimite) > 0;
                    return false;
                })
                .collect(Collectors.toList());
        }

        model.addAttribute("insumos", insumosFiltrados);
        model.addAttribute("nombreInsumo", nombreInsumo);
        model.addAttribute("categoria", categoria);
        model.addAttribute("proveedor", proveedor);
        model.addAttribute("estadoStock", estadoStock);

        return "compra_reporte_insumos";
    }
}
