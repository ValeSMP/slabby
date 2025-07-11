package com.valesmp.slabby;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;

/**
 * SlabbyHelper provides a static singleton instance to the Slabby API service. It is only supposed to be used in cases where dependency injection is impossible.
 */
@UtilityClass
@Accessors(fluent = true)
public final class SlabbyHelper {

    @Getter
    private SlabbyAPI api;

    /**
     * Used by Slabby for initializing the static API singleton. You are not supposed to call this method!
     */
    public void init(@NonNull final SlabbyAPI api) {
        if (SlabbyHelper.api != null) {
            throw new UnsupportedOperationException("API already initialized");
        }
        SlabbyHelper.api = api;
    }

}
