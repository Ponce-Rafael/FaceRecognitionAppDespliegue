# db_manager.py
import sqlite3
from datetime import datetime
import os

DB_PATH = os.path.join(os.path.dirname(__file__), "database.db")

def crear_tabla_usuarios():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS usuarios (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL,
            rostro_vector BLOB NOT NULL,
            rostro_imagen BLOB, 
            fecha_registro TEXT
        )
    ''')
    conn.commit()
    conn.close()

def insertar_usuario(nombre, rostro_vector, rostro_imagen):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        INSERT INTO usuarios (nombre, rostro_vector, rostro_imagen, fecha_registro)
        VALUES (?, ?, ?, ?)
    ''', (nombre, rostro_vector, rostro_imagen, datetime.now().isoformat()))
    user_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return user_id


def obtener_usuarios():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT nombre, rostro_vector FROM usuarios")
    usuarios = cursor.fetchall()
    conn.close()
    return usuarios

if __name__ == "__main__":
    crear_tabla_usuarios()
