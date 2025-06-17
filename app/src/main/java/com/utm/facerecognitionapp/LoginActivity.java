package com.utm.facerecognitionapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
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
    static final int REQUEST_IMAGE_CAPTURE = 2;
    Bitmap faceBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnScanFace = findViewById(R.id.btnScanFace);

        btnScanFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            faceBitmap = (Bitmap) extras.get("data");

            if (faceBitmap != null) {
                Toast.makeText(this, "✅ Rostro Capturado", Toast.LENGTH_SHORT).show();
                enviarRostroAlServidor(faceBitmap);
            } else {
                Toast.makeText(this, "❌ No se Pudo Capturar el Rostro", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enviarRostroAlServidor(Bitmap bitmap) {
        String url = "http://192.168.1.101:5000/verificar"; // IP de tu backend

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
                            Toast.makeText(LoginActivity.this, "Bienvenido: " + nombre, Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                            intent.putExtra("nombre", nombre);
                            startActivity(intent);
                            finish();

                        } else if (json.has("mensaje")) {
                            String mensaje = json.getString("mensaje");
                            Toast.makeText(LoginActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Respuesta inesperada: " + response, Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "Error al procesar respuesta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
