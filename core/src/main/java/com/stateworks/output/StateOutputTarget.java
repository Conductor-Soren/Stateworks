package com.stateworks.output;

import com.stateworks.visual.*;

import com.stateworks.core.context.StateContext;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
public interface StateOutputTarget {

    void apply(
            StateOutput output,
            long currentTime
    );
}