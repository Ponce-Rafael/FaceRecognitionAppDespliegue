import sqlite3
from datetime import datetime
import os

DB_PATH = os.path.join(os.path.dirname(__file__), "database.db")

# -------------------------
# TABLAS
# -------------------------

def crear_tabla_usuarios():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS usuarios (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL,
            rostro_vector BLOB NOT NULL,
            rostro_imagen BLOB, 
            fecha_registro TEXT
        )
    ''')
    conn.commit()
    conn.close()

def crear_tabla_productos():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS productos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL,
            precio REAL NOT NULL,
            imagen BLOB,
            categoria TEXT NOT NULL CHECK(categoria IN ('suplementos', 'accesorios')),
            fecha_registro TEXT
        )
    ''')
    conn.commit()
    conn.close()

def crear_tabla_favoritos():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS favoritos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            usuario_id INTEGER NOT NULL,
            producto_id INTEGER NOT NULL,
            fecha TEXT,
            UNIQUE(usuario_id, producto_id),
            FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
            FOREIGN KEY (producto_id) REFERENCES productos(id)
        )
    ''')
    conn.commit()
    conn.close()

def crear_tabla_carrito():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS carrito (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            usuario_id INTEGER NOT NULL,
            producto_id INTEGER NOT NULL,
            cantidad INTEGER NOT NULL,
            subtotal REAL NOT NULL,
            fecha TEXT,
            FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
            FOREIGN KEY (producto_id) REFERENCES productos(id)
        )
    ''')
    conn.commit()
    conn.close()

def crear_tabla_ordenes():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS ordenes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            usuario_id INTEGER NOT NULL,
            total REAL NOT NULL,
            fecha TEXT NOT NULL,
            FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        )
    ''')
    conn.commit()
    conn.close()

def crear_tabla_orden_detalle():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS orden_detalle (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            orden_id INTEGER NOT NULL,
            producto_id INTEGER NOT NULL,
            cantidad INTEGER NOT NULL,
            subtotal REAL NOT NULL,
            FOREIGN KEY (orden_id) REFERENCES ordenes(id),
            FOREIGN KEY (producto_id) REFERENCES productos(id)
        )
    ''')
    conn.commit()
    conn.close()

# -------------------------
# INSERCIONES
# -------------------------

def insertar_usuario(nombre, rostro_vector, rostro_imagen):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        INSERT INTO usuarios (nombre, rostro_vector, rostro_imagen, fecha_registro)
        VALUES (?, ?, ?, ?)
    ''', (nombre, rostro_vector, rostro_imagen, datetime.now().isoformat()))
    user_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return user_id

def insertar_producto(nombre, precio, imagen, categoria):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        INSERT INTO productos (nombre, precio, imagen, categoria, fecha_registro)
        VALUES (?, ?, ?, ?, ?)
    ''', (nombre, precio, imagen, categoria, datetime.now().isoformat()))
    producto_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return producto_id

def insertar_a_favoritos(usuario_id, producto_id):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    try:
        cursor.execute('''
            INSERT INTO favoritos (usuario_id, producto_id, fecha)
            VALUES (?, ?, ?)
        ''', (usuario_id, producto_id, datetime.now().isoformat()))
        conn.commit()
    except sqlite3.IntegrityError:
        print("Este producto ya está en favoritos.")
    finally:
        conn.close()

def insertar_al_carrito(usuario_id, producto_id, cantidad):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Obtener el precio del producto
    cursor.execute("SELECT precio FROM productos WHERE id = ?", (producto_id,))
    row = cursor.fetchone()
    if not row:
        conn.close()
        raise ValueError("Producto no encontrado")

    precio = row[0]

    # Verificar si ya existe el producto en el carrito del usuario
    cursor.execute("SELECT id, cantidad FROM carrito WHERE usuario_id = ? AND producto_id = ?", (usuario_id, producto_id))
    existente = cursor.fetchone()

    if existente:
        carrito_id, cantidad_actual = existente
        nueva_cantidad = cantidad_actual + cantidad
        nuevo_subtotal = round(precio * nueva_cantidad, 2)

        cursor.execute("""
            UPDATE carrito 
            SET cantidad = ?, subtotal = ?, fecha = ?
            WHERE id = ?
        """, (nueva_cantidad, nuevo_subtotal, datetime.now().isoformat(), carrito_id))
    else:
        subtotal = round(precio * cantidad, 2)
        cursor.execute("""
            INSERT INTO carrito (usuario_id, producto_id, cantidad, subtotal, fecha)
            VALUES (?, ?, ?, ?, ?)
        """, (usuario_id, producto_id, cantidad, subtotal, datetime.now().isoformat()))

    conn.commit()
    conn.close()
    return round(precio * cantidad, 2)

# -------------------------
# CONSULTAS
# -------------------------

def obtener_usuarios():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT nombre, rostro_vector FROM usuarios")
    usuarios = cursor.fetchall()
    conn.close()
    return usuarios

def obtener_favoritos(usuario_id):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        SELECT p.id, p.nombre, p.precio, p.categoria
        FROM productos p
        JOIN favoritos f ON p.id = f.producto_id
        WHERE f.usuario_id = ?
    ''', (usuario_id,))
    favoritos = cursor.fetchall()
    conn.close()
    return favoritos

if __name__ == "__main__":
    crear_tabla_usuarios()
    crear_tabla_productos()
    crear_tabla_favoritos()
    crear_tabla_carrito()
    crear_tabla_ordenes()
    crear_tabla_orden_detalle()
    print("Tablas creadas correctamente.")
