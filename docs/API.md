# Local Controller API

## Overview

The Spring Boot application exposes a JSON REST API on localhost port 3000.
It is the only authority for profiles, the active profile, the Arduino serial
connection, and action configuration. The React frontend is expected to consume
this API; it must not communicate with the Arduino directly.

Base URL:

    http://localhost:3000/api

JSON request bodies must use Content-Type: application/json. Browser requests
from localhost ports 8080 and 8081 are allowed by CORS.

## Concepts

- A profile has a unique identifier, a name, a display order, and exactly four
  action bindings.
- Bindings must use button numbers 1, 2, 3, and 4 exactly once.
- Button 5 is not configurable. It moves to the next profile and loops back to
  the first profile.
- The active profile is held in memory. It starts as the first profile by
  position after the backend restarts.
- The TC1602A 16×2 display receives the active profile name. Before sending it,
  the backend replaces line breaks with spaces and unsupported non-ASCII
  characters with question marks. The firmware shows 16 characters directly;
  longer names pause, then scroll automatically.

## Data types

### Action types

| Type | Value | Effect |
| --- | --- | --- |
| PASTE | null is recommended | Sends Paste on the local operating system. |
| KEYBOARD_SHORTCUT | Shortcut string | Sends a shortcut such as CMD_OR_CTRL+T. |
| OPEN_URL | Absolute URL | Opens the URL in the default browser. |
| LAUNCH_APPLICATION | Command and arguments | Starts a local command without a shell. |

Shortcut strings contain letters, digits, plus, underscore, or hyphen only.
Examples: CMD_OR_CTRL+T, CTRL+SHIFT+P, CMD+V. CMD_OR_CTRL selects Command on
macOS and Control on other systems.

### Profile response

    {
      "id": "e70f3a6d-7c1c-4b6d-8e92-6790fa5e67f1",
      "name": "Browser",
      "position": 1,
      "actions": [
        {"buttonNumber": 1, "label": "New tab", "type": "KEYBOARD_SHORTCUT", "value": "CMD_OR_CTRL+T"},
        {"buttonNumber": 2, "label": "Website", "type": "OPEN_URL", "value": "https://example.com"},
        {"buttonNumber": 3, "label": "Paste", "type": "PASTE", "value": null},
        {"buttonNumber": 4, "label": "Launch", "type": "LAUNCH_APPLICATION", "value": "open"}
      ]
    }

### Profile request

Create and update requests use the following shape:

    {
      "name": "Browser",
      "actions": [
        {"buttonNumber": 1, "label": "New tab", "type": "KEYBOARD_SHORTCUT", "value": "CMD_OR_CTRL+T"},
        {"buttonNumber": 2, "label": "Website", "type": "OPEN_URL", "value": "https://example.com"},
        {"buttonNumber": 3, "label": "Paste", "type": "PASTE", "value": null},
        {"buttonNumber": 4, "label": "Launch", "type": "LAUNCH_APPLICATION", "value": "open"}
      ]
    }

Constraints:

- name: non-empty, maximum 32 characters;
- actions: exactly four entries;
- buttonNumber: every value from 1 through 4 occurs exactly once;
- label: non-empty, maximum 32 characters;
- value: maximum 512 characters.

## Endpoints

### Get application status

    GET /status

Returns 200 with the active profile and serial connection state.

    {
      "activeProfile": { ...profile response... },
      "serial": {"connected": true, "port": "/dev/ttyACM0", "error": null}
    }

### List profiles

    GET /profiles

Returns 200 and a profile-response array ordered by position.

### Create profile

    POST /profiles

Send a profile request. Returns 201 and the created profile. The new profile is
placed last and does not automatically become active.

### Update profile

    PUT /profiles/{id}

Send a complete profile request. Returns 200 and the updated profile. Updating
the active profile immediately sends its new name to the Arduino display.

### Delete profile

    DELETE /profiles/{id}

Returns 204. The final remaining profile cannot be deleted. If the active
profile is deleted, the first remaining profile becomes active.

### Reorder profiles

    PUT /profiles/order

Send every existing profile identifier exactly once, in the desired order:

    {"ids":["e70f3a6d-7c1c-4b6d-8e92-6790fa5e67f1","46d2a457-65bb-4a98-87cf-b8f93c1279a4"]}

Returns 200 with the reordered profile array.

### Activate profile

    POST /profiles/{id}/activate

Returns 200 and the activated profile, and sends its name to the Arduino if it
is connected.

### List serial ports

    GET /serial/ports

Returns 200:

    [{"name":"/dev/ttyACM0","description":"Arduino Uno"}]

### Connect to a serial port

    PUT /serial/port

    {"port":"/dev/ttyACM0"}

Returns 200 with:

    {"connected":true,"port":"/dev/ttyACM0","error":null}

The port must be returned by the port-list endpoint. The backend uses 115200
baud and expects newline-delimited BUTTON_1 through BUTTON_5 messages.

### Disconnect from the serial port

    DELETE /serial/port

Returns 204.

## Errors

The API returns a JSON error body for domain and lookup failures:

    {"error":"Profile not found"}

| Status | Meaning |
| --- | --- |
| 400 | Invalid action/profile data, invalid reordering, or an action cannot run. |
| 404 | Unknown profile or serial port. |
| 500 | Unexpected server failure. |

Bean-validation errors use Spring Boot’s standard validation response format.

## Interactive specification

When the backend is running, OpenAPI Swagger UI is available at:

    http://localhost:3000/swagger-ui/index.html
