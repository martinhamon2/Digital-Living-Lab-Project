#!/usr/bin/env python3
"""Receive BUTTON_1 from an Arduino Uno and paste in the focused application."""

import argparse
import platform
import shutil
import subprocess
import sys
import time

import serial
from serial.tools import list_ports


MACOS_PASTE_SCRIPT = '''
tell application "System Events"
    keystroke "v" using command down
end tell
'''


def paste() -> None:
    """Paste using the shortcut native to the current operating system."""
    operating_system = platform.system()

    if operating_system == "Darwin":
        subprocess.run(["osascript", "-e", MACOS_PASTE_SCRIPT], check=True)
        return

    if operating_system == "Windows":
        command = (
            "Add-Type -AssemblyName System.Windows.Forms; "
            "[System.Windows.Forms.SendKeys]::SendWait('^v')"
        )
        subprocess.run(["powershell", "-NoProfile", "-Command", command], check=True)
        return

    if operating_system == "Linux":
        if shutil.which("xdotool"):
            subprocess.run(["xdotool", "key", "--clearmodifiers", "ctrl+v"], check=True)
            return
        if shutil.which("wtype"):
            subprocess.run(
                ["wtype", "-M", "ctrl", "-k", "v", "-m", "ctrl"], check=True
            )
            return
        raise RuntimeError("Linux needs xdotool (X11) or wtype (Wayland).")

    raise RuntimeError(f"Unsupported operating system: {operating_system}")


def print_ports() -> int:
    ports = list(list_ports.comports())
    if not ports:
        print("No serial ports found. Connect the Arduino, then try again.")
        return 1
    for port in ports:
        print(f"{port.device}\t{port.description}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Turn Arduino BUTTON_1 serial events into paste shortcuts."
    )
    parser.add_argument("--port", help="e.g. /dev/cu.usbmodem1101 or COM3")
    parser.add_argument("--baud", type=int, default=115200)
    parser.add_argument("--list-ports", action="store_true", help="List serial ports")
    args = parser.parse_args()

    if args.list_ports:
        return print_ports()
    if not args.port:
        parser.error("--port is required unless --list-ports is used")

    try:
        with serial.Serial(args.port, args.baud, timeout=0.5) as device:
            # Opening an Uno serial port resets it. Ignore startup data.
            time.sleep(2)
            device.reset_input_buffer()
            print(f"Listening on {args.port}. Press Ctrl+C to stop.")

            while True:
                message = device.readline().decode("utf-8", errors="replace").strip()
                if message != "BUTTON_1":
                    continue

                print("Button pressed → paste")
                try:
                    paste()
                except (subprocess.CalledProcessError, RuntimeError) as error:
                    print(
                        "The operating system rejected the keyboard action. "
                        "See the README for platform-specific setup.",
                        file=sys.stderr,
                    )
                    print(error, file=sys.stderr)
    except serial.SerialException as error:
        print(f"Could not open serial port {args.port}: {error}", file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("\nBridge stopped.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
