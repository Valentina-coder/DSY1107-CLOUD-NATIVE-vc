package com.example.Backend.controller;



import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

import com.example.Backend.model.Categoria;
import com.example.Backend.repository.CategoriaRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
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
    public ResponseEntity<Categoria> crearCategoria(@Valid @RequestBody Categoria categoria) {
        Categoria nueva = categoriaRepository.save(categoria);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }
}