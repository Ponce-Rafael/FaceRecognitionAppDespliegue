# Imagen base con dlib preinstalado (Python 3.8 + dlib)
FROM bamos/dlib:python3

# Crear carpeta de trabajo
WORKDIR /app

# Copiar solo los archivos necesarios
COPY . /app

# Instalar las demás dependencias del proyecto
RUN pip install --upgrade pip
RUN pip install flask face_recognition numpy opencv-python pillow Rx

# Exponer el puerto de Flask
EXPOSE 5000

# Ejecutar tu app Flask
CMD ["python", "Backend/app.py"]
