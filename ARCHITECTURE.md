# Target Architecture — Arduino Profile Controller

## Purpose

This project will become a local Stream-Deck-like controller: five physical
Arduino buttons trigger computer actions based on the active profile.

Each profile contains four configurable actions. Buttons 1–4 execute these
actions. Button 5 is permanently reserved for moving to the next profile; it
loops back to the first profile after the last one. An Arduino display shows
the active profile name.

The product must work locally, without cloud services and without requiring the
configuration web page to stay open.

## Intended user experience

1. The user connects the Arduino over USB and starts the local application.
2. The application restores the last active profile, or the first profile on
   first use, and sends its name to the display.
3. Buttons 1–4 run the corresponding action from the active profile.
4. Button 5 activates the next profile, updates the display, and persists that
   selection.
5. The local web interface lets users create, reorder, edit, and delete
   profiles and configure actions for buttons 1–4.

| Active profile | Button 1 | Button 2 | Button 3 | Button 4 | Button 5 |
| --- | --- | --- | --- | --- | --- |
| Video editing | Cut | Split | Undo | Export | Next profile |
| Web browsing | New tab | Next tab | Close tab | Reload | Next profile |

## Architecture principles

- **Simple Arduino**: it reads and debounces inputs, emits serial events, and
  displays data sent by the computer. It does not know profiles, shortcuts, or
  applications.
- **Backend as source of truth**: the local backend owns persisted
  configuration, active-profile state, serial communication, and action
  execution.
- **Frontend for configuration only**: React uses the backend API; the
  controller still works when the browser tab is closed.
- **Separation of concerns**: hardware I/O, profile/action selection, and
  operating-system integration are separate layers.
- **Local by default**: the API is exposed only on localhost and configuration
  remains on the user’s computer.
- **Explicit protocol**: serial messages are small, text-based, validated, and
  easy to troubleshoot.

## Target architecture

    Arduino Uno
      • 5 debounced buttons and a TC1602A parallel 16×2 LCD
      • emits BUTTON_1 through BUTTON_5
      • receives display commands
                 │ USB / serial, 115200 baud
                 ▼
    Local Spring Boot application
      • serial adapter and connection handling
      • profile engine and persistent configuration
      • action engine and operating-system adapter
      • HTTP API, validation, and logs
                 │ HTTP on localhost:3000
                 ▼
    React web interface
      • profile configuration, active-profile state, device status

During development, Vite serves the frontend on port 8080. In a packaged
release, Spring Boot can serve the compiled frontend so users run one process.
The repository also includes a Docker deployment for the backend API and H2
profile data. Docker must not be considered a replacement for the desktop
installation: host keyboard automation and application launches cannot be
performed from a standard Docker Desktop container.

## Component responsibilities

### Arduino firmware

- Read five buttons and debounce each button independently.
- Emit exactly one event per press.
- Receive and display the active profile name.
- Safely ignore unknown or incomplete input.

It must not select or execute actions. This avoids duplicate configuration and
makes web changes immediately effective. The selected V1 display is the
TC1602A-21T(R)(3), a 16×2 HD44780-compatible **parallel** LCD. It is wired in
4-bit mode with RS on D7, Enable on D8, and D4–D7 on D9–D12; pin RW is tied to
GND. A 10 kΩ contrast potentiometer connects to VO and the backlight requires
a 220–330 Ω series resistor unless the exact module specifies otherwise.

The first LCD line is the fixed label “Active profile”. The second line shows
the profile name. Names longer than 16 characters wait 1.2 seconds and then
scroll every 300 ms, with a three-character blank gap before repeating. The
backend sends a maximum 32-character profile name and converts line breaks to
spaces and unsupported non-ASCII characters to question marks before sending
it to the LCD.

### Local backend

- Discover or select the Arduino serial port and report its current connection
  state. Automatic reconnection after unplugging is a planned reliability
  improvement.
- Validate serial messages without crashing on unexpected input.
- Maintain active-profile state, calculate the next profile, synchronize the
  display, and persist settings.
- Resolve a pressed button to its configured action and execute it through an
  operating-system adapter.
- Expose validated configuration and state through an HTTP API.

BUTTON_1 is only a physical event. The backend first translates it to button 1
in the active profile, then to its configured action.

### Frontend

The frontend is a configuration console, not a hardware controller. It should
provide profile ordering; profile creation, editing, duplication, and deletion;
button labels and action types; connection and active-profile status; and clear
save/validation feedback. A preview of the five buttons and display is useful.

## Serial protocol

Use UTF-8 lines terminated by a newline. A compact text protocol is preferable
to JSON for this device link.

Arduino to computer:

    BUTTON_1
    BUTTON_2
    BUTTON_3
    BUTTON_4
    BUTTON_5

Each message represents one debounced press. Button 5 is always handled as
fixed profile navigation by the backend.

Computer to Arduino:

    PROFILE:Video editing

The firmware displays the text after PROFILE:. It accepts up to 32 printable
ASCII characters and scrolls values longer than the 16-character row. READY is
emitted on startup. The backend currently sends the active profile after a
serial connection is opened; a future acknowledgement can confirm that the
display update was applied.

