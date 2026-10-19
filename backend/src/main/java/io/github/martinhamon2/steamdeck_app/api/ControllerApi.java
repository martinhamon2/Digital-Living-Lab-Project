package io.github.martinhamon2.steamdeck_app.api;

import io.github.martinhamon2.steamdeck_app.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ControllerApi {
    private final ProfileService profiles;
    private final SerialService serial;
    public ControllerApi(ProfileService profiles, SerialService serial) { this.profiles = profiles; this.serial = serial; }
    @GetMapping("/status") public StatusResponse status() { return new StatusResponse(profiles.active(), serial.status()); }
    @GetMapping("/profiles") public List<ProfileResponse> profiles() { return profiles.all(); }
    @PostMapping("/profiles") @ResponseStatus(HttpStatus.CREATED) public ProfileResponse create(@Valid @RequestBody ProfileRequest request) { return profiles.create(request); }
    @PutMapping("/profiles/{id}") public ProfileResponse update(@PathVariable UUID id, @Valid @RequestBody ProfileRequest request) { return profiles.update(id, request); }
    @DeleteMapping("/profiles/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { profiles.delete(id); }
    @PutMapping("/profiles/order") public List<ProfileResponse> reorder(@Valid @RequestBody ProfileOrderRequest request) { return profiles.reorder(request.ids()); }
    @PostMapping("/profiles/{id}/activate") public ProfileResponse activate(@PathVariable UUID id) { return profiles.activate(id); }
    @GetMapping("/serial/ports") public List<SerialService.PortInfo> ports() { return serial.ports(); }
    @PutMapping("/serial/port") public SerialService.ConnectionStatus connect(@Valid @RequestBody SerialPortRequest request) { return serial.connect(request.port()); }
    @DeleteMapping("/serial/port") @ResponseStatus(HttpStatus.NO_CONTENT) public void disconnect() { serial.disconnect(); }
    public record SerialPortRequest(@jakarta.validation.constraints.NotBlank String port) {}
    public record ProfileOrderRequest(@jakarta.validation.constraints.NotEmpty List<UUID> ids) {}
    public record StatusResponse(ProfileResponse activeProfile, SerialService.ConnectionStatus serial) {}
}
