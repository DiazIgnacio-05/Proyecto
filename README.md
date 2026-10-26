# Vidriera

Plataforma para que comercios locales tengan su sitio web: menú digital para restaurantes, catálogo
para tiendas de ropa y negocios de artículos, y pedidos por WhatsApp. Una sola aplicación atiende a
todos los comercios (multi-tenant); cada cliente nuevo se da de alta por configuración.

> Estado: **Etapa 2**. Ya funciona el sitio público del restaurante con QR por mesa y pedido por
> WhatsApp. Los otros rubros, los paneles y el despliegue llegan en las próximas etapas.

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
- Hoja de QR para imprimir: http://sabores-del-valle.localhost:8080/mesas/imprimir
- Imagen de vista previa al compartir el link: http://sabores-del-valle.localhost:8080/vista-previa.png

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

## Configuración

Todas las claves están documentadas en [.env.example](.env.example). En producción se pasan como
variables de entorno; el archivo `.env` nunca se sube al repositorio.

| Variable | Para qué |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL |
| `VIDRIERA_DOMINIO_BASE` | Dominio de los subdominios (`localhost` en desarrollo) |
| `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD` | Super-admin inicial (contraseña de 12+ caracteres) |

## Estructura

```
src/main/java/ar/com/vidriera/
  tenant/     aislamiento multi-tenant y resolución del comercio por subdominio
  comercio/   comercio (cliente), rubros, estados, módulos y horarios
  catalogo/   categorías, productos, variantes e imágenes
  usuario/    usuarios, roles y login
  config/     configuración y seguridad
  publico/    sitio público, SEO e imagen de vista previa
  pedido/     pedido por WhatsApp
  qr/         códigos QR por mesa
  comun/      utilidades (formato de precios)
src/main/resources/
  db/migration/   migraciones Flyway (esquema)
  db/demo/        comercios de demostración (perfiles dev y test)
  templates/      vistas Thymeleaf
```

Las decisiones de arquitectura están en [CLAUDE.md](CLAUDE.md).

## Alta de un cliente nuevo

Se hará desde el panel de super-admin (Etapa 5). La guía completa llega en la Etapa 6.

## Despliegue

Guía en la Etapa 6 (Docker Compose + Caddy en un VPS, con Cloudflare delante).

## Problemas comunes

**`PKIX path building failed` al compilar.** Algún antivirus (por ejemplo Avast con "análisis HTTPS")
intercepta las conexiones y Java no confía en su certificado. Creá el archivo `.mvn/jvm.config` con:

```
-Djavax.net.ssl.trustStoreType=Windows-ROOT
```

Así Maven usa los certificados de Windows. El archivo está en `.gitignore` porque es propio de cada PC.
