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

public final class NotCondition implements Condition {

    private final Condition condition;

    public NotCondition(Condition condition) {
        this.condition = condition;
    }

    @Override
    public boolean evaluate(StateContext context) {
        return !condition.evaluate(context);
    }

    @Override
    public String describe() {
        return "NOT[" + condition.describe() + "]";
    }
}