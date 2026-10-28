-- Comercios ficticios de demostración (uno por rubro), para desarrollo y para mostrar en ventas.
-- Es una migración "repetible" (R__): Flyway la vuelve a ejecutar cada vez que cambia este archivo.
-- Por eso primero borra los comercios demo (el ON DELETE CASCADE borra todos sus datos) y los recrea.
--
-- Usuarios demo (contraseña "demo1234" en los tres):
--   sabores@vidriera.test  · nogal@vidriera.test  · alameda@vidriera.test
-- Los números de WhatsApp son ficticios: reemplazalos por el tuyo antes de una demo en vivo.

DELETE FROM comercio WHERE slug IN ('sabores-del-valle', 'nogal', 'la-alameda');

-- ---------------------------------------------------------------------------------------------
-- Comercios
-- ---------------------------------------------------------------------------------------------
INSERT INTO comercio (slug, nombre, rubro, estado, descripcion, whatsapp, telefono, email, direccion,
                      latitud, longitud, color_primario, color_secundario, instagram, facebook,
                      seo_titulo, seo_descripcion, cantidad_mesas, ofrece_envio, fecha_alta,
                      vencimiento_abono)
VALUES
    ('sabores-del-valle', 'Sabores del Valle', 'RESTAURANTE', 'ACTIVO',
     'Cocina regional catamarqueña: empanadas, locro y cabrito al horno de barro.',
     '3834000001', '383 4000001', 'hola@saboresdelvalle.test',
     'Av. Belgrano 1250, San Fernando del Valle de Catamarca', -28.468900, -65.784200,
     '#8C2F1B', '#E9B44C', 'saboresdelvalle', NULL,
     'Sabores del Valle · Cocina catamarqueña',
     'Empanadas, locro y cabrito. Mirá el menú y pedí por WhatsApp.', 12, TRUE,
     CURRENT_DATE, CURRENT_DATE + 30),
    ('nogal', 'Nogal Indumentaria', 'ROPA', 'ACTIVO',
     'Ropa urbana para todos los días. Envíos en el Valle Central.',
     '3834000002', NULL, 'ventas@nogal.test',
     'Rivadavia 640, San Fernando del Valle de Catamarca', -28.469600, -65.779500,
     '#2E3A59', '#C9A227', 'nogal.indumentaria', 'nogalindumentaria',
     'Nogal Indumentaria', 'Remeras, buzos y jeans. Consultá talles y colores por WhatsApp.', 0, TRUE,
     CURRENT_DATE, CURRENT_DATE + 30),
    ('la-alameda', 'Bazar La Alameda', 'ARTICULOS', 'ACTIVO',
     'Bazar, regalería y artículos para el hogar.',
     '3834000003', '383 4000003', NULL,
     'Sarmiento 455, San Fernando del Valle de Catamarca', -28.466800, -65.776300,
     '#2F6B4F', '#F2C14E', 'bazarlaalameda', NULL,
     'Bazar La Alameda', 'Todo para tu casa y para regalar. Buscá y pedí por WhatsApp.', 0, FALSE,
     CURRENT_DATE - 60, CURRENT_DATE - 3);  -- abono vencido, para mostrar el aviso en el super-admin

INSERT INTO usuario (email, password_hash, nombre, rol, comercio_id)
SELECT v.email, '$2a$10$DiyQTermoXLlrNcWaeqzROq6YZXCq2cy94Os29ThvRIYzEi2.XXqC', v.nombre, 'DUENO', c.id
FROM (VALUES ('sabores-del-valle', 'sabores@vidriera.test', 'Marta Quiroga'),
             ('nogal', 'nogal@vidriera.test', 'Lucas Herrera'),
             ('la-alameda', 'alameda@vidriera.test', 'Rosa Barrionuevo')) AS v(slug, email, nombre)
JOIN comercio c ON c.slug = v.slug;

