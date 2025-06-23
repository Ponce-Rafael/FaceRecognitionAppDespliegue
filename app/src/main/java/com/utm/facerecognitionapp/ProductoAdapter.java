package com.utm.facerecognitionapp;

import com.utm.facerecognitionapp.AppConfig;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Producto> productos;
    private int usuarioId;

    public ProductoAdapter(Context context, ArrayList<Producto> productos, int usuarioId) {
        this.context = context;
        this.productos = productos;
        this.usuarioId = usuarioId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorito, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Producto producto = productos.get(position);

        holder.txtNombre.setText(producto.getNombre());
        holder.txtPrecio.setText("$" + producto.getPrecio());
        holder.imgProducto.setImageBitmap(producto.getImagen());

        holder.btnEliminar.setOnClickListener(v -> {
            eliminarFavorito(producto.getNombre(), position);
        });
    }

    @Override
    public int getItemCount() {
        return productos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtPrecio;
        ImageView imgProducto;
        ImageButton btnEliminar;

        public ViewHolder(View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtPrecio = itemView.findViewById(R.id.txtPrecio);
            imgProducto = itemView.findViewById(R.id.imgProducto);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }

    private void eliminarFavorito(String nombreProducto, int position) {
        String url = AppConfig.BASE_URL + "/favoritos/eliminar/" + usuarioId + "/" + nombreProducto;

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    productos.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "✅ Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(context, "❌ Error al eliminar", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(context).add(request);
    }
}
