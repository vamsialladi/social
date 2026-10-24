package com.truehunt.social.model;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "network_profiles")
public class NetworkProfileEntity {

    @Id
    private String id; // e.g., "maya", "rohan"

    private String name;
    private String initials;
    private String status = "active"; // "active", "offline"
    private String role;
    private String location;
    private String mutualConnections;
    private String avatarClass = "network-avatar-coral";
    private boolean connected = false;
    private boolean following = false;

    public NetworkProfileEntity() {}

    public NetworkProfileEntity(String id, String name, String initials, String status, String role, String location, String mutualConnections, String avatarClass) {
        this.id = id;
        this.name = name;
        this.initials = initials;
        this.status = status;
        this.role = role;
        this.location = location;
        this.mutualConnections = mutualConnections;
        this.avatarClass = avatarClass;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getMutualConnections() { return mutualConnections; }
    public void setMutualConnections(String mutualConnections) { this.mutualConnections = mutualConnections; }

    public String getAvatarClass() { return avatarClass; }
    public void setAvatarClass(String avatarClass) { this.avatarClass = avatarClass; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public boolean isFollowing() { return following; }
    public void setFollowing(boolean following) { this.following = following; }

    public Map<String, Object> toDto() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", id);
        dto.put("name", name != null ? name : "");
        dto.put("initials", initials != null ? initials : "");
        dto.put("status", status != null ? status : "active");
        dto.put("role", role != null ? role : "");
        dto.put("location", location != null ? location : "");
        dto.put("mutualConnections", mutualConnections != null ? mutualConnections : "");
        dto.put("avatarClass", avatarClass != null ? avatarClass : "network-avatar-coral");
        dto.put("isConnected", connected);
        dto.put("isFollowing", following);
        return dto;
    }
}