## Domain model

    Profile
      id: UUID
      name: String
      position: Integer
      actions: 4 ActionBinding values

    ActionBinding
      button: 1 | 2 | 3 | 4
      label: String
      action: Action

    Action
      type: KEYBOARD_SHORTCUT | OPEN_URL | LAUNCH_APPLICATION | ...
      parameters: data validated for the selected type

    ControllerState
      activeProfileId: UUID
      serialConnection: CONNECTED | DISCONNECTED | ERROR
      selectedPort: String?

Button 5 is intentionally absent from ActionBinding: it is a fixed product
rule. JSON is sufficient for V1 configuration. The existing JPA, H2, and Flyway
dependencies also support an embedded database if migrations and a stricter
persistence model are preferred.

## Initial action scope

V1 should include only:

1. Keyboard shortcuts.
2. Opening a URL.
3. Launching a local application.

Arbitrary scripts, complex macros, plugins, and network automations should be
postponed because they add security and permission concerns. Shortcuts also
require OS-specific handling: Accessibility permission on macOS, X11 versus
Wayland behavior on Linux, and possible Windows application restrictions.

## Indicative local API

These routes are targets; they are not implemented yet.

| Method | Route | Purpose |
| --- | --- | --- |
| GET | /api/status | Application, connection, and active-profile state |
| GET | /api/profiles | Ordered profiles |
| POST | /api/profiles | Create a profile |
| PUT | /api/profiles/{id} | Update a profile |
| DELETE | /api/profiles/{id} | Delete a profile |
| PUT | /api/profiles/order | Reorder profiles |
| POST | /api/profiles/{id}/activate | Select a profile |
| GET | /api/serial/ports | List serial ports |
| PUT | /api/serial/port | Select or reconnect a port |

Server-Sent Events or WebSockets can later publish connection, button, and
profile changes to the UI. They are optional for V1.

## Current repository state

### Working proof of concept

Main contains a working single-button prototype:

- Arduino Uno sketch with one button on D2, INPUT_PULLUP, and 35 ms debounce.
- BUTTON_1 emitted at 115200 baud.
- Python serial bridge in bridge/cmd_v_bridge.py.
- Paste execution: Cmd+V on macOS, Ctrl+V on Windows and Linux.
- Setup and troubleshooting documentation.
- Verified Uno compile: 1,938 bytes of flash (6%) and 202 bytes of RAM (9%).

It does not yet include five buttons, the display, profiles, persistence, or a
configuration UI.

### Backend and frontend foundation

The initial foundation from Martin/initializing-backend-and-frontend has been
merged into main through pull request #9:

- Java 17 / Spring Boot 3.4.2 backend in backend/.
- Web, validation, JPA, H2, Flyway, Actuator, and OpenAPI dependencies.
- Backend configuration targeting port 3000.
- React 19 / TypeScript / Vite frontend in frontend/.
- Vite development server targeting port 8080.

The backend now provides profile persistence, a default profile, REST endpoints,
serial-port listing/connection, profile switching through BUTTON_5, and the
first built-in action types. Persistence currently uses JPA schema updates
against the local H2 database; Flyway migrations are reserved for a later
schema-evolution step. The frontend remains the standard Vite template and has
not been modified.

## Architecture decision

Spring Boot is the central local application. It will progressively replace the
Python bridge for serial communication, profile management, and action
execution. The Python bridge remains a useful behavioral reference, but must
not become a second source of truth or be indirectly driven by the backend.

## Recommended delivery plan

1. Install and verify the backend prerequisites on each target computer.
2. Assemble the five-button LCD hardware and validate the bidirectional
   protocol.
3. Connect the future React interface to the existing profile and serial API.
4. Add profile reordering and an explicit configuration UI for the action
   types.
5. Add USB reconnection, profile-engine unit tests, API integration tests, and
   manual OS testing.
6. Replace JPA automatic schema updates with Flyway migrations before a
   long-lived release.
7. Package the application as a simple, single-process user installation.

## Deployment

The root Dockerfile builds the Spring Boot backend with Java 17. The compose
file exposes port 3000 only on localhost and stores H2 data in the
controller-data named volume. See README.md for the exact commands and for the
Linux-only serial-device mapping option.

For the intended desktop controller use case, run the backend directly on the
host. This is necessary for macOS Accessibility permission and for actions that
must target the active host application. Docker is useful for API development,
data persistence tests, and Linux device experiments.

## Decisions to make before implementation

- Use the TC1602A-21T(R)(3) 16×2 parallel LCD with a contrast potentiometer
  and protected backlight.
- Select pins for four more buttons without conflicting with serial use.
- Choose JSON or H2/Flyway for V1 persistence.
- Define V1 action types and parameter formats.
- Decide whether startup restores the last profile or uses the first one.
- Choose automatic detection, manual serial-port selection, or both.
- Define officially supported operating systems for the first milestone.

## First usable release success criteria

- Each of five hardware buttons emits one reliable event per press.
- Button 5 cycles every profile in order and loops back to the first.
- The active profile is correctly displayed after changes and reconnection.
- Buttons 1–4 work even when the web interface is closed.
- Users can create and edit profiles locally.
- Missing hardware, inaccessible ports, and rejected OS actions are clearly
  reported.
