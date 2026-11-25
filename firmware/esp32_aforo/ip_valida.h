#pragma once

#include <cstdio>

// Acepta solo IPv4 decimal completa: cuatro octetos 0-255 y nada despues.
inline bool ipValida(const char* texto) {
    if (texto == nullptr) {
        return false;
    }
    int a = 0, b = 0, c = 0, d = 0;
    char resto = '\0';
    if (std::sscanf(texto, "%d.%d.%d.%d%c", &a, &b, &c, &d, &resto) != 4) {
        return false;
    }
    return a >= 0 && a <= 255 && b >= 0 && b <= 255 && c >= 0 && c <= 255 && d >= 0
           && d <= 255;
}
