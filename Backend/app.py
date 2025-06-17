# app.py
from flask import Flask, request, jsonify
import numpy as np
import cv2
import io
from PIL import Image
import os
import base64
import uuid
from db_manager import insertar_usuario, obtener_usuarios

app = Flask(__name__)

# Cargar modelo de detección
base_dir = os.path.dirname(__file__)
modelFile = os.path.join(base_dir, "models", "res10_300x300_ssd_iter_140000.caffemodel")
configFile = os.path.join(base_dir, "models", "deploy.prototxt")

net = cv2.dnn.readNetFromCaffe(configFile, modelFile)

def detect_faces(image):
    (h, w) = image.shape[:2]
    blob = cv2.dnn.blobFromImage(cv2.resize(image, (300, 300)), 1.0,
                                 (300, 300), (104.0, 177.0, 123.0))
    net.setInput(blob)
    detections = net.forward()
    faces = []
    for i in range(detections.shape[2]):
        confidence = detections[0, 0, i, 2]
        if confidence > 0.5:
            box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
            (startX, startY, endX, endY) = box.astype("int")
            faces.append((startX, startY, endX, endY))
    return faces

@app.route('/registro', methods=['POST'])
def registrar_usuario():
    try:
        data = request.get_json()
        if not data or 'imagen' not in data or 'nombre' not in data:
            return jsonify({'error': 'Faltan datos'}), 400

        img_base64 = data['imagen']
        nombre = data['nombre']

        img_bytes = base64.b64decode(img_base64)
        image_pil = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        image_np = np.array(image_pil)[:, :, ::-1]

        faces = detect_faces(image_np)
        if len(faces) == 0:
            return jsonify({'error': 'No se detectó rostro'}), 400

        (startX, startY, endX, endY) = faces[0]
        face_img = image_np[startY:endY, startX:endX]

        # output_dir = os.path.join(base_dir, "rostros")
        # os.makedirs(output_dir, exist_ok=True)
        # file_path = os.path.join(output_dir, f"{nombre}_{uuid.uuid4().hex}.jpg")
        # cv2.imwrite(file_path, face_img)

        _, img_encoded = cv2.imencode('.jpg', face_img)
        rostro_bytes = img_encoded.tobytes()

        insertar_usuario(nombre, rostro_bytes)

        return jsonify({
            'mensaje': 'Rostro detectado y guardado correctamente',
            'box': [int(startX), int(startY), int(endX), int(endY)]
        }), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/verificar', methods=['POST'])
def verificar_usuario():
    try:
        data = request.get_json()
        if not data or 'imagen' not in data:
            return jsonify({'error': 'Falta la imagen'}), 400

        img_base64 = data['imagen']
        img_bytes = base64.b64decode(img_base64)
        image_pil = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        image_np = np.array(image_pil)[:, :, ::-1]

        faces = detect_faces(image_np)
        if len(faces) == 0:
            return jsonify({'error': 'No se detectó rostro'}), 400

        (startX, startY, endX, endY) = faces[0]
        input_face = image_np[startY:endY, startX:endX]
        input_face_resized = cv2.resize(input_face, (150, 150))

        mejor_similitud = 0
        usuario_encontrado = None

        usuarios = obtener_usuarios()
        for nombre, rostro_blob in usuarios:
            nparr = np.frombuffer(rostro_blob, np.uint8)
            imagen_guardada = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            if imagen_guardada is None:
                continue
            imagen_guardada_resized = cv2.resize(imagen_guardada, (150, 150))

            resultado = cv2.matchTemplate(imagen_guardada_resized, input_face_resized, cv2.TM_CCOEFF_NORMED)
            similitud = cv2.minMaxLoc(resultado)[1]

            if similitud > mejor_similitud:
                mejor_similitud = similitud
                usuario_encontrado = nombre

        if mejor_similitud > 0.80:
            return jsonify({'mensaje': 'Rostro verificado', 'usuario': usuario_encontrado}), 200
        else:
            return jsonify({'mensaje': 'No se encontró coincidencia'}), 401

    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
