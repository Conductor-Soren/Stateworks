package com.stateworks.core.context;

import java.util.Set;

/**
 * Platform-neutral read-only view of the state at a Stateworks evaluation
 * target. The core engine depends only on these values and never on Minecraft
 * world, position, or block-state classes.
 */
public interface StateContext {

    /** Returns the namespaced identifier of the block at this context. */
    String blockId();

    /** Returns a block property value as text, or null when absent. */
    String propertyValue(String propertyName);

    /** Returns the names of all properties available on this block. */
    Set<String> propertyNames();

    /** Returns a numeric property value, or null when absent/non-numeric. */
    Number getNumericProperty(String propertyName);

    /** Returns whether this block belongs to the supplied namespaced tag. */
    boolean isInBlockTag(String tagId);
}
