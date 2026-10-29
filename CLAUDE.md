# Vidriera — Contexto para Claude

## Qué es
Plataforma SaaS **multi-tenant** para comercios de Catamarca (restaurantes, tiendas de ropa, venta de
artículos). Una sola aplicación sirve los sitios de todos los clientes; cada comercio se da de alta por
configuración, nunca tocando código. Se vende con abono mensual. Nombre "Vidriera" es provisional.

El autor (Ignacio) es estudiante de ingeniería y sabe Java/POO/JUnit: explicar en 2-3 líneas cualquier
herramienta, patrón o framework nuevo. Si algo es ambiguo, preguntar antes de suponer. Todo en español
(Argentina), precios en pesos.

## Stack
- Java 21 (el JDK local es 22), Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation), Hibernate 7.
- Thymeleaf (render en servidor) + CSS propio con variables + JS vanilla mínimo. Sin build de Node.
- PostgreSQL 17 + Flyway. Tests: JUnit 5, AssertJ, Spring Boot Test, Testcontainers.
- Producción: Docker Compose (PostgreSQL + app + Caddy) en un VPS, Cloudflare delante, fotos en un
  volumen del VPS, backups diarios con copia en Cloudflare R2 (rclone). Guía: `docs/despliegue.md`.

## Comandos
```bash
docker compose up -d                 # PostgreSQL local (lee .env)
./mvnw spring-boot:run               # arranca con perfil dev (carga comercios demo)
./mvnw test                          # unitarios + integración (los de integración requieren Docker)
./mvnw -Dtest=AislamientoMultiTenantTest test   # un test puntual
```
Producción (en el VPS, desde `deploy/` con `deploy/.env`): `docker compose -f docker-compose.prod.yml up -d --build`.
CI: `.github/workflows/tests.yml` corre `./mvnw -B test` en GitHub Actions en cada push y PR, y otro job
construye la imagen de Docker y valida el Compose y el Caddyfile de producción.
`mvnw` debe quedar con permiso de ejecución en git (`git update-index --chmod=+x mvnw`).

Sitios demo: http://sabores-del-valle.localhost:8080 · http://nogal.localhost:8080 · http://la-alameda.localhost:8080
Panel: http://app.localhost:8080/login (dueños demo `sabores@`, `nogal@`, `alameda@vidriera.test`, clave `demo1234`).

## Estructura (package by feature, `src/main/java/ar/com/vidriera/`)
- `tenant/` TenantContext, resolver de Hibernate, EntidadDelComercio, resolución de comercio por host.
- `comercio/` Comercio (el tenant), Rubro, EstadoComercio, Modulo, horarios (FranjaHoraria, CalendarioAtencion).
- `catalogo/` Categoria, Producto, Variante, ImagenProducto.
- `usuario/` Usuario, Rol, login (UsuarioDetailsService, LoginController), límite de intentos, cambio de contraseña.
- `admin/` panel del comercio: `FiltroPanelDelComercio` (tenant desde el usuario), servicios, formularios y
  controladores de productos, categorías, horarios, "Mi comercio", QR de mesas y cuenta.
- `imagen/` subida de fotos: `ProcesadorImagenes` (valida y achica), `AlmacenamientoImagenes` (disco local).
- `superadmin/` panel del super-admin: `AltaComercioService` (alta en dos transacciones),
  `GestionComerciosService` (listado, estados, abono, módulos, reseteo de clave), controladores y vistas.
- `config/` propiedades tipadas (`vidriera.*`), seguridad, reloj.
- `publico/` sitio público: `restaurante.html` (menú) o `catalogo.html` (grilla con buscador, filtros y
  talles) según `rubro.usaBuscador()`; partes comunes en `fragments/sitio.html`. SEO (`MetadatosPagina`)
  e imagen de vista previa.
- `pedido/` pedido por WhatsApp: formulario, validación contra la base, mensaje y `NumeroWhatsApp`.
- `qr/` generador de QR (SVG con ZXing); la hoja para imprimir está en el panel (`/admin/mesas`).
- `comun/` utilidades compartidas (`FormatoPesos`, bean `@pesos` en Thymeleaf; `LectorPesos` para formularios).
- Vistas en `templates/` (`fragments/head.html` = SEO + colores del comercio); estáticos en `static/css`, `static/js`.
- Migraciones en `src/main/resources/db/migration`; datos demo en `db/demo/R__datos_demo.sql` (solo perfiles dev/test).
- `Dockerfile` (raíz); `deploy/` (Compose de producción, `Caddyfile`, `.env.example`, `backup.sh`,
  `restaurar.sh`); `docs/` (guía de despliegue y módulos futuros). Perfil `prod` en `application-prod.yml`.

