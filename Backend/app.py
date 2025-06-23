from flask import Flask, request, jsonify
from datetime import datetime
from db_manager import *
import face_recognition
from PIL import Image
import numpy as np
import sqlite3
import base64
import cv2
import os
import io

app = Flask(__name__)
base_dir = os.path.dirname(__file__)
DB_NAME = os.path.join(base_dir, "database.db")

# Inicializar tabla al arrancar
crear_tablas()

# --------------------------------------#
# Usuarios
# --------------------------------------#

@app.route('/registro', methods=['POST'])
def registrar_usuario():
    try:
        data = request.get_json()
        nombre = data.get('nombre')
        imagen_base64 = data.get('imagen')

        if not nombre or not imagen_base64:
            return jsonify({'error': 'Faltan datos'}), 400

        img_bytes = base64.b64decode(imagen_base64)
        image_pil = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        image_np = np.array(image_pil)

        face_encodings = face_recognition.face_encodings(image_np)
        if not face_encodings:
            return jsonify({'error': 'No se detectó ningún rostro'}), 400

        image_bgr = cv2.cvtColor(image_np, cv2.COLOR_RGB2BGR)
        _, buffer = cv2.imencode('.jpg', image_bgr)
        rostro_imagen_bytes = buffer.tobytes()

        user_id = insertar_usuario(nombre, face_encodings[0].tobytes(), rostro_imagen_bytes)

        img_filename = f"{user_id}_{nombre}.jpg"
        rostros_dir = os.path.join(base_dir, "rostros")
        os.makedirs(rostros_dir, exist_ok=True)
        img_path = os.path.join(rostros_dir, img_filename)
        with open(img_path, 'wb') as f:
            f.write(rostro_imagen_bytes)

        return jsonify({'mensaje': '✅ Rostro registrado correctamente'}), 200
    except Exception as e:
        print("❌ ERROR EN /registro:", str(e))
        return jsonify({'error': str(e)}), 500

@app.route('/verificar', methods=['POST'])
def verificar_usuario():
    try:
        data = request.get_json()
        imagen_base64 = data.get('imagen')

        if not imagen_base64:
            return jsonify({'error': 'Falta la imagen'}), 400

        img_bytes = base64.b64decode(imagen_base64)
        image_pil = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        image_np = np.array(image_pil)

        face_encodings = face_recognition.face_encodings(image_np)
        if not face_encodings:
            return jsonify({'mensaje': 'No se detectó ningún rostro'}), 400

        encoding_input = face_encodings[0]
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("SELECT id, nombre, rostro_vector FROM usuarios")
        usuarios = cursor.fetchall()
        conn.close()

        mejores_distancias = []
        for id_usuario, nombre, vec in usuarios:
            distancia = face_recognition.face_distance([np.frombuffer(vec, dtype=np.float64)], encoding_input)[0]
            mejores_distancias.append((id_usuario, nombre, distancia))

        if not mejores_distancias:
            return jsonify({'mensaje': 'No hay usuarios registrados'}), 404

        id_encontrado, nombre_mas_cercano, distancia = min(mejores_distancias, key=lambda x: x[2])

        if distancia < 0.6:
            return jsonify({'mensaje': 'Rostro verificado', 'usuario': nombre_mas_cercano, 'usuario_id': id_encontrado}), 200
        else:
            return jsonify({'mensaje': 'No se encontró coincidencia'}), 401

    except Exception as e:
        return jsonify({'error': str(e)}), 500

# --------------------------------------- #
# Carrito
# --------------------------------------- #

