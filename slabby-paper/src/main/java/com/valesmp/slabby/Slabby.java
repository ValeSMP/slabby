package com.valesmp.slabby;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.valesmp.slabby.command.SlabbyCommand;
import com.valesmp.slabby.config.BukkitSlabbyMessages;
import com.valesmp.slabby.config.BukkitSlabbyConfig;
import com.valesmp.slabby.config.SlabbyConfig;
import com.valesmp.slabby.listener.SlabbyListener;
import com.valesmp.slabby.permission.SlabbyPermissions;
import com.valesmp.slabby.service.BukkitExceptionService;
import com.valesmp.slabby.service.ExceptionService;
import com.valesmp.slabby.shop.BukkitShopOperations;
import com.valesmp.slabby.shop.SQLiteShopRepository;
import com.valesmp.slabby.shop.ShopOperations;
import com.valesmp.slabby.wrapper.claim.ClaimWrapper;
import com.valesmp.slabby.wrapper.claim.LandsClaimWrapper;
import com.valesmp.slabby.wrapper.economy.EconomyWrapper;
import com.valesmp.slabby.wrapper.economy.VaultEconomyWrapper;
import com.valesmp.slabby.wrapper.permission.BukkitPermissionWrapper;
import com.valesmp.slabby.wrapper.permission.PermissionWrapper;
import com.valesmp.slabby.wrapper.serialization.BukkitSerializationWrapper;
import com.valesmp.slabby.wrapper.serialization.SerializationWrapper;
import com.valesmp.slabby.wrapper.sound.BukkitSoundWrapper;
import com.valesmp.slabby.wrapper.sound.SoundWrapper;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.angeschossen.lands.api.LandsIntegration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Command;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.dependency.*;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import com.valesmp.slabby.importer.slabbo.SlabboShop;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

@Plugin(name = "Slabby", version = "1.1.2")
@ApiVersion(ApiVersion.Target.v1_20)
@DependsOn(value = {
        @Dependency("Vault")
})
@SoftDependsOn(value = {
        @SoftDependency("Lands")
})
@Permissions(value = {
        @Permission(name = SlabbyPermissions.SHOP_INTERACT, desc = "Allows for interacting with shops"),
        @Permission(name = SlabbyPermissions.SHOP_MODIFY, desc = "Allows for creation, modification and deletion of new shops"),
        @Permission(name = SlabbyPermissions.SHOP_LINK, desc = "Allows for linking inventories to shops"),
        @Permission(name = SlabbyPermissions.SHOP_NOTIFY, desc = "Allows shop owners to receive notifications"),
        @Permission(name = SlabbyPermissions.SHOP_LOGS, desc = "Allows shop owners to view their shop logs"),
        @Permission(name = SlabbyPermissions.SHOP_RESTORE, desc = "Allows shop owners to restore their deleted shops"),

        @Permission(name = SlabbyPermissions.ADMIN_RELOAD, desc = "Allows admins to reload Slabby"),
        @Permission(name = SlabbyPermissions.ADMIN_TOGGLE, desc = "Allows admins to interact with any shop as if it was their own"),
        @Permission(name = SlabbyPermissions.ADMIN_IMPORT, desc = "Allows admins to import shops from other plugins"),
        @Permission(name = SlabbyPermissions.ADMIN_RESTORE, desc = "Allows admins to restore shops from other players"),

        @Permission(name = SlabbyPermissions.LOCATE_ITEM, desc = "Allows players to locate slabby shops")
})
@Commands(value = {
        @Command(name = "slabby", desc = "Slabby's command for everything", permission = "slabby"),
})
@Accessors(fluent = true)
public final class Slabby extends JavaPlugin implements SlabbyAPI {

    static {
        ConfigurationSerialization.registerClass(SlabboShop.class, "Shop");
    }

    @Getter
    private SQLiteShopRepository repository;

    @Getter
    private EconomyWrapper economy;

    @Getter
    private final PermissionWrapper permission = new BukkitPermissionWrapper();

    @Getter
    private final SoundWrapper sound = new BukkitSoundWrapper();

    @Getter
    private ClaimWrapper claim;

    @Getter
    private final SerializationWrapper serialization = new BukkitSerializationWrapper();

    @Getter
    private SlabbyConfig configuration;

    @Getter
    private BukkitSlabbyMessages messages;

