package com.example.Backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Backend.model.Categoria;
import com.example.Backend.model.Producto;
import com.example.Backend.repository.CategoriaRepository;
import com.example.Backend.repository.ProductoRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoController(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping("/productos")
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    @PostMapping("/admin/productos")
    public ResponseEntity<?> crearProducto(@RequestBody Producto producto) {
        try {
            // Buscamos la categoría real en la base de datos antes de guardar
            if (producto.getCategoria() != null && producto.getCategoria().getId() != null) {
                Categoria cat = categoriaRepository.findById(producto.getCategoria().getId())
                        .orElseThrow(() -> new RuntimeException("La categoría especificada no existe en la BD"));
                producto.setCategoria(cat);
            }

            Producto nuevo = productoRepository.save(producto);
            return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace(); // Imprime la falla exacta en la consola de Java
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar producto: " + e.getMessage());
        }
    }

    @PutMapping("/admin/productos/{id}")
    public ResponseEntity<?> actualizarProducto(@PathVariable Long id, @RequestBody Producto detalles) {
        try {
            return productoRepository.findById(id).map(prod -> {
                prod.setNombre(detalles.getNombre());
                prod.setPrecio(detalles.getPrecio());

                if (detalles.getCategoria() != null && detalles.getCategoria().getId() != null) {
                    Categoria cat = categoriaRepository.findById(detalles.getCategoria().getId()).orElse(null);
                    if (cat != null) prod.setCategoria(cat);
                }

                Producto actualizado = productoRepository.save(prod);
                return ResponseEntity.ok(actualizado);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/productos/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        try {
            if (!productoRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            productoRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar: " + e.getMessage());
        }
    }
}