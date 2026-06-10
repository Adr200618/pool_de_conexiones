-- ============================================
-- SCRIPT DE INICIALIZACIÓN PARA MYSQL (WINDOWS)
-- ============================================

-- Crear la base de datos (si no existe)
CREATE DATABASE IF NOT EXISTS testdb;

-- Usar la base de datos
USE testdb;

-- Crear la tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id INT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear índice para búsquedas rápidas por ID
CREATE INDEX idx_usuarios_id ON usuarios(id);

-- Crear índice para búsquedas por email
CREATE INDEX idx_usuarios_email ON usuarios(email);

-- Insertar algunos datos de prueba iniciales
INSERT IGNORE INTO usuarios (id, nombre, email) VALUES 
    (1, 'Admin', 'admin@test.com'),
    (2, 'Usuario Prueba', 'test@test.com');

-- Verificar que los datos se insertaron correctamente
SELECT COUNT(*) as total_usuarios FROM usuarios;

-- Mostrar los primeros 5 usuarios
SELECT * FROM usuarios LIMIT 5;

-- ============================================
-- CÓMO EJECUTAR ESTE SCRIPT EN WINDOWS:
-- ============================================
-- 1. Abrir Command Prompt o PowerShell
-- 2. Navegar a C:\Program Files\MySQL\MySQL Server 8.0\bin
-- 3. Ejecutar: mysql -u root -p < C:\ConnectionPoolProject\database\init_mysql.sql
-- 4. Ingresar la contraseña de MySQL (root)