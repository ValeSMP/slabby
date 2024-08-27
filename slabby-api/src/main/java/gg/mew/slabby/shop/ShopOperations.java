package gg.mew.slabby.shop;

import gg.mew.slabby.exception.SlabbyException;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public interface ShopOperations {

    Map<UUID, ShopWizard> wizards();

    ShopWizard wizard(final UUID uniqueId);
    ShopWizard wizardOf(final UUID uniqueId, final Shop shop);

    void ifWizard(final UUID uniqueId, final Consumer<ShopWizard> action);
    void ifWizardOrElse(final UUID uniqueId, final Consumer<ShopWizard> action, final Runnable orElse);

    void buy(final UUID uniqueId, final Shop shop) throws SlabbyException;
    void sell(final UUID uniqueId, final Shop shop) throws SlabbyException;

    void withdraw(final UUID uniqueId, final Shop shop, final int amount) throws SlabbyException;
    void deposit(final UUID uniqueId, final Shop shop, final int amount) throws SlabbyException;

    Map<UUID, Double> splitCost(final double amount, final Shop shop);

    void linkShop(final UUID uniqueId, final ShopWizard wizard, final int x, final int y, final int z, final String world) throws SlabbyException;

    void unlinkShop(final UUID uniqueId, final Shop shop) throws SlabbyException;

    void createOrUpdateShop(final UUID uniqueId, final ShopWizard wizard) throws SlabbyException;

    void removeShop(final UUID uniqueId, final Shop shop) throws SlabbyException;

    void removeAndSpawnDisplayItem(final Integer oldX, final Integer oldY, final Integer oldZ, final String oldWorld, final Shop shop);


}