-- ---------------------------------------------------------------------------------------------
-- Horarios (turnos cortados con siesta; el restaurante cierra después de medianoche)
-- ---------------------------------------------------------------------------------------------
INSERT INTO horario_atencion (comercio_id, dia_semana, apertura, cierre)
SELECT c.id, d.dia, f.apertura, f.cierre
FROM comercio c
CROSS JOIN (VALUES ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY'), ('SATURDAY'), ('SUNDAY')) AS d(dia)
CROSS JOIN (VALUES (TIME '12:00', TIME '15:30'), (TIME '20:30', TIME '00:30')) AS f(apertura, cierre)
WHERE c.slug = 'sabores-del-valle';

INSERT INTO horario_atencion (comercio_id, dia_semana, apertura, cierre)
SELECT c.id, d.dia, f.apertura, f.cierre
FROM comercio c
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY'), ('SATURDAY')) AS d(dia)
CROSS JOIN (VALUES (TIME '09:30', TIME '13:00'), (TIME '17:30', TIME '21:30')) AS f(apertura, cierre)
WHERE c.slug = 'nogal';

INSERT INTO horario_atencion (comercio_id, dia_semana, apertura, cierre)
SELECT c.id, v.dia, v.apertura, v.cierre
FROM comercio c
CROSS JOIN (VALUES ('MONDAY', TIME '09:00', TIME '13:00'), ('MONDAY', TIME '17:00', TIME '21:00'),
                   ('TUESDAY', TIME '09:00', TIME '13:00'), ('TUESDAY', TIME '17:00', TIME '21:00'),
                   ('WEDNESDAY', TIME '09:00', TIME '13:00'), ('WEDNESDAY', TIME '17:00', TIME '21:00'),
                   ('THURSDAY', TIME '09:00', TIME '13:00'), ('THURSDAY', TIME '17:00', TIME '21:00'),
                   ('FRIDAY', TIME '09:00', TIME '13:00'), ('FRIDAY', TIME '17:00', TIME '21:00'),
                   ('SATURDAY', TIME '09:00', TIME '13:30')) AS v(dia, apertura, cierre)
WHERE c.slug = 'la-alameda';

-- ---------------------------------------------------------------------------------------------
-- Categorías
-- ---------------------------------------------------------------------------------------------
INSERT INTO categoria (comercio_id, nombre, orden)
SELECT c.id, v.nombre, v.orden
FROM (VALUES ('sabores-del-valle', 'Empanadas', 1), ('sabores-del-valle', 'Platos regionales', 2),
             ('sabores-del-valle', 'Minutas', 3), ('sabores-del-valle', 'Postres', 4),
             ('sabores-del-valle', 'Bebidas', 5),
             ('nogal', 'Remeras', 1), ('nogal', 'Buzos y camperas', 2), ('nogal', 'Pantalones', 3),
             ('nogal', 'Accesorios', 4),
             ('la-alameda', 'Cocina', 1), ('la-alameda', 'Mate', 2), ('la-alameda', 'Electro', 3),
             ('la-alameda', 'Regalería', 4)) AS v(slug, nombre, orden)
JOIN comercio c ON c.slug = v.slug;

-- ---------------------------------------------------------------------------------------------
-- Productos: Sabores del Valle (restaurante)
-- ---------------------------------------------------------------------------------------------
INSERT INTO producto (comercio_id, categoria_id, nombre, descripcion, precio, disponible, destacado, orden)
SELECT cat.comercio_id, cat.id, v.nombre, v.descripcion, v.precio, v.disponible, v.destacado, v.orden
FROM (VALUES
    ('Empanadas', 'Empanada de carne cortada a cuchillo', 'Al horno de barro, con papa y huevo.', 1800, TRUE, TRUE, 1),
    ('Empanadas', 'Empanada de pollo', 'Pollo, morrón y cebolla de verdeo.', 1700, TRUE, FALSE, 2),
    ('Empanadas', 'Empanada de queso y cebolla', 'Queso cremoso y cebolla caramelizada.', 1700, TRUE, FALSE, 3),
    ('Empanadas', 'Docena de empanadas', 'Elegí los gustos al hacer el pedido.', 19500, TRUE, TRUE, 4),
    ('Platos regionales', 'Locro criollo', 'Maíz, zapallo, porotos, panceta y chorizo colorado.', 12000, TRUE, TRUE, 1),
    ('Platos regionales', 'Cabrito al horno', 'Porción con papas al horno y ensalada criolla.', 22000, FALSE, FALSE, 2),
    ('Platos regionales', 'Humita en chala', 'Choclo rallado, zapallo y queso de cabra.', 8500, TRUE, FALSE, 3),
    ('Platos regionales', 'Tamales (2 unidades)', 'Harina de maíz rellena de carne y especias.', 7800, TRUE, FALSE, 4),
    ('Minutas', 'Milanesa napolitana con papas fritas', 'Ternera, jamón, queso y salsa de tomate.', 15000, TRUE, FALSE, 1),
    ('Minutas', 'Lomito completo', 'Lomo, jamón, queso, huevo, lechuga y tomate. Con papas.', 16500, TRUE, FALSE, 2),
    ('Postres', 'Dulce de cayote con nuez', 'Dulce artesanal catamarqueño con nueces.', 5500, TRUE, TRUE, 1),
    ('Postres', 'Flan casero con dulce de leche', NULL, 5000, TRUE, FALSE, 2),
    ('Bebidas', 'Agua mineral 500 ml', NULL, 2200, TRUE, FALSE, 1),
    ('Bebidas', 'Gaseosa 1,5 L', 'Sabores: cola, lima-limón o naranja.', 4500, TRUE, FALSE, 2),
    ('Bebidas', 'Vino de Fiambalá (botella)', 'Torrontés o malbec de la región.', 11000, TRUE, FALSE, 3)
) AS v(categoria, nombre, descripcion, precio, disponible, destacado, orden)
JOIN categoria cat ON cat.nombre = v.categoria
JOIN comercio c ON c.id = cat.comercio_id AND c.slug = 'sabores-del-valle';

-- ---------------------------------------------------------------------------------------------
-- Productos: Nogal Indumentaria (ropa)
-- ---------------------------------------------------------------------------------------------
INSERT INTO producto (comercio_id, categoria_id, nombre, descripcion, precio, disponible, destacado, orden)
SELECT cat.comercio_id, cat.id, v.nombre, v.descripcion, v.precio, v.disponible, v.destacado, v.orden
FROM (VALUES
    ('Remeras', 'Remera básica de algodón', 'Algodón peinado 24/1, corte regular.', 14000, TRUE, TRUE, 1),
    ('Remeras', 'Remera oversize estampada', 'Estampa "Valle Central" en serigrafía.', 19000, TRUE, FALSE, 2),
    ('Buzos y camperas', 'Buzo canguro frisa', 'Frisa invisible, capucha y bolsillo canguro.', 38000, TRUE, TRUE, 1),
    ('Buzos y camperas', 'Campera de jean', 'Jean rígido con lavado medio.', 52000, FALSE, FALSE, 2),
    ('Pantalones', 'Jean recto', 'Tiro medio, denim con elastano.', 42000, TRUE, FALSE, 1),
    ('Pantalones', 'Jogger de algodón', 'Puño elastizado y cordón.', 27000, TRUE, FALSE, 2),
    ('Accesorios', 'Gorra trucker', 'Talle único regulable.', 12000, TRUE, FALSE, 1),
    ('Accesorios', 'Medias pack x3', 'Algodón, talle único.', 6500, TRUE, FALSE, 2)
) AS v(categoria, nombre, descripcion, precio, disponible, destacado, orden)
JOIN categoria cat ON cat.nombre = v.categoria
JOIN comercio c ON c.id = cat.comercio_id AND c.slug = 'nogal';

INSERT INTO variante (comercio_id, producto_id, talle, color, disponible)
SELECT p.comercio_id, p.id, v.talle, v.color, v.disponible
FROM (VALUES
    ('Remera básica de algodón', 'S', 'Blanco', TRUE), ('Remera básica de algodón', 'M', 'Blanco', TRUE),
    ('Remera básica de algodón', 'L', 'Blanco', FALSE), ('Remera básica de algodón', 'M', 'Negro', TRUE),
    ('Remera básica de algodón', 'L', 'Negro', TRUE), ('Remera básica de algodón', 'XL', 'Negro', TRUE),
    ('Remera oversize estampada', 'M', 'Arena', TRUE), ('Remera oversize estampada', 'L', 'Arena', TRUE),
    ('Buzo canguro frisa', 'S', 'Gris melange', TRUE), ('Buzo canguro frisa', 'M', 'Gris melange', FALSE),
    ('Buzo canguro frisa', 'L', 'Verde oliva', TRUE),
    ('Campera de jean', 'M', 'Azul', FALSE), ('Campera de jean', 'L', 'Azul', FALSE),
    ('Jean recto', '38', 'Azul', TRUE), ('Jean recto', '40', 'Azul', TRUE), ('Jean recto', '42', 'Azul', FALSE),
    ('Jean recto', '44', 'Negro', TRUE),
    ('Jogger de algodón', 'M', 'Negro', TRUE), ('Jogger de algodón', 'L', 'Gris', TRUE),
    ('Gorra trucker', NULL, 'Negro', TRUE), ('Gorra trucker', NULL, 'Beige', TRUE)
) AS v(producto, talle, color, disponible)
JOIN producto p ON p.nombre = v.producto
JOIN comercio c ON c.id = p.comercio_id AND c.slug = 'nogal';

-- ---------------------------------------------------------------------------------------------
-- Productos: Bazar La Alameda (artículos)
-- ---------------------------------------------------------------------------------------------
INSERT INTO producto (comercio_id, categoria_id, nombre, descripcion, precio, disponible, destacado, orden)
SELECT cat.comercio_id, cat.id, v.nombre, v.descripcion, v.precio, v.disponible, v.destacado, v.orden
FROM (VALUES
    ('Cocina', 'Juego de ollas antiadherentes x5', 'Aluminio con recubrimiento antiadherente, tapas de vidrio.', 89000, TRUE, TRUE, 1),
    ('Cocina', 'Set de cuchillos con taco', '6 piezas, acero inoxidable.', 34000, TRUE, FALSE, 2),
    ('Cocina', 'Tabla de algarrobo', '40 x 25 cm, ideal para asado.', 18500, TRUE, FALSE, 3),
    ('Cocina', 'Frascos herméticos x3', 'Vidrio con tapa de bambú.', 15000, FALSE, FALSE, 4),
    ('Mate', 'Termo de acero 1 L', 'Doble pared, mantiene el agua caliente 24 h.', 32000, TRUE, TRUE, 1),
    ('Mate', 'Mate de calabaza forrado', 'Forrado en cuero, con virola de alpaca.', 16000, TRUE, FALSE, 2),
    ('Mate', 'Bombilla de alpaca', 'Pico de loro, filtro desarmable.', 9500, TRUE, FALSE, 3),
    ('Electro', 'Pava eléctrica con corte a 80 °C', '1,7 L, ideal para mate.', 45000, TRUE, TRUE, 1),
    ('Electro', 'Licuadora 600 W', 'Vaso de 1,5 L, 2 velocidades.', 58000, TRUE, FALSE, 2),
    ('Regalería', 'Vela aromática de soja', 'Aromas: vainilla, lavanda o jazmín.', 8500, TRUE, FALSE, 1),
    ('Regalería', 'Portarretrato de madera 15x20', NULL, 7500, TRUE, FALSE, 2),
    ('Regalería', 'Taza de cerámica artesanal', 'Hecha a mano en Catamarca.', 9800, TRUE, FALSE, 3)
) AS v(categoria, nombre, descripcion, precio, disponible, destacado, orden)
JOIN categoria cat ON cat.nombre = v.categoria
JOIN comercio c ON c.id = cat.comercio_id AND c.slug = 'la-alameda';
