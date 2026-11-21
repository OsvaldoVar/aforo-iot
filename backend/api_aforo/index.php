<?php
// Punto de entrada de diagnóstico para verificar el estado de la API.

require_once __DIR__ . '/lib/respuesta.php';

enviarCabeceras();
exigirMetodo('GET');

responder(true, 'API Aforo IoT operativa', [
    'servicio' => 'api_aforo',
    'version' => '0.1.0',
    'configurado' => file_exists(__DIR__ . '/config.php'),
    'fecha_servidor' => date('c'),
]);
