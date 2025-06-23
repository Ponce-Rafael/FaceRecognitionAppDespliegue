package com.utm.facerecognitionapp;

import com.utm.facerecognitionapp.AppConfig;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class PedidosActivity extends AppCompatActivity {

    LinearLayout contenedorPedidos;
    int usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedidos);

        contenedorPedidos = findViewById(R.id.contenedorPedidos);

        // ✅ Obtener usuario_id de SharedPreferences
        usuarioId = getSharedPreferences("usuario", MODE_PRIVATE).getInt("usuario_id", -1);
        if (usuarioId == -1) {
            Toast.makeText(this, "⚠️ Usuario no identificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        obtenerPedidosDesdeServidor();
    }

    private void obtenerPedidosDesdeServidor() {
        String url = AppConfig.BASE_URL + "/ordenes/" + usuarioId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray ordenes = response.getJSONArray("ordenes");

                        for (int i = 0; i < ordenes.length(); i++) {
                            JSONObject orden = ordenes.getJSONObject(i);

//                            int id = orden.getInt("id");
                            String fecha = orden.getString("fecha");
                            double total = orden.getDouble("total");

                            LinearLayout pedidoLayout = new LinearLayout(this);
                            pedidoLayout.setOrientation(LinearLayout.VERTICAL);
                            pedidoLayout.setPadding(20, 20, 20, 20);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(0, 0, 0, 30);
                            pedidoLayout.setLayoutParams(params);
                            pedidoLayout.setBackgroundColor(0x33FFFFFF);

                            TextView txtPedido = new TextView(this);
                            txtPedido.setText("Fecha: " + (fecha));
                            txtPedido.setTextColor(0xFFFFFFFF);
                            txtPedido.setTypeface(null, Typeface.BOLD);
                            txtPedido.setTextSize(16);
                            pedidoLayout.addView(txtPedido);

                            JSONArray items = orden.getJSONArray("items");
                            for (int j = 0; j < items.length(); j++) {
                                JSONObject item = items.getJSONObject(j);
                                String nombre = item.getString("producto");
                                int cantidad = item.getInt("cantidad");
                                double subtotal = item.getDouble("subtotal");

                                TextView txtItem = new TextView(this);
                                txtItem.setText("- " + nombre + "  x " + cantidad + " - $" + subtotal);
                                txtItem.setTextColor(0xFFCCCCCC);
                                txtItem.setTextSize(14);
                                pedidoLayout.addView(txtItem);
                            }

                            TextView txtTotal = new TextView(this);
                            txtTotal.setText("Total: $" + total + " ");
                            txtTotal.setTextColor(0xFFFFFFFF);
                            txtTotal.setTypeface(null, Typeface.BOLD_ITALIC);
                            txtTotal.setTextSize(14);
                            txtTotal.setGravity(Gravity.END);
                            pedidoLayout.addView(txtTotal);

                            contenedorPedidos.addView(pedidoLayout);
                        }

                    } catch (Exception e) {
                        Toast.makeText(this, "❌ Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "❌ Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

}
