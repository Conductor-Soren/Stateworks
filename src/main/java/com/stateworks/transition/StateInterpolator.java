package com.stateworks.transition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

public final class StateInterpolator {

    private StateInterpolator() {
    }

    public static double linear(
            double from,
            double to,
            float progress
    ) {
        float clamped =
                Math.max(0.0f, Math.min(1.0f, progress));

        return from + (to - from) * clamped;
    }

    /**
     * Interpolates two state values using Stateworks' default value rules.
     *
     * Numeric values are linearly interpolated. Non-numeric values remain at
     * the previous value until the transition completes, at which point the
     * next value is returned.
     */
    public static Object interpolate(
            Object from,
            Object to,
            float progress
    ) {
        float clamped =
                Math.max(0.0f, Math.min(1.0f, progress));

        if (from instanceof Number fromNumber
                && to instanceof Number toNumber) {
            return linear(
                    fromNumber.doubleValue(),
                    toNumber.doubleValue(),
                    clamped
            );
        }

        return clamped >= 1.0f ? to : from;
    }
}