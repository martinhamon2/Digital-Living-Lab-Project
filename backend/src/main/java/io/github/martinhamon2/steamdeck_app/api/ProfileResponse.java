package io.github.martinhamon2.steamdeck_app.api;

import io.github.martinhamon2.steamdeck_app.domain.*;
import java.util.*;

public record ProfileResponse(UUID id, String name, int position, List<ActionResponse> actions) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(profile.getId(), profile.getName(), profile.getPosition(),
            profile.getActions().stream().map(a -> new ActionResponse(a.getButtonNumber(), a.getLabel(), a.getType(), a.getValue())).toList());
    }
    public record ActionResponse(int buttonNumber, String label, ActionType type, String value) {}
}
