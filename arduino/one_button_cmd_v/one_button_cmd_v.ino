

const byte BUTTON_PIN = 2;
const unsigned long DEBOUNCE_MS = 35;

bool stableState = HIGH;
bool lastRawState = HIGH;
unsigned long lastChangeAt = 0;

void setup() {
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  Serial.begin(115200);
}

void loop() {
  const bool rawState = digitalRead(BUTTON_PIN);

  if (rawState != lastRawState) {
    lastRawState = rawState;
    lastChangeAt = millis();
  }

  if (rawState != stableState && millis() - lastChangeAt >= DEBOUNCE_MS) {
    stableState = rawState;

    if (stableState == LOW) {
      Serial.println("BUTTON_1");
    }
  }
}
