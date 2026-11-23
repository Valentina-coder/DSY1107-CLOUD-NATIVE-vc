package com.example.Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private Double precio;

    @PositiveOrZero
    private Integer stock = 0;

    @PositiveOrZero
    @Column(name = "stock_minimo")
    private Integer stockMinimo = 5;

    @Transient
    @JsonIgnore
    private boolean stockMinimoSpecified;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    public Producto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }
    public Integer getStock() { return stock == null ? 0 : stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getStockMinimo() { return stockMinimo == null ? 5 : stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
        this.stockMinimoSpecified = true;
    }
    @JsonIgnore
    public boolean isStockMinimoSpecified() { return stockMinimoSpecified; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}