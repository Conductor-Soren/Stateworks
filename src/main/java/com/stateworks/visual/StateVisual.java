package com.stateworks.visual;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;


public record StateVisual(
        String stateName,
        String model
) {
    public StateVisual {
        if (stateName == null || stateName.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks visual state name cannot be null or blank"
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks visual model cannot be null or blank"
            );
        }
    }
}