package com.wayas.app.model;
import com.fasterxml.jackson.annotation.JsonIgnore; 
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate; 
import java.util.Objects;

@Entity
@Table(name = "tb_insumo")
public class Insumo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idInsumo;
    @Column(nullable = false, length = 150) private String descripcion;
    private String categoria; 
    @Column(length = 20) private String unidadMedida;
    @Column(precision = 10, scale = 3) private BigDecimal stockActual = BigDecimal.ZERO;
    @Column(precision = 10, scale = 3) private BigDecimal stockMinimo = BigDecimal.ZERO; 
    @Column(precision = 10, scale = 2) private BigDecimal precioCompra = BigDecimal.ZERO;
    private LocalDate fechaVencimiento; 
    private String estado = "activo"; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prov")
    private Proveedor proveedor;

	public Integer getIdInsumo() {
		return idInsumo;
	}

	public void setIdInsumo(Integer idInsumo) {
		this.idInsumo = idInsumo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public String getUnidadMedida() {
		return unidadMedida;
	}

	public void setUnidadMedida(String unidadMedida) {
		this.unidadMedida = unidadMedida;
	}

	public BigDecimal getStockActual() {
		return stockActual;
	}

	public void setStockActual(BigDecimal stockActual) {
		this.stockActual = stockActual;
	}

	public BigDecimal getStockMinimo() {
		return stockMinimo;
	}

	public void setStockMinimo(BigDecimal stockMinimo) {
		this.stockMinimo = stockMinimo;
	}

	public BigDecimal getPrecioCompra() {
		return precioCompra;
	}

	public void setPrecioCompra(BigDecimal precioCompra) {
		this.precioCompra = precioCompra;
	}

	public LocalDate getFechaVencimiento() {
		return fechaVencimiento;
	}

	public void setFechaVencimiento(LocalDate fechaVencimiento) {
		this.fechaVencimiento = fechaVencimiento;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	@JsonIgnore 
	public Proveedor getProveedor() {
		return proveedor;
	}

	public void setProveedor(Proveedor proveedor) {
		this.proveedor = proveedor;
	}

	@Override
	public int hashCode() {
		return Objects.hash(categoria, descripcion, estado, fechaVencimiento, idInsumo, precioCompra, proveedor,
				stockActual, stockMinimo, unidadMedida);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Insumo other = (Insumo) obj;
		return Objects.equals(categoria, other.categoria) && Objects.equals(descripcion, other.descripcion)
				&& Objects.equals(estado, other.estado) && Objects.equals(fechaVencimiento, other.fechaVencimiento)
				&& Objects.equals(idInsumo, other.idInsumo) && Objects.equals(precioCompra, other.precioCompra)
				&& Objects.equals(proveedor, other.proveedor) && Objects.equals(stockActual, other.stockActual)
				&& Objects.equals(stockMinimo, other.stockMinimo) && Objects.equals(unidadMedida, other.unidadMedida);
	} 
}
