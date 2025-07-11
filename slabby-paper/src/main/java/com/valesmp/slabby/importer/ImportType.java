package com.valesmp.slabby.importer;

import com.valesmp.slabby.importer.slabbo.SlabboImporter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Accessors(fluent = true, chain = false)
@Getter
public enum ImportType {

    SLABBO(new SlabboImporter());

    private final Importer importer;

}
