package com.example.Backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import com.example.Backend.model.Categoria;
import com.example.Backend.model.Producto;
import com.example.Backend.repository.CategoriaRepository;
import com.example.Backend.repository.ProductoRepository;
import com.example.Backend.messaging.ProductoEventPublisher;

import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoEventPublisher productoEventPublisher;

    public ProductoController(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            ProductoEventPublisher productoEventPublisher) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoEventPublisher = productoEventPublisher;
    }

    @GetMapping("/productos")
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    @PostMapping("/admin/productos")
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody Producto producto) {
        if (producto.getCategoria() != null && producto.getCategoria().getId() != null) {
            Categoria categoria = categoriaRepository.findById(producto.getCategoria().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "La categoría especificada no existe"));
            producto.setCategoria(categoria);
        }

        producto.setStockMinimo(producto.getStockMinimo());
        Producto nuevo = productoRepository.save(producto);
        productoEventPublisher.publicarProductoCreado(nuevo);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @PutMapping("/admin/productos/{id}")
    public ResponseEntity<Producto> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody Producto detalles) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        producto.setNombre(detalles.getNombre());
        producto.setPrecio(detalles.getPrecio());
        if (detalles.isStockMinimoSpecified()) {
            producto.setStockMinimo(detalles.getStockMinimo());
        }

        if (detalles.getCategoria() != null && detalles.getCategoria().getId() != null) {
            Categoria categoria = categoriaRepository.findById(detalles.getCategoria().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "La categoría especificada no existe"));
            producto.setCategoria(categoria);
        }

        return ResponseEntity.ok(productoRepository.save(producto));
    }

    @DeleteMapping("/admin/productos/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
        }
        productoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}