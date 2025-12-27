package boomcow.minezero.config;

import boomcow.minezero.ConfigHandler;
import boomcow.minezero.MineZeroMain;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("config." + MineZeroMain.MOD_ID + ".title"));
            
            builder.setSavingRunnable(() -> {
                AutoConfig.getConfigHolder(ConfigHandler.class).save();
            });

            ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config." + MineZeroMain.MOD_ID + ".category.general"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            List<String> deathChimeOptions = Arrays.asList("CLASSIC", "ALTERNATE");
            
            general.addEntry(entryBuilder.startSelector(
                                    Text.translatable("config." + MineZeroMain.MOD_ID + ".option.deathChime"),
                                    deathChimeOptions.toArray(new String[0]),
                                    ConfigHandler.get().deathChime
                            )
                            .setDefaultValue("CLASSIC")
                            .setTooltip(Text.translatable("config." + MineZeroMain.MOD_ID + ".option.deathChime.tooltip"))
                            .setSaveConsumer(newValue -> ConfigHandler.get().deathChime = newValue)
                            .build()
            );

            return builder.build();
        };
    }
}
