package com.valesmp.slabby.shop.log;

public record LocationChanged(Integer x, Integer y, Integer z, String world) {

    public boolean isRemoved() {
        return x == null && y == null && z == null && world == null;
    }

}
