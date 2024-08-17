package gg.mew.slabby.config;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@Accessors(fluent = true, chain = false)
@Getter
@ConfigSerializable
public final class BukkitSlabbyConfig implements SlabbyConfig {

    @Comment("Maximum amount of items a shop can keep in stock at one time.")
    private int maxStock;

    @Comment("Item used for creating or deleting shops.")
    private String item;

    @Comment("Options for the database")
    private BukkitDatabase database;

    @Comment("Defaults for new shops.")
    private BukkitDefaults defaults;

    @Comment("Options for restocking shops.")
    private BukkitRestock restock;

    @Comment("Options for lands")
    private BukkitLands lands;

    @Accessors(fluent = true, chain = false)
    @Getter
    @ConfigSerializable
    final static class BukkitDatabase implements Database {

        @Comment("SQLite3 connection url")
        private String url;

    }

    @Accessors(fluent = true, chain = false)
    @Getter
    @ConfigSerializable
    final static class BukkitRestock implements Restock {

        @Comment("Options for shop punching.")
        private BukkitPunch punch;

        @Comment("Options for chest linking.")
        private BukkitChests chests;

        @Accessors(fluent = true)
        @Getter
        @ConfigSerializable
        final static class BukkitPunch implements Punch {

            @Comment("Owners can punch their shop to restock items.")
            private boolean enabled;

            @Comment("Owners can punch their shop while crouching to restock all items at once.")
            private boolean bulk;

            @Comment("Owners can punch their shop with a shulker box to restock using the matching items within.")
            private boolean shulker;

        }

        @Accessors(fluent = true)
        @Getter
        @ConfigSerializable
        final static class BukkitChests implements Chests {

            @Comment("Chests can be linked to a shop.")
            private boolean enabled;

            @Comment("Options for refilling linked chests")
            private BukkitHoppers hoppers;

            @Accessors(fluent = true)
            @Getter
            @ConfigSerializable
            final static class BukkitHoppers implements Hoppers {

                @Comment("Hoppers can refill a linked chest.")
                private boolean enabled;

                @Comment("If true, items will be removed from the linked inventory in batches of shop quantity or the item's max quantity, whichever is less.")
                private boolean batches;

            }

        }

    }

    @Accessors(fluent = true)
    @Getter
    @ConfigSerializable
    final static class BukkitDefaults implements Defaults {

        @Comment("Default buy price for new shops.")
        private double buyPrice;

        @Comment("Default sell price for new shops.")
        private double sellPrice;

        @Comment("Default quantity for new shops.")
        private int quantity;

        @Comment("Default note for new shops.")
        private String note;

    }

    @Accessors(fluent = true, chain = false)
    @Getter
    @ConfigSerializable
    final static class BukkitLands implements Lands {

        @Comment("minX coordinate for area where slabbo-maps works")
        private int minX;

        @Comment("minZ coordinate for area where slabbo-maps works")
        private int minZ;

        @Comment("maxX coordinate for area where slabbo-maps works")
        private int maxX;

        @Comment("maxZ coordinate for area where slabbo-maps works")
        private int maxZ;

        @Comment("World for the area where slabbo-maps works")
        private String world;

    }

}
