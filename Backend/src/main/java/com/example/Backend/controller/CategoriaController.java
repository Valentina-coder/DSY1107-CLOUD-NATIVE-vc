package com.example.Backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Backend.model.Categoria;
import com.example.Backend.repository.CategoriaRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping("/public/categorias")
    public List<Categoria> obtenerCategorias() {
        return categoriaRepository.findAll();
    }

    @PostMapping("/admin/categorias")
    public ResponseEntity<?> crearCategoria(@RequestBody Categoria categoria) {
        try {
            Categoria nueva = categoriaRepository.save(categoria);
            return new ResponseEntity<>(nueva, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace(); // Imprime la falla exacta en la consola de Java
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar categoría: " + e.getMessage());
        }
    }
}