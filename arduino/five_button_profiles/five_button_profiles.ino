#include <LiquidCrystal.h>
#include <string.h>

const byte BUTTON_PINS[] = {2, 3, 4, 5, 6};
const byte BUTTON_COUNT = sizeof(BUTTON_PINS) / sizeof(BUTTON_PINS[0]);
const unsigned long DEBOUNCE_MS = 35;

// TC1602A-21T(R)(3), HD44780-compatible, 4-bit parallel connection.
const byte LCD_RS = 7;
const byte LCD_ENABLE = 8;
const byte LCD_D4 = 9;
const byte LCD_D5 = 10;
const byte LCD_D6 = 11;
const byte LCD_D7 = 12;
const byte LCD_COLUMNS = 16;
const byte PROFILE_BUFFER_SIZE = 33;  // Backend profile names are limited to 32 characters.
const unsigned long SCROLL_START_DELAY_MS = 1200;
const unsigned long SCROLL_STEP_MS = 300;
const byte SCROLL_GAP = 3;

LiquidCrystal lcd(LCD_RS, LCD_ENABLE, LCD_D4, LCD_D5, LCD_D6, LCD_D7);

bool stableStates[BUTTON_COUNT];
bool lastRawStates[BUTTON_COUNT];
unsigned long lastChangeAt[BUTTON_COUNT];
char incomingLine[PROFILE_BUFFER_SIZE + 9];
byte incomingLength = 0;
char profileName[PROFILE_BUFFER_SIZE] = "Waiting for PC";
byte scrollOffset = 0;
unsigned long profileChangedAt = 0;
unsigned long lastScrollAt = 0;

void printPadded(const char *text) {
  byte column = 0;
  while (*text != '\0' && column < LCD_COLUMNS) {
    lcd.print(*text++);
    column++;
  }
  while (column++ < LCD_COLUMNS) lcd.print(' ');
}

void drawProfileName() {
  byte length = strlen(profileName);
  lcd.setCursor(0, 1);
  if (length <= LCD_COLUMNS) {
    printPadded(profileName);
    return;
  }

  // Scroll with three blank columns between the end and the restart.
  for (byte column = 0; column < LCD_COLUMNS; column++) {
    byte sourceIndex = (scrollOffset + column) % (length + SCROLL_GAP);
    lcd.print(sourceIndex < length ? profileName[sourceIndex] : ' ');
  }
}

void setProfileName(const char *newName) {
  strncpy(profileName, newName, PROFILE_BUFFER_SIZE - 1);
  profileName[PROFILE_BUFFER_SIZE - 1] = '\0';
  scrollOffset = 0;
  profileChangedAt = millis();
  lastScrollAt = profileChangedAt;
  lcd.clear();
  lcd.setCursor(0, 0);
  printPadded("Active profile");
  drawProfileName();
}

void updateProfileScroll() {
  byte length = strlen(profileName);
  if (length <= LCD_COLUMNS || millis() - profileChangedAt < SCROLL_START_DELAY_MS) return;
  if (millis() - lastScrollAt < SCROLL_STEP_MS) return;
  scrollOffset = (scrollOffset + 1) % (length + SCROLL_GAP);
  lastScrollAt = millis();
  drawProfileName();
}

void handleIncomingLine() {
  incomingLine[incomingLength] = '\0';
  const char prefix[] = "PROFILE:";
  if (strncmp(incomingLine, prefix, sizeof(prefix) - 1) == 0) {
    setProfileName(incomingLine + sizeof(prefix) - 1);
  }
  incomingLength = 0;
}

void readComputerMessages() {
  while (Serial.available()) {
    char character = (char)Serial.read();
    if (character == '\r') continue;
    if (character == '\n') {
      handleIncomingLine();
    } else if (incomingLength < sizeof(incomingLine) - 1) {
      incomingLine[incomingLength++] = character;
    } else {
      // Drop an overlong line so a malformed message cannot corrupt the display state.
      incomingLength = 0;
    }
  }
}

void setup() {
  Serial.begin(115200);
  for (byte index = 0; index < BUTTON_COUNT; index++) {
    pinMode(BUTTON_PINS[index], INPUT_PULLUP);
    stableStates[index] = HIGH;
    lastRawStates[index] = HIGH;
    lastChangeAt[index] = 0;
  }

  lcd.begin(LCD_COLUMNS, 2);
  setProfileName("Waiting for PC");
  Serial.println("READY");
}

void loop() {
  readComputerMessages();
  updateProfileScroll();

  for (byte index = 0; index < BUTTON_COUNT; index++) {
    bool rawState = digitalRead(BUTTON_PINS[index]);
    if (rawState != lastRawStates[index]) {
      lastRawStates[index] = rawState;
      lastChangeAt[index] = millis();
    }
    if (rawState != stableStates[index] && millis() - lastChangeAt[index] >= DEBOUNCE_MS) {
      stableStates[index] = rawState;
      if (stableStates[index] == LOW) {
        Serial.print("BUTTON_");
        Serial.println(index + 1);
      }
    }
  }
}
