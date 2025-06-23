package com.utm.facerecognitionapp;

import com.utm.facerecognitionapp.AppConfig;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.*;
import com.android.volley.toolbox.*;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;

public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder> {

    private Context context;
    private ArrayList<JSONObject> lista;
    private int usuarioId;
    private Runnable onEliminar;

    public CarritoAdapter(Context context, ArrayList<JSONObject> lista, int usuarioId, Runnable onEliminar) {
        this.context = context;
        this.lista = lista;
        this.usuarioId = usuarioId;
        this.onEliminar = onEliminar;
    }

    @NonNull
    @Override
    public CarritoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_carrito, parent, false);
        return new CarritoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarritoViewHolder holder, int position) {
        JSONObject item = lista.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    class CarritoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProducto;
        TextView txtNombre, txtCantidadSubtotal;
        ImageButton btnEliminar;

        CarritoViewHolder(View itemView) {
            super(itemView);
            imgProducto = itemView.findViewById(R.id.imgProducto);
            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtCantidadSubtotal = itemView.findViewById(R.id.txtCantidadSubtotal);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }

        void bind(JSONObject item) {
            try {
                txtNombre.setText(item.getString("nombre"));
                txtCantidadSubtotal.setText("x" + item.getInt("cantidad") + " - $" + item.getDouble("subtotal"));

                String base64Img = item.getString("imagen");
                byte[] decoded = Base64.getDecoder().decode(base64Img);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                imgProducto.setImageBitmap(bitmap);

                btnEliminar.setOnClickListener(v -> {
                    int carritoId = item.optInt("carrito_id");
                    eliminarDelCarrito(carritoId);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void eliminarDelCarrito(int carritoId) {
            String url = AppConfig.BASE_URL + "/carrito/eliminar";
            JSONObject json = new JSONObject();
            try {
                json.put("carrito_id", carritoId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json,
                    response -> {
                        Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show();
                        onEliminar.run();  // vuelve a cargar el carrito
                    },
                    error -> Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
            );

            Volley.newRequestQueue(context).add(request);
        }
    }
}