## Decisiones de arquitectura
1. **Multi-tenant por columna** (`comercio_id` en cada tabla del comercio, base y esquema compartidos).
   Barato y simple para decenas de clientes; una migración sirve para todos.
2. **Aislamiento en capas**: (a) `@TenantId` de Hibernate filtra y completa `comercio_id` según
   `TenantContext`; (b) sin comercio en contexto se usa el id 0 → no se ve nada (falla cerrado);
   (c) claves foráneas compuestas `(id, comercio_id)` impiden relacionar filas de comercios distintos;
   (d) `AislamientoMultiTenantTest` lo verifica.
3. **Resolución del comercio**: sitio público por subdominio (`<slug>.<dominio-base>`, o dominio propio);
   en paneles, el comercio sale SIEMPRE del usuario autenticado, nunca de la URL ni de un formulario.
4. `Comercio` y `Usuario` NO son entidades con tenant (el resolvedor de hosts, el login y el super-admin
   necesitan verlos a todos). El modo `TenantContext.ejecutarComoSistema` (tenant raíz, sin filtro) es solo
   para tareas del super-admin.
5. **Rubros** como enum con capacidades (`usaMesas`, `usaVariantes`, `usaBuscador`); el código pregunta por
   la capacidad, no por el rubro. **Módulos pagos** en `comercio_modulo` y `comercio.tieneModulo(...)`.
6. **Pedidos por WhatsApp sin guardarse** en el MVP. El carrito vive en el navegador (`carrito.js`,
   localStorage); al confirmar se hace un POST a `/pedido` con ids y cantidades, el servidor busca nombres
   y precios en la base (filtrada por tenant), arma el mensaje y responde 303 a `wa.me`. Nunca confiar en
   precios que vengan del navegador. Modalidades: MESA (solo si vino por QR), RETIRO, ENVIO (si
   `comercio.ofreceEnvio`).
   `/pedido` está exento de CSRF a propósito: no guarda nada ni usa sesión, y un token obligaría a crear
   sesión por visitante. Por eso el form usa `action` plano y no `th:action`.
7. Super-admin creado al arrancar desde `SUPERADMIN_EMAIL`/`SUPERADMIN_PASSWORD`; nunca en migraciones.
8. Estados: ACTIVO / SUSPENDIDO (sitio 503, panel solo lectura) / BAJA (sin acceso, datos conservados).
9. **QR por mesa**: cada QR apunta a `<sitio>/?mesa=N`. La hoja está en el panel (`/admin/mesas`); como
   el panel vive en otro host, la URL del sitio la arma `UrlsDelComercio`.
10. **SEO y vista previa**: `og:*` en `fragments/head.html`. Si el comercio no tiene portada, `og:image`
    es `/vista-previa.png`, generada con Java2D (en Docker hará falta instalar fuentes, ver Etapa 6).
11. **Rendimiento**: HTML del servidor + 1 CSS + 1 JS, sin fuentes web. CSS/JS con hash en el nombre y
    caché de 1 año (`spring.web.resources.chain`). `default_batch_fetch_size: 50` evita el N+1.
12. **Panel del comercio** en `app.<dominio>/admin` (en un subdominio de comercio `/admin` y `/login`
    dan 404). `FiltroPanelDelComercio` corre después de Spring Security: toma el `comercioId` del
    `UsuarioAutenticado`, carga el comercio, fija `TenantContext` y aplica reglas de estado:
    BAJA → cierra sesión; contraseña temporal → solo `/admin/cuenta`; SUSPENDIDO → GET sí, POST no.
    Servicios del panel buscan `Comercio` siempre por `TenantContext.actual()`, nunca por un id del form.
    Ids ajenos dan 404 (`RecursoNoEncontradoException`), igual que inexistentes.
13. **Login propio** (`/login`, campos `email`/`password`, CSRF). Tras 5 fallos el email queda bloqueado
    15 min (`LimitadorIntentosLogin`, en memoria). Mensajes de error que no revelan si el email existe.
