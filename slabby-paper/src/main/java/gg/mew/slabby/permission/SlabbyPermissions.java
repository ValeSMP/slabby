package gg.mew.slabby.permission;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class SlabbyPermissions {

    private final String BASE = "slabby";

    public final String SHOP_BASE = BASE + ".shop.";
    public final String SHOP_INTERACT = SHOP_BASE + "interact";
    public final String SHOP_MODIFY = SHOP_BASE + "modify";
    public final String SHOP_LINK = SHOP_BASE + "link";
    public final String SHOP_NOTIFY = SHOP_BASE + "notify";
    public final String SHOP_LOGS = SHOP_BASE + "logs";
    public final String SHOP_RESTORE = SHOP_BASE + "restore";

    public final String ADMIN_BASE = BASE + ".admin.";
    public final String ADMIN_RELOAD = ADMIN_BASE + "reload";
    public final String ADMIN_TOGGLE = ADMIN_BASE + "toggle";
    public final String ADMIN_RESTORE = ADMIN_BASE + "restore";
    public final String ADMIN_IMPORT = ADMIN_BASE + "import";

    public final String SLABBO_MAPS_LOCATE_ITEM = "slabby.slabbomaps.locate.shop";

}
