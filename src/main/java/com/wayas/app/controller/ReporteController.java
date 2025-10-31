package com.wayas.app.controller;

import com.wayas.app.model.Insumo;
import com.wayas.app.service.InsumoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reporte")
public class ReporteController {

    @Autowired
    private InsumoService insumoService;
    
    @GetMapping("/insumos")
    public String reporteInsumos(
            @RequestParam(required = false) String nombreInsumo,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String estadoStock,
            Model model) {
        
        List<Insumo> insumosFiltrados = insumoService.listarTodos();

        if (nombreInsumo != null && !nombreInsumo.trim().isEmpty()) {
            insumosFiltrados = insumosFiltrados.stream()
                    .filter(ins -> ins.getDescripcion() != null &&
                            ins.getDescripcion().toLowerCase().contains(nombreInsumo.toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (categoria != null && !categoria.trim().isEmpty()) {
            insumosFiltrados = insumosFiltrados.stream()
                    .filter(ins -> categoria.equalsIgnoreCase(ins.getCategoria()))
                    .collect(Collectors.toList());
        }
        if (proveedor != null && !proveedor.trim().isEmpty()) {
            insumosFiltrados = insumosFiltrados.stream()
                    .filter(ins -> ins.getProveedor() != null &&
                            ins.getProveedor().getRazonSocial() != null &&
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
