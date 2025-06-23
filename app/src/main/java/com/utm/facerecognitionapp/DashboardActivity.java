package com.utm.facerecognitionapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

public class DashboardActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ImageButton btnMenu, btnNavCart, btnCarrito;
    LinearLayout containerProductos;
    TextView tabSupplements, tabAccessories;
    EditText etBuscar;

    private String categoriaActual = "suplementos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        btnNavCart = findViewById(R.id.btnNavCart);
        btnCarrito = findViewById(R.id.btnCarrito);
        containerProductos = findViewById(R.id.containerProductos);
        tabSupplements = findViewById(R.id.tabSupplements);
        tabAccessories = findViewById(R.id.tabAccessories);
        etBuscar = findViewById(R.id.etBuscar);

        NavigationView navView = findViewById(R.id.navView);
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.item_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
            } else if (id == R.id.item_pedidos) {
                startActivity(new Intent(this, PedidosActivity.class));
            } else if (id == R.id.item_favoritos) {
                startActivity(new Intent(this, FavoritosActivity.class));
            } else if (id == R.id.item_logout) {
                getSharedPreferences("usuario", MODE_PRIVATE).edit().clear().apply();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnCarrito.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CarritoActivity.class);
            startActivity(intent);
        });

        tabSupplements.setOnClickListener(v -> {
            categoriaActual = "suplementos";
            cargarProductosPorCategoria(categoriaActual);
            tabSupplements.setTextColor(Color.parseColor("#FF4444"));
            tabAccessories.setTextColor(Color.parseColor("#AAAAAA"));
        });

        tabAccessories.setOnClickListener(v -> {
            categoriaActual = "accesorios";
            cargarProductosPorCategoria(categoriaActual);
            tabAccessories.setTextColor(Color.parseColor("#FF4444"));
            tabSupplements.setTextColor(Color.parseColor("#AAAAAA"));
        });

        etBuscar.setOnEditorActionListener((v, actionId, event) -> {
            String texto = etBuscar.getText().toString().trim();
            if (!texto.isEmpty()) {
                buscarProductos(texto);
            } else {
                cargarProductosPorCategoria(categoriaActual);
            }
            return true;
        });

        btnNavCart.setOnClickListener(v -> {
            cargarProductosPorCategoria("suplementos");
            tabSupplements.setTextColor(Color.parseColor("#FF4444"));
            tabAccessories.setTextColor(Color.parseColor("#AAAAAA"));
            etBuscar.setText("");
        });

        cargarProductosPorCategoria(categoriaActual);
        tabSupplements.setTextColor(Color.parseColor("#FF4444"));
    }

    private void cargarProductosPorCategoria(String categoria) {
        String url = "http://192.168.1.101:5000/productos/categoria/" + categoria;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    containerProductos.removeAllViews();
                    try {
                        int usuarioId = getSharedPreferences("usuario", MODE_PRIVATE).getInt("id", -1);

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject prod = response.getJSONObject(i);
                            int productoId = prod.getInt("id");
                            String nombre = prod.getString("nombre");
                            double precio = prod.getDouble("precio");
                            String base64Imagen = prod.optString("imagen");

                            View card = getLayoutInflater().inflate(R.layout.item_producto_dinamico, containerProductos, false);

                            TextView txtNombre = card.findViewById(R.id.txtNombre);
                            TextView txtPrecio = card.findViewById(R.id.txtPrecio);
                            ImageView imgProducto = card.findViewById(R.id.imgProducto);
                            ImageButton btnFavorito = card.findViewById(R.id.btnFavorito);
                            ImageButton btnAgregarCarrito = card.findViewById(R.id.btnAgregarCarrito);

                            txtNombre.setText(nombre);
                            txtPrecio.setText("$" + precio);

                            if (base64Imagen != null && !base64Imagen.isEmpty() && !base64Imagen.equals("null")) {
                                byte[] decodedBytes = Base64.decode(base64Imagen, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                imgProducto.setImageBitmap(bitmap);
                            } else {
                                imgProducto.setImageResource(R.drawable.protein);
                            }

                            btnFavorito.setOnClickListener(v -> {
                                String urlFav = "http://192.168.1.101:5000/favoritos/agregar";
                                JSONObject json = new JSONObject();
                                try {
                                    json.put("usuario_id", usuarioId);
                                    json.put("producto_id", productoId);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                JsonObjectRequest favRequest = new JsonObjectRequest(Request.Method.POST, urlFav, json,
                                        response1 -> Toast.makeText(this, "✅ Agregado a favoritos", Toast.LENGTH_SHORT).show(),
                                        error -> Toast.makeText(this, "❌ Error al agregar favorito", Toast.LENGTH_SHORT).show()
                                );
                                queue.add(favRequest);
                            });

                            btnAgregarCarrito.setOnClickListener(v -> {
                                String urlCarrito = "http://192.168.1.101:5000/carrito/agregar";
                                JSONObject json = new JSONObject();
                                try {
                                    json.put("usuario_id", usuarioId);
                                    json.put("producto_id", productoId);
                                    json.put("cantidad", 1);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                JsonObjectRequest cartRequest = new JsonObjectRequest(Request.Method.POST, urlCarrito, json,
                                        response2 -> Toast.makeText(this, "✅ Producto agregado al carrito", Toast.LENGTH_SHORT).show(),
                                        error -> Toast.makeText(this, "❌ Error al agregar al carrito", Toast.LENGTH_SHORT).show()
                                );
                                queue.add(cartRequest);
                            });

                            containerProductos.addView(card);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "⚠️ Error al procesar productos", Toast.LENGTH_SHORT).show();
                        Log.e("PRODUCTOS_ERROR", e.toString());
                    }
                },
                error -> {
                    Toast.makeText(this, "❌ Error al obtener productos", Toast.LENGTH_SHORT).show();
                    Log.e("PRODUCTOS_REQUEST", error.toString());
                });

        queue.add(request);
    }

    private void buscarProductos(String texto) {
        String url = "http://192.168.1.101:5000/productos/buscar/" + texto;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    containerProductos.removeAllViews();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject prod = response.getJSONObject(i);
                            String nombre = prod.getString("nombre");
                            double precio = prod.getDouble("precio");
                            String base64Imagen = prod.optString("imagen");

                            View card = getLayoutInflater().inflate(R.layout.item_producto_dinamico, containerProductos, false);

                            TextView txtNombre = card.findViewById(R.id.txtNombre);
                            TextView txtPrecio = card.findViewById(R.id.txtPrecio);
                            ImageView imgProducto = card.findViewById(R.id.imgProducto);

                            txtNombre.setText(nombre);
                            txtPrecio.setText("$" + precio);

                            if (base64Imagen != null && !base64Imagen.isEmpty() && !base64Imagen.equals("null")) {
                                byte[] decodedBytes = Base64.decode(base64Imagen, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                imgProducto.setImageBitmap(bitmap);
                            } else {
                                imgProducto.setImageResource(R.drawable.protein);
                            }

                            containerProductos.addView(card);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "⚠️ Error al procesar búsqueda", Toast.LENGTH_SHORT).show();
                        Log.e("BUSQUEDA_ERROR", e.toString());
                    }
                },
                error -> {
                    Toast.makeText(this, "❌ Error al buscar productos", Toast.LENGTH_SHORT).show();
                    Log.e("BUSQUEDA_REQUEST", error.toString());
                });

        queue.add(request);
    }
}
