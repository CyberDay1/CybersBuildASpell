# Build a Spell 1.0 — Patch Notes

---

## v1.0.1

> A modifier-system rework: the modifiers that shape a whole projectile — Double, Split, Accelerate, Pierce, Bounce, Return, and Chain, alongside the timing modifiers Delay, Echo, Linger, and Duration — now belong to the delivery rather than to a single effect, and the Spell Builder gives them their own category and their own place beside the delivery. Chain is rebuilt to spread the projectile from whatever it strikes: it arcs to nearby creatures on an entity hit, or hops between blocks of the same kind on a solid impact. Linger now settles into a vanilla-style lingering-potion cloud that keeps pulsing its effect.
>
> Alongside it: a rebalance that brings the three wands far closer together and makes armour count against stacked Damage, and the first round of fixes from reports since launch, including an Arcane Altar crash and the spell list going blank after a death.
>
> **Supports:** Minecraft 26.1.x (Java 25) · Minecraft 26.2 (Java 25) · Minecraft 1.21.1 (Java 21)

### Modifier Rework

- **Projectile- and timing-shaping modifiers now attach to the delivery, not an effect.** Eleven modifiers — Double, Split, Accelerate, Pierce, Bounce, Return, Chain, Delay, Echo, Linger, and Duration — act on the spell's delivery as a whole, so they no longer bind to whichever effect you happened to drop them beside. Under the hood these already applied to the projectile regardless of where they sat, so this makes the spell read the way it behaves: they live in one place on the delivery, and the remaining modifiers stay attached to their individual effects. Existing saved spells carry over on their own: any of these modifiers left sitting on an effect is moved onto the delivery the moment the spell loads, so nothing you have built breaks.
- **Chain now spreads the projectile from whatever it strikes.** Instead of forking a single effect onto same-type creatures, Chain steers the whole projectile once it has spent its Pierce and Bounce, and it takes one of two forms depending on what it lands on:
    - **On a creature, it arcs between entities.** It jumps to the nearest living thing around it, then the next, and so on. Any kind of creature counts, and it never doubles back on one it has already struck.
    - **On a block, it hops between like blocks.** It leaps to the nearest block of the same kind, then keeps hopping from there.

  Either way, it resolves the entire spell at every target it reaches and keeps going for as many jumps as its Chain level allows, one more per stack. How far it reaches per jump and how many jumps each level grants are both server-configurable.
- **Linger settles into a lingering-potion cloud.** A Linger spell now leaves behind a vanilla-style potion cloud, tinted to the spell's own colour, that keeps re-casting its effect on everything inside it for the duration. The way it works is unchanged: only the look now matches a real lingering potion.

### Spell Builder

- **A dedicated "Delivery Modifiers" category.** The component palette splits its modifiers in two: the delivery modifiers get their own labelled section directly under Delivery Methods, and the effect modifiers keep theirs below the effects. In the build chain the delivery's modifiers sit beside the delivery itself, exactly the way an effect's modifiers sit beside the effect.
- **Modifiers that cannot work on your delivery are visibly out of reach.** The projectile-shaping modifiers only do anything when the spell actually launches a projectile, which means a Cast or Tracking delivery. Choose a Rune, Trap, Self, Sight, or Touch delivery and those modifiers grey out in the palette with a note explaining why, so you are never spending mana on a Pierce that will never fire. If you had one placed and then switch to a delivery it no longer fits, it stays put but is outlined in amber as a heads-up rather than silently doing nothing.

### Balance

- **The three wands sit far closer together.** A wand's Spell Power bonus is now +6 on a Worn wand, +15 on a Carved one and +30 on a Runic one, down from +10, +25 and +75. Counting the Spell Power every caster starts with, a plain Damage effect now lands for 2 bare-handed, 3.2 with a Worn wand, 5 with a Carved one and 8 with a Runic one: that top end is a netherite sword swing. The old spread put a Runic wand eight and a half times above bare hands, a wider gap than the entire wood-to-netherite sword progression, and no single damage figure could sit sensibly across a range that broad.
- **Repeating the same effect in one spell costs more each time.** The second copy is priced above the first, the third above the second, and so on, so a wall of one effect is no longer bought at a flat rate. How steeply the price climbs is server-configurable, and setting it to 1 restores the old flat pricing exactly.
- **Stacked Damage lands as a single blow.** Copies of Damage in one spell are added together and dealt once, so armour, Protection and Resistance all measure themselves against the full total. Each copy used to strike separately and slip past the brief invulnerability that normally follows a hit, which is what made stacking Damage so much cheaper than it looked. Echo is not folded in: each replay still arrives later and lands as its own separate blow, at 80% of the one before it by default, and that falloff is server-configurable.
- **Existing worlds pick up the new numbers.** Server settings written before this release are brought up to date the first time the world loads. Anything you had already tuned yourself is left exactly as you set it.

### Fixes

- **The Arcane Altar no longer crashes when a slot holds more than 64 of a material.** A slot still takes up to 320, and altars filled before this release keep what is in them.
- **Your spells and the component palette no longer go blank.** They survive dying and travelling between dimensions, so there is nothing left to relog for.
- **Enchantments now do what they say.** Spell Power is read from whatever is in your main hand, and Mana Pool and Mana Regeneration are read from worn armour. The altar now only offers an enchantment on an item where it will actually be read, so levels can no longer be spent on a pairing that does nothing.
- **Launch, Pull, Push, Yeet and Slam now move players.** They already worked on every other creature; a player is now carried the same way.
- **The Spell Visuals screen applies again.** Colour, shape and trail choices hold instead of being dropped on the way back to the builder. The same fix means resizing the window or toggling fullscreen no longer discards a spell you are part-way through building.
- **On Minecraft 1.21.1, a cast now leaves the same brief shimmer it has always left on 26.x.** Wherever a spell resolves — a projectile landing, a combo going off, or a cast that needs no projectile — a puff of enchanting glyphs marks the spot, spread to match the spell's range and gone in about a second and a half. It is decoration only and applies nothing on its own. Minecraft 1.21.1 simply never drew it, so a spell read flatter there than the very same spell on 26.x. Linger and Duration spells are the one exception on every version: they leave their own lingering cloud, so they get no separate puff.
- **Conjure builds through its own Light.** Anything a block can normally be placed into — light, grass, snow, water — now counts as free space for Conjure, which is already how it behaved when paired with Fill. A plain Conjure used to fill only true air, so casting one into an area you had just lit left a gap wherever a light happened to sit.
- **Light no longer lasts forever.** The lights a cast places now clear themselves after a minute, and how long they last is server-configurable. The countdown is stored with the world rather than the session, so it keeps running across a reload instead of stranding the lights. For the permanent lights left behind by v1.0.0, which could not be mined in survival: Break now clears them, and casting Light over them turns them into ones that expire.

### Documentation

- **The guidebook and the component reference now say what Fill actually does.** Fill reshapes a spell into a solid sphere around the point it lands, and it measures distance alone with no line of sight, so the sphere reaches straight through stone into sealed rooms and caves you cannot see. Break will hollow out far more than the room you are standing in, and Conjure will pack every open space in range, including the ones behind a wall. None of that behaviour has changed: the description was simply wrong, and it also called the shape a cube and implied Increased Area would widen it, which it does not.
