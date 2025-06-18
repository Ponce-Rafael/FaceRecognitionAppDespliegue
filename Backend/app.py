from flask import Flask, request, jsonify
import numpy as np
import cv2
import io
from PIL import Image
import os
import base64
import uuid
import sqlite3
from datetime import datetime

app = Flask(__name__)

# Paths
base_dir = os.path.dirname(__file__)
modelFile = os.path.join(base_dir, "models", "res10_300x300_ssd_iter_140000.caffemodel")
configFile = os.path.join(base_dir, "models", "deploy.prototxt")
rostros_dir = os.path.join(base_dir, "rostros")
os.makedirs(rostros_dir, exist_ok=True)

# Cargar modelo Caffe
net = cv2.dnn.readNetFromCaffe(configFile, modelFile)

# Conexión a la base de datos
DB_NAME = os.path.join(base_dir, "database.db")

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
            faces.append(box.astype("int"))
    return faces

def guardar_en_db(nombre, rostro_bytes):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    fecha = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute("INSERT INTO usuarios (nombre, rostro_vector, fecha_registro) VALUES (?, ?, ?)", 
                   (nombre, rostro_bytes, fecha))
    conn.commit()
    conn.close()

@app.route('/registro', methods=['POST'])
def registrar_usuario():
    try:
        data = request.get_json()
        if not data or 'imagen' not in data or 'nombre' not in data:
            return jsonify({'error': 'Faltan datos'}), 400

        nombre = data['nombre']
        img_bytes = base64.b64decode(data['imagen'])
        image_pil = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        image_np = np.array(image_pil)[:, :, ::-1]

        faces = detect_faces(image_np)
        if not faces:
            return jsonify({'error': 'No se detectó rostro'}), 400

        (startX, startY, endX, endY) = faces[0]
        face_img = image_np[startY:endY, startX:endX]
        face_resized = cv2.resize(face_img, (150, 150))

        # Guardar imagen localmente
        filename = f"{nombre}_{uuid.uuid4().hex}.jpg"
        file_path = os.path.join(rostros_dir, filename)
        cv2.imwrite(file_path, face_resized)

        # Guardar en base de datos
        _, buffer = cv2.imencode('.jpg', face_resized)
        guardar_en_db(nombre, buffer.tobytes())

        return jsonify({'mensaje': '✔️ Rostro registrado correctamente'}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


# @app.route('/verificar', methods=['POST'])
# def verificar_usuario():
#     try:
#         data = request.get_json()
#         if 'imagen' not in data:
#             return jsonify({'error': 'Falta la imagen'}), 400

#         img_bytes = base64.b64decode(data['imagen'])
#         image_pil = Image.open(io.BytesIO(img_bytes)).convert("RGB")
#         image_np = np.array(image_pil)[:, :, ::-1]

#         faces = detect_faces(image_np)
#         if not faces:
#             return jsonify({'error': 'No se detectó rostro'}), 400

#         (startX, startY, endX, endY) = faces[0]
#         input_face = image_np[startY:endY, startX:endX]
#         input_face = cv2.resize(input_face, (150, 150))
#         input_face_gray = cv2.cvtColor(input_face, cv2.COLOR_BGR2GRAY)
#         input_face_gray = cv2.equalizeHist(input_face_gray)

#         mejor_similitud = 0
#         usuario_encontrado = None

#         for filename in os.listdir(rostros_dir):
#             if filename.endswith(".jpg"):
#                 img_path = os.path.join(rostros_dir, filename)
#                 img_bd = cv2.imread(img_path)
#                 if img_bd is None:
#                     continue
#                 img_bd = cv2.resize(img_bd, (150, 150))
#                 img_bd_gray = cv2.cvtColor(img_bd, cv2.COLOR_BGR2GRAY)
#                 img_bd_gray = cv2.equalizeHist(img_bd_gray)

#                 resultado = cv2.matchTemplate(img_bd_gray, input_face_gray, cv2.TM_CCOEFF_NORMED)
#                 similitud = cv2.minMaxLoc(resultado)[1]

#                 if similitud > mejor_similitud:
#                     mejor_similitud = similitud
#                     usuario_encontrado = filename.split("_")[0]

#         if mejor_similitud > 0.85:
#             return jsonify({'mensaje': 'Rostro verificado', 'usuario': usuario_encontrado}), 200
#         else:
#             return jsonify({'mensaje': 'No se encontró coincidencia'}), 401

#     except Exception as e:
#         return jsonify({'error': str(e)}), 500

@app.route('/verificar', methods=['POST'])
def verificar_usuario():
    try:
        data = request.get_json()
        if 'imagen' not in data:
            return jsonify({'error': 'Falta la imagen'}), 400

        # Decodificar la imagen recibida
        img_bytes = base64.b64decode(data['imagen'])
        image_pil = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        image_np = np.array(image_pil)[:, :, ::-1]

        # Detección de rostro
        faces = detect_faces(image_np)
        if not faces:
            return jsonify({'error': 'No se detectó rostro'}), 400

        (startX, startY, endX, endY) = faces[0]
        input_face = image_np[startY:endY, startX:endX]
        input_face = cv2.resize(input_face, (150, 150))

        # Preprocesamiento del rostro
        input_face_gray = cv2.cvtColor(input_face, cv2.COLOR_BGR2GRAY)
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        input_face_gray = clahe.apply(input_face_gray)
        input_face_gray = cv2.GaussianBlur(input_face_gray, (5, 5), 0)

        def aplicar_mascara(imagen):
            mask = np.zeros(imagen.shape, dtype=np.uint8)
            h, w = imagen.shape
            center = (w // 2, h // 2)
            radius = min(center[0], center[1], w - center[0], h - center[1])
            cv2.circle(mask, center, radius, 255, -1)
            return cv2.bitwise_and(imagen, imagen, mask=mask)

        input_face_gray = aplicar_mascara(input_face_gray)

        # Comparación con rostros registrados
        mejor_similitud = 0
        usuario_encontrado = None

        for filename in os.listdir(rostros_dir):
            if filename.endswith(".jpg"):
                img_path = os.path.join(rostros_dir, filename)
                img_bd = cv2.imread(img_path)
                if img_bd is None:
                    continue

                img_bd = cv2.resize(img_bd, (150, 150))
                img_bd_gray = cv2.cvtColor(img_bd, cv2.COLOR_BGR2GRAY)
                img_bd_gray = clahe.apply(img_bd_gray)
                img_bd_gray = cv2.GaussianBlur(img_bd_gray, (5, 5), 0)
                img_bd_gray = aplicar_mascara(img_bd_gray)

                resultado = cv2.matchTemplate(img_bd_gray, input_face_gray, cv2.TM_CCOEFF_NORMED)
                similitud = cv2.minMaxLoc(resultado)[1]

                if similitud > mejor_similitud:
                    mejor_similitud = similitud
                    usuario_encontrado = filename.split("_")[0]

        # Ajuste del umbral de similitud
        if mejor_similitud > 0.80:
            return jsonify({'mensaje': 'Rostro verificado', 'usuario': usuario_encontrado}), 200
        else:
            return jsonify({'mensaje': 'No se encontró coincidencia'}), 401

    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
