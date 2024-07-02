package gg.mew.slabby.gui;

import gg.mew.slabby.SlabbyAPI;
import gg.mew.slabby.shop.Shop;
import gg.mew.slabby.shop.ShopOperations;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@UtilityClass
public final class GuiHelper {

    public Supplier<? extends ItemProvider> itemStack(final Material material, final BiConsumer<ItemStack, ItemMeta> action) {
        return () -> s -> {
            final var itemStack = new ItemStack(material);
            final var meta = itemStack.getItemMeta();

            action.accept(itemStack, meta);

            itemStack.setItemMeta(meta);

            return itemStack;
        };
    }

    public Component localize(final ShopOperations.ShopOperationResult result) {
        return switch (result.cause()) {
            case INSUFFICIENT_BALANCE_TO_WITHDRAW -> Component.text("You don't have enough funds!", NamedTextColor.RED);
            case INSUFFICIENT_BALANCE_TO_DEPOSIT ->
                    Component.text("The shop doesn't have enough funds!", NamedTextColor.RED);
            case INSUFFICIENT_STOCK_TO_WITHDRAW -> Component.text("This shop is out of stock!", NamedTextColor.RED);
            case INSUFFICIENT_STOCK_TO_DEPOSIT -> Component.text("You don't have enough items", NamedTextColor.RED);
            case OPERATION_NO_PERMISSION -> Component.text("You don't have permission to do this!", NamedTextColor.RED);
            case OPERATION_NOT_ALLOWED, OPERATION_FAILED, NONE ->
                    Component.text("Something went wrong!", NamedTextColor.RED);
        };
    }

    public SimpleItem commandBlock(final SlabbyAPI api, final Shop shop, final ItemStack itemStack) {
        return new SimpleItem(itemStack(Material.COMMAND_BLOCK, (it, meta) -> {
            meta.displayName(api.messages().commandBlock().title());

            final var owners = shop.owners()
                    .stream()
                    //TODO: use player display name
                    .map(o -> Bukkit.getOfflinePlayer(o.uniqueId()).getName())
                    .toArray(String[]::new);

            meta.lore(new ArrayList<>() {{
                add(api.messages().commandBlock().owners(owners));
                add(api.messages().commandBlock().selling(itemStack.displayName()));

                if (shop.buyPrice() != null) {
                    final var buyPriceEach = shop.buyPrice() == 0 ? 0 : shop.buyPrice() / shop.quantity();
                    add(api.messages().commandBlock().buyPrice(shop.quantity(), shop.buyPrice(), buyPriceEach));
                }

                if (shop.sellPrice() != null) {
                    final var sellPriceEach = shop.sellPrice() == 0 ? 0 : shop.sellPrice() / shop.quantity();
                    add(api.messages().commandBlock().sellPrice(shop.quantity(), shop.sellPrice(), sellPriceEach));
                }
            }});
        }).get());
    }

}
