package io.github.martinhamon2.steamdeck_app.service;

import com.fazecast.jSerialComm.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SerialService {
    private final ObjectProvider<ProfileService> profiles;
    private SerialPort port;
    private String error;
    public SerialService(ObjectProvider<ProfileService> profiles, @Value("${controller.serial-port:}") String configuredPort) {
        this.profiles = profiles;
        if (!configuredPort.isBlank()) connect(configuredPort);
    }
    public List<PortInfo> ports() {
        return Arrays.stream(SerialPort.getCommPorts()).map(p -> new PortInfo(p.getSystemPortName(), p.getDescriptivePortName())).toList();
    }
    public synchronized ConnectionStatus connect(String portName) {
        disconnect();
        SerialPort candidate = Arrays.stream(SerialPort.getCommPorts()).filter(p -> p.getSystemPortName().equals(portName)).findFirst()
            .orElseThrow(() -> new NoSuchElementException("Serial port not found: " + portName));
        candidate.setBaudRate(115200);
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        if (!candidate.openPort()) { error = "Could not open " + portName; return status(); }
        candidate.addDataListener(new SerialPortMessageListener() {
            public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }
            public byte[] getMessageDelimiter() { return new byte[] {'\n'}; }
            public boolean delimiterIndicatesEndOfMessage() { return true; }
            public void serialEvent(SerialPortEvent event) { receive(new String(event.getReceivedData(), StandardCharsets.UTF_8).trim()); }
        });
        port = candidate;
        error = null;
        // The Arduino may have started before the backend opened its port.
        // Always push the current state after a successful connection.
        sendProfileName(profiles.getObject().active().name());
        return status();
    }
    public synchronized void disconnect() { if (port != null) { port.removeDataListener(); port.closePort(); port = null; } }
    public synchronized ConnectionStatus status() { return new ConnectionStatus(port != null && port.isOpen(), port == null ? null : port.getSystemPortName(), error); }
    public synchronized void sendProfileName(String name) {
        if (port == null || !port.isOpen()) return;
        String displayName = name.replaceAll("[\\r\\n]", " ").replaceAll("[^\\x20-\\x7E]", "?");
        byte[] message = ("PROFILE:" + displayName + "\n").getBytes(StandardCharsets.UTF_8);
        port.writeBytes(message, message.length);
    }
    private void receive(String message) {
        if (!message.matches("BUTTON_[1-5]")) return;
        profiles.getObject().handleButton(message.charAt(message.length() - 1) - '0');
    }
    @PreDestroy void close() { disconnect(); }
    public record PortInfo(String name, String description) {}
    public record ConnectionStatus(boolean connected, String port, String error) {}
}
