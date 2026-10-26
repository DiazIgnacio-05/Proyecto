package ar.com.vidriera.comercio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Horarios del comercio actual, listos para mostrar. */
@Service
public class AtencionService {

    /** Lo que necesita la vista: el cartel de estado (si hay horarios) y la tabla semanal. */
    public record InfoAtencion(Optional<DescripcionAtencion.Estado> estado, List<DescripcionAtencion.Dia> semana) {
    }

    private final HorarioAtencionRepository horarios;
    private final Clock reloj;

    public AtencionService(HorarioAtencionRepository horarios, Clock reloj) {
        this.horarios = horarios;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public InfoAtencion infoActual() {
        CalendarioAtencion calendario = new CalendarioAtencion(
                horarios.findAll().stream().map(HorarioAtencion::getFranja).toList());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return new InfoAtencion(DescripcionAtencion.estado(calendario, ahora),
                DescripcionAtencion.semana(calendario, ahora.getDayOfWeek()));
    }
}
