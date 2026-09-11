package com.stateworks.visual;

public record StateVisual(
        String stateName,
        String model
) {
    public StateVisual {
        if (stateName == null || stateName.isBlank()) {
            throw new IllegalArgumentException("Stateworks visual state name cannot be null or blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Stateworks visual model cannot be null or blank");
        }
    }
}