14. **Imágenes**: se aceptan JPG/PNG hasta 10 MB; se valida el encabezado antes de decodificar (máx.
    40 MP), se aplica la orientación EXIF y se descartan metadatos (GPS). Fotos → JPEG 480 y 1200 px;
    logo → PNG 256 px. Rutas `c<comercioId>/...` con UUID (nombres nunca reutilizados → caché de 1 año en
    `/imagenes/**`). Los archivos viejos se borran recién después del commit (`AlConfirmar`).
    Carpeta: `VIDRIERA_IMAGENES_DIR` (default `./uploads`); en producción será un volumen o R2.
15. Formularios del panel: Post/Redirect/Get con mensajes flash (`ok` / `error`). Precios como texto
    interpretado por `LectorPesos` ("12.500", "1.250,50").
16. **Variantes (ropa)**: cada `Variante` es una combinación talle/color con stock propio; el precio es
    el del producto. El panel genera combinaciones desde listas ("S, M, L" × "Negro, Blanco"), sin
    repetir (máx. 60 por producto). Un producto con variantes solo se puede pedir eligiendo una
    disponible; en el carrito la clave es `productoId:varianteId` y el pedido manda `lineas[i].varianteId`.
    El servidor valida que la variante sea de ese producto y tenga stock.
17. **Buscador del catálogo**: GET `/?q=...&categoria=...` (funciona sin JS y el link se puede
    compartir). Filtra en memoria con `BuscadorCatalogo`: todas las palabras, sin importar mayúsculas
    ni tildes, en nombre, descripción o categoría. Una categoría que no es del comercio se ignora.
18. **Super-admin** (`app.<dominio>/superadmin`, rol SUPER_ADMIN):
    - Alta: comercio + dueño con contraseña temporal (`GeneradorContrasenas`, SecureRandom, sin
      caracteres confusos) que debe cambiar al entrar + categorías sugeridas del rubro. Dos
      transacciones (el tenant se fija al abrir la sesión): si falla la segunda se borra el comercio.
    - La contraseña temporal se muestra UNA vez (mensaje flash) con link para mandarla por WhatsApp;
      nunca se guarda en texto plano. Resetear clave hace lo mismo y desbloquea el login.
    - Suspensión MANUAL (decisión de Ignacio): el listado avisa vencidos (`EstadoAbono`) y el
      super-admin suspende. "Registrar pago" suma un mes desde el vencimiento (o desde hoy si ya
      venció) y reactiva si estaba suspendido.
    - Contar productos de todos los comercios usa `ejecutarComoSistema` + transacción NUEVA
      (REQUIRES_NEW): dentro de una transacción ya abierta el modo sistema no tendría efecto.
19. **Producción** (Etapa 6): Cloudflare (DNS con registro `*` proxied, SSL Full strict) → Caddy →
    app → PostgreSQL. Solo Caddy publica puertos. Caddy usa el certificado de origen de Cloudflare
    para `<dominio>` y `*.<dominio>`; los dominios propios de clientes van sin Cloudflare (CNAME a
    `clientes.<dominio>`, DNS only) y Caddy les saca certificado de Let's Encrypt "on demand",
    preguntando antes a `GET /interno/dominio-permitido?domain=` (`DominiosPermitidosController`:
    200 solo si el dominio es de un comercio no dado de baja con el módulo DOMINIO_PROPIO). Esa ruta
    no pasa por `FiltroComercioPorHost` (llega con host `app:8080`) y Caddy la bloquea desde afuera.
    - Perfil `prod`: `forward-headers-strategy: native` (Tomcat acepta X-Forwarded-* solo de IPs
      internas → `request.getScheme()` = https), cookie de sesión `Secure`, sin datos demo, y
      `VIDRIERA_DOMINIO_BASE`/`DB_*` obligatorias.
    - Imagen: JRE 21 + `fonts-dejavu-core` (vista previa) + curl (healthcheck con `Host: app.<dominio>`);
      corre como usuario sin privilegios (uid 10001); fotos en el volumen `/datos/imagenes`.
    - Backups (`deploy/backup.sh`, cron 4:15): `pg_dump -Fc` + tar de las fotos, 14/7 días locales,
      copia en R2 con rclone (60 días). `restaurar.sh` probado de punta a punta.

## Convenciones
- Código y dominio en español, sin ñ ni tildes en identificadores (`DUENO`, `Categoria`). Sin Lombok.
- Entidades con constructor protegido para JPA + constructor/fábrica con las validaciones de negocio.
- Toda entidad de un comercio extiende `EntidadDelComercio`. **Prohibido SQL nativo sobre tablas de
  comercio** (el filtro de tenant no se aplica). No agregar setter de `comercioId`.
- Precios con `BigDecimal`. Hora actual siempre desde el `Clock` inyectado (zona America/Argentina/Catamarca).
- `ddl-auto: validate`: el esquema solo cambia con una nueva migración `V<n>__descripcion.sql`.
  Nunca editar una migración ya aplicada.
- Sin open-session-in-view: cargar lo que la vista necesita en el servicio (fetch join / DTO).
- Secretos solo en variables de entorno / `.env` (ignorado por git). `.env.example` documenta las claves.
- Tests: unitarios para lógica pura; integración extendiendo `PruebaDeIntegracion` (se omiten sin Docker).
- Commits pequeños, en español, Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).

## Trampas conocidas
- Avast intercepta HTTPS: en esta PC Maven necesita `.mvn/jvm.config` con
  `-Djavax.net.ssl.trustStoreType=Windows-ROOT` (archivo local, ignorado por git).
- No configurar `hibernate.jdbc.time_zone`: desplaza los valores `TIME` (horarios) al leerlos.
- Hibernate fija el tenant al abrir la sesión: cambiar `TenantContext` dentro de una transacción no tiene efecto.
- El día de la semana se guarda como texto ('MONDAY'): ordenar en Java, no con ORDER BY.
- Colores en CSS inline: usar `[(${...})]` (sin escapar; ya vienen validados). `[[${...}]]` escapa el `#`.
- `[hidden]` necesita `display: none !important` en el CSS: una clase con `display` le gana al atributo.
- `RedirectView` hacia afuera: `setExposeModelAttributes(false)` o Spring agrega el modelo como query params.
- Páginas sin fondo explícito se ven oscuras si el navegador está en modo oscuro: fijar `background`.
- **Borrados masivos saltean el filtro de tenant**: `deleteAllInBatch()`, `deleteAllByIdInBatch()` y
  `@Modifying` con DELETE/UPDATE en JPQL afectan a TODOS los comercios. Borrar con
  `repo.deleteAll(repo.findAll())` (entidad por entidad, ya filtradas).
- Tests de integración: NO usar `@Transactional` en la clase de test (la sesión se abriría antes de fijar
  el tenant). Para leer colecciones lazy, `PruebaDelPanel.enTransaccion(...)` fija el tenant y después
  abre la transacción.
- `<fieldset>` tiene ancho mínimo = contenido: poner `min-width: 0` o se desborda en celular.
- Si los tests dicen "Skipped" en los de integración, Docker Desktop no está corriendo (pasa tras reiniciar
  la PC): iniciarlo y repetir. Un resultado con omitidos no cuenta como verificado.
- `curl` desde Git Bash en Windows no manda UTF-8 en `-d`: para probar textos con tildes, usar el navegador
  o los tests (Tomcat responde 400 a parámetros mal codificados).
- Las tarjetas del catálogo llevan también la clase `.producto` (la usa `carrito.js`): sus estilos de fila
  se anulan en `.tarjeta-producto`.
- Thymeleaf 3.1 bloquea `T(clase)` en las vistas: pasar listas de enums desde el controlador.
- Claude no puede leer contraseñas de `.env` (bloqueado a propósito): para probar el super-admin en el
  navegador, que Ignacio inicie sesión; la cobertura automática está en `SuperAdminTest`.
- `docker build` en esta PC falla al bajar Maven (Avast intercepta HTTPS también dentro de Docker).
  Para probar la imagen: `./mvnw -DskipTests package` y `docker buildx build --build-context
  compilacion=<carpeta con src/app.jar> .` (reemplaza la etapa de compilación). La CI sí la compila entera.
- Para probar el stack de producción en local: copia de `deploy/` en el scratchpad, `.env` con valores de
  prueba, certificado autofirmado para `vidriera.test`, `docker compose -p vidriera-prueba ...` y
  `curl -k --resolve app.vidriera.test:443:127.0.0.1`. En Git Bash, `MSYS_NO_PATHCONV=1` para que no
  convierta rutas como `/datos/imagenes` en rutas de Windows.

## Estado por etapas
- [x] Etapa 1: estructura, modelo multi-tenant, datos demo, tests de aislamiento y horarios.
- [x] Etapa 2: sitio público restaurante + QR + pedido por WhatsApp.
- [x] Etapa 3: panel del comercio con autenticación.
- [x] Etapa 4: rubros ropa y artículos.
- [x] Etapa 5: panel super-admin (alta, baja, suspensión).
- [x] Etapa 6: despliegue (Docker, Caddy, Cloudflare, backups) y guías; módulos futuros en `docs/modulos-futuros.md`.
