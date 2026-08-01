package com.blamejared.controlling.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    KEYBINDING(new MixinBuilder().setPhase(Phase.EARLY).addClientMixins("MixinKeyBinding")),
    GAME_SETTINGS(new MixinBuilder().setPhase(Phase.EARLY).addClientMixins("MixinGameSettings"));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
