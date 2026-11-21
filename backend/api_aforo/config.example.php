<?php
// Plantilla de configuración del backend.
// Copiar como config.php y ajustar credenciales locales (config.php está en .gitignore).

define('DB_HOST', 'localhost');
define('DB_PORT', 3306);
define('DB_NAME', 'aforo_iot');
// Usuario de MariaDB creado en los pasos de instalación del README.
define('DB_USER', 'aforo');
define('DB_PASS', 'cambia-esta-clave');
define('DB_CHARSET', 'utf8mb4');

define('TOKEN_HORAS', 24);
define('ZONA_HORARIA', 'America/Mexico_City');
define('CORS_ORIGEN', '*');