@app.route('/carrito/agregar', methods=['POST'])
def agregar_carrito():
    try:
        data = request.get_json()
        usuario_id = data.get("usuario_id")
        producto_id = data.get("producto_id")
        cantidad = data.get("cantidad", 1)

        if not usuario_id or not producto_id:
            return jsonify({'error': 'Faltan datos'}), 400

        subtotal = insertar_al_carrito(usuario_id, producto_id, cantidad)
        return jsonify({'mensaje': 'Producto agregado al carrito', 'subtotal': subtotal}), 200

    except ValueError as ve:
        return jsonify({'error': str(ve)}), 404
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/carrito', methods=['POST'])
def obtener_carrito():
    try:
        data = request.get_json()
        usuario_id = data.get("usuario_id")

        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("""
            SELECT c.id, p.nombre, p.imagen, c.cantidad, c.subtotal
            FROM carrito c
            JOIN productos p ON c.producto_id = p.id
            WHERE c.usuario_id = ?
        """, (usuario_id,))
        items = cursor.fetchall()
        conn.close()

        resultado = []
        for id_carrito, nombre, imagen_blob, cantidad, subtotal in items:
            imagen_base64 = base64.b64encode(imagen_blob).decode("utf-8") if imagen_blob else ""
            resultado.append({
                "carrito_id": id_carrito,
                "nombre": nombre,
                "cantidad": cantidad,
                "subtotal": subtotal,
                "imagen": imagen_base64
            })

        return jsonify({"items": resultado}), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/carrito/eliminar/<int:carrito_id>', methods=['DELETE'])
