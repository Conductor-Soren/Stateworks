package com.stateworks.core.context;

import com.stateworks.output.OutputDefinition;
import com.stateworks.output.StateOutputTarget;

/** Platform capability for creating a target for a declared Stateworks output. */
public interface StateOutputContext extends StateContext {

    StateOutputTarget createOutputTarget(OutputDefinition definition);
}
