package com.wayas.app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.wayas.app.model.Insumo;

@Entity
@Table(name = "tb_compra")
public class Compra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, length = 20) private String codigoCompra; 

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_requerimiento", unique = true) 
    private Requerimiento requerimiento;

    private LocalDate fechaCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prov") 
    private Proveedor proveedor;

    @Column(precision = 10, scale = 2) private BigDecimal montoTotal;
    private String nroFactura;
    @Column(columnDefinition = "TEXT") private String detalleInsumosComprados; 
    private String estado; 
    
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCompra> detalles = new ArrayList<>();

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCodigoCompra() {
		return codigoCompra;
	}
	public void setCodigoCompra(String codigoCompra) {
		this.codigoCompra = codigoCompra;
	}
	public Requerimiento getRequerimiento() {
		return requerimiento;
	}
	public void setRequerimiento(Requerimiento requerimiento) {
		this.requerimiento = requerimiento;
	}
	public LocalDate getFechaCompra() {
		return fechaCompra;
	}
	public void setFechaCompra(LocalDate fechaCompra) {
		this.fechaCompra = fechaCompra;
	}
	public Proveedor getProveedor() {
		return proveedor;
	}
	public void setProveedor(Proveedor proveedor) {
		this.proveedor = proveedor;
	}
	public BigDecimal getMontoTotal() {
		return montoTotal;
	}
	public void setMontoTotal(BigDecimal montoTotal) {
		this.montoTotal = montoTotal;
	}
	public String getNroFactura() {
		return nroFactura;
	}
	public void setNroFactura(String nroFactura) {
		this.nroFactura = nroFactura;
	}
	public String getDetalleInsumosComprados() {
		return detalleInsumosComprados;
	}
	public void setDetalleInsumosComprados(String detalleInsumosComprados) {
		this.detalleInsumosComprados = detalleInsumosComprados;
	}
	public String getEstado() {
		return estado;
	}
	public void setEstado(String estado) {
		this.estado = estado;
	}
    
    public List<DetalleCompra> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompra> detalles) {
        this.detalles = detalles;
    }
    public void agregarDetalle(Insumo insumo, BigDecimal cantidad) {
        DetalleCompra detalle = new DetalleCompra();
        detalle.setCompra(this);
        detalle.setInsumo(insumo);
        detalle.setCantidad(cantidad);
        this.detalles.add(detalle);
    }
}
