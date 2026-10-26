package ar.com.vidriera;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de los tests de integración: levantan la aplicación completa contra un PostgreSQL real
 * dentro de un contenedor Docker (Testcontainers). Spring reutiliza el mismo contexto y el mismo
 * contenedor entre todas las clases que extienden de esta.
 *
 * <p>Sin Docker disponible estos tests se marcan como omitidos ("skipped") en lugar de fallar.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class PruebaDeIntegracion {
}
