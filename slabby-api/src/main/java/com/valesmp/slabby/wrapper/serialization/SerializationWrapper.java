package com.valesmp.slabby.wrapper.serialization;

import com.valesmp.slabby.exception.SlabbyException;

public interface SerializationWrapper {

    String serialize(final Object item) throws SlabbyException;

    <T> T deserialize(final String item) throws SlabbyException;

}