    @Getter
    private final ExceptionService exceptionService = new BukkitExceptionService(this);

    @Getter
    private final ShopOperations operations = new BukkitShopOperations(this);

    private final YamlConfigurationLoader configLoader = YamlConfigurationLoader.builder()
            .path(Path.of(getDataFolder().getAbsolutePath(), "config.yml"))
            .build();

    private final YamlConfigurationLoader messagesLoader = YamlConfigurationLoader.builder()
            .path(Path.of(getDataFolder().getAbsolutePath(), "messages.yml"))
            .build();

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    private final Map<UUID, Boolean> adminMode = new HashMap<>();

    @Getter
    private final NamespacedKey deleteKey = new NamespacedKey(this, "delete");

    @Override
    public void reload() {
        setupConfig();
    }

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().warning("Error while setting up economy");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupConfig()) {
            getLogger().warning("Error while setting up config");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupRepository()) {
            getLogger().warning("Error while setting up repository");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (this.getServer().getPluginManager().isPluginEnabled("Lands"))
            this.claim = new LandsClaimWrapper(LandsIntegration.of(this));

        final var commandManager = new PaperCommandManager(this);

        addSlabboMapsCommand(commandManager);

        commandManager.registerCommand(new SlabbyCommand(this));

        SlabbyHelper.init(this);

        getServer().getPluginManager().registerEvents(new SlabbyListener(this), this);

        getServer().getServicesManager().register(SlabbyAPI.class, this, this, ServicePriority.Highest);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> this.repository.shopCache().expire(Duration.ofMinutes(1)), 20 * 60, 20 * 60);
    }

    private void addSlabboMapsCommand(final PaperCommandManager commandManager) {
        commandManager.getCommandContexts().registerContext(ItemStack.class, c -> {
            final var item = c.popFirstArg();
            try {
                return Bukkit.getItemFactory().createItemStack(item);
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(String.format("Unknown item '%s'", item), false);
            }
        });
        commandManager.getCommandCompletions().registerStaticCompletion("items", Arrays.stream(Material.values()).map(it -> it.getKey().asString()).toArray(String[]::new));
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(this);
        HandlerList.unregisterAll(this);
        if (this.repository != null) {
            this.repository.close();
        }
    }

    private boolean setupRepository() {
        try {
            this.repository = new SQLiteShopRepository(this);
            this.repository.initialize();
        } catch (SQLException e) {
            exceptionService().logToConsole("Error while trying to initialize database repository", e);
            throw new RuntimeException(e);
        }

        return true;
    }

    private boolean setupConfig() {
        try {
            saveDefaultConfig();
            final var configRoot = configLoader.load();
            this.configuration = configRoot.get(BukkitSlabbyConfig.class);

            saveResource("messages.yml", false);
            final var messagesRoot = messagesLoader.load();
            this.messages = messagesRoot.get(BukkitSlabbyMessages.class);
        } catch (final ConfigurateException e) {
            exceptionService().logToConsole("Error while trying to load configurations", e);
            throw new RuntimeException(e);
        }

        return true;
    }

    private boolean setupEconomy() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault"))
            return false;

        final var economyRegistration = getServer().getServicesManager().getRegistration(Economy.class);

        if (economyRegistration == null)
            return false;

        this.economy = new VaultEconomyWrapper(economyRegistration.getProvider());

        return true;
    }

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    @Override
    public Date legacyNow() {
        return new Date();
    }

    @Override
    public File directory() {
        return getDataFolder();
    }

    @Override
    public <T> T fromJson(final String json, final Class<? extends T> theClass) {
        return gson.fromJson(json, theClass);
    }

    @Override
    public String toJson(final Object data) {
        return gson.toJson(data);
    }

    @Override
    public Logger logger() {
        return getLogger();
    }

    @Override
    public boolean isAdminMode(final UUID uniqueId) {
        if (!permission.hasPermission(uniqueId, SlabbyPermissions.ADMIN_TOGGLE))
            return false;

        return adminMode.getOrDefault(uniqueId, false);
    }

    @Override
    public boolean setAdminMode(final UUID uniqueId, final boolean adminMode) {
        if (!permission.hasPermission(uniqueId, SlabbyPermissions.ADMIN_TOGGLE))
            return false;

        this.adminMode.put(uniqueId, adminMode);

        return adminMode;
    }
}
