package com.example.aforoiot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public final class DialogoServidor {
    private DialogoServidor() {
    }

    public static void mostrar(Context contexto, Runnable alGuardar) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.dialog_servidor, null);
        TextInputLayout tilServidor = vista.findViewById(R.id.tilServidor);
        TextInputEditText etServidor = vista.findViewById(R.id.etServidor);
        etServidor.setText(Config.baseUrl(contexto));
        etServidor.setSelection(etServidor.length());

        AlertDialog dialogo = new MaterialAlertDialogBuilder(contexto)
                .setTitle(R.string.servidor_titulo)
                .setView(vista)
                .setPositiveButton(R.string.guardar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();

        dialogo.setOnShowListener(ignorado -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String texto = etServidor.getText() == null
                            ? "" : etServidor.getText().toString();
                    String url = Config.normalizarBaseUrl(texto);
                    if (url == null) {
                        tilServidor.setError(contexto.getString(R.string.servidor_invalido));
                        return;
                    }
                    Config.guardarBaseUrl(contexto, url);
                    dialogo.dismiss();
                    if (alGuardar != null) {
                        alGuardar.run();
                    }
                }));

        dialogo.show();
    }
}
