package com.stateworks.transition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

public record StateTransition<T>(
        VirtualState<T> previous,
        VirtualState<T> next,
        long startTime,
        long duration
) {

    public float getProgress(long currentTime) {
        if (duration <= 0) {
            return 1.0f;
        }

        long elapsed = currentTime - startTime;

        if (elapsed <= 0) {
            return 0.0f;
        }

        if (elapsed >= duration) {
            return 1.0f;
        }

        return (float) elapsed / duration;
    }

    public boolean isComplete(long currentTime) {
        return getProgress(currentTime) >= 1.0f;
    }
}