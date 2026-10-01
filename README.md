# One-button Stream Deck — paste test

This first prototype makes one pushbutton paste into the currently focused
application: **Command + V** on macOS, **Ctrl + V** on Windows and Linux.

The Arduino Uno R3 sends `BUTTON_1` through its USB serial connection. A small
program on the computer receives that message and triggers the appropriate
paste shortcut in the currently active application.

> The Uno R3 cannot act as a USB keyboard by itself, so the computer bridge is
> required for this version.

## Wiring

Use the breadboard and one of the kit's pushbuttons:

| Button leg | Connect to |
| --- | --- |
| One leg | Arduino **D2** |
| Diagonally opposite leg | Arduino **GND** |

No resistor is needed: the sketch enables Arduino's internal pull-up resistor.
The other two button legs are duplicates and can remain unconnected. Make sure
the button straddles the centre gap of the breadboard. If it does not work,
rotate the button by 90 degrees.

## 1. Upload the Arduino sketch

1. Connect the Uno to the computer with USB.
2. Open `arduino/one_button_cmd_v/one_button_cmd_v.ino` in Arduino IDE or Zed.
3. Select **Arduino Uno** and its USB port, then upload.
4. Open Serial Monitor at **115200 baud**. Each press must print `BUTTON_1`.
   Close Serial Monitor afterwards: only one program can use the port at once.

## 2. Install and start the computer bridge

Install Python 3 if needed. From this project folder, create an isolated Python
environment and install the only dependency once.

**macOS / Linux:**

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r bridge/requirements.txt
```

**Windows PowerShell:**

```powershell
py -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r bridge\requirements.txt
```

Find the Arduino serial port on any system:

```bash
python bridge/cmd_v_bridge.py --list-ports
```

On Windows the port is usually `COM3`, `COM4`, etc. On macOS it usually begins
with `/dev/cu.usbmodem`; on Linux it is commonly `/dev/ttyACM0`.

Start the bridge, replacing the example port with yours:

```bash
python bridge/cmd_v_bridge.py --port /dev/cu.usbmodemXXXX
```

On Windows:

```powershell
python bridge\cmd_v_bridge.py --port COM3
```

With the bridge running, select a text field in any app, copy some text, and
press the physical button. The copied text should paste into that field. Stop
the bridge with `Ctrl+C`.

### Platform notes

- **macOS:** grant your terminal/Python **Accessibility** permission in
  **System Settings → Privacy & Security → Accessibility**, then restart the
  bridge.
- **Windows:** the bridge uses Windows PowerShell already included with Windows.
  If antivirus blocks simulated keystrokes, allow Python.
- **Linux (X11):** install `xdotool`, for example `sudo apt install xdotool`.
- **Linux (Wayland):** install `wtype`, for example `sudo apt install wtype`.
  Some Wayland desktops deliberately block global simulated keystrokes; use an
  X11 session or configure the desktop's permitted input method if needed.

## Troubleshooting

- `Could not open serial port`: close Arduino Serial Monitor and confirm the
  port shown by `--list-ports`.
- The bridge logs `BUTTON_1` but nothing pastes: review the platform notes for
  permissions or the required Linux utility.
- The action goes to the wrong app: the shortcut is sent to whichever text field
  has focus when the button is pressed.
