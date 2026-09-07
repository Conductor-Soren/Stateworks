package com.stateworks.condition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

@FunctionalInterface
public interface Condition {

    boolean evaluate(StateContext context);

    default String describe() {
        return getClass().getSimpleName();
    }
}