package com.example.aforoiot;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.android.volley.VolleyError;

import org.json.JSONObject;

public class OperacionPersonas extends ViewModel {
    static final String RUTA = "app/personas.php";

    private final MutableLiveData<Resultado> resultado = new MutableLiveData<>();
    private boolean enCurso;

    boolean enCurso() {
        return enCurso;
    }

    LiveData<Resultado> resultado() {
        return resultado;
    }

    void consumirResultado() {
        resultado.setValue(null);
    }

    void guardar(Context contexto, JSONObject cuerpo, int mensajeExito) {
        if (enCurso) {
            return;
        }
        enCurso = true;
        Api.postSinReintento(contexto, RUTA, cuerpo,
                respuesta -> terminar(new Resultado(respuesta, null, mensajeExito)),
                error -> terminar(new Resultado(null, error, mensajeExito)));
    }

    private void terminar(Resultado nuevoResultado) {
        enCurso = false;
        resultado.setValue(nuevoResultado);
    }

    static class Resultado {
        final JSONObject respuesta;
        final VolleyError error;
        final int mensajeExito;

        Resultado(JSONObject respuesta, VolleyError error, int mensajeExito) {
            this.respuesta = respuesta;
            this.error = error;
            this.mensajeExito = mensajeExito;
        }
    }
}
