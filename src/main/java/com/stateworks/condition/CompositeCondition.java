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

import java.util.List;

public final class CompositeCondition implements Condition {

    public enum Mode {
        ALL,
        ANY
    }

    private final Mode mode;
    private final List<Condition> conditions;

    public CompositeCondition(Mode mode, List<Condition> conditions) {
        this.mode = mode;
        this.conditions = conditions;
    }

    @Override
    public boolean evaluate(StateContext context) {
        return switch (mode) {
            case ALL -> {
                for (Condition condition : conditions) {
                    if (!condition.evaluate(context)) {
                        yield false;
                    }
                }

                yield true;
            }

            case ANY -> {
                for (Condition condition : conditions) {
                    if (condition.evaluate(context)) {
                        yield true;
                    }
                }

                yield false;
            }
        };
    }

    @Override
    public String describe() {
        StringBuilder result = new StringBuilder();

        result.append(mode.name()).append("[");

        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }

            result.append(conditions.get(i).describe());
        }

        result.append("]");

        return result.toString();
    }
}