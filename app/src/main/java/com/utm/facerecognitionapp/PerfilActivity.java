package com.utm.facerecognitionapp;

import android.os.Bundle;
import android.widget.TextView;
import android.content.SharedPreferences; // ← Este es el que faltaba

import androidx.appcompat.app.AppCompatActivity;

public class PerfilActivity extends AppCompatActivity {

    TextView txtNombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        txtNombre = findViewById(R.id.txtNombre);

        SharedPreferences prefs = getSharedPreferences("usuario", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");

        txtNombre.setText("@ " + nombre);
    }
}
