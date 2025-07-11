package com.valesmp.slabby.exception;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;

@Accessors(fluent = true, chain = false)
@Getter
public final class FaultException extends SlabbyException {

    private final Component component;

    public FaultException(final Component component, final Throwable throwable) {
        super(throwable);

        this.component = component;
    }

    public FaultException(final Component component) {
        this.component = component;
    }

}
