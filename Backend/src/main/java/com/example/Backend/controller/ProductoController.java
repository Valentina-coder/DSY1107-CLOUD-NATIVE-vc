package com.example.Backend.controller;

import com.example.Backend.model.Producto;
import com.example.Backend.services.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // ✅ LECTURA PÚBLICA (sin token)
    @GetMapping("/public/productos")
    public ResponseEntity<List<Producto>> getProductosPublico() {
        List<Producto> productos = productoService.obtenerTodos();
        return ResponseEntity.ok(productos);
    }

    // ✅ LECTURA PROTEGIDA (requiere token válido)
    @GetMapping("/productos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Producto>> getProductos() {
        List<Producto> productos = productoService.obtenerTodos();
        return ResponseEntity.ok(productos);
    }

    // ✅ LECTURA PROTEGIDA POR ID (requiere token válido)
    @GetMapping("/productos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Producto> getProductoById(@PathVariable Long id) {
        Optional<Producto> producto = productoService.buscarPorId(id);
        if (producto.isPresent()) {
            return ResponseEntity.ok(producto.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ CREACIÓN (requiere scope producto:write)
    @PostMapping("/productos")
    @PreAuthorize("hasAnyAuthority('SCOPE_producto:write', 'SCOPE_admin')")
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody Producto producto) {
        // Validación básica
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Producto productoGuardado = productoService.crear(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(productoGuardado);
    }

    // ✅ ACTUALIZACIÓN (requiere scope producto:write)
    @PutMapping("/productos/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_producto:write', 'SCOPE_admin')")
    public ResponseEntity<Producto> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody Producto productoActualizado) {
        
        Optional<Producto> productoExistente = productoService.buscarPorId(id);
        
        if (productoExistente.isPresent()) {
            Producto producto = productoExistente.get();
            
            // Actualizar campos
            if (productoActualizado.getNombre() != null) {
                producto.setNombre(productoActualizado.getNombre());
            }
            if (productoActualizado.getPrecio() != null) {
                producto.setPrecio(productoActualizado.getPrecio());
            }
            if (productoActualizado.getCategoria() != null) {
                producto.setCategoria(productoActualizado.getCategoria());
            }
            
            Producto productoGuardado = productoService.actualizar(producto);
            return ResponseEntity.ok(productoGuardado);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ ELIMINACIÓN (requiere scope producto:write o admin)
    @DeleteMapping("/productos/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_producto:write', 'SCOPE_admin')")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        if (productoService.existe(id)) {
            productoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}