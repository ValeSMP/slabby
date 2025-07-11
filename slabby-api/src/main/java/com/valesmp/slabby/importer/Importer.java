package com.valesmp.slabby.importer;

import com.valesmp.slabby.SlabbyAPI;

@FunctionalInterface
public interface Importer {

    void onImport(final SlabbyAPI api);

}
