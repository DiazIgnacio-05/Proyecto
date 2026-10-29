#!/usr/bin/env bash
# Restaura un backup hecho con backup.sh. REEMPLAZA los datos actuales.
# Uso: ./restaurar.sh /var/backups/vidriera/db_2026-11-02_0415.dump [/var/backups/vidriera/imagenes_2026-11-02_0415.tar.gz]
set -euo pipefail

cd "$(dirname "$0")"

DUMP="${1:?Indicá el archivo .dump a restaurar}"
FOTOS="${2:-}"
[[ -f "$DUMP" ]] || { echo "No existe $DUMP"; exit 1; }
[[ -z "$FOTOS" || -f "$FOTOS" ]] || { echo "No existe $FOTOS"; exit 1; }

DB_USER="$(grep -E '^DB_USER=' .env | tail -n 1 | cut -d= -f2- | tr -d '\r"' || true)"
COMPOSE=(docker compose -f docker-compose.prod.yml)

read -r -p "Se van a reemplazar los datos actuales con $DUMP. ¿Seguir? (escribí si) " respuesta
[[ "$respuesta" == "si" ]] || { echo "Cancelado"; exit 1; }

# Sin la app corriendo, nadie escribe en la base mientras se restaura.
"${COMPOSE[@]}" stop app caddy
"${COMPOSE[@]}" exec -T postgres pg_restore -U "${DB_USER:-vidriera}" -d vidriera --clean --if-exists --no-owner < "$DUMP"

if [[ -n "$FOTOS" ]]; then
    # Contenedor temporal de la app (sin arrancarla) para escribir en el volumen de fotos.
    "${COMPOSE[@]}" run --rm --no-deps -T --entrypoint sh app \
        -c 'find /datos/imagenes -mindepth 1 -delete && tar xzf - -C /datos/imagenes' < "$FOTOS"
fi

"${COMPOSE[@]}" up -d
echo "Restauración terminada."
