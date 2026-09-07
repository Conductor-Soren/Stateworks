package com.stateworks.signal;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

/**
 * Interpolates Stateworks presentation signals between two numeric values.
 */
public final class SignalInterpolator {

    private SignalInterpolator() {
    }

    public static double linear(
            double previous,
            double next,
            float progress
    ) {
        double clamped = Math.max(0.0D, Math.min(1.0D, progress));
        return previous + (next - previous) * clamped;
    }
}
