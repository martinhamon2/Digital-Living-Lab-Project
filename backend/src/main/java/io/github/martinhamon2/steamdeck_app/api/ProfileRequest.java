package io.github.martinhamon2.steamdeck_app.api;

import io.github.martinhamon2.steamdeck_app.domain.ActionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ProfileRequest(@NotBlank @Size(max = 32) String name, @NotNull @Size(min = 4, max = 4) List<@Valid ActionRequest> actions) {
    public record ActionRequest(@Min(1) @Max(4) int buttonNumber, @NotBlank @Size(max = 32) String label,
                                @NotNull ActionType type, @Size(max = 512) String value) {}
}
