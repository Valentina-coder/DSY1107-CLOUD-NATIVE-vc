package com.example.Backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.Backend.model.Categoria;
import com.example.Backend.model.Producto;
import com.example.Backend.repository.CategoriaRepository;
import com.example.Backend.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private ProductoRepository productoRepository;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void savesCategoryAndProduct() {
		Categoria categoria = categoriaRepository.findAll().stream()
				.filter(c -> "Categoría de prueba".equals(c.getNombre()))
				.findFirst()
				.orElseGet(() -> categoriaRepository.save(new Categoria("Categoría de prueba")));

		Producto producto = new Producto();
		producto.setNombre("Producto de prueba");
		producto.setPrecio(12.5);
		producto.setCategoria(categoria);

		Producto guardado = productoRepository.save(producto);

		assertThat(guardado.getId()).isNotNull();
		assertThat(productoRepository.findById(guardado.getId()))
				.isPresent()
				.get()
				.extracting(Producto::getNombre)
				.isEqualTo("Producto de prueba");
		assertThat(categoriaRepository.findAll())
				.extracting(Categoria::getNombre)
				.contains("Categoría de prueba");
	}
}
