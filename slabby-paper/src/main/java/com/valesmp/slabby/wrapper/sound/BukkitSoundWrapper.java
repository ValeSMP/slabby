package com.valesmp.slabby.wrapper.sound;

import com.valesmp.slabby.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.Objects;
import java.util.UUID;

public final class BukkitSoundWrapper implements SoundWrapper {

    @Override
    public void play(final UUID uniqueId, final Shop shop, final Sounds sound) {
        final var player = Bukkit.getPlayer(uniqueId);

        final var x = shop.x() != null ? shop.x() : player.getLocation().getBlockX();
        final var y = shop.y() != null ? shop.y() : player.getLocation().getBlockY();
        final var z = shop.z() != null ? shop.z() : player.getLocation().getBlockZ();
        final var world = shop.world() != null ? shop.world() : player.getLocation().getWorld().getName();

        play(uniqueId, x, y, z, world, sound);
    }

    @Override
    public void play(final UUID uniqueId, final int x, final int y, final int z, final String world, final Sounds sound) {
        Objects.requireNonNull(Bukkit.getPlayer(uniqueId))
                .playSound(new Location(Bukkit.getWorld(world), x, y, z), switch (sound) {
                    case SUCCESS -> Sound.ENTITY_PLAYER_LEVELUP;
                    case MODIFY_SUCCESS -> Sound.BLOCK_ANVIL_USE;
                    case DESTROY -> Sound.BLOCK_ANVIL_BREAK;
                    case NAVIGATION, BLOCKED, CANCEL -> Sound.BLOCK_COMPARATOR_CLICK;
                    case BUY_SELL_SUCCESS -> Sound.ENTITY_ITEM_PICKUP;
                    case AWAITING_INPUT -> Sound.ENTITY_VILLAGER_AMBIENT;
                    case DING -> Sound.ENTITY_ARROW_HIT;
                }, 1, 1);
    }

}