def eliminar_item_carrito(carrito_id):
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM carrito WHERE id = ?", (carrito_id,))
        conn.commit()
        conn.close()
        return jsonify({'mensaje': 'Producto eliminado del carrito'}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/carrito/total', methods=['POST'])
def total_carrito():
    try:
        data = request.get_json()
        usuario_id = data.get("usuario_id")

        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("SELECT SUM(subtotal) FROM carrito WHERE usuario_id = ?", (usuario_id,))
        total = cursor.fetchone()[0] or 0.0
        conn.close()

        return jsonify({'total': round(total, 2)}), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500

# --------------------------------------- #
# Productos
# --------------------------------------- #

@app.route('/productos', methods=['GET'])
def obtener_productos():
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute('SELECT id, nombre, precio, imagen, categoria FROM productos')
    productos = cursor.fetchall()
    conn.close()

    lista = []
    for row in productos:
        id_, nombre, precio, imagen_blob, categoria = row
        imagen_base64 = base64.b64encode(imagen_blob).decode("utf-8") if imagen_blob else ""
        lista.append({
            'id': id_,
            'nombre': nombre,
            'precio': precio,
            'imagen': imagen_base64,
            'categoria': categoria
        })

    return jsonify(lista), 200

@app.route('/productos/categoria/<categoria>', methods=['GET'])
def productos_por_categoria(categoria):
    categoria = categoria.lower()
    if categoria not in ["suplementos", "accesorios"]:
        return jsonify({'error': 'Categoría no permitida'}), 400

    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute('''
        SELECT id, nombre, precio, categoria, imagen
        FROM productos
        WHERE LOWER(categoria) = ?
    ''', (categoria,))
    productos = cursor.fetchall()
    conn.close()

    lista = []
    for row in productos:
        producto = {
            'id': row[0],
            'nombre': row[1],
            'precio': row[2],
            'categoria': row[3],
            'imagen': base64.b64encode(row[4]).decode('utf-8') if row[4] else None
        }
        lista.append(producto)

    return jsonify(lista), 200

@app.route('/productos/buscar/<texto>', methods=['GET'])
def buscar_productos(texto):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute('''
        SELECT id, nombre, precio, categoria, imagen 
        FROM productos 
        WHERE LOWER(nombre) LIKE LOWER(?)
    ''', (f'%{texto}%',))
    productos = cursor.fetchall()
    conn.close()

    lista = []
    for row in productos:
        id_, nombre, precio, categoria, imagen = row
        base64_img = base64.b64encode(imagen).decode('utf-8') if imagen else ''
        lista.append({
            'id': id_,
            'nombre': nombre,
            'precio': precio,
            'categoria': categoria,
            'imagen': base64_img
        })

    return jsonify(lista), 200

# --------------------------------------- #
# Favoritos
# --------------------------------------- #

@app.route('/favoritos/agregar', methods=['POST'])
def agregar_favorito():
    data = request.get_json()
    usuario_id = data.get("usuario_id")
    producto_id = data.get("producto_id")

    if not usuario_id or not producto_id:
        return jsonify({"error": "Faltan datos"}), 400

    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute("SELECT 1 FROM favoritos WHERE usuario_id=? AND producto_id=?", (usuario_id, producto_id))
    if cursor.fetchone():
        conn.close()
        return jsonify({"mensaje": "Ya está en favoritos"}), 200

    cursor.execute("INSERT INTO favoritos (usuario_id, producto_id) VALUES (?, ?)", (usuario_id, producto_id))
    conn.commit()
    conn.close()
    return jsonify({"mensaje": "Agregado a favoritos"}), 200

@app.route('/favoritos/<int:usuario_id>', methods=['GET'])
def obtener_favoritos(usuario_id):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute("""
        SELECT p.nombre, p.precio, p.imagen
        FROM favoritos f
        JOIN productos p ON f.producto_id = p.id
        WHERE f.usuario_id = ?
    """, (usuario_id,))
    productos = cursor.fetchall()
    conn.close()

    lista = []
    for nombre, precio, imagen_bytes in productos:
        imagen_base64 = base64.b64encode(imagen_bytes).decode('utf-8') if imagen_bytes else ''
        lista.append({
            "nombre": nombre,
            "precio": precio,
            "imagen": imagen_base64
        })

    return jsonify({"items": lista})

@app.route('/favoritos/eliminar/<int:usuario_id>/<string:nombre_producto>', methods=['DELETE'])
def eliminar_favorito(usuario_id, nombre_producto):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute("""
        DELETE FROM favoritos
        WHERE usuario_id = ? AND producto_id IN (
            SELECT id FROM productos WHERE nombre = ?
        )
    """, (usuario_id, nombre_producto))
    conn.commit()
    conn.close()
    return jsonify({"mensaje": "Favorito eliminado"}), 200


# --------------------------------------- #
# Ordenes
# --------------------------------------- #

@app.route('/ordenes/<int:usuario_id>', methods=['GET'])
def obtener_ordenes(usuario_id):
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT o.id, o.fecha, o.total, p.nombre, od.cantidad, od.subtotal
            FROM ordenes o
            JOIN orden_detalle od ON o.id = od.orden_id
            JOIN productos p ON od.producto_id = p.id
            WHERE o.usuario_id = ?
            ORDER BY o.fecha DESC
        """, (usuario_id,))
        resultados = cursor.fetchall()
        conn.close()

        ordenes_dict = {}
        for orden_id, fecha_str, total, producto, cantidad, subtotal in resultados:

            # ✅ Formatear fecha en español
            fecha_obj = datetime.fromisoformat(fecha_str)
            fecha_formateada = fecha_obj.strftime("%d de %B de %Y, %I:%M %p")

            if orden_id not in ordenes_dict:
                ordenes_dict[orden_id] = {
                    "id": orden_id,
                    "fecha": fecha_formateada,
                    "total": round(total, 2),
                    "items": []
                }

            ordenes_dict[orden_id]["items"].append({
                "producto": producto,
                "cantidad": cantidad,
                "subtotal": round(subtotal, 2)
            })

        return jsonify({"ordenes": list(ordenes_dict.values())}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/orden/finalizar', methods=['POST'])
def finalizar_orden():
    try:
        data = request.get_json()
        usuario_id = data.get("usuario_id")

        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT producto_id, cantidad, subtotal
            FROM carrito
            WHERE usuario_id = ?
        """, (usuario_id,))
        items = cursor.fetchall()

        if not items:
            return jsonify({"error": "El carrito está vacío"}), 400

        total = round(sum(sub for _, _, sub in items), 2)
        fecha = datetime.now().isoformat()
        cursor.execute("""
            INSERT INTO ordenes (usuario_id, total, fecha)
            VALUES (?, ?, ?)
        """, (usuario_id, total, fecha))
        orden_id = cursor.lastrowid

        for producto_id, cantidad, subtotal in items:
            cursor.execute("""
                INSERT INTO orden_detalle (orden_id, producto_id, cantidad, subtotal)
                VALUES (?, ?, ?, ?)
            """, (orden_id, producto_id, cantidad, round(subtotal, 2)))

        cursor.execute("DELETE FROM carrito WHERE usuario_id = ?", (usuario_id,))
        conn.commit()
        conn.close()
        return jsonify({"mensaje": "Orden finalizada con éxito"}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

    


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
