package io.github.martinhamon2.steamdeck_app.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "action_bindings", uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "button_number"}))
public class ActionBinding {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "profile_id")
    private Profile profile;
    @Column(name = "button_number", nullable = false)
    private int buttonNumber;
    @Column(nullable = false, length = 32)
    private String label;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private ActionType type;
    @Column(length = 512)
    private String value;

    protected ActionBinding() {}
    public ActionBinding(int buttonNumber, String label, ActionType type, String value) {
        this.buttonNumber = buttonNumber; this.label = label; this.type = type; this.value = value;
    }
    public int getButtonNumber() { return buttonNumber; }
    public String getLabel() { return label; }
    public ActionType getType() { return type; }
    public String getValue() { return value; }
    public void setProfile(Profile profile) { this.profile = profile; }
}
