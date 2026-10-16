package com.example.Backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.Backend.model.Categoria;
import com.example.Backend.model.Producto;
import com.example.Backend.repository.CategoriaRepository;
import com.example.Backend.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:backend-tests;DB_CLOSE_DELAY=-1",
		"spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private ProductoRepository productoRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void rabbitManagementRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/admin/rabbitmq/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"test.queue","durable":true,"exclusive":false,"autoDelete":false}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rabbitManagementRequiresAdministratorPermission() throws Exception {
		mockMvc.perform(post("/api/admin/rabbitmq/queues")
				.with(jwt().authorities(new SimpleGrantedAuthority("recurso:write")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"test.queue","durable":true,"exclusive":false,"autoDelete":false}
						"""))
				.andExpect(status().isForbidden());
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
