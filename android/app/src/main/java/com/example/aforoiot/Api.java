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

    public static void get(Context contexto, String ruta,
            Response.Listener<JSONObject> alResponder, Response.ErrorListener alFallar) {
        solicitud(contexto, Request.Method.GET, ruta, null, alResponder, alFallar);
    }

    public static void post(Context contexto, String ruta, JSONObject cuerpo,
            Response.Listener<JSONObject> alResponder, Response.ErrorListener alFallar) {
        solicitud(contexto, Request.Method.POST, ruta, cuerpo, alResponder, alFallar);
    }

    public static boolean esNoAutorizado(VolleyError error) {
        return error.networkResponse != null && error.networkResponse.statusCode == 401;
    }

    public static String mensajeDeError(Context contexto, VolleyError error) {
        NetworkResponse respuesta = error.networkResponse;
        if (respuesta != null) {
            if (respuesta.data != null) {
                try {
                    JSONObject json = new JSONObject(new String(respuesta.data, StandardCharsets.UTF_8));
                    String mensaje = json.optString("mensaje", "");
                    if (!mensaje.isEmpty()) {
                        return mensaje;
                    }
                } catch (JSONException ignorada) {
                    // El servidor respondió sin JSON; se usa el mensaje de error genérico.
                }
            }
            return contexto.getString(R.string.error_servidor);
        }
        return contexto.getString(R.string.error_conexion);
    }

    private static void solicitud(Context contexto, int metodo, String ruta, JSONObject cuerpo,
            Response.Listener<JSONObject> alResponder, Response.ErrorListener alFallar) {
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
        solicitud.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1f));
        cola(aplicacion).add(solicitud);
    }

    private static synchronized RequestQueue cola(Context contexto) {
        if (cola == null) {
            cola = Volley.newRequestQueue(contexto);
        }
        return cola;
    }
}
