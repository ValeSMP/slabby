package com.valesmp.slabby.config;

import net.kyori.adventure.text.Component;

import java.util.Date;

public interface SlabbyMessages {

    Client client();
    Create create();
    Destroy destroy();
    Log log();
    Modify modify();
    Owner owner();
    Restore restore();

    General general();
    CommandBlock commandBlock();
    Command command();

    interface Client {

        interface Buy {
            Component title(final Component displayName, final int quantity);
            Component price(final double price);
            Component stock(final int stock);
            Component stacks(final int stacks);
            Component message(final Component displayName, final int quantity, final double buyPrice);
            Component messageOwner(final Component client, final int quantity, final Component item, final double total);
            Component insufficientBalance();
        }

        interface Sell {
            Component title(final Component displayName, final int quantity);
            Component price(final double price);
            Component stock(final int stock);
            Component stacks(final int stacks);
            Component message(final Component displayName, final int quantity, final double sellPrice);
            Component messageOwner(final Component client, final int quantity, final Component item, final double total);
            Component insufficientBalance();
        }

        interface Funds {
            Component title();
            Component balance(final double balance);
        }

        interface Note {
            Component title();
        }

        Buy buy();
        Sell sell();
        Funds funds();
        Component title();
        Note note();
    }

    interface CommandBlock {
        Component title();
        Component owners(final String[] names);
        Component selling(final Component displayName);
        Component buyPrice(final int quantity, final double buyPrice, final double eachPrice);
        Component sellPrice(final int quantity, final double sellPrice, final double eachPrice);
    }

    interface Create {
        Component title();
    }

    interface Destroy {

        interface Confirm {
            Component title();
            Component description();
        }

        interface Cancel {
            Component title();
        }

        Component title();
        Confirm confirm();
        Cancel cancel();
    }

    interface Log {

        interface Buy {
            Component title();
            Component amount(final double amount);
            Component quantity(final int quantity);
        }

        interface Sell {
            Component title();
            Component amount(final double amount);
            Component quantity(final int quantity);
        }

        interface Deposit {
            Component title();
            Component amount(final int amount);
        }

        interface Withdraw {
            Component title();
            Component amount(final int amount);
        }

        interface LocationChanged {
            Component title();
            Component x(final int x);
            Component y(final int y);
            Component z(final int z);
            Component world(final String world);
        }

        interface InventoryLinkChanged {
            Component title();
            Component x(final int x);
            Component y(final int y);
            Component z(final int z);
            Component world(final String world);
            Component removed();
        }

        interface BuyPriceChanged {
            Component title();
            Component from(final Double amount);
            Component to(final Double amount);
        }

        interface SellPriceChanged {
            Component title();
            Component from(final Double amount);
            Component to(final Double amount);
        }

        interface QuantityChanged {
            Component title();
            Component from(final int quantity);
            Component to(final int quantity);
        }

        interface NoteChanged {
            Component title();
            Component from(final String note);
            Component to(final String note);
        }

        interface NameChanged {
            Component title();
            Component from(final String name);
            Component to(final String name);
        }

        interface ShopDestroyed {
            Component title();
        }

        Component title();

        Buy buy();
        Sell sell();
        Deposit deposit();
        Withdraw withdraw();
        LocationChanged locationChanged();
        InventoryLinkChanged inventoryLinkChanged();
        BuyPriceChanged buyPriceChanged();
        SellPriceChanged sellPriceChanged();
        QuantityChanged quantityChanged();
        NoteChanged noteChanged();
        NameChanged nameChanged();
        ShopDestroyed shopDestroyed();

        Component player(final Component displayName);
        Component date(final Date date);
    }

    interface Modify {

        interface Note {
            Component title();
            Component request();
        }

        interface Buy {
            Component title();
            Component amount(final double amount);
            Component notForSale();
            Component request();
        }

        interface Sell {
            Component title();
            Component amount(final double amount);
            Component request();
            Component notBuying();
        }

        interface Quantity {
            Component title();
            Component amount(final int quantity);
            Component description();
            Component request();
            Component minMax(final int max);
        }

        interface Confirm {
            Component title();
            Component description();
            Component location(final String world, final int x, final int y, final int z);
        }

        interface Cancel {
            Component title();
        }

        interface Move {
            Component title();
            Component location(final int x, final int y, final int z, final String world);
            Component message();
        }

        Component title();
        Component clickToSet();
        Component invalidNumber();
        Component decimalPlaces();
        Component minimumPrice();

        Note note();
        Buy buy();
        Sell sell();
        Quantity quantity();
        Confirm confirm();
        Cancel cancel();
        Move move();
    }

    interface Owner {

        interface Deposit {
            Component title(final Component displayName);
            Component bulk();
            Component insufficientStock();
        }

        interface Withdraw {
            Component title(final Component displayName);
            Component bulk();
            Component insufficientStock();
        }

        interface ChangeRate {
            Component title();
            Component amount(final int amount);
        }

        interface Logs {
            Component title();
        }

        interface InventoryLink {

            interface Cancel {
                Component title();
                Component message();
            }

            Component title();
            Component description();
            Component message();

            Cancel cancel();
        }

        interface Modify {
            Component title();
        }

        interface Customer {
            Component title();
        }

        Component title();
        Component stock(final int stock);
        Component stacks(final int stacks);

        Deposit deposit();
        Withdraw withdraw();
        ChangeRate changeRate();
        Logs logs();
        InventoryLink inventoryLink();
        Modify modify();
        Customer customer();
    }

    interface Command {

        interface Reload {
            Component message();
        }

        interface Admin {
            Component enabled();
            Component disabled();
        }

        interface Importer {
            Component message();
        }

        Reload reload();
        Admin admin();
        Importer importer();

    }

    interface Restore {
        Component title();
        Component message();
        Component buyPrice(final double price);
        Component sellPrice(final double price);
        Component quantity(final int quantity);
        Component stock(final int stock);
        Component note(final String note);
        Component owners(final String[] owners);
    }

    interface General {
        Component nextPage();
        Component previousPage();
        Component noInventorySpace();
        Component unrecoverableException();
        Component shopOutOfSpace();
    }

}
