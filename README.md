# Vidriera

[![Tests](https://github.com/DiazIgnacio-05/Proyecto/actions/workflows/tests.yml/badge.svg)](https://github.com/DiazIgnacio-05/Proyecto/actions/workflows/tests.yml)

Plataforma para que comercios locales tengan su sitio web: menú digital para restaurantes, catálogo
para tiendas de ropa y negocios de artículos, y pedidos por WhatsApp. Una sola aplicación atiende a
todos los comercios (multi-tenant); cada cliente nuevo se da de alta por configuración.

> Estado: **Etapa 6 (MVP completo)**. Funcionan los sitios de los tres rubros (restaurante con QR por
> mesa; tienda de ropa con talles y colores; venta de artículos con buscador y filtros), el pedido por
> WhatsApp, el panel del comercio, el panel de super-admin (alta, suspensión, baja y abonos) y el
> despliegue en producción con HTTPS y backups ([guía](docs/despliegue.md)).

## Requisitos

- **JDK 21 o superior** (probado con Temurin 22).
- **Docker Desktop** (PostgreSQL local y tests de integración). En Windows 10 Home se instala con WSL2.
- Git. No hace falta instalar Maven: el proyecto trae Maven Wrapper (`mvnw`).

## Instalación local

```bash
git clone https://github.com/DiazIgnacio-05/Proyecto.git
cd Proyecto
cp .env.example .env        # completar DB_PASSWORD y SUPERADMIN_PASSWORD
docker compose up -d        # levanta PostgreSQL
./mvnw spring-boot:run      # arranca la app con el perfil dev (carga los comercios demo)
```

En Windows (PowerShell o CMD) usá `mvnw.cmd` en lugar de `./mvnw`.

Abrí en el navegador (Chrome y Firefox resuelven `*.localhost` a tu PC sin configurar nada):

| Comercio demo | Rubro | URL | Usuario del panel |
|---|---|---|---|
| Sabores del Valle | Restaurante | http://sabores-del-valle.localhost:8080 | sabores@vidriera.test |
| Nogal Indumentaria | Tienda de ropa | http://nogal.localhost:8080 | nogal@vidriera.test |
| Bazar La Alameda | Venta de artículos | http://la-alameda.localhost:8080 | alameda@vidriera.test |

Qué probar en el restaurante demo (conviene usar la vista de celular del navegador, F12 → íconos de
dispositivo):

- Menú por categorías, cartel "Abierto ahora / Cerrado", horarios, mapa y contacto.
- Armar un pedido y enviarlo: se abre WhatsApp con el mensaje escrito (productos, cantidades y total).
- QR de mesa: http://sabores-del-valle.localhost:8080/?mesa=5 simula haber escaneado el QR de la mesa 5.
- Imagen de vista previa al compartir el link: http://sabores-del-valle.localhost:8080/vista-previa.png

En la tienda de ropa y el bazar:

- Grilla de productos con buscador (sin importar tildes: "pava electrica") y filtros por categoría:
  http://la-alameda.localhost:8080/?q=mate
- Elegir talle y color antes de agregar; los talles agotados se ven pero no se pueden elegir. El mensaje
  de WhatsApp incluye el talle: "2 x Remera básica de algodón (talle M, Negro)".

### Panel del comercio

Entrá a http://app.localhost:8080/login con el usuario del comercio (tabla de arriba). Desde el panel
el dueño, sin conocimientos técnicos y desde el celular, puede:

- Cargar, editar y borrar productos; marcarlos "sin stock" con un toque; subir hasta 5 fotos por
  producto (se achican solas para que el sitio cargue rápido).
- En ropa: cargar talles y colores escribiendo "S, M, L" y "Negro, Blanco" (se crean todas las
  combinaciones) y marcar sin stock un talle puntual.
- Crear, ordenar y ocultar categorías.
- Cargar los horarios de la semana (hasta dos turnos por día).
- Editar los datos del comercio: WhatsApp, contacto, mapa, redes, colores, logo, portada y textos para
  Google; activar envíos a domicilio y la cantidad de mesas.
- Imprimir los QR de las mesas y cambiar su contraseña.

La contraseña de los usuarios demo es `demo1234`. El super-admin se crea al primer arranque con
`SUPERADMIN_EMAIL` y `SUPERADMIN_PASSWORD` del `.env`. Los números de WhatsApp de la demo son
ficticios: cambialos por el tuyo antes de mostrarla a un cliente.

## Tests

```bash
./mvnw test
```

Los tests de integración levantan un PostgreSQL descartable con Testcontainers; si Docker no está
corriendo se marcan como omitidos (no fallan). Los más importantes:

- `AislamientoMultiTenantTest`: un comercio nunca ve ni modifica datos de otro.
- `DatosDemoTest`: las migraciones coinciden con las entidades y los datos demo son válidos.
- `CalendarioAtencionTest` / `FranjaHorariaTest`: "abierto ahora", turnos cortados y cierre después de medianoche.
- `PedidoControllerTest`: el pedido usa los precios de la base, rechaza productos sin stock o de otro
  comercio, mesas inexistentes y envíos en comercios que no los hacen.
- `NumeroWhatsAppTest`: normaliza "0383 15 4000001", "+54 9 383..." y otras formas al formato de WhatsApp.
- `SitioRestauranteTest`: menú, SEO, QR por mesa y vista previa de punta a punta.
- `AislamientoDelPanelTest`: el dueño de un comercio no puede ver, editar ni borrar datos de otro
  aunque pruebe con sus ids.
- `SeguridadDelPanelTest`: login, bloqueo tras 5 intentos, CSRF, panel solo en `app.`, comercio
  suspendido en solo lectura, contraseña temporal obligatoria de cambiar.
- `ProductosDelPanelTest` / `ProcesadorImagenesTest`: alta y edición de productos, fotos optimizadas y
  rechazo de archivos que no son imágenes.
- `SitioCatalogoTest` / `BuscadorCatalogoTest`: catálogo con talles, búsqueda sin tildes, filtros y
  categorías ajenas ignoradas. `VariantesDelPanelTest`: talles y colores desde el panel y aislamiento.
- `SuperAdminTest`: alta con contraseña temporal (y login con ella), suspensión, pago, baja, reseteo de
  clave, dominio propio y que un dueño no pueda entrar al super-admin. `EstadoAbonoTest`: vencimientos.
- `DominiosPermitidosTest`: el servidor solo pide certificados HTTPS para dominios propios dados de alta.

La CI además construye la imagen de Docker y valida el Compose y el Caddyfile de producción.

## Configuración

Todas las claves están documentadas en [.env.example](.env.example) (desarrollo) y
[deploy/.env.example](deploy/.env.example) (producción). Los archivos `.env` nunca se suben al
repositorio.

| Variable | Para qué |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL |
| `VIDRIERA_DOMINIO_BASE` | Dominio de los subdominios (`localhost` en desarrollo) |
| `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD` | Super-admin inicial (contraseña de 12+ caracteres) |
| `VIDRIERA_IMAGENES_DIR` | Carpeta donde se guardan las fotos subidas (por defecto `./uploads`) |
| `ACME_EMAIL` | Solo producción: email para los certificados de Let's Encrypt (dominios propios) |
| `BACKUP_DIR`, `RCLONE_DESTINO` | Solo producción: carpeta de backups y copia en Cloudflare R2 |

## Estructura

```
src/main/java/ar/com/vidriera/
  tenant/     aislamiento multi-tenant y resolución del comercio por subdominio
  comercio/   comercio (cliente), rubros, estados, módulos y horarios
  catalogo/   categorías, productos, variantes e imágenes
  usuario/    usuarios, roles, login y contraseñas
  admin/      panel del comercio
  superadmin/ panel del super-admin
  imagen/     subida, optimización y almacenamiento de fotos
  config/     configuración y seguridad
  publico/    sitio público, SEO e imagen de vista previa
  pedido/     pedido por WhatsApp
  qr/         códigos QR por mesa
  comun/      utilidades (formato de precios)
src/main/resources/
  db/migration/   migraciones Flyway (esquema)
  db/demo/        comercios de demostración (perfiles dev y test)
  templates/      vistas Thymeleaf
Dockerfile        imagen de producción
deploy/           Compose de producción, Caddyfile, backup y restauración
docs/             guía de despliegue y módulos futuros
```

Las decisiones de arquitectura están en [CLAUDE.md](CLAUDE.md).

## Alta de un cliente nuevo

No hace falta tocar código, el servidor ni el DNS. Desde el panel de super-admin
(`https://app.<tu-dominio>/login`; en desarrollo http://app.localhost:8080/login, con
`SUPERADMIN_EMAIL` / `SUPERADMIN_PASSWORD` del `.env`):

1. **+ Nuevo comercio**: nombre, subdominio (se sugiere solo a partir del nombre), rubro, WhatsApp,
   nombre y email del dueño, y hasta cuándo está pago el abono.
2. Al crear, se muestran **una sola vez** los datos para el dueño (dirección del panel, email y
   contraseña temporal) con un botón para mandárselos por WhatsApp. El sitio ya queda publicado en
   `https://<subdominio>.<tu-dominio>` con las categorías típicas del rubro.
3. El dueño entra, elige su contraseña y carga productos, fotos, horarios, logo y colores (o lo hacés
   vos como parte del servicio).
4. Desde "QR de mesas" imprime la hoja de QR (restaurantes) y comparte el link del sitio en sus redes.

Cada mes, en la ficha del comercio: **Registrar pago** suma un mes (y reactiva si estaba suspendido).
El listado avisa qué abonos están vencidos o por vencer; si no paga, **Suspender** muestra "sitio no
disponible" y deja el panel en solo lectura, sin perder nada. **Dar de baja** lo quita del todo,
conservando los datos. También podés activar módulos pagos (el dominio propio se configura como
indica la [guía de despliegue](docs/despliegue.md#8-dominio-propio-de-un-cliente-módulo-pago)), dejar
notas internas y generar una contraseña nueva si el dueño la olvidó.

## Despliegue

Un VPS chico (Ubuntu, 2 GB de RAM) con Docker Compose: PostgreSQL + la app + Caddy (HTTPS), con
Cloudflare delante y backups diarios a Cloudflare R2. Paso a paso, con dominio, DNS, certificados,
backups, actualizaciones y dominios propios de clientes: **[docs/despliegue.md](docs/despliegue.md)**.

```bash
cd deploy && cp .env.example .env    # completar
docker compose -f docker-compose.prod.yml up -d --build
```

## Módulos futuros

Lista priorizada de funciones para cobrar aparte (estadísticas, promociones, pedidos registrados,
Mercado Pago, reservas...) en **[docs/modulos-futuros.md](docs/modulos-futuros.md)**.

## Problemas comunes

**`PKIX path building failed` al compilar.** Algún antivirus (por ejemplo Avast con "análisis HTTPS")
intercepta las conexiones y Java no confía en su certificado. Creá el archivo `.mvn/jvm.config` con:

```
-Djavax.net.ssl.trustStoreType=Windows-ROOT
```

Así Maven usa los certificados de Windows. El archivo está en `.gitignore` porque es propio de cada PC.

**`docker build` falla con "wget: Failed to fetch ... apache-maven".** Es el mismo antivirus: dentro de
Docker no se puede usar el truco de arriba. En esa PC, desactivá el análisis HTTPS del antivirus
mientras construís, o compilá el `.jar` con `./mvnw -DskipTests package` y construí solo la etapa
final:

```bash
mkdir -p /tmp/ctx/src && cp target/vidriera-*.jar /tmp/ctx/src/app.jar
docker buildx build --load -t vidriera-app --build-context compilacion=/tmp/ctx .
```

En el VPS y en GitHub Actions no pasa.
