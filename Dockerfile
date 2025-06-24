# Imagen base oficial de Python compatible con dlib
FROM python:3.10-slim

# Instalar dependencias necesarias para compilar dlib
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    libopenblas-dev \
    liblapack-dev \
    libx11-dev \
    libgtk-3-dev \
    libboost-all-dev \
    && rm -rf /var/lib/apt/lists/*

# Crear directorio de trabajo
WORKDIR /app

# Copiar el proyecto completo
COPY . /app

# Actualizar pip
RUN pip install --upgrade pip

# Instalar dependencias de forma controlada (con --use-pep517 para dlib)
RUN pip install --use-pep517 \
    dlib==19.24.1 \
    face-recognition==1.3.0 \
    face_recognition_models==0.3.0 \
    flask==3.1.1 \
    numpy==1.23.5 \
    opencv-python==4.11.0.86 \
    pillow==11.2.1 \
    Rx==3.2.0 \
    Werkzeug==3.1.3 \
    Jinja2==3.1.6 \
    click==8.2.1 \
    colorama==0.4.6 \
    itsdangerous==2.2.0 \
    blinker==1.9.0 \
    MarkupSafe==3.0.2

# Exponer puerto
EXPOSE 5000

# Comando para iniciar Flask
CMD ["python", "Backend/app.py"]
