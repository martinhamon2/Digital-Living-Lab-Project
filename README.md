# Arduino Profile Controller

This is a local five-button controller. Buttons 1–4 execute actions from the
active profile; button 5 always selects the next profile. A TC1602A-21T(R)(3)
parallel 16×2 LCD displays the active profile name. The Spring Boot backend
owns profiles, the Arduino serial connection, and action execution.

The existing React/Vite frontend is deliberately untouched. It is available for
another team member to turn into the configuration interface, using the API
described in ARCHITECTURE.md.

## Hardware

Required components:

- Arduino Uno R3 and USB cable;
- five momentary pushbuttons;
- TC1602A-21T(R)(3) 16×2 character LCD, or another HD44780-compatible
  **parallel** 16-pin LCD;
- 10 kΩ linear potentiometer for LCD contrast;
- 220–330 Ω resistor for the LCD backlight;
- breadboard and jumper wires.

The sketch uses the official Arduino LiquidCrystal library. It is often bundled
with Arduino IDE; if it is not available, open Library Manager, search for
**LiquidCrystal** by Arduino, and install it. This is the standard library for
HD44780-compatible parallel LCDs.

### Wiring

Each pushbutton uses the Arduino internal pull-up resistor. Connect one button
leg to the listed digital pin and the diagonally opposite leg to GND. No
external resistor is needed. Place each button across the breadboard centre
gap; rotate it by 90 degrees if it does not work.

| Control | Arduino pin |
| --- | --- |
| Button 1 | D2 |
| Button 2 | D3 |
| Button 3 | D4 |
| Button 4 | D5 |
| Button 5 — next profile | D6 |
| Other leg of every button | GND |
| LCD pin 1 — VSS | GND |
| LCD pin 2 — VDD | 5V |
| LCD pin 3 — VO | 10 kΩ potentiometer wiper |
| LCD pin 4 — RS | D7 |
| LCD pin 5 — RW | GND |
| LCD pin 6 — E | D8 |
| LCD pins 7–10 — D0–D3 | Not connected |
| LCD pin 11 — D4 | D9 |
| LCD pin 12 — D5 | D10 |
| LCD pin 13 — D6 | D11 |
| LCD pin 14 — D7 | D12 |
| LCD pin 15 — A / LED+ | 5V through a 220–330 Ω resistor |
| LCD pin 16 — K / LED− | GND |

Connect one outer potentiometer pin to 5V and the other outer pin to GND. Turn
it slowly after powering the circuit: a blank display is often only a contrast
setting issue. The LCD is a **parallel** module, not an I²C module; do not
connect it to A4/A5 and do not use an I²C backpack address.

Do not use D0 or D1: USB serial communication uses them. Do not connect the
backlight LED directly to 5V unless the exact LCD variant documents an onboard
current-limiting resistor.

## Upload the Arduino firmware

1. Connect the Uno over USB.
2. Open arduino/five_button_profiles/five_button_profiles.ino.
3. Select **Arduino Uno** and its USB port, then upload.
4. Open Serial Monitor at 115200 baud. It prints READY at startup; pressing the
   buttons prints BUTTON_1 through BUTTON_5.
5. Close Serial Monitor before starting the backend, because only one process
   can open the serial port.

The first line reads **Active profile**. A profile name up to 16 characters is
shown as-is on the second line. Longer names pause for 1.2 seconds, then scroll
left every 300 ms with a three-space gap before repeating. The backend removes
line breaks and replaces non-ASCII display characters with a question mark,
because the standard HD44780 character set is not UTF-8.

If the backlight is on but no characters appear, first adjust the contrast
potentiometer. Then check that RW is connected to GND, the 4-bit data pins are
connected in the exact order above, and the 16-pin connector is oriented from
pin 1 (VSS) to pin 16 (K).

## Run the local application

Java 17 or later is required. From the repository root:

    cd backend
    sh ./mvnw spring-boot:run

On Windows:

    cd backend
    .\mvnw.cmd spring-boot:run

The application starts at http://localhost:3000. It creates a local H2 database
under backend/data/ and creates a **Default profile** with four actions on its
first launch: Paste, Copy, Undo, and Select all.

## Docker deployment

Docker is provided for running the API and preserving profile configuration:

    docker compose up --build -d

The API remains available only on http://localhost:3000. Profiles are stored in
the named Docker volume `controller-data`. Stop the service with:

    docker compose down

Use `docker compose down -v` only when you intentionally want to delete all
stored profiles.

### Arduino access in Docker

Docker can pass a USB serial device through on Linux. First find the device on
the host, then start the service with it mapped into the container:

    ARDUINO_DEVICE=/dev/ttyACM0
    docker compose run --rm --service-ports --device=$ARDUINO_DEVICE:$ARDUINO_DEVICE controller

Connect through the API using the same device path. Permissions and stable
device names are host-specific; a udev rule is recommended for a long-running
Linux setup.

On macOS and Windows, Docker Desktop does not offer direct access to a host
Arduino serial device in this simple configuration. More importantly, keyboard
shortcuts and application launches from a container occur **inside the
container**, not in the host desktop session. Docker is therefore suitable for
API development and profile persistence, and potentially Linux serial testing,
but the normal host installation is required for production desktop actions.

## Connect the Arduino

List available ports:

    curl http://localhost:3000/api/serial/ports

Connect by replacing the port name:

    curl -X PUT http://localhost:3000/api/serial/port \
      -H 'Content-Type: application/json' \
      -d '{"port":"/dev/cu.usbmodemXXXX"}'

Use a port such as COM3 on Windows or /dev/ttyACM0 on Linux. The backend sends
the active profile name to the LCD once connected.

## API quick reference

- GET /api/status — active profile and serial connection.
- GET /api/profiles — profiles and actions.
- POST /api/profiles — create a profile.
- PUT /api/profiles/{id} — update a profile.
- DELETE /api/profiles/{id} — delete a profile.
- PUT /api/profiles/order — reorder profiles with an ids array.
- POST /api/profiles/{id}/activate — switch profile.
- GET /api/serial/ports — list serial ports.
- PUT /api/serial/port — connect to a port.
- DELETE /api/serial/port — disconnect.

A profile request always contains all four configurable buttons. For example:

    {
      "name": "Browser",
      "actions": [
        {"buttonNumber": 1, "label": "New tab", "type": "KEYBOARD_SHORTCUT", "value": "CMD_OR_CTRL+T"},
        {"buttonNumber": 2, "label": "Open site", "type": "OPEN_URL", "value": "https://example.com"},
        {"buttonNumber": 3, "label": "Paste", "type": "PASTE", "value": null},
        {"buttonNumber": 4, "label": "Launch app", "type": "LAUNCH_APPLICATION", "value": "open"}
      ]
    }

Supported action types are PASTE, KEYBOARD_SHORTCUT, OPEN_URL, and
LAUNCH_APPLICATION. KEYBOARD_SHORTCUT accepts basic values such as
CMD_OR_CTRL+T, CTRL+SHIFT+P, or CMD+V. LAUNCH_APPLICATION runs the command and
space-separated arguments as supplied; do not use it with untrusted values.

Swagger UI is at http://localhost:3000/swagger-ui/index.html while the backend
is running. The complete API contract is in [docs/API.md](docs/API.md).

## Platform notes

- **macOS:** grant the Java application or its launcher Accessibility permission
  in System Settings → Privacy & Security → Accessibility for shortcuts to work.
- **Windows:** the application uses PowerShell SendKeys for shortcuts. Security
  software may need to allow the Java process.
- **Linux:** shortcut execution expects xdotool on an X11 desktop. Wayland
  support depends on desktop security policy and is not part of V1.
