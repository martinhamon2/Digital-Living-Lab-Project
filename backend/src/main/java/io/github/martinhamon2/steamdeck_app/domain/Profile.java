package io.github.martinhamon2.steamdeck_app.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "profiles")
public class Profile {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 32)
    private String name;
    @Column(nullable = false, unique = true)
    private int position;
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("buttonNumber ASC")
    private List<ActionBinding> actions = new ArrayList<>();

    protected Profile() {}
    public Profile(String name, int position) { this.name = name; this.position = position; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getPosition() { return position; }
    public List<ActionBinding> getActions() { return actions; }
    public void setName(String name) { this.name = name; }
    public void setPosition(int position) { this.position = position; }
    public void replaceActions(List<ActionBinding> replacements) {
        actions.clear();
        replacements.forEach(action -> { action.setProfile(this); actions.add(action); });
    }
}
