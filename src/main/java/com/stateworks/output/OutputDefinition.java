package com.stateworks.output;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

import java.util.List;
import java.util.Map;

public record OutputDefinition(
        String type,
        List<Integer> offset,
        boolean relativeToFacing,
        String property,
        Map<String, Object> values
) {

    public OutputDefinition {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks output type cannot be null or blank"
            );
        }

        offset = offset == null
                ? List.of(0, 0, 0)
                : List.copyOf(offset);

        if (offset.size() != 3) {
            throw new IllegalArgumentException(
                    "Stateworks output offset must contain "
                            + "exactly three values"
            );
        }

        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks output property cannot be null "
                            + "or blank"
            );
        }

        values = values == null
                ? Map.of()
                : Map.copyOf(values);
    }
}