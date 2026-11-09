package com.blamejared.controlling.config;

import com.blamejared.controlling.Controlling;
import com.blamejared.controlling.client.gui.ColorPalette;
import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = Controlling.MODID, category = "display", filename = "controlling")
public class ControllingConfig {

    @Config.Comment({ "Color set for the controls list and the visual keyboard.",
            "DEFAULT is readable with any one of the three common types of color blindness.",
            "The per-type options gain a little more separation for that type at the others' expense.",
            "HIGH_CONTRAST separates states by lightness, for greyscale vision or a washed out display." })
    @Config.LangKey("config.controlling.palette")
    @Config.DefaultEnum("DEFAULT")
    public static ColorPalette palette = ColorPalette.DEFAULT;
}
