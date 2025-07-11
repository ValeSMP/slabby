package com.valesmp.slabby.shop.log;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true, chain = false)
@Getter
@Setter
@RequiredArgsConstructor
public abstract class ValueChanged<T> {
    private final T from;
    private final T to;

    public static final class Double extends ValueChanged<java.lang.Double> {
        public Double(final java.lang.Double from, final java.lang.Double to) {
            super(from, to);
        }
    }

    public static final class Int extends ValueChanged<Integer> {
        public Int(final Integer from, final Integer to) {
            super(from, to);
        }
    }

    public static final class String extends ValueChanged<java.lang.String> {
        public String(final java.lang.String from, final java.lang.String to) {
            super(from, to);
        }
    }
}
