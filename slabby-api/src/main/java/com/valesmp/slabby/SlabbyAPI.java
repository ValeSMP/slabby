package com.valesmp.slabby;

import com.valesmp.slabby.config.SlabbyMessages;
import com.valesmp.slabby.config.SlabbyConfig;
import com.valesmp.slabby.service.ExceptionService;
import com.valesmp.slabby.shop.ShopOperations;
import com.valesmp.slabby.shop.ShopRepository;
import com.valesmp.slabby.wrapper.claim.ClaimWrapper;
import com.valesmp.slabby.wrapper.economy.EconomyWrapper;
import com.valesmp.slabby.wrapper.permission.PermissionWrapper;
import com.valesmp.slabby.wrapper.serialization.SerializationWrapper;
import com.valesmp.slabby.wrapper.sound.SoundWrapper;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

public interface SlabbyAPI {

    EconomyWrapper economy();

    PermissionWrapper permission();

    SoundWrapper sound();

    ClaimWrapper claim();

    SerializationWrapper serialization();

    ShopRepository repository();

    ShopOperations operations();

    SlabbyConfig configuration();

    LocalDateTime now();

    Date legacyNow();

    File directory();

    ExceptionService exceptionService();

    <T> T fromJson(final String json, final Class<? extends T> theClass);

    String toJson(final Object data);

    SlabbyMessages messages();

    Logger logger();

    void reload();

    boolean isAdminMode(final UUID uniqueId);

    boolean setAdminMode(final UUID uniqueId, final boolean adminMode);

}
