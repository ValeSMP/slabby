package gg.mew.slabby.service;

import gg.mew.slabby.SlabbyAPI;
import gg.mew.slabby.exception.*;
import gg.mew.slabby.wrapper.sound.Sounds;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

@RequiredArgsConstructor
public final class BukkitExceptionService implements ExceptionService {

    private final SlabbyAPI api;

    @Override
    public void logToConsole(final String message, final Throwable exception) {
        api.logger().log(Level.SEVERE, message, exception);
    }

    @Override
    public void logToPlayer(final UUID uniqueId, final SlabbyException exception) {
        final var player = Objects.requireNonNull(Bukkit.getPlayer(uniqueId));

        if (exception.getCause() != null)
            logToConsole("SlabbyException had an underlying cause", exception.getCause());

        player.sendMessage(switch (exception) {
            case FaultException e -> e.component();
            case InsufficientBalanceToBuyException ignored -> api.messages().client().buy().insufficientBalance();
            case InsufficientBalanceToSellException ignored -> api.messages().client().sell().insufficientBalance();
            case NoPermissionException ignored -> Bukkit.permissionMessage();
            case PlayerOutOfStockException ignored -> api.messages().owner().deposit().insufficientStock();
            case ShopOutOfStockException ignored -> api.messages().owner().withdraw().insufficientStock();
            case PlayerOutOfInventorySpaceException ignored -> api.messages().general().noInventorySpace();
            case UnrecoverableException ignored -> api.messages().general().unrecoverableException();
            case ShopOutOfSpaceException ignored -> api.messages().general().shopOutOfSpace();
            //NOTE: A SlabbyException is never thrown on its own, so technically this won't ever happen, but I have to put it here to satisfy the compiler
            case SlabbyException ignored -> api.messages().general().unrecoverableException();
        });

        api.sound().play(uniqueId, player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ(),
                player.getLocation().getWorld().getName(),
                Sounds.BLOCKED);
    }

    @Override
    public boolean tryCatch(final UUID uniqueId, final Runnable action) {
        try {
            action.run();
            return true;
        } catch (final SlabbyException e) {
            this.logToPlayer(uniqueId, e);

            if (e instanceof UnrecoverableException)
                this.logToConsole("UnrecoverableException", e);

            return false;
        }
    }
}
