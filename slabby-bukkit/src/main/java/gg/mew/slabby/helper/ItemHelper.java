package gg.mew.slabby.helper;

import lombok.experimental.UtilityClass;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

@UtilityClass
public final class ItemHelper {

    public int countSimilar(final Inventory inventory, final ItemStack itemStack) {
        return Arrays.stream(inventory.getContents())
                .filter(Objects::nonNull)
                .filter(itemStack::isSimilar)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    public boolean hasSpace(final Inventory inventory, final ItemStack itemStack, final int amount) {
        var result = amount;

        for (final var i : inventory.getStorageContents()) {
            if (result <= 0)
                break;

            if (i == null)
                result -= itemStack.getMaxStackSize();
            else if (i.isSimilar(itemStack) && i.getAmount() < itemStack.getMaxStackSize())
                result -= itemStack.getMaxStackSize() - i.getAmount();
        }

        return result <= 0;
    }


    public int getSpace(final Inventory inventory, final ItemStack itemStack) {
        var result = 0;

        for (final var i : inventory.getStorageContents()) {
            if (i == null)
                result += itemStack.getMaxStackSize();
            else if (i.isSimilar(itemStack) && i.getAmount() < itemStack.getMaxStackSize())
                result += itemStack.getMaxStackSize() - i.getAmount();
        }

        return result;
    }

    public String toName(final ItemStack itemStack) {
        return itemStack.getType().getKey().asString() + itemStack.getItemMeta().getAsString();
    }

}
