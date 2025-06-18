import requests
import base64

# Ruta a una imagen JPG con un rostro claro y visible
with open("obama.jpg", "rb") as img_file:
    imagen_base64 = base64.b64encode(img_file.read()).decode('utf-8')

payload = {
    "nombre": "Prueba_PC",
    "imagen": imagen_base64
}

url = "http://192.168.1.101:5000/registro"

# url de zona gamer 192.168.140.236

response = requests.post(url, json=payload)
print("Código de estado:", response.status_code)
print("Respuesta:", response.text)
