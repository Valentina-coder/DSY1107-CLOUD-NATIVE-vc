package com.example.Backend.config;

import com.example.Backend.model.Categoria;
import com.example.Backend.repository.CategoriaRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final CategoriaRepository categoriaRepository;

    public DataInitializer(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (categoriaRepository.count() == 0) {
            List.of("Iluminación", "Hogar y Muebles", "Electrónica")
                    .forEach(nombre -> categoriaRepository.save(new Categoria(nombre)));
        }
    }
}
