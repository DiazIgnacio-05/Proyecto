package ar.com.vidriera.comercio;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import ar.com.vidriera.tenant.EntidadDelComercio;

/** Una franja de atención guardada en la base. La lógica de horarios está en {@link FranjaHoraria}. */
@Entity
public class HorarioAtencion extends EntidadDelComercio {

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DayOfWeek dia;

    @Column(nullable = false)
    private LocalTime apertura;

    @Column(nullable = false)
    private LocalTime cierre;

    protected HorarioAtencion() {
    }

    public HorarioAtencion(DayOfWeek dia, LocalTime apertura, LocalTime cierre) {
        cambiar(new FranjaHoraria(dia, apertura, cierre));
    }

    public FranjaHoraria getFranja() {
        return new FranjaHoraria(dia, apertura, cierre);
    }

    public void cambiar(FranjaHoraria franja) {
        this.dia = franja.dia();
        this.apertura = franja.apertura();
        this.cierre = franja.cierre();
    }
}
