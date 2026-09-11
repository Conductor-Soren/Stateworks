package com.stateworks.output;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

public interface StateOutputTarget {

    void apply(
            StateOutput output,
            long currentTime
    );
}