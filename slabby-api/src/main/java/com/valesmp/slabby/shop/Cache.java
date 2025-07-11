package com.valesmp.slabby.shop;

import java.time.Duration;

public interface Cache {

    void expire(final Duration time);

}
