package com.valesmp.slabby.permission;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class SlabbyPermissions {

    private static final String BASE = "slabby";

    public static final String SHOP_BASE = BASE + ".shop.";
    public static final String SHOP_INTERACT = SHOP_BASE + "interact";
    public static final String SHOP_MODIFY = SHOP_BASE + "modify";
    public static final String SHOP_LINK = SHOP_BASE + "link";
    public static final String SHOP_NOTIFY = SHOP_BASE + "notify";
    public static final String SHOP_LOGS = SHOP_BASE + "logs";
    public static final String SHOP_RESTORE = SHOP_BASE + "restore";

    public static final String ADMIN_BASE = BASE + ".admin.";
    public static final String ADMIN_RELOAD = ADMIN_BASE + "reload";
    public static final String ADMIN_TOGGLE = ADMIN_BASE + "toggle";
    public static final String ADMIN_RESTORE = ADMIN_BASE + "restore";
    public static final String ADMIN_IMPORT = ADMIN_BASE + "import";
    public static final String ADMIN_RESET_DISPLAYS = ADMIN_BASE + "resetdisplays";
    public static final String ADMIN_SET_OWNER = ADMIN_BASE + "setowner";

    public static final String LOCATE_ITEM = SHOP_BASE + "locate";

}
