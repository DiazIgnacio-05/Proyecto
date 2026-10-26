package ar.com.vidriera.admin;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.vidriera.comercio.FranjaHoraria;
import ar.com.vidriera.comercio.HorarioAtencion;
import ar.com.vidriera.comercio.HorarioAtencionRepository;

/** Horarios del comercio logueado. Guardar reemplaza la semana completa. */
@Service
public class HorarioAdminService {

    private final HorarioAtencionRepository horarios;

    public HorarioAdminService(HorarioAtencionRepository horarios) {
        this.horarios = horarios;
    }

    @Transactional(readOnly = true)
    public HorariosForm formulario() {
        Map<DayOfWeek, List<FranjaHoraria>> porDia = horarios.findAll().stream()
                .map(HorarioAtencion::getFranja)
                .sorted(Comparator.comparing(FranjaHoraria::apertura))
                .collect(Collectors.groupingBy(FranjaHoraria::dia));
        HorariosForm form = HorariosForm.semanaVacia();
        for (HorariosForm.Dia dia : form.getDias()) {
            List<FranjaHoraria> franjas = porDia.getOrDefault(dia.getDia(), List.of());
            if (!franjas.isEmpty()) {
                dia.setApertura1(franjas.get(0).apertura());
                dia.setCierre1(franjas.get(0).cierre());
            }
            if (franjas.size() > 1) {
                dia.setApertura2(franjas.get(1).apertura());
                dia.setCierre2(franjas.get(1).cierre());
            }
        }
        return form;
    }

    @Transactional
    public void guardar(HorariosForm form) {
        List<FranjaHoraria> nuevas = validar(form);
        // Se borran entidad por entidad (filtradas por comercio). Un borrado masivo con
        // deleteAllInBatch() o un DELETE en JPQL NO pasa por el filtro de tenant y borraría
        // los horarios de todos los comercios.
        horarios.deleteAll(horarios.findAll());
        horarios.flush();
        nuevas.forEach(f -> horarios.save(new HorarioAtencion(f.dia(), f.apertura(), f.cierre())));
    }

    static List<FranjaHoraria> validar(HorariosForm form) {
        List<FranjaHoraria> franjas = new ArrayList<>();
        for (HorariosForm.Dia dia : form.getDias()) {
            if (dia.getDia() == null) {
                throw new OperacionRechazadaException("Falta el día de la semana");
            }
            agregar(franjas, dia, dia.getApertura1(), dia.getCierre1());
            agregar(franjas, dia, dia.getApertura2(), dia.getCierre2());
        }
        return franjas;
    }

    private static void agregar(List<FranjaHoraria> franjas, HorariosForm.Dia dia, LocalTime apertura,
            LocalTime cierre) {
        if (apertura == null && cierre == null) {
            return;
        }
        if (apertura == null || cierre == null) {
            throw new OperacionRechazadaException(dia.getNombre() + ": completá la hora de apertura y la de cierre");
        }
        if (apertura.equals(cierre)) {
            throw new OperacionRechazadaException(dia.getNombre() + ": la apertura y el cierre no pueden ser iguales");
        }
        franjas.add(new FranjaHoraria(dia.getDia(), apertura, cierre));
    }
}
