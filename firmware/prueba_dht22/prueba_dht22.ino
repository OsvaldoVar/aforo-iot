#include <DHT.h>
#include <math.h>

#define DHT_PIN 4
#define INTERVALO_LECTURA_MS 2000UL

DHT dht(DHT_PIN, DHT22);
unsigned long ultimaLectura = 0;

void setup() {
    Serial.begin(115200);
    dht.begin();
    ultimaLectura = millis();
}

void loop() {
    const unsigned long ahora = millis();
    if (ahora - ultimaLectura < INTERVALO_LECTURA_MS) {
        return;
    }
    ultimaLectura = ahora;

    const float temperatura = dht.readTemperature();
    const float humedad = dht.readHumidity();

    if (isnan(temperatura) || isnan(humedad)) {
        Serial.println("Lectura invalida del DHT22. Se reintentara en 2 segundos.");
        return;
    }

    Serial.print("Temperatura: ");
    Serial.print(temperatura, 1);
    Serial.print(" °C | Humedad: ");
    Serial.print(humedad, 1);
    Serial.println(" %HR");
}
