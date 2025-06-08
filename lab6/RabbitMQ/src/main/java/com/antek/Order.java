package com.antek;

public class Order {

    private final String teamName;
    private final EquipmentType equipmentType;

    public Order(String teamName, EquipmentType equipmentType) {
        this.teamName = teamName;
        this.equipmentType = equipmentType;
    }

    public String toMessage() {
        return teamName + ':' + equipmentType.toString();
    }

    public Order fromMessage(String message) {
         String[] parts = message.split(":");
         return new Order(parts[0],EquipmentType.valueOf(parts[1]));
    }
}
