package io.github.martinhamon2.steamdeck_app.service;

import io.github.martinhamon2.steamdeck_app.domain.*;
import org.springframework.stereotype.Service;
import java.awt.Desktop;
import java.net.URI;
import java.util.*;

@Service
public class ActionExecutor {
    public void execute(ActionBinding action) {
        try {
            switch (action.getType()) {
                case PASTE -> keyboardShortcut("CMD_OR_CTRL+V");
                case KEYBOARD_SHORTCUT -> keyboardShortcut(requireValue(action));
                case OPEN_URL -> Desktop.getDesktop().browse(URI.create(requireValue(action)));
                case LAUNCH_APPLICATION -> new ProcessBuilder(requireValue(action).trim().split("\\s+")).start();
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not run action '" + action.getLabel() + "': " + error.getMessage(), error);
        }
    }

    private String requireValue(ActionBinding action) {
        if (action.getValue() == null || action.getValue().isBlank()) throw new IllegalArgumentException("This action needs a value");
        return action.getValue();
    }

    private void keyboardShortcut(String shortcut) throws Exception {
        if (!shortcut.matches("[A-Za-z0-9+_-]{1,64}")) throw new IllegalArgumentException("Invalid shortcut");
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String normalized = shortcut.startsWith("CMD_OR_CTRL+")
            ? (os.contains("mac") ? "CMD+" : "CTRL+") + shortcut.substring("CMD_OR_CTRL+".length())
            : shortcut;
        if (os.contains("mac")) {
            String[] parts = normalized.split("\\+");
            String key = parts[parts.length - 1].toLowerCase(Locale.ROOT);
            List<String> modifiers = new ArrayList<>();
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equalsIgnoreCase("CMD")) modifiers.add("command down");
                if (parts[i].equalsIgnoreCase("CTRL")) modifiers.add("control down");
                if (parts[i].equalsIgnoreCase("ALT")) modifiers.add("option down");
                if (parts[i].equalsIgnoreCase("SHIFT")) modifiers.add("shift down");
            }
            String script = "tell application \"System Events\" to keystroke \"" + key + "\" using {" + String.join(", ", modifiers) + "}";
            new ProcessBuilder("osascript", "-e", script).start();
        } else if (os.contains("win")) {
            String keys = normalized.replace("CTRL", "^").replace("ALT", "%").replace("SHIFT", "+").replace("CMD", "^").replace("+", "");
            new ProcessBuilder("powershell", "-NoProfile", "-Command", "Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.SendKeys]::SendWait('" + keys + "')").start();
        } else {
            new ProcessBuilder("xdotool", "key", "--clearmodifiers", normalized.toLowerCase(Locale.ROOT).replace("+", "+")).start();
        }
    }
}
