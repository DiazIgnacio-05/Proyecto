# Despliegue en producción

Guía para poner Vidriera en Internet en un servidor propio (VPS). Se hace una sola vez; después,
cada cliente nuevo se da de alta desde el panel de super-admin sin tocar el servidor.

```
Celular ──HTTPS──> Cloudflare ──HTTPS──> Caddy ──HTTP──> app (Spring Boot) ──> PostgreSQL
 (visitante)       (CDN, caché de        (puertos        (red interna          (red interna
                    fotos, CSS y JS)      80 y 443)        de Docker)            de Docker)
```

- **Cloudflare** (plan gratis): DNS del dominio, HTTPS hacia el visitante, caché de fotos/CSS/JS en
  Buenos Aires y protección básica contra ataques.
- **Caddy**: servidor web que recibe HTTPS y se lo pasa a la app. Para los dominios propios de los
  clientes saca certificados de Let's Encrypt solo.
- **Docker Compose**: levanta la base, la app y Caddy con un comando y los reinicia si se caen o si se
  reinicia el servidor.
- Las fotos quedan en un volumen de Docker del servidor. Hay un backup diario de la base y de las fotos
  con copia en Cloudflare R2.

Los archivos están en [deploy/](../deploy): `docker-compose.prod.yml`, `Caddyfile`, `.env.example`,
`backup.sh` y `restaurar.sh`. El `Dockerfile` está en la raíz.

## 1. Qué hace falta

| Qué | Dónde | Costo aproximado |
|---|---|---|
| Servidor Ubuntu 24.04, 2 GB de RAM, 1-2 vCPU, 25+ GB de disco | Hetzner (CX22), DigitalOcean, Vultr; u Oracle Cloud "Always Free" (ARM) | USD 0 a 7 por mes |
| Dominio (ej. `vidriera.com.ar`) | NIC Argentina (nic.ar), con clave fiscal | pago anual |
| Cuenta de Cloudflare | cloudflare.com | gratis |
| Bucket de Cloudflare R2 para backups | dentro de Cloudflare | gratis hasta 10 GB |

Con 2 GB de RAM alcanza para decenas de comercios: la app usa unos 400 MB y PostgreSQL menos de 200 MB.

En los ejemplos el dominio es `vidriera.com.ar` y la IP del servidor `203.0.113.10`: reemplazalos por
los tuyos.

## 2. Dominio y Cloudflare

1. En Cloudflare: **Add a site** → `vidriera.com.ar` → plan Free. Cloudflare te da dos *nameservers*.
2. En NIC Argentina, en el dominio → **Delegaciones**: borrá las que haya y cargá los dos nameservers
   de Cloudflare. Tarda de minutos a unas horas.
3. En Cloudflare → **DNS → Records**, creá estos registros:

   | Tipo | Nombre | Contenido | Proxy |
   |---|---|---|---|
   | A | `vidriera.com.ar` (`@`) | `203.0.113.10` | Proxied (nube naranja) |
   | A | `*` | `203.0.113.10` | Proxied (nube naranja) |
   | A | `clientes` | `203.0.113.10` | **DNS only** (nube gris) |

   El `*` hace que cualquier subdominio (`app.`, `sabores-del-valle.`, etc.) llegue al servidor: por
   eso dar de alta un comercio no requiere tocar el DNS. `clientes` es el destino de los dominios
   propios (paso 8) y va sin proxy.
4. **SSL/TLS → Overview**: modo **Full (strict)** (Cloudflare se conecta al servidor por HTTPS y
   verifica el certificado).
5. **SSL/TLS → Origin Server → Create Certificate**: dejá los hosts `vidriera.com.ar` y
   `*.vidriera.com.ar`, validez 15 años. Te muestra dos textos: **guardalos ahora** (la clave privada
   no se vuelve a mostrar). Los vas a pegar en el servidor en el paso 4.
6. **SSL/TLS → Edge Certificates**: activá **Always Use HTTPS**.

> El certificado de origen solo sirve entre Cloudflare y tu servidor. Los navegadores ven el
> certificado de Cloudflare.

No hace falta ninguna regla de caché: Cloudflare ya guarda las imágenes, el CSS y el JS (la app les
pone caché de 1 año y un nombre nuevo cada vez que cambian) y nunca guarda el HTML, así los cambios
del panel se ven al instante.

## 3. Preparar el servidor

Conectate por SSH (`ssh root@203.0.113.10`) y ejecutá:

```bash
# Actualizaciones de seguridad automáticas
apt update && apt upgrade -y
apt install -y unattended-upgrades git ufw
dpkg-reconfigure -plow unattended-upgrades

# Firewall: solo SSH, HTTP y HTTPS
ufw allow OpenSSH
ufw allow 80
ufw allow 443
ufw enable

# Docker (script oficial)
curl -fsSL https://get.docker.com | sh

# Si el servidor tiene 2 GB de RAM o menos: 2 GB de swap para compilar sin quedarse sin memoria
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

Recomendado: entrar por SSH con clave pública y desactivar el login por contraseña
(`PasswordAuthentication no` en `/etc/ssh/sshd_config`).

## 4. Instalar Vidriera

```bash
git clone https://github.com/DiazIgnacio-05/Proyecto.git /opt/vidriera
cd /opt/vidriera/deploy
cp .env.example .env
chmod 600 .env
nano .env
```

Completá en `.env`:

- `DB_PASSWORD`: una clave larga (`openssl rand -base64 24` genera una).
- `VIDRIERA_DOMINIO_BASE`: tu dominio, sin `www` ni `https` (ej. `vidriera.com.ar`).
- `SUPERADMIN_EMAIL` y `SUPERADMIN_PASSWORD`: tu usuario de super-admin (12+ caracteres).
- `ACME_EMAIL`: tu email (Let's Encrypt avisa ahí si hay problemas con un certificado).

Pegá el certificado de origen de Cloudflare (paso 2.5):

```bash
mkdir -p certs
nano certs/origen.pem       # pegar "Origin Certificate"
nano certs/origen-key.pem   # pegar "Private Key"
chmod 600 certs/origen-key.pem
```

La app corre en Docker con un usuario sin privilegios; Caddy es el que lee los certificados.

Levantá todo (la primera vez compila la app: 3 a 10 minutos):

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps        # los tres en "running"; app pasa a "healthy"
docker compose -f docker-compose.prod.yml logs -f app   # Ctrl+C para salir
```

Entrá a `https://app.vidriera.com.ar/login` con el super-admin y dá de alta el primer comercio. Su
sitio queda en `https://<subdominio>.vidriera.com.ar`.

> En producción **no** se cargan los comercios demo (solo en el perfil `dev`).

## 5. Backups

`deploy/backup.sh` guarda cada día la base (`pg_dump`) y las fotos en `/var/backups/vidriera`
(14 días de la base, 7 de las fotos) y sube una copia a R2 (se borran allá a los 60 días).

1. En Cloudflare → **R2** → **Create bucket** → `vidriera-backups`.
2. **R2 → Manage API tokens → Create API token**: permiso *Object Read & Write*, solo ese bucket.
   Anotá *Access Key ID*, *Secret Access Key* y el *endpoint* (`https://<id>.r2.cloudflarestorage.com`).
3. En el servidor, instalá y configurá rclone (programa para copiar archivos a la nube):

   ```bash
   curl https://rclone.org/install.sh | bash
   rclone config
   ```

   `n` (nuevo) → nombre `r2` → tipo `s3` → proveedor `Cloudflare` → pegá Access Key y Secret →
   endpoint del paso 2 → el resto por defecto. Probalo con `rclone lsd r2:`.
4. Probá el backup a mano y programalo todos los días a las 4:15:

   ```bash
   /opt/vidriera/deploy/backup.sh
   ls -lh /var/backups/vidriera
   rclone ls r2:vidriera-backups
   (crontab -l 2>/dev/null; echo '15 4 * * * /opt/vidriera/deploy/backup.sh >> /var/log/vidriera-backup.log 2>&1') | crontab -
   ```

**Restaurar** (por ejemplo en un servidor nuevo, después del paso 4):

```bash
cd /opt/vidriera/deploy
rclone copy r2:vidriera-backups /var/backups/vidriera   # si el servidor es nuevo
./restaurar.sh /var/backups/vidriera/db_2026-11-02_0415.dump /var/backups/vidriera/imagenes_2026-11-02_0415.tar.gz
```

Conviene probar una restauración una vez (por ejemplo en otro VPS de prueba). Un backup que nunca se
restauró no está probado.

## 6. Actualizar a una versión nueva

Después de fusionar cambios en `main` (con la CI en verde):

```bash
cd /opt/vidriera
git pull
cd deploy
docker compose -f docker-compose.prod.yml up -d --build
```

Flyway aplica solo las migraciones nuevas al arrancar. El sitio queda sin servicio unos 30 a 60
segundos mientras la app reinicia; conviene actualizar de noche. Si algo sale mal, `git checkout
<commit anterior>` y el mismo comando de nuevo (las migraciones no se deshacen: por eso nunca hay que
editar una ya aplicada).

## 7. Mantenimiento

- **Logs**: `docker compose -f docker-compose.prod.yml logs --tail 200 app` (o `caddy`).
- **Monitoreo**: creá un monitor gratuito en UptimeRobot (o similar) que consulte
  `https://app.vidriera.com.ar/login` cada 5 minutos y te avise por email o Telegram si se cae.
- **Espacio**: `df -h` y `docker system df`. `docker image prune` borra imágenes viejas de la app.
- **Contraseña del super-admin**: se cambia desde el panel (Mi cuenta). `SUPERADMIN_PASSWORD` solo se
  usa para crear el usuario la primera vez.
- **Reinicio del servidor**: los contenedores vuelven solos (`restart: unless-stopped`).

## 8. Dominio propio de un cliente (módulo pago)

Para que un comercio use `www.sucomercio.com.ar` en lugar de `sucomercio.vidriera.com.ar`:

1. En el super-admin, en la ficha del comercio: activá **Dominio propio** y cargá
   `www.sucomercio.com.ar`.
2. El cliente (o vos, con su acceso) crea en el DNS de **su** dominio:

   | Tipo | Nombre | Contenido |
   |---|---|---|
   | CNAME | `www` | `clientes.vidriera.com.ar` |

   Si su dominio está en Cloudflare, el registro va con la nube **gris** (DNS only).
3. Listo: en la primera visita Caddy pide el certificado a Let's Encrypt (tarda unos segundos) y el
   sitio responde en ese dominio. Caddy solo pide certificados para dominios dados de alta en el
   super-admin.

Para que el dominio sin `www` (`sucomercio.com.ar`) también funcione, lo más simple es configurar en
el proveedor del dominio una redirección a `https://www.sucomercio.com.ar`.

## 9. Problemas comunes

| Síntoma | Causa probable |
|---|---|
| Cloudflare muestra "Error 526" | El certificado de origen no está en `deploy/certs` o el modo SSL no es Full (strict) con ese certificado. Ver `logs caddy`. |
| "Error 521/522" | La app o Caddy no están corriendo, o el firewall bloquea el 443. `ps` y `ufw status`. |
| Todo da 404 | `VIDRIERA_DOMINIO_BASE` no coincide con el dominio real. |
| La app se reinicia sola | Falta memoria: `docker stats`; agregá swap o un servidor más grande. |
| La vista previa de WhatsApp sale sin texto | Faltan fuentes en la imagen (el Dockerfile instala `fonts-dejavu-core`; reconstruí con `--build`). |
| Un dominio propio no carga | El CNAME no apunta a `clientes.<dominio>`, o el dominio no está cargado (o el módulo desactivado) en el super-admin. `logs caddy` muestra el motivo. |
