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
crear_tabla_usuarios()

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

        # Convertimos nuevamente a formato JPEG para guardar la imagen
        image_bgr = cv2.cvtColor(image_np, cv2.COLOR_RGB2BGR)
        _, buffer = cv2.imencode('.jpg', image_bgr)
        rostro_imagen_bytes = buffer.tobytes()

        # 🟢 Insertar en la base de datos y obtener ID
        user_id = insertar_usuario(nombre, face_encodings[0].tobytes(), rostro_imagen_bytes)

        # 🟢 Guardar la imagen como archivo físico con ID y nombre
        img_filename = f"{user_id}_{nombre}.jpg"
        rostros_dir = os.path.join(base_dir, "rostros")
        os.makedirs(rostros_dir, exist_ok=True)
        img_path = os.path.join(rostros_dir, img_filename)
        with open(img_path, 'wb') as f:
            f.write(rostro_imagen_bytes)

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
        usuarios = obtener_usuarios()

        mejores_distancias = [(nombre, face_recognition.face_distance([np.frombuffer(vec, dtype=np.float64)], encoding_input)[0])
                              for nombre, vec in usuarios]

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
