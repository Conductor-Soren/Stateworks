package com.stateworks.signal;

import com.stateworks.visual.*;

import com.stateworks.core.context.StateContext;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.output.*;
public record SignalValue(
        String name,
        double value
) {
}
