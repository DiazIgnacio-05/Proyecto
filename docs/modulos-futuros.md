# Módulos futuros

Funciones para cobrar aparte del abono básico. La plataforma ya está preparada: cada comercio tiene
su lista de módulos contratados (`comercio_modulo`), el super-admin los activa desde la ficha del
comercio y el código pregunta `comercio.tieneModulo(Modulo.X)`. Agregar uno nuevo es sumar un valor al
enum `Modulo`, escribir la función y activarla solo donde se pague.

El orden sigue tres criterios: cuánto lo valora el comercio (lo que lo hace pagar o no darse de baja),
cuánto cuesta hacerlo y qué riesgo agrega (datos personales, dinero, normativa).

| # | Módulo | Para quién | Esfuerzo | Por qué en este lugar |
|---|---|---|---|---|
| 1 | **Dominio propio** | Todos | Listo | Ya funciona (Etapas 5 y 6). Es fácil de vender: "tu marca, tu dirección". |
| 2 | **Estadísticas** | Todos | Bajo | Visitas por día, productos más vistos, clics en "Pedir por WhatsApp". Le muestra al dueño que el sitio le trae clientes, y eso es lo que más ayuda a que renueve el abono. Se cuentan eventos en el servidor, sin cookies de seguimiento ni Google Analytics. |
| 3 | **Promociones** | Todos | Bajo | Precio tachado ("antes $12.000, ahora $9.500"), cartel de "2x1" o "Hoy envío gratis" en la portada. Solo cambia el producto y la vista. |
| 4 | **Pedidos registrados** | Restaurantes y artículos | Medio | Cada pedido además se guarda y aparece en el panel ("nuevos", "preparando", "entregados"), con aviso al celular del dueño. Implica guardar nombre y teléfono del cliente: hay que cumplir la Ley 25.326 de datos personales (aviso de privacidad y borrado después de un tiempo). |
| 5 | **Costo de envío por zona** | Restaurantes y artículos | Medio | El comercio carga barrios o zonas con su precio; el total del pedido ya incluye el envío. Depende de que el cliente elija su zona en el carrito. |
| 6 | **Reservas de mesa** | Restaurantes | Medio | Formulario con día, hora y cantidad de personas; el dueño las ve y confirma desde el panel. Reutiliza los horarios de atención que ya existen. |
| 7 | **Stock por cantidad** | Ropa y artículos | Medio | Hoy hay "con stock / sin stock". Con cantidades, cada pedido registrado (módulo 4) descuenta stock. Conviene después del 4. |
| 8 | **Cobro online con Mercado Pago** | Todos | Alto | El cliente paga al hacer el pedido. Requiere que cada comercio conecte su cuenta de Mercado Pago (OAuth), recibir notificaciones de pago (webhooks) y manejar pagos rechazados o devueltos. Mucho valor, pero es dinero real: hacerlo cuando los pedidos registrados estén probados. |
| 9 | **Usuarios empleados** | Comercios con personal | Bajo | Más de un usuario por comercio, con permisos (por ejemplo, solo marcar sin stock). El modelo ya admite varios usuarios por comercio. |
| 10 | **Multi-sucursal** | Cadenas chicas | Alto | Un comercio con varias direcciones, horarios y WhatsApp. Cambia el modelo de datos; solo si aparece un cliente que lo pague. |
| 11 | **Facturación electrónica (ARCA)** | Todos | Muy alto | Factura C/B desde el panel. Normativa cambiante, certificados digitales por comercio y responsabilidad fiscal: mejor integrarse con un servicio que ya lo resuelva antes que hacerlo desde cero. |

## Mejoras de la plataforma (sin cobro aparte)

No se venden como módulo, pero conviene hacerlas a medida que crezca la cantidad de clientes:

- **Cobro automático del abono** con suscripciones de Mercado Pago: registra el pago solo y evita
  perseguir a los clientes. Hoy el pago se registra a mano en el super-admin.
- **Aviso de vencimiento** por email o WhatsApp al dueño unos días antes de que venza el abono.
- **Fotos en Cloudflare R2** cuando el disco del servidor empiece a quedar chico: es cambiar la
  implementación de `AlmacenamientoImagenes` (ya es una interfaz).
- **Plantillas visuales** por rubro (dos o tres diseños para elegir) para que los sitios no se vean
  todos iguales.
- **Formato WebP** para las fotos: pesan un 25-35 % menos que JPEG.
- **Límite de intentos de login compartido** (hoy vive en memoria; alcanza mientras haya un solo
  servidor).
