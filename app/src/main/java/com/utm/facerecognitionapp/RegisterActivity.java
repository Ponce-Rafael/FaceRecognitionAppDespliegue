package com.utm.facerecognitionapp;

import com.utm.facerecognitionapp.AppConfig;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends Activity {

    EditText etNombre;
    Button btnSignUp;
    ImageButton btnScanFace;
    TextView tvGoLogin;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri imageUri;
    File imageFile;
    private boolean yaRegistrado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.etNombre);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnScanFace = findViewById(R.id.btnScanFace);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        // Subrayar y poner en rojo el texto "Log in"
        TextView tvSignUp = findViewById(R.id.tvGoLogin);
        SpannableString content = new SpannableString("Log in");
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        tvSignUp.setText(content);

        // Ir a Login
        tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        // Escanear rostro
        btnScanFace.setOnClickListener(v -> {
            try {
                imageFile = File.createTempFile("face_", ".jpg", getExternalCacheDir());
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "❌ Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
            }
        });

        btnSignUp.setOnClickListener(v -> {
            if (yaRegistrado) return;
            yaRegistrado = true;
            prepararRegistro();
        });
    }

    private void prepararRegistro() {
        String nombre = etNombre.getText().toString().trim();

        if (nombre.isEmpty()) {
            etNombre.setError("Ingrese un Nombre Válido");
            yaRegistrado = false;
            return;
        }

        if (imageFile == null || !imageFile.exists()) {
            Toast.makeText(this, "⚠️ Debe escanear el rostro primero", Toast.LENGTH_SHORT).show();
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
        Bitmap resized = Bitmap.createScaledBitmap(rgbBitmap, 800, 800, true);
        String imagenBase64 = convertirABase64(resized);

        if (imagenBase64 == null || imagenBase64.isEmpty()) {
            Toast.makeText(this, "⚠️ Imagen vacía. Escanee nuevamente.", Toast.LENGTH_SHORT).show();
            yaRegistrado = false;
            return;
        }

        enviarDatosAlServidor(nombre, imagenBase64);
    }

    private String convertirABase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imagenBytes = stream.toByteArray();
        return Base64.encodeToString(imagenBytes, Base64.NO_WRAP);
    }

    private void enviarDatosAlServidor(String nombre, String imagenBase64) {

        String URL = AppConfig.BASE_URL + "/registro";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        String mensaje = json.optString("mensaje", "✅ Registro exitoso");
                        Toast.makeText(RegisterActivity.this, mensaje, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this, "❌ Error interpretando respuesta", Toast.LENGTH_SHORT).show();
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
                try {
                    Map<String, String> datos = new HashMap<>();
                    datos.put("nombre", nombre);
                    datos.put("imagen", imagenBase64);
                    JSONObject jsonObject = new JSONObject(datos);
                    return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

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
