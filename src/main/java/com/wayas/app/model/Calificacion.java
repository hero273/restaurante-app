package com.wayas.app.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tb_calificacion")
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prov")
    private Proveedor proveedor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra", unique = true) // Una calificación por compra
    private Compra compra;

    private LocalDate fecha;
    
    // Usaremos valores numéricos para Bueno(3), Regular(2), Malo(1)
    private int calidad; 
    private int peso;
    private int puntualidad;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // --- Getters y Setters ---
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public int getCalidad() {
        return calidad;
    }

    public void setCalidad(int calidad) {
        this.calidad = calidad;
    }

    public int getPeso() {
        return peso;
    }

    public void setPeso(int peso) {
        this.peso = peso;
    }

    public int getPuntualidad() {
        return puntualidad;
    }

    public void setPuntualidad(int puntualidad) {
        this.puntualidad = puntualidad;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    // Helpers para convertir de 3,2,1 a texto
    public String getCalidadTexto() {
        if (calidad == 3) return "Bueno";
        if (calidad == 2) return "Regular";
        if (calidad == 1) return "Malo";
        return "-";
    }
    
    public String getPesoTexto() {
        if (peso == 3) return "Bueno";
        if (peso == 2) return "Regular";
        if (peso == 1) return "Malo";
        return "-";
    }

    public String getPuntualidadTexto() {
        if (puntualidad == 3) return "Bueno";
        if (puntualidad == 2) return "Regular";
        if (puntualidad == 1) return "Malo";
        return "-";
    }
}
