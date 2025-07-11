package gg.mew.slabby.wrapper.serialization;

import gg.mew.slabby.exception.SlabbyException;

public interface SerializationWrapper {

    String serialize(final Object item) throws SlabbyException;

    <T> T deserialize(final String item) throws SlabbyException;

}
