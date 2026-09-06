package buildaspell.datagen;

import com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache;
import com.klikli_dev.modonomicon.api.datagen.NeoBookProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static buildaspell.BuildASpell.MOD_ID;

public class ModDataGenerators {
    public static void onGatherData(GatherDataEvent.Server event) {
        var enUsCache = new LanguageProviderCache("en_us");

        // Book provider generates data/modonomicon/books/... JSON.
        // The null is the research cache. Handing BookProvider a cache makes it stash the research
        // bundle for a separate ResearchProvider to write instead of writing it itself, which is
        // for merging hand-authored research across books; with one book and only the research the
        // combos gating generates, a null lets BookProvider write the bundle directly.
        event.addProvider(NeoBookProvider.of(event, enUsCache, null, new SpellGuidebook(MOD_ID)));

        // Lang provider generates assets/lang/en_us.json (merges book translations + mod translations)
        var output = event.getGenerator().getPackOutput();
        event.addProvider(new SpellGuidebookLangProvider(output, enUsCache));
    }
}
