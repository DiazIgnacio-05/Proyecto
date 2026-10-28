package ar.com.vidriera.admin;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import ar.com.vidriera.comercio.DescripcionAtencion;

/**
 * La semana completa en un solo formulario: cada día tiene hasta dos turnos (por ejemplo mañana y
 * tarde, con la siesta en el medio). Un día sin turnos está cerrado.
 */
public class HorariosForm {

    private List<Dia> dias = new ArrayList<>();

    public static HorariosForm semanaVacia() {
        HorariosForm form = new HorariosForm();
        for (DayOfWeek dia : DayOfWeek.values()) {
            Dia d = new Dia();
            d.setDia(dia);
            form.dias.add(d);
        }
        return form;
    }

    public static class Dia {

        private DayOfWeek dia;

        // El <input type="time"> del navegador manda la hora como "20:30".
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime apertura1;
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime cierre1;
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime apertura2;
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime cierre2;

        public String getNombre() {
            return dia == null ? "" : DescripcionAtencion.nombre(dia);
        }

        public DayOfWeek getDia() {
            return dia;
        }

        public void setDia(DayOfWeek dia) {
            this.dia = dia;
        }

        public LocalTime getApertura1() {
            return apertura1;
        }

        public void setApertura1(LocalTime apertura1) {
            this.apertura1 = apertura1;
        }

        public LocalTime getCierre1() {
            return cierre1;
        }

        public void setCierre1(LocalTime cierre1) {
            this.cierre1 = cierre1;
        }

        public LocalTime getApertura2() {
            return apertura2;
        }

        public void setApertura2(LocalTime apertura2) {
            this.apertura2 = apertura2;
        }

        public LocalTime getCierre2() {
            return cierre2;
        }

        public void setCierre2(LocalTime cierre2) {
            this.cierre2 = cierre2;
        }
    }

    public List<Dia> getDias() {
        return dias;
    }

    public void setDias(List<Dia> dias) {
        this.dias = dias;
    }
}
