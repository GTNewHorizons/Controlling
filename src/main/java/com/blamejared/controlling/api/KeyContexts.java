package com.blamejared.controlling.api;

import net.minecraft.client.Minecraft;

/** Built-in {@link KeyContext} singletons. Their ids are reserved by {@link KeyContextRegistry}. */
public final class KeyContexts {

    /** Active/relevant everywhere; conflicts with every context. */
    public static final KeyContext UNIVERSAL = new KeyContext("universal") {

        @Override
        public boolean conflicts(KeyContext other) {
            return true;
        }
    };

    /** World-only binds; conflicts only with other IN_GAME binds. */
    public static final KeyContext IN_GAME = new KeyContext("in_game") {

        @Override
        public boolean conflicts(KeyContext other) {
            return other == this;
        }

        @Override
        public boolean isActive() {
            return Minecraft.getMinecraft().currentScreen == null;
        }
    };

    /** GUI-only binds; conflicts only with other GUI binds. */
    public static final KeyContext GUI = new KeyContext("gui") {

        @Override
        public boolean conflicts(KeyContext other) {
            return other == this;
        }

        @Override
        public boolean isActive() {
            return Minecraft.getMinecraft().currentScreen != null;
        }
    };

    /** The built-ins, in declaration order. Package-private: only the registry seeds from it. */
    static final KeyContext[] VALUES = { UNIVERSAL, IN_GAME, GUI };

    private KeyContexts() {}
}
