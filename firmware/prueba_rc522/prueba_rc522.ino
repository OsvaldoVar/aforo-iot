#include <SPI.h>
#include <MFRC522.h>

#define SS_PIN 5
#define RST_PIN 22
#define SCK_PIN 18
#define MISO_PIN 19
#define MOSI_PIN 23

MFRC522 lector(SS_PIN, RST_PIN);

void setup() {
    Serial.begin(115200);
    SPI.begin(SCK_PIN, MISO_PIN, MOSI_PIN, SS_PIN);
    lector.PCD_Init();
    Serial.println("Acerque una tarjeta al lector RC522.");
}

void loop() {
    if (!lector.PICC_IsNewCardPresent() || !lector.PICC_ReadCardSerial()) {
        return;
    }

    Serial.print("UID: ");
    for (byte indice = 0; indice < lector.uid.size; ++indice) {
        Serial.printf("%02X", static_cast<unsigned int>(lector.uid.uidByte[indice]));
    }
    Serial.println();

    // Deja la tarjeta en reposo para evitar repetir su UID mientras siga en el campo.
    lector.PICC_HaltA();
}
