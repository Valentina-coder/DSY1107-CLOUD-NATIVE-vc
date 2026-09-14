package com.example.Backend.services;

import com.example.Backend.model.Producto;
import com.example.Backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

	private final ProductoRepository productoRepository;

	public ProductoService(ProductoRepository productoRepository) {
		this.productoRepository = productoRepository;
	}

	public List<Producto> obtenerTodos() {
		return productoRepository.findAll();
	}

	public Optional<Producto> buscarPorId(Long id) {
		return productoRepository.findById(id);
	}

	public Producto crear(Producto producto) {
		return productoRepository.save(producto);
	}

	public Producto actualizar(Producto producto) {
		return productoRepository.save(producto);
	}

	public boolean existe(Long id) {
		return productoRepository.existsById(id);
	}

	public void eliminar(Long id) {
		productoRepository.deleteById(id);
	}
}
