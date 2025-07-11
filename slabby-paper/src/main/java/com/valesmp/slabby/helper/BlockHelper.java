package com.valesmp.slabby.helper;

import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;

@UtilityClass
public final class BlockHelper {

    public boolean isShopAllowed(final Block block) {
        return (block.getBlockData() instanceof Stairs stairs && stairs.getHalf() == Bisected.Half.TOP) || block.getBlockData() instanceof Slab;
    }

    public boolean isInventoryAllowed(final Block block) {
        return block.getType() == Material.CHEST;
    }

    public boolean isSlabbyBlock(final Block block) {
        return isInventoryAllowed(block) || isShopAllowed(block);
    }

}
