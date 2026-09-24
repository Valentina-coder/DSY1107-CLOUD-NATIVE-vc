INSERT INTO categorias (id, nombre) VALUES (1, 'Iluminación');
INSERT INTO categorias (id, nombre) VALUES (2, 'Hogar y Muebles');
INSERT INTO categorias (id, nombre) VALUES (3, 'Electrónica');

-- Ajustar la secuencia del auto-incremento para que el próximo ID sea 4
ALTER TABLE categorias ALTER COLUMN id RESTART WITH 4;