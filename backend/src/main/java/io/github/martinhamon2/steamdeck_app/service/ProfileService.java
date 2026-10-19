package io.github.martinhamon2.steamdeck_app.service;

import io.github.martinhamon2.steamdeck_app.api.*;
import io.github.martinhamon2.steamdeck_app.domain.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ProfileService {
    private final ProfileRepository repository;
    private final ActionExecutor executor;
    private final SerialService serial;
    private UUID activeProfileId;
    public ProfileService(ProfileRepository repository, ActionExecutor executor, SerialService serial) {
        this.repository = repository; this.executor = executor; this.serial = serial;
    }
    @PostConstruct @Transactional
    void initialize() {
        if (repository.count() == 0) {
            Profile profile = new Profile("Default profile", 1);
            profile.replaceActions(List.of(new ActionBinding(1, "Paste", ActionType.PASTE, null),
                new ActionBinding(2, "Copy", ActionType.KEYBOARD_SHORTCUT, "CMD_OR_CTRL+C"),
                new ActionBinding(3, "Undo", ActionType.KEYBOARD_SHORTCUT, "CMD_OR_CTRL+Z"),
                new ActionBinding(4, "Select all", ActionType.KEYBOARD_SHORTCUT, "CMD_OR_CTRL+A")));
            activeProfileId = repository.save(profile).getId();
        } else activeProfileId = repository.findAllByOrderByPositionAsc().get(0).getId();
        sendActiveProfile();
    }
    public List<ProfileResponse> all() { return repository.findAllByOrderByPositionAsc().stream().map(ProfileResponse::from).toList(); }
    public ProfileResponse active() { return ProfileResponse.from(active()); }
    @Transactional public ProfileResponse create(ProfileRequest request) {
        Profile profile = new Profile(request.name().trim(), (int) repository.count() + 1);
        apply(profile, request); return ProfileResponse.from(repository.save(profile));
    }
    @Transactional public ProfileResponse update(UUID id, ProfileRequest request) {
        Profile profile = find(id); apply(profile, request); ProfileResponse result = ProfileResponse.from(profile);
        if (id.equals(activeProfileId)) sendActiveProfile(); return result;
    }
    @Transactional public void delete(UUID id) {
        if (repository.count() == 1) throw new IllegalStateException("At least one profile is required");
        repository.delete(find(id));
        List<Profile> profiles = repository.findAllByOrderByPositionAsc();
        for (int i = 0; i < profiles.size(); i++) profiles.get(i).setPosition(i + 1);
        if (id.equals(activeProfileId)) { activeProfileId = profiles.get(0).getId(); sendActiveProfile(); }
    }
    @Transactional public List<ProfileResponse> reorder(List<UUID> orderedIds) {
        List<Profile> current = repository.findAllByOrderByPositionAsc();
        if (orderedIds.size() != current.size() || new HashSet<>(orderedIds).size() != current.size()
            || !new HashSet<>(orderedIds).equals(current.stream().map(Profile::getId).collect(java.util.stream.Collectors.toSet()))) {
            throw new IllegalArgumentException("The order must contain every profile exactly once");
        }
        for (int index = 0; index < orderedIds.size(); index++) find(orderedIds.get(index)).setPosition(index + 1);
        return repository.findAllByOrderByPositionAsc().stream().map(ProfileResponse::from).toList();
    }
    public synchronized ProfileResponse activate(UUID id) { activeProfileId = find(id).getId(); sendActiveProfile(); return active(); }
    public synchronized void handleButton(int button) {
        if (button == 5) { List<Profile> profiles = repository.findAllByOrderByPositionAsc(); int index = 0;
            for (; index < profiles.size(); index++) if (profiles.get(index).getId().equals(activeProfileId)) break;
            activeProfileId = profiles.get((index + 1) % profiles.size()).getId(); sendActiveProfile(); return; }
        if (button < 1 || button > 4) return;
        active().getActions().stream().filter(a -> a.getButtonNumber() == button).findFirst().ifPresent(executor::execute);
    }
    private Profile active() { return find(activeProfileId); }
    private Profile find(UUID id) { return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Profile not found")); }
    private void apply(Profile profile, ProfileRequest request) {
        if (request.actions().stream().map(ProfileRequest.ActionRequest::buttonNumber).distinct().count() != 4) throw new IllegalArgumentException("Buttons 1 to 4 must each be configured once");
        profile.setName(request.name().trim());
        profile.replaceActions(request.actions().stream().map(a -> new ActionBinding(a.buttonNumber(), a.label().trim(), a.type(), a.value())).toList());
    }
    private void sendActiveProfile() { if (activeProfileId != null) serial.sendProfileName(active().getName()); }
}
