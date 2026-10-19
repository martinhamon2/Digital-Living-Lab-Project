package io.github.martinhamon2.steamdeck_app.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    List<Profile> findAllByOrderByPositionAsc();
}
