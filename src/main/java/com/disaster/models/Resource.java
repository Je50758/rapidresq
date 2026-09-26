package com.disaster.models;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Represents physical equipment, vehicles, or kits assigned to a response team.
 * Demonstrates OOP Composition within ResponseTeam.
 */
@Entity
@Table(name = "resources")
public class Resource {
    public enum Type {
        VEHICLE,
        EQUIPMENT,
        HEAVY_EQUIPMENT,
        MEDICAL_UNIT,
        RESCUE_GEAR,
        COMMUNICATION_KIT
    }

    @Id
    private String id;
    private String name;

    @Enumerated(EnumType.STRING)
    private Type type;

    private int quantity;
    private String operationalCondition;

    public Resource() {
    }

    public Resource(String id, String name, Type type, int quantity, String operationalCondition) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.quantity = Math.max(1, quantity);
        this.operationalCondition = operationalCondition != null ? operationalCondition : "Optimal";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public String getOperationalCondition() {
        return operationalCondition;
    }

    public void setOperationalCondition(String operationalCondition) {
        this.operationalCondition = operationalCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;
        Resource resource = (Resource) o;
        return Objects.equals(id, resource.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s (x%d, %s - %s)", name, quantity, type, operationalCondition);
    }
}
