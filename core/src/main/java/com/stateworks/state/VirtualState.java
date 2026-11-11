package com.stateworks.state;

import com.stateworks.visual.*;

import com.stateworks.core.context.StateContext;
import com.stateworks.condition.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
public record VirtualState<T>(String name, T value) {
}