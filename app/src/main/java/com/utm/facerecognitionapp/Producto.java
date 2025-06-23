package com.utm.facerecognitionapp;

import android.graphics.Bitmap;

public class Producto {
    private String nombre;
    private String descripcion;
    private double precio;
    private Bitmap imagen;

    public Producto(String nombre, String descripcion, double precio, Bitmap imagen) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.imagen = imagen;
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public double getPrecio() { return precio; }
    public Bitmap getImagen() { return imagen; }
}
