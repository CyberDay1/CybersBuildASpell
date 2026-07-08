package buildaspell.client.gui;

import buildaspell.spell.data.ComponentDisplay;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side palette handle for a datapack-authored effect that has no backing enum
 * constant. Carries the effect id and its synced {@link ComponentDisplay} so the spell
 * builder can render, name and price it just like a built-in {@code SpellEffect}.
 */
public record DatapackEffect(ResourceLocation id, ComponentDisplay display) {}
