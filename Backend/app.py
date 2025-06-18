from flask import Flask, request, jsonify
from datetime import datetime
from db_manager import *
import face_recognition
from PIL import Image
import numpy as np
import sqlite3
import base64
import io
import os

app = Flask(__name__)
base_dir = os.path.dirname(__file__)
DB_NAME = os.path.join(base_dir, "database.db")

# Inicializar la base de datos
crear_tabla_usuarios()

def guardar_usuario(nombre, encoding):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    fecha = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    rostro_blob = encoding.tobytes()
    cursor.execute("INSERT INTO usuarios (nombre, rostro_vector, fecha_registro) VALUES (?, ?, ?)",
                   (nombre, rostro_blob, fecha))
    conn.commit()
    conn.close()

def cargar_usuarios():
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute("SELECT nombre, rostro_vector FROM usuarios")
    usuarios = cursor.fetchall()
    conn.close()
    return [(nombre, np.frombuffer(rostro_vector, dtype=np.float64)) for nombre, rostro_vector in usuarios]

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

        guardar_usuario(nombre, face_encodings[0])
        return jsonify({'mensaje': '✔️ Rostro registrado correctamente'}), 200
    except Exception as e:
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
            return jsonify({'error': 'No se detectó ningún rostro'}), 400

        encoding_input = face_encodings[0]
        usuarios = cargar_usuarios()

        mejores_distancias = [(nombre, face_recognition.face_distance([enc], encoding_input)[0])
                              for nombre, enc in usuarios]

        if not mejores_distancias:
            return jsonify({'mensaje': 'No hay usuarios registrados'}), 404

        nombre_mas_cercano, distancia = min(mejores_distancias, key=lambda x: x[1])

        if distancia < 0.6:
            return jsonify({'mensaje': 'Rostro verificado', 'usuario': nombre_mas_cercano}), 200
        else:
            return jsonify({'mensaje': 'No se encontró coincidencia'}), 401

    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')




