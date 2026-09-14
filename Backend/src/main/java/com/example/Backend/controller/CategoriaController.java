package com.example.Backend.controller;



import org.springframework.web.bind.annotation.*;

import com.example.Backend.model.Categoria;
import com.example.Backend.services.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/public/categorias")
    public List<Categoria> obtenerCategorias() {
        return categoriaService.obtenerTodas();
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<Categoria> crearCategoria(@Valid @RequestBody Categoria categoria) {
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(categoriaService.crear(categoria));
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<Categoria> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody Categoria categoriaActualizada) {
        Optional<Categoria> categoriaExistente = categoriaService.buscarPorId(id);

        if (categoriaExistente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (categoriaActualizada.getNombre() == null || categoriaActualizada.getNombre().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Categoria categoria = categoriaExistente.get();
        categoria.setNombre(categoriaActualizada.getNombre());
        return ResponseEntity.ok(categoriaService.actualizar(categoria));
    }

    @DeleteMapping("/categorias/{id}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id) {
        if (!categoriaService.existe(id)) {
            return ResponseEntity.notFound().build();
        }

        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}