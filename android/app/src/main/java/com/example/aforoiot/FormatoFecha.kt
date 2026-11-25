package com.example.aforoiot

object FormatoFecha {
    private val MESES = arrayOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic"
    )

    @JvmStatic
    fun fechaHora(fecha: String?): String {
        if (fecha == null || fecha.length < 16) {
            return ""
        }
        return try {
            val dia = fecha.substring(8, 10).toInt()
            val mes = fecha.substring(5, 7).toInt()
            val hora = fecha.substring(11, 16)
            "$dia ${MESES[mes - 1]}, $hora"
        } catch (e: RuntimeException) {
            fecha
        }
    }
}
