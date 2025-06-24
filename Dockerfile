# Imagen base de Python 3.10 (es compatible con dlib)
FROM python:3.10-slim

# Instalar librerías del sistema necesarias para compilar dlib
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    libopenblas-dev \
    liblapack-dev \
    libx11-dev \
    libgtk-3-dev \
    && rm -rf /var/lib/apt/lists/*

# Crear carpeta de trabajo
WORKDIR /app

# Copiar el proyecto completo al contenedor
COPY . /app

# Actualizar pip e instalar las dependencias
RUN pip install --upgrade pip
RUN pip install --use-pep517 -r requirements.txt

# Exponer el puerto usado por Flask
EXPOSE 5000

# Comando para ejecutar el servidor Flask
CMD ["python", "app.py"]
