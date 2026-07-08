package buildaspell.datagen;

import com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache;
import com.klikli_dev.modonomicon.api.datagen.NeoBookProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static buildaspell.BuildASpell.MOD_ID;

public class ModDataGenerators {
    public static void onGatherData(GatherDataEvent event) {
        var enUsCache = new LanguageProviderCache("en_us");

        // Book provider generates data/modonomicon/books/... JSON
        event.addProvider(NeoBookProvider.of(event, new SpellGuidebook(MOD_ID, enUsCache)));

        // Lang provider generates assets/lang/en_us.json (merges book translations + mod translations)
        var output = event.getGenerator().getPackOutput();
        event.addProvider(new SpellGuidebookLangProvider(output, enUsCache));
    }
}
