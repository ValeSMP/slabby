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
    public String serialize(final Object obj) throws SlabbyException {
        //NOTE: adds extra nbt tag for non-stackables, so only do this for stackables
        if (obj instanceof ItemStack itemStack && itemStack.getMaxStackSize() != 1)
            itemStack.setAmount(1);

        final var byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            try (final var bukkitObjectOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream)) {
                bukkitObjectOutputStream.writeObject(obj);
            }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (final IOException e) {
            throw new UnrecoverableException("Error while attempting to serialize item", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(final String obj) throws SlabbyException {
        final var byteArrayInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(obj));
        try {
            try (final var bukkitObjectInputStream = new BukkitObjectInputStream(byteArrayInputStream)) {
                return (T) bukkitObjectInputStream.readObject();
            }
        } catch (final IOException | ClassNotFoundException e) {
            throw new UnrecoverableException("Error while attempting to deserialize item", e);
        }
    }

}
