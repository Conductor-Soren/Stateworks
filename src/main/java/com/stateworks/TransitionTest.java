package com.stateworks;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;


public class TransitionTest {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        long duration = 1000;

        StateTransition transition = new StateTransition(
                new VirtualState("off", false),
                new VirtualState("on", true),
                startTime,
                duration
        );

        System.out.println("=== Stateworks Transition Test ===");

        System.out.println("Start:    " + transition.getProgress(startTime));
        System.out.println("25%:      " + transition.getProgress(startTime + 250));
        System.out.println("50%:      " + transition.getProgress(startTime + 500));
        System.out.println("75%:      " + transition.getProgress(startTime + 750));
        System.out.println("Complete: " + transition.getProgress(startTime + 1000));

        System.out.println("=================================");
    }
}