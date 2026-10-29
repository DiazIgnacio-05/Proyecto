#!/usr/bin/env bash
# Backup diario de Vidriera: base de datos (pg_dump) + fotos subidas.
#  - Guarda los archivos en BACKUP_DIR (14 días de la base, 7 de las fotos).
#  - Si RCLONE_DESTINO está definido, sube una copia fuera del servidor (Cloudflare R2) y borra allá
#    las de más de 60 días.
# Se programa con cron (ver docs/despliegue.md):
#   15 4 * * * /opt/vidriera/deploy/backup.sh >> /var/log/vidriera-backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")"

# Lee una variable del .env sin ejecutarlo (las claves pueden tener caracteres especiales).
leer_var() {
    grep -E "^$1=" .env | tail -n 1 | cut -d= -f2- | tr -d '\r' | sed -e 's/^"//' -e 's/"$//' || true
}

DB_USER="$(leer_var DB_USER)"
BACKUP_DIR="$(leer_var BACKUP_DIR)"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/vidriera}"
RCLONE_DESTINO="$(leer_var RCLONE_DESTINO)"
COMPOSE=(docker compose -f docker-compose.prod.yml)
FECHA="$(date +%F_%H%M)"

mkdir -p "$BACKUP_DIR"
# Si algo falla a mitad, no quedan archivos temporales a medio escribir.
trap 'rm -f "$BACKUP_DIR"/.*.tmp' EXIT
echo "[$(date '+%F %T')] Backup $FECHA en $BACKUP_DIR"

# Base de datos en formato "custom" de PostgreSQL (comprimido; se restaura con pg_restore).
# Se escribe primero a un temporal: si falla a mitad, no queda un backup roto con nombre válido.
"${COMPOSE[@]}" exec -T postgres pg_dump -U "${DB_USER:-vidriera}" -d vidriera -Fc > "$BACKUP_DIR/.db_$FECHA.tmp"
mv "$BACKUP_DIR/.db_$FECHA.tmp" "$BACKUP_DIR/db_$FECHA.dump"

# Fotos: se empaquetan desde el contenedor de la app, que tiene el volumen montado.
"${COMPOSE[@]}" exec -T app tar czf - -C /datos/imagenes . > "$BACKUP_DIR/.imagenes_$FECHA.tmp"
mv "$BACKUP_DIR/.imagenes_$FECHA.tmp" "$BACKUP_DIR/imagenes_$FECHA.tar.gz"

# Retención local.
find "$BACKUP_DIR" -name 'db_*.dump' -mtime +14 -delete
find "$BACKUP_DIR" -name 'imagenes_*.tar.gz' -mtime +7 -delete

if [[ -n "$RCLONE_DESTINO" ]]; then
    rclone copy "$BACKUP_DIR" "$RCLONE_DESTINO" --exclude '.*'
    rclone delete "$RCLONE_DESTINO" --min-age 60d
    echo "Copia subida a $RCLONE_DESTINO"
fi

echo "[$(date '+%F %T')] Listo: $(du -sh "$BACKUP_DIR" | cut -f1) en total"
