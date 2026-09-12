package com.example.Backend.controller;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Backend.model.Producto;
import com.example.Backend.repository.ProductoRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductoController {

    private final ProductoRepository productoRepository;

    public ProductoController(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    // Lectura protegible: requiere estar autenticado (200 / 401)
    @GetMapping("/productos")
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    // Escritura protegible: requiere rol o scope de administrador (201 / 403)
    @PostMapping("/admin/productos")
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody Producto producto) {
        Producto nuevo = productoRepository.save(producto);
        return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
    }
}