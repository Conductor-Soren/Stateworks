package com.stateworks.network;

import com.stateworks.Stateworks;
import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.client.*;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record VisualStatePayload(
        BlockPos position,
        String stateSetId,
        String stateName,
        String previousStateName,
        String visualTransitionName,
        long transitionElapsed,
        long transitionDuration,
        Map<String, Double> previousSignals,
        Map<String, Double> signals
) implements CustomPacketPayload {

    public static final Type<VisualStatePayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            Stateworks.MODID,
                            "visual_state"
                    )
            );

    private static final StreamCodec<
            net.minecraft.network.FriendlyByteBuf,
            Optional<String>
            > OPTIONAL_STRING =
            ByteBufCodecs.optional(
                    ByteBufCodecs.STRING_UTF8
            );

    private static final StreamCodec<
            net.minecraft.network.FriendlyByteBuf,
            Map<String, Double>
            > SIGNALS =
            ByteBufCodecs.map(
                    HashMap::new,
                    ByteBufCodecs.STRING_UTF8,
                    ByteBufCodecs.DOUBLE
            );

    public VisualStatePayload {
        previousSignals = previousSignals == null
                ? Map.of()
                : Map.copyOf(previousSignals);
        signals = signals == null
                ? Map.of()
                : Map.copyOf(signals);
    }

    public static final StreamCodec<
            net.minecraft.network.FriendlyByteBuf,
            VisualStatePayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    VisualStatePayload::position,

                    ByteBufCodecs.STRING_UTF8,
                    VisualStatePayload::stateSetId,

                    ByteBufCodecs.STRING_UTF8,
                    VisualStatePayload::stateName,

                    OPTIONAL_STRING,
                    payload ->
                            Optional.ofNullable(
                                    payload.previousStateName()
                            ),

                    OPTIONAL_STRING,
                    payload ->
                            Optional.ofNullable(
                                    payload.visualTransitionName()
                            ),

                    ByteBufCodecs.VAR_LONG,
                    VisualStatePayload::transitionElapsed,

                    ByteBufCodecs.VAR_LONG,
                    VisualStatePayload::transitionDuration,

                    SIGNALS,
                    VisualStatePayload::previousSignals,

                    SIGNALS,
                    VisualStatePayload::signals,

                    (
                            position,
                            stateSetId,
                            stateName,
                            previousStateName,
                            visualTransitionName,
                            transitionElapsed,
                            transitionDuration,
                            previousSignals,
                            signals
                    ) ->
                            new VisualStatePayload(
                                    position,
                                    stateSetId,
                                    stateName,
                                    previousStateName.orElse(null),
                                    visualTransitionName.orElse(null),
                                    transitionElapsed,
                                    transitionDuration,
                                    previousSignals,
                                    signals
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
