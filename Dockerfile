# Imagen de producción de Vidriera. Se construye en dos etapas:
#  1. "compilacion": usa el JDK y Maven Wrapper para generar el .jar (sin correr los tests; eso lo hace la CI).
#  2. imagen final: solo el JRE + el .jar, más chica y sin herramientas de compilación.
# Uso: docker compose -f deploy/docker-compose.prod.yml up -d --build

FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /src
# Primero solo lo necesario para bajar dependencias: si no cambia el pom.xml, Docker reutiliza esta capa.
COPY mvnw pom.xml ./
COPY .mvn/wrapper .mvn/wrapper
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -DskipTests package && cp target/vidriera-*.jar /src/app.jar

FROM eclipse-temurin:21-jre
# Fuentes para la imagen de vista previa (Java2D no trae ninguna) y curl para el healthcheck.
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core curl \
    && rm -rf /var/lib/apt/lists/*
# La app no corre como root. La carpeta de fotos se monta como volumen y hereda este dueño.
RUN useradd --system --uid 10001 vidriera \
    && mkdir -p /datos/imagenes && chown -R vidriera /datos
USER vidriera
WORKDIR /app
COPY --from=compilacion /src/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    VIDRIERA_IMAGENES_DIR=/datos/imagenes \
    TZ=America/Argentina/Catamarca \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"
EXPOSE 8080

# La app responde según el host: se consulta el login del panel (app.<dominio>).
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS -o /dev/null -H "Host: app.${VIDRIERA_DOMINIO_BASE}" http://localhost:8080/login || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
