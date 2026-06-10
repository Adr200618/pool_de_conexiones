-- ============================================
-- SCRIPT DE INICIALIZACIÓN PARA POSTGRESQL (WINDOWS)
-- ============================================

-- Crear la base de datos (si no existe)
CREATE DATABASE testdb;

-- Conectarse a la base de datos
\c testdb;

-- Crear la tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear índice para búsquedas rápidas por ID
CREATE INDEX IF NOT EXISTS idx_usuarios_id ON usuarios(id);

-- Crear índice para búsquedas por email
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);

-- Insertar algunos datos de prueba iniciales
INSERT INTO usuarios (id, nombre, email) VALUES 
    (1, 'Admin', 'admin@test.com'),
    (2, 'Usuario Prueba', 'test@test.com')
ON CONFLICT (id) DO NOTHING;

-- Verificar que los datos se insertaron correctamente
SELECT COUNT(*) as total_usuarios FROM usuarios;

-- Mostrar los primeros 5 usuarios
SELECT * FROM usuarios LIMIT 5;

-- ============================================
-- CÓMO EJECUTAR ESTE SCRIPT EN WINDOWS:
-- ============================================
-- 1. Abrir Command Prompt o PowerShell
-- 2. Navegar a C:\Program Files\PostgreSQL\15\bin
-- 3. Ejecutar: psql -U postgres -f C:\ConnectionPoolProject\database\init_postgres.sql
-- 4. Ingresar la contraseña de PostgreSQL (postgres)