package com.example.aforoiot;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Api {
    private static RequestQueue cola;

    private Api() {
    }

    public static void post(Context contexto, String ruta, JSONObject cuerpo,
            Response.Listener<JSONObject> alResponder, Response.ErrorListener alFallar) {
        solicitar(contexto, Request.Method.POST, ruta, cuerpo, 1, alResponder, alFallar);
    }

    public static void postSinReintento(Context contexto, String ruta, JSONObject cuerpo,
            Response.Listener<JSONObject> alResponder, Response.ErrorListener alFallar) {
        solicitar(contexto, Request.Method.POST, ruta, cuerpo, 0, alResponder, alFallar);
    }

    public static Request<JSONObject> get(Context contexto, String ruta,
            Response.Listener<JSONObject> alResponder, Response.ErrorListener alFallar) {
        return solicitar(contexto, Request.Method.GET, ruta, null, 1, alResponder, alFallar);
    }

    private static Request<JSONObject> solicitar(Context contexto, int metodo, String ruta,
            JSONObject cuerpo, int reintentos, Response.Listener<JSONObject> alResponder,
            Response.ErrorListener alFallar) {
        Context aplicacion = contexto.getApplicationContext();
        JsonObjectRequest solicitud = new JsonObjectRequest(
                metodo, Config.BASE_URL + ruta, cuerpo, alResponder, alFallar) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> encabezados = new HashMap<>();
                String token = new SessionManager(aplicacion).obtenerToken();
                if (token != null) {
                    encabezados.put("X-Token", token);
                }
                return encabezados;
            }
        };
        solicitud.setRetryPolicy(new DefaultRetryPolicy(10000, reintentos, 1f));
        if (metodo == Request.Method.GET) {
            solicitud.setShouldCache(false);
        }
        return cola(aplicacion).add(solicitud);
    }

    public static String mensajeDeError(Context contexto, VolleyError error) {
        NetworkResponse respuesta = error.networkResponse;
        if (respuesta != null && respuesta.data != null) {
            try {
                JSONObject json = new JSONObject(new String(respuesta.data, StandardCharsets.UTF_8));
                String mensaje = json.optString("mensaje", "");
                if (!mensaje.isEmpty()) {
                    return mensaje;
                }
            } catch (JSONException ignorada) {
                // La respuesta no era JSON; se usa el mensaje genérico.
            }
        }
        return contexto.getString(R.string.error_conexion);
    }

    private static synchronized RequestQueue cola(Context contexto) {
        if (cola == null) {
            cola = Volley.newRequestQueue(contexto);
        }
        return cola;
    }
}
