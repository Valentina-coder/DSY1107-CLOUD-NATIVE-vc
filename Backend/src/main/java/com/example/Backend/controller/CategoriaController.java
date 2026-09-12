package com.example.Backend.controller;



import org.springframework.web.bind.annotation.*;

import com.example.Backend.model.Categoria;
import com.example.Backend.repository.CategoriaRepository;

import java.util.List;

@RestController
@RequestMapping("/api/public/categorias")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    public List<Categoria> obtenerCategorias() {
        return categoriaRepository.findAll();
    }
}