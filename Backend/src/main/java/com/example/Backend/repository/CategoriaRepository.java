package com.example.Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Backend.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}