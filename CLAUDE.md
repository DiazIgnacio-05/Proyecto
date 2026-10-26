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
- Producción (Etapa 6): Docker Compose + Caddy en un VPS, Cloudflare delante, imágenes en Cloudflare R2.

## Comandos
```bash
docker compose up -d                 # PostgreSQL local (lee .env)
./mvnw spring-boot:run               # arranca con perfil dev (carga comercios demo)
./mvnw test                          # unitarios + integración (los de integración requieren Docker)
./mvnw -Dtest=AislamientoMultiTenantTest test   # un test puntual
```
CI: `.github/workflows/tests.yml` corre `./mvnw -B test` en GitHub Actions en cada push y PR.
`mvnw` debe quedar con permiso de ejecución en git (`git update-index --chmod=+x mvnw`).

Sitios demo: http://sabores-del-valle.localhost:8080 · http://nogal.localhost:8080 · http://la-alameda.localhost:8080

## Estructura (package by feature, `src/main/java/ar/com/vidriera/`)
- `tenant/` TenantContext, resolver de Hibernate, EntidadDelComercio, resolución de comercio por host.
- `comercio/` Comercio (el tenant), Rubro, EstadoComercio, Modulo, horarios (FranjaHoraria, CalendarioAtencion).
- `catalogo/` Categoria, Producto, Variante, ImagenProducto.
- `usuario/` Usuario, Rol, login (UsuarioDetailsService), creación del super-admin.
- `config/` propiedades tipadas (`vidriera.*`), seguridad, reloj.
- `publico/` controladores del sitio público. Futuro: `admin/`, `superadmin/`, `pedido/`, `qr/`, `imagen/`.
- Migraciones en `src/main/resources/db/migration`; datos demo en `db/demo/R__datos_demo.sql` (solo perfiles dev/test).

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
6. Pedidos por WhatsApp sin guardarse en el MVP (carrito en el navegador, link `wa.me`).
7. Super-admin creado al arrancar desde `SUPERADMIN_EMAIL`/`SUPERADMIN_PASSWORD`; nunca en migraciones.
8. Estados: ACTIVO / SUSPENDIDO (sitio 503, panel solo lectura) / BAJA (sin acceso, datos conservados).

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

## Estado por etapas
- [x] Etapa 1: estructura, modelo multi-tenant, datos demo, tests de aislamiento y horarios.
- [ ] Etapa 2: sitio público restaurante + QR + pedido por WhatsApp.
- [ ] Etapa 3: panel del comercio con autenticación.
- [ ] Etapa 4: rubros ropa y artículos.
- [ ] Etapa 5: panel super-admin (alta, baja, suspensión).
- [ ] Etapa 6: despliegue y guía de alta de clientes.
