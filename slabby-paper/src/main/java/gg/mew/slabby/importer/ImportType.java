package gg.mew.slabby.importer;

import gg.mew.slabby.importer.slabbo.SlabboImporter;
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
