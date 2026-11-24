package com.example.aforoiot;

public final class FormatoFecha {
    private static final String[] MESES = {
            "ene", "feb", "mar", "abr", "may", "jun",
            "jul", "ago", "sep", "oct", "nov", "dic"
    };

    private FormatoFecha() {
    }

    public static String fechaHora(String fecha) {
        if (fecha == null || fecha.length() < 16) {
            return "";
        }
        try {
            int dia = Integer.parseInt(fecha.substring(8, 10));
            int mes = Integer.parseInt(fecha.substring(5, 7));
            String hora = fecha.substring(11, 16);
            return dia + " " + MESES[mes - 1] + ", " + hora;
        } catch (RuntimeException e) {
            return fecha;
        }
    }
}
