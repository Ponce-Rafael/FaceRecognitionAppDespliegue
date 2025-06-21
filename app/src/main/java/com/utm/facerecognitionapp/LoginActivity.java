package com.utm.facerecognitionapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends Activity {

    ImageButton btnScanFace;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    Bitmap faceBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnScanFace = findViewById(R.id.btnScanFace);

        // Usuario presiona botón para escanear
        btnScanFace.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        });

        // Subrayar y poner en rojo "Sign Up"
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        SpannableString content = new SpannableString("Sing Up");
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        tvSignUp.setText(content);
        tvSignUp.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            faceBitmap = (Bitmap) extras.get("data");

            if (faceBitmap != null) {
                Toast.makeText(this, "✅ Rostro capturado", Toast.LENGTH_SHORT).show();
                enviarRostroAlServidor(faceBitmap);
            } else {
                Toast.makeText(this, "❌ No se pudo capturar el rostro", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enviarRostroAlServidor(Bitmap bitmap) {
        String url = "http://192.168.1.101:5000/verificar";

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 500, 500, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        String imagenBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);

                        if (json.has("usuario")) {
                            String nombre = json.getString("usuario");
                            // Toast.makeText(LoginActivity.this, "✅ Bienvenido: " + nombre, Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                            intent.putExtra("nombre", nombre);
                            startActivity(intent);
                            finish();

                        } else if (json.has("mensaje")) {
                            String mensaje = json.getString("mensaje");

                            switch (mensaje) {
                                case "No se detectó ningún rostro":
                                    Toast.makeText(LoginActivity.this, "❌ Rostro no detectado. Intenta de nuevo.", Toast.LENGTH_LONG).show();
                                    break;
                                case "No se encontró coincidencia":
                                    Toast.makeText(LoginActivity.this, "⚠️ Rostro no reconocido. No existe en el sistema.", Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    Toast.makeText(LoginActivity.this, "ℹ️ " + mensaje, Toast.LENGTH_LONG).show();
                            }

                        } else {
                            Toast.makeText(LoginActivity.this, "⚠️ Respuesta inesperada del servidor", Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "❌ Error procesando la respuesta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(LoginActivity.this, "❌ Error de conexión o servidor", Toast.LENGTH_LONG).show()
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                Map<String, String> datos = new HashMap<>();
                datos.put("imagen", imagenBase64);
                JSONObject objeto = new JSONObject(datos);
                return objeto.toString().getBytes();
            }
        };

        queue.add(request);
    }
}
