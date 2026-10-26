package ar.com.vidriera.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración propia de la aplicación (prefijo {@code vidriera.} en application.yml).
 * Spring completa este record automáticamente y falla al arrancar si un valor tiene formato inválido.
 *
 * @param dominioBase dominio bajo el cual cada comercio tiene su subdominio
 *                    ({@code localhost} en desarrollo, {@code vidriera.com.ar} en producción)
 * @param zonaHoraria zona horaria de los comercios (para "abierto ahora")
 * @param superAdmin  credenciales iniciales del super-admin
 */
@ConfigurationProperties(prefix = "vidriera")
public record VidrieraProperties(String dominioBase, ZoneId zonaHoraria, SuperAdmin superAdmin) {

    public record SuperAdmin(String email, String password) {
    }
}
