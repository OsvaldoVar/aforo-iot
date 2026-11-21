-- Datos de prueba iniciales para Aforo IoT
-- Importar despues de database/schema.sql

USE aforo_iot;

-- Usuario administrador de prueba (contrasena en texto plano: admin123)
INSERT INTO usuarios (nombre, correo, password_hash, token, rol) VALUES
('Administrador', 'admin@aforoiot.local', '$2y$10$hJOJNBh0FRoFo8WfCA0gKeuors1x73NKl7CtqXBN0BMOIj6QQBPIa', NULL, 'admin');

-- Dispositivo ESP32 de demostracion y sus limites iniciales
INSERT INTO dispositivos (id, nombre, ubicacion, api_key, ultima_conexion, activo) VALUES
(1, 'ESP32 Laboratorio', 'Laboratorio de Redes - Puerta Principal', 'demo-esp32-key-0001', NOW(), 1);

INSERT INTO limites (dispositivo_id, temp_max, hum_max, aforo_max) VALUES
(1, 30.00, 75.00, 10);

-- Personas registradas con tarjetas RFID (UID hexadecimal en mayusculas)
INSERT INTO personas (id, nombre, uid_rfid, activo) VALUES
(1, 'Ana Martinez', 'A1B2C3D4', 1),
(2, 'Carlos Lopez', 'B2C3D4E5', 1),
(3, 'Sofia Ramirez', 'C3D4E5F6', 1);

-- Historial de accesos: Ana y Sofia ya salieron; Carlos permanece dentro
INSERT INTO registros_acceso (persona_id, dispositivo_id, tipo, fecha) VALUES
(1, 1, 'entrada', NOW() - INTERVAL 3 HOUR),
(1, 1, 'salida',  NOW() - INTERVAL 2 HOUR),
(3, 1, 'entrada', NOW() - INTERVAL 2 HOUR),
(3, 1, 'salida',  NOW() - INTERVAL 1 HOUR),
(2, 1, 'entrada', NOW() - INTERVAL 45 MINUTE);

-- Mediciones recientes del sensor DHT22 (ultimas 5 horas)
INSERT INTO mediciones (dispositivo_id, temperatura, humedad, fecha) VALUES
(1, 24.50, 52.00, NOW() - INTERVAL 5 HOUR),
(1, 25.80, 54.50, NOW() - INTERVAL 4 HOUR),
(1, 27.20, 58.00, NOW() - INTERVAL 3 HOUR),
(1, 29.10, 61.30, NOW() - INTERVAL 2 HOUR),
(1, 31.40, 64.00, NOW() - INTERVAL 1 HOUR),
(1, 28.60, 59.80, NOW() - INTERVAL 15 MINUTE);

-- Alerta generada por superar el limite de temperatura
INSERT INTO alertas (dispositivo_id, tipo, mensaje, valor, fecha, atendida) VALUES
(1, 'temperatura', 'Temperatura por encima del limite configurado (30.00 C)', 31.40, NOW() - INTERVAL 1 HOUR, 0);
