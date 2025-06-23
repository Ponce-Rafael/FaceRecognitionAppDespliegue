package com.utm.facerecognitionapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class CarritoActivity extends AppCompatActivity {

    LinearLayout containerCarrito;
    TextView txtTotal;
    Button btnFinalizar;
    RequestQueue queue;
    int usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrito);

        containerCarrito = findViewById(R.id.containerCarrito);
        txtTotal = findViewById(R.id.txtTotal);
        btnFinalizar = findViewById(R.id.btnFinalizarCompra);

        queue = Volley.newRequestQueue(this);
        usuarioId = getSharedPreferences("usuario", MODE_PRIVATE).getInt("id", -1);

        obtenerProductosCarrito();

        btnFinalizar.setOnClickListener(v -> {
            JSONObject json = new JSONObject();
            try {
                json.put("usuario_id", usuarioId);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al generar la orden", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = "http://192.168.1.101:5000/orden/finalizar";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json,
                    response -> {
                        Toast.makeText(this, "✅ Compra finalizada", Toast.LENGTH_SHORT).show();
                        obtenerProductosCarrito();
                    },
                    error -> Toast.makeText(this, "❌ No se pudo finalizar la compra", Toast.LENGTH_SHORT).show()
            );

            queue.add(request);
        });
    }

    private void obtenerProductosCarrito() {
        containerCarrito.removeAllViews();

        JSONObject json = new JSONObject();
        try {
            json.put("usuario_id", usuarioId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String url = "http://192.168.1.101:5000/carrito";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json,
                response -> {
                    try {
                        JSONArray items = response.getJSONArray("items");
                        LayoutInflater inflater = LayoutInflater.from(this);

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            int carritoId = item.getInt("carrito_id");
                            String nombre = item.getString("nombre");
                            int cantidad = item.getInt("cantidad");
                            double subtotal = item.getDouble("subtotal");
                            String base64img = item.getString("imagen");

                            View itemView = inflater.inflate(R.layout.item_carrito, null);

                            ImageView imgProducto = itemView.findViewById(R.id.imgProducto);
                            TextView txtNombre = itemView.findViewById(R.id.txtNombre);
                            TextView txtCantidadSubtotal = itemView.findViewById(R.id.txtCantidadSubtotal);
                            ImageButton btnEliminar = itemView.findViewById(R.id.btnEliminar);

                            if (!base64img.isEmpty()) {
                                byte[] decoded = Base64.decode(base64img, Base64.DEFAULT);
                                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                imgProducto.setImageBitmap(bmp);
                            }

                            txtNombre.setText(nombre);
                            txtCantidadSubtotal.setText("x" + cantidad + " - $" + subtotal);

                            btnEliminar.setOnClickListener(v -> eliminarItemCarrito(carritoId));

                            containerCarrito.addView(itemView);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "❌ Error al procesar datos", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "❌ No se pudo cargar el carrito", Toast.LENGTH_SHORT).show()
        );

        String urlTotal = "http://192.168.1.101:5000/carrito/total";
        JsonObjectRequest totalRequest = new JsonObjectRequest(Request.Method.POST, urlTotal, json,
                response -> {
                    double total = response.optDouble("total", 0.0);
                    txtTotal.setText("Total: $" + total);
                },
                error -> txtTotal.setText("Total: $0.00")
        );

        queue.add(request);
        queue.add(totalRequest);
    }

    private void eliminarItemCarrito(int carritoId) {
        String url = "http://192.168.1.101:5000/carrito/eliminar/" + carritoId;

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> obtenerProductosCarrito(),
                error -> Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }
}
