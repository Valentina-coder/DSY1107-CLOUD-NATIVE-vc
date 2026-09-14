package com.example.Backend.services;

import com.example.Backend.model.Categoria;
import com.example.Backend.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

	private final CategoriaRepository categoriaRepository;

	public CategoriaService(CategoriaRepository categoriaRepository) {
		this.categoriaRepository = categoriaRepository;
	}

	public List<Categoria> obtenerTodas() {
		return categoriaRepository.findAll();
	}

	public Categoria crear(Categoria categoria) {
		return categoriaRepository.save(categoria);
	}

	public Optional<Categoria> buscarPorId(Long id) {
		return categoriaRepository.findById(id);
	}

	public Categoria actualizar(Categoria categoria) {
		return categoriaRepository.save(categoria);
	}

	public boolean existe(Long id) {
		return categoriaRepository.existsById(id);
	}

	public void eliminar(Long id) {
		categoriaRepository.deleteById(id);
	}
}
