package com.example.Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Backend.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}