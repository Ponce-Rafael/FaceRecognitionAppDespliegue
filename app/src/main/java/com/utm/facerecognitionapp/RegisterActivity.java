package com.utm.facerecognitionapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends Activity {

    EditText inputUsername;
    Button btnRegister;
    ImageButton btnScanFace;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri imageUri;
    File imageFile;
    private boolean yaRegistrado = false; // Flag para evitar múltiples envíos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputUsername = findViewById(R.id.inputUsername);
        btnRegister = findViewById(R.id.btnRegister);
        btnScanFace = findViewById(R.id.btnScanFace);

        btnScanFace.setOnClickListener(v -> {
            try {
                imageFile = File.createTempFile("face_", ".jpg", getExternalCacheDir());
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
            }
        });

        btnRegister.setOnClickListener(v -> {
            if (yaRegistrado) return;
            yaRegistrado = true;
            prepararRegistro();
        });
    }

    private void prepararRegistro() {
        String username = inputUsername.getText().toString().trim();

        if (username.isEmpty()) {
            inputUsername.setError("Ingrese un Nombre Válido");
            yaRegistrado = false;
            return;
        }

        if (imageFile == null || !imageFile.exists()) {
            Toast.makeText(this, "Debe escanear el rostro primero", Toast.LENGTH_SHORT).show();
            yaRegistrado = false;
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

        if (bitmap == null) {
            Toast.makeText(this, "❌ Error al cargar la imagen. Intente nuevamente.", Toast.LENGTH_LONG).show();
            yaRegistrado = false;
            return;
        }

        Bitmap rgbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resized = Bitmap.createScaledBitmap(rgbBitmap, 500, 500, true);
        String imagenBase64 = convertirABase64(resized);

        enviarDatosAlServidor(username, imagenBase64);
    }

    private String convertirABase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imagenBytes = stream.toByteArray();
        return Base64.encodeToString(imagenBytes, Base64.NO_WRAP);
    }

    private void enviarDatosAlServidor(String nombre, String imagenBase64) {
        String URL = "http://192.168.1.101:5000/registro";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        String mensaje = json.optString("mensaje", "✅ Registro exitoso");
                        Toast.makeText(RegisterActivity.this, mensaje, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this, "Error al interpretar respuesta del servidor", Toast.LENGTH_SHORT).show();
                    } finally {
                        yaRegistrado = false;
                    }
                },
                error -> {
                    Toast.makeText(RegisterActivity.this, "❌ Error al registrar: " + error.toString(), Toast.LENGTH_LONG).show();
                    yaRegistrado = false;
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                Map<String, String> datos = new HashMap<>();
                datos.put("nombre", nombre);
                datos.put("imagen", imagenBase64);

                JSONObject jsonObject = new JSONObject(datos);
                return jsonObject.toString().getBytes();
            }
        };

        queue.add(request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

            if (bitmap != null) {
                Toast.makeText(this, "✅ Imagen capturada correctamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ Error al procesar la imagen capturada", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
