import sqlite3
import os
from datetime import datetime

base_dir = os.path.dirname(__file__)
DB_PATH = os.path.join(base_dir, "database.db")

# def borrar_todos_los_productos():
#     conn = sqlite3.connect(DB_PATH)
#     cursor = conn.cursor()
#     cursor.execute("DELETE FROM orden_detalle")
#     conn.commit()
#     conn.close()
#     print("🗑️ Todos los productos eliminados.")

def insertar_producto(nombre, precio, ruta_imagen, categoria):
    with open(ruta_imagen, "rb") as f:
        imagen = f.read()

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        INSERT INTO productos (nombre, precio, imagen, categoria, fecha_registro)
        VALUES (?, ?, ?, ?, ?)
    ''', (nombre, precio, imagen, categoria, datetime.now().isoformat()))
    conn.commit()
    conn.close()
    print(f"✅ Producto '{nombre}' insertado correctamente.")

# def actualizar_precio_producto():
#     conn = sqlite3.connect(DB_PATH)
#     cursor = conn.cursor()
#     cursor.execute("UPDATE productos SET precio = 24.99 WHERE id = 5")
#     conn.commit()
#     conn.close()
#     print("Producto Actualizado.")


if __name__ == "__main__":

    # Borrar productos existentes
    # borrar_todos_los_productos()

    # Actualizar precios de productos
    # actualizar_precio_producto()

    # Insertar solo los nuevos
    
    # insertar_producto(
    #     "Camisa Negra",
    #     5.50,
    #     os.path.join(base_dir, "image", "ropa", "camisaNegra.png"),
    #     "accesorios"
    # )

    insertar_producto(
        "Proteina Whey 2 kg",
        82.99,
        os.path.join(base_dir, "image", "suplementos", "protein.pngggg"),
        "suplementos"
    )

