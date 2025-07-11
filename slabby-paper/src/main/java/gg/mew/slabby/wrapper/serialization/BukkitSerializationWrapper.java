package gg.mew.slabby.wrapper.serialization;

import gg.mew.slabby.exception.SlabbyException;
import gg.mew.slabby.exception.UnrecoverableException;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class BukkitSerializationWrapper implements SerializationWrapper {

    @Override
    public String serialize(final Object item) throws SlabbyException {
        if (!(item instanceof ItemStack itemStack))
            throw new UnrecoverableException("Object is not an instance of ItemStack");

        itemStack.setAmount(1);

        return Base64.getEncoder().encodeToString(itemStack.serializeAsBytes());
    }

    @Override
    public <T> T deserialize(final String item) throws SlabbyException {
        //noinspection unchecked
        return (T) ItemStack.deserializeBytes(Base64.getDecoder().decode(item));
    }

}
