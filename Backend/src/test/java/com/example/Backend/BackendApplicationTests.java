package com.example.Backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.Backend.model.Categoria;
import com.example.Backend.model.NotificationRequest;
import com.example.Backend.model.Producto;
import com.example.Backend.repository.CategoriaRepository;
import com.example.Backend.repository.NotificationRequestRepository;
import com.example.Backend.repository.ProductoRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:backend-tests;DB_CLOSE_DELAY=-1",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.kafka.admin.auto-create=false",
		"spring.kafka.listener.auto-startup=false"
})
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private NotificationRequestRepository notificationRequestRepository;

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
	void kafkaManagementRequiresAdministratorPermission() throws Exception {
		mockMvc.perform(post("/api/admin/kafka/topics")
				.with(jwt().authorities(new SimpleGrantedAuthority("recurso:write")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"test-topic","partitions":1,"replicationFactor":1}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void kafkaManagementRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/admin/kafka/topics")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"test-topic","partitions":1,"replicationFactor":1}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void eventPublishingRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/admin/events/inventory")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productoId":42,"stock":17}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void eventPublishingRejectsInvalidPayloads() throws Exception {
		mockMvc.perform(post("/api/admin/events/inventory")
				.with(jwt().authorities(new SimpleGrantedAuthority("recurso:write")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productoId":0,"stock":-1}
						"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/admin/events/notifications")
				.with(jwt().authorities(new SimpleGrantedAuthority("recurso:write")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":" "}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void productCreationRejectsNegativeMinimumStock() throws Exception {
		mockMvc.perform(post("/api/admin/productos")
				.with(jwt().authorities(new SimpleGrantedAuthority("recurso:write")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":"Producto inválido","precio":100,"stockMinimo":-1}
						"""))
				.andExpect(status().isBadRequest());
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
		assertThat(guardado.getStock()).isZero();
		assertThat(guardado.getStockMinimo()).isEqualTo(5);
		assertThat(productoRepository.findById(guardado.getId()))
				.isPresent()
				.get()
				.extracting(Producto::getNombre)
				.isEqualTo("Producto de prueba");
		assertThat(categoriaRepository.findAll())
				.extracting(Categoria::getNombre)
				.contains("Categoría de prueba");
	}

	@Test
	@Transactional
	void productUpdatePreservesAnExistingMinimumUnlessNewOneIsProvided() throws Exception {
		Producto producto = new Producto();
		producto.setNombre("Producto con mínimo");
		producto.setPrecio(12.5);
		producto.setStockMinimo(8);
		Producto guardado = productoRepository.saveAndFlush(producto);

		mockMvc.perform(put("/api/admin/productos/{id}", guardado.getId())
				.with(jwt().authorities(new SimpleGrantedAuthority("recurso:write")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":"Producto editado","precio":15}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stockMinimo").value(8));

		mockMvc.perform(put("/api/admin/productos/{id}", guardado.getId())
				.with(jwt().authorities(new SimpleGrantedAuthority("recurso:write")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":"Producto editado","precio":15,"stockMinimo":3}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stockMinimo").value(3));
	}

	@Test
	@Transactional
	void persistsNotificationRequest() {
		NotificationRequest request = notificationRequestRepository.save(
				new NotificationRequest("stock-bajo", "{\"productoId\":42}", Instant.now()));

		assertThat(request.getId()).isNotNull();
		assertThat(notificationRequestRepository.findById(request.getId()))
				.isPresent()
				.get()
				.extracting(NotificationRequest::getType)
				.isEqualTo("stock-bajo");
	}
}
