package buildaspell.spell.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * Datapack definition of a spell delivery method. Carries the numeric scalars a delivery
 * contributes to spell behaviour (e.g. projectile speed/spread for the cast deliveries).
 * Built-in deliveries ship as bundled JSON so a pack can retune these.
 */
public record DeliveryDefinition(Map<String, Double> params, ComponentDisplay display) {

    public static final Codec<DeliveryDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("params", Map.of())
                    .forGetter(DeliveryDefinition::params),
            ComponentDisplay.CODEC.optionalFieldOf("display", ComponentDisplay.EMPTY).forGetter(DeliveryDefinition::display)
    ).apply(inst, DeliveryDefinition::new));

    public double param(String name, double fallback) {
        return params.getOrDefault(name, fallback);
    }
}
