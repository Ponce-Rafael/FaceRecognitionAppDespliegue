# Imagen base con dlib y face_recognition ya preinstalados
FROM bamos/dlib:python3

# Crear carpeta de trabajo
WORKDIR /app

# Copiar el código al contenedor
COPY . /app

# Instalar solo dependencias adicionales (sin dlib ni face_recognition)
RUN pip install --upgrade pip
RUN pip install flask numpy opencv-python pillow Rx

# Exponer el puerto de Flask
EXPOSE 5000

# Ejecutar la app Flask
CMD ["python", "Backend/app.py"]
