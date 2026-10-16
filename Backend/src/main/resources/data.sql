INSERT INTO categorias (nombre)
SELECT 'Iluminación'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Iluminación');

INSERT INTO categorias (nombre)
SELECT 'Hogar y Muebles'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Hogar y Muebles');

INSERT INTO categorias (nombre)
SELECT 'Electrónica'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Electrónica');