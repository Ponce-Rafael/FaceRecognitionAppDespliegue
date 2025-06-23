package com.utm.facerecognitionapp;

import com.utm.facerecognitionapp.AppConfig;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;

public class FavoritosActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ProductoAdapter adapter;
    ArrayList<Producto> listaFavoritos;
    HashSet<String> productosAgregados;
    int usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoritos);

        recyclerView = findViewById(R.id.recyclerFavoritos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listaFavoritos = new ArrayList<>();
        productosAgregados = new HashSet<>();

        // ✅ Obtener ID del usuario desde SharedPreferences
        usuarioId = getSharedPreferences("usuario", MODE_PRIVATE).getInt("usuario_id", -1); // ✅ después

        if (usuarioId == -1) {
            Toast.makeText(this, "⚠️ Usuario no identificado", Toast.LENGTH_SHORT).show();
            finish(); // cierra la actividad si no hay ID
            return;
        }

        // ✅ Enviar el usuarioId al adapter
        adapter = new ProductoAdapter(this, listaFavoritos, usuarioId);
        recyclerView.setAdapter(adapter);

        // ✅ Cargar favoritos desde el backend
        cargarFavoritos(usuarioId);
    }

    private void cargarFavoritos(int idUsuario) {
        String url = AppConfig.BASE_URL + "/favoritos/" + idUsuario;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray productos = response.getJSONArray("items");
                        for (int i = 0; i < productos.length(); i++) {
                            JSONObject item = productos.getJSONObject(i);
                            String nombre = item.getString("nombre");

                            // ✅ Evitar productos duplicados
                            if (!productosAgregados.contains(nombre)) {
                                String descripcion = item.optString("descripcion", "");
                                double precio = item.getDouble("precio");
                                String imagenBase64 = item.getString("imagen");

                                byte[] imageBytes = Base64.decode(imagenBase64, Base64.DEFAULT);
                                Bitmap imagen = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                                listaFavoritos.add(new Producto(nombre, descripcion, precio, imagen));
                                productosAgregados.add(nombre);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "⚠️ Error al procesar datos", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "❌ Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }
}
