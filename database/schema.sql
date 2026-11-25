-- Esquema de base de datos para el sistema Aforo IoT
-- Compatible con MariaDB 10.4+ y MySQL 8.0+. Importar con: sudo mariadb < database/schema.sql

CREATE DATABASE IF NOT EXISTS aforo_iot
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE aforo_iot;

-- Limpieza en orden inverso de llaves foraneas para permitir reimportacion
DROP VIEW IF EXISTS personas_dentro;
DROP TABLE IF EXISTS comandos;
DROP TABLE IF EXISTS alertas;
DROP TABLE IF EXISTS limites;
DROP TABLE IF EXISTS mediciones;
DROP TABLE IF EXISTS registros_acceso;
DROP TABLE IF EXISTS tarjetas_pendientes;
DROP TABLE IF EXISTS dispositivos;
DROP TABLE IF EXISTS personas;
DROP TABLE IF EXISTS usuarios;

-- Usuarios con acceso a la aplicacion movil
CREATE TABLE usuarios (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    token VARCHAR(128) NULL DEFAULT NULL,
    rol ENUM('admin', 'usuario') NOT NULL DEFAULT 'usuario',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usuarios_correo (correo),
    INDEX idx_usuarios_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Personas registradas y su tarjeta RFID asignada (UID hexadecimal en mayusculas)
CREATE TABLE personas (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    uid_rfid VARCHAR(32) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_personas_uid_rfid (uid_rfid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dispositivos ESP32 registrados en el sistema
CREATE TABLE dispositivos (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150) NULL DEFAULT NULL,
    api_key VARCHAR(64) NOT NULL,
    ultima_conexion DATETIME NULL DEFAULT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_dispositivos_api_key (api_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tarjetas RFID leidas por un ESP32 que aun no pertenecen a una persona
CREATE TABLE tarjetas_pendientes (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    uid_rfid VARCHAR(32) NOT NULL,
    dispositivo_id INT UNSIGNED NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tarjetas_pendientes_uid_rfid (uid_rfid),
    INDEX idx_tarjetas_pendientes_dispositivo (dispositivo_id),
    INDEX idx_tarjetas_pendientes_fecha (fecha),
    CONSTRAINT fk_tarjetas_pendientes_dispositivos
        FOREIGN KEY (dispositivo_id) REFERENCES dispositivos (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Historial de entradas y salidas por persona y dispositivo
CREATE TABLE registros_acceso (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    persona_id INT UNSIGNED NOT NULL,
    dispositivo_id INT UNSIGNED NOT NULL,
    tipo ENUM('entrada', 'salida') NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_registros_acceso_persona (persona_id),
    INDEX idx_registros_acceso_dispositivo (dispositivo_id),
    INDEX idx_registros_acceso_fecha (fecha),
    INDEX idx_registros_acceso_persona_disp_fecha (persona_id, dispositivo_id, fecha, id),
    CONSTRAINT fk_registros_acceso_personas
        FOREIGN KEY (persona_id) REFERENCES personas (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_registros_acceso_dispositivos
        FOREIGN KEY (dispositivo_id) REFERENCES dispositivos (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Historial de mediciones de temperatura y humedad del sensor DHT22
CREATE TABLE mediciones (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    dispositivo_id INT UNSIGNED NOT NULL,
    temperatura DECIMAL(5,2) NOT NULL,
    humedad DECIMAL(5,2) NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_mediciones_dispositivo (dispositivo_id),
    INDEX idx_mediciones_fecha (fecha),
    INDEX idx_mediciones_dispositivo_fecha (dispositivo_id, fecha),
    CONSTRAINT fk_mediciones_dispositivos
        FOREIGN KEY (dispositivo_id) REFERENCES dispositivos (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Limites configurables de clima y aforo maximo por dispositivo
CREATE TABLE limites (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    dispositivo_id INT UNSIGNED NOT NULL,
    temp_max DECIMAL(5,2) NOT NULL DEFAULT 30.00,
    hum_max DECIMAL(5,2) NOT NULL DEFAULT 75.00,
    aforo_max INT UNSIGNED NOT NULL DEFAULT 20,
    PRIMARY KEY (id),
    UNIQUE KEY uq_limites_dispositivo (dispositivo_id),
    CONSTRAINT fk_limites_dispositivos
        FOREIGN KEY (dispositivo_id) REFERENCES dispositivos (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alertas generadas al superar limites de temperatura, humedad o aforo
CREATE TABLE alertas (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    dispositivo_id INT UNSIGNED NOT NULL,
    tipo ENUM('temperatura', 'humedad', 'aforo') NOT NULL,
    mensaje VARCHAR(255) NOT NULL,
    valor DECIMAL(6,2) NULL DEFAULT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atendida TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_alertas_dispositivo (dispositivo_id),
    INDEX idx_alertas_fecha (fecha),
    INDEX idx_alertas_atendida (atendida),
    CONSTRAINT fk_alertas_dispositivos
        FOREIGN KEY (dispositivo_id) REFERENCES dispositivos (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comandos remotos encolados desde la aplicacion hacia un ESP32
CREATE TABLE comandos (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    dispositivo_id INT UNSIGNED NOT NULL,
    accion ENUM('abrir_acceso', 'alarma') NOT NULL,
    estado ENUM('pendiente', 'ejecutado') NOT NULL DEFAULT 'pendiente',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_ejecucion DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_comandos_dispositivo (dispositivo_id),
    INDEX idx_comandos_estado (estado),
    INDEX idx_comandos_fecha_creacion (fecha_creacion),
    CONSTRAINT fk_comandos_dispositivos
        FOREIGN KEY (dispositivo_id) REFERENCES dispositivos (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Vista de personas actualmente dentro por dispositivo
-- Una persona se considera dentro si su ultimo registro en ese dispositivo es 'entrada'
CREATE VIEW personas_dentro AS
SELECT
    p.id,
    p.nombre,
    r.dispositivo_id,
    r.fecha AS hora_entrada
FROM registros_acceso r
INNER JOIN personas p ON p.id = r.persona_id
WHERE r.tipo = 'entrada'
  AND NOT EXISTS (
      SELECT 1
      FROM registros_acceso r2
      WHERE r2.persona_id = r.persona_id
        AND r2.dispositivo_id = r.dispositivo_id
        AND (
            r2.fecha > r.fecha
            OR (r2.fecha = r.fecha AND r2.id > r.id)
        )
  );
