package com.blamejared.controlling.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    KEYBINDING(new MixinBuilder().setPhase(Phase.EARLY).addClientMixins("MixinKeyBinding")),
    GAME_SETTINGS(new MixinBuilder().setPhase(Phase.EARLY).addClientMixins("MixinGameSettings")),
    GUI_SCREEN(new MixinBuilder().setPhase(Phase.EARLY).addClientMixins("MixinGuiScreen")),

    NEI_GUI_CONTAINER_MANAGER(new MixinBuilder().setPhase(Phase.LATE).addRequiredMod(TargetMods.NOT_ENOUGH_ITEMS)
            .addClientMixins("MixinNEIGuiContainerManager"));

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
