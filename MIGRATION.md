# Migration Notes

## 5.5

 - Glass is no longer transparent by default (spells won't shoot through glass, too exploitable!)
   This can be modified by copying the "transparent" list into your materials.yml and adding glass, etc back in.

## 5.4

 - Spell worth values are now defined in SP. There is a "worth_sp" parameter in config.yml that is used to scale
   them back up to currency prices, this defaults to 10.
 - The spell shop now defaults to working with skill points. To disable this, set "use_sp: false" on the spellshop in spells.yml.
 - Wands now use a Skill Point system for progression. This replaces the enchanting system.
   To switch this back, set "sp_enabled: false" in config.yml.
 - Disabled combining wands on an anvil by default (use enable_combining: true in config.yml to change back)
 - Projectile, TNT, ThrowItem, ThrowBlock and Firework all track their projectiles now.
   This means the actions won't complete until the projectile hits or expires, this may 
   impact existing spells that use these actions coupled with Repeat.
 - Crafting once again gives you the "default" wand, not "beginner". Default wand changed to
   include the Magic Missile spell.
 - Wand crafting recipe changed from "beginner" to "wand"
 - Updated PreciousStones integration to latest version (unfortunately NOT backwards compatible!)

## 5.2.2

 - Undo action "target_self" parameter changed to "target_caster"
 - Range parameter now defaults to zero. Target type defaults to "none" unless range is set.
 - The target_self flag has been generalized, defaults to true if target type is "self".
   - May affect Hat-based spells, add "target_self: true" if necessary.

## 5.2.0

 - The /cast command now bypasses cooldowns. If you don't want this, set "cast_console_cooldown_reduction: 0" in config.yml!
   (This was undone in the next version- I'm still looking for a good way to change this)
   
## 5.1.9

 - Changed icon for architect wand, you may need to tweak existing ones with
   /wand configure icon gold_pickaxe

## 5.1

 - Remove the "elder" wand from survival configs.
   - You can copy it back in if this is a problem for you
   - It conflicted with the potter elder wand in combined configurations

## 4.9.7

 * Build permissions for spells have been split into build vs break.
   This mainly affects Towny integration, but if you are using the bypass_build
   config option, or the bypass_build permission you may need to include bypass_break
   for the same effect.

## 4.9

 * Haste, health regen and hunger regen were removed in place of potion effects.
 * You may have (Admin, Wolf) wands that made you invulnerable but no longer do.
   You'll need to get a new one, or do "/wand configure protected true"

## 4.8.5

 * The worth_items list in config.yml has changed to "currency", and the format is different.
   If you have a customized physical economy, please take note!

## 4.8

Spell shops were re-balanced. You can now tweak the "worth_base" value in config.yml to scale
spell prices up or down.

Spell shops now default to an emerald-based economy (rather than XP) if Vault is not found.
This is configurable via worth_items in config.yml.

## 4.6

4.6 is a major change under the hood, the biggest since 4.0. I've tried to keep everything
mostly the same, but there are some changes to be aware of.

### General updates

 * MagicWorlds will need to be updated to 1.2
 * Changed configuration options related to construction interval / max blocks
 * The default restricted material list is much less restrictive. Building with TNT, emeralds, ore, etc is now allowed.
 * Updated and fixed dtlTraders integration.
   - You will need to fix your spell and wand shops- sorry
   - You may need to disable brush/spell/wand glow until an issue in dtlTraders is resolved

### Custom spell configurations

If you have a custom spell configuration with "actions:" in it, you may need to update:

 * Iterate action renamed to Line
 * ReplaceMaterial action renamed to ModifyBlock
 * CoverAction split into Disc and Cover
   * To reproduce previous behavior, change "Cover" to "Disc" and add a "Cover" under it
   * Disc builds a Disc, Cover searches for the topmost block, up or down, on one block

### API

Just in case anyone is actually using the API:

 * Changed API cast() methods to return success/failure

## 4.5

 * The "undo" spell changed its key to "rollback" to match the spell's name.

## 4.4

 * FlowerSpell flower lists moved to parameters section
 * Multi-target base spells no longer supported, must use ConeOfEffectAction (see: laser, push)
 * API Change: Spell.playEffects changed to use a CastContext
 * Engineering brush selection has changed- use "brush_mode: inventory" for old behavior, or hook up brushselect

## 4.0

 * Many default spells migrated to the new "action" system.
 * Default configs changed drastically, all auto-undo.
 * Enchanting path options changed from max_xp_max and max_xp_regeneration to max_mana and max_mana_regeneration
 * Commands in command spell parameters have changed slightly.
   (@t -> @tn, @p -> @pn for target and player names)

## 3.9.4

 * All of the particle effect names have changed (internal EffectLib change).
   There is a PHP script available to help migrate custom configs.
 * WorldGuard 6 or higher is now required for region support!

## 3.8

 * The "enable_creative_mode_ejecting" in config.yml now controls the creative mode special
   behavior. If you have "enable_custom_item_hacks" set to false, you probably want to set
   enable_creative_mode_ejecting to false also.

## 3.7

 * Some magic items (spells, upgrades) may need to be updated. Sorry!
 * The "xp" casting cost has changed to "mana" ("xp" is still valid, but consumes player XP)
   * If you have a completely custom spells.yml, search are replace "xp:" for "mana:" to fix.

## 3.6

 * Drop support for Cratbukkit 1.6 - 1.7.2 is now the minimum required version
 * Update to EffectLib 2.0 - all effect class names have changed

## 3.5

 * The configuration option "wand_enchantable_item" is now off by default. This means
   that wands won't change to a wooden sword when enchanting. If you are using a pre-1.7.9
   version of Bukkit, you may (or may not) need to put this option back in config.yml

## 3.4

 * The particle effect names have changed to the EffectLib versions is all places:
 
 https://github.com/Slikey/EffectLib/blob/master/src/main/java/de/slikey/effectlib/util/ParticleEffect.java

## 3.3

 * Enchanting is now customizable via multiple enchanting paths. If you had previously modified the "random"
   template in wands.yml, you will need to transfer your work to enchanting.yml.
   There are also now multiple different enchanting paths, review wands.defaults.yml,
   you will need to override "path" for wands if you want them to continue to all use
   the same enchanting path (now called "master").

 * Crafting is now customizable, there are several new recipes apart from the old
   blaze rod + nether star default. Please review crafting.defaults.yml and adjust
   accordingly, especially if you have a customized crafting configuration.


## 3.2

 * FamiliarSpell has been modified, you will need to update any customized
   configurations of Mob, Monster, Familiar or Farm. The spell now supports
   custom weighted probability lists for mob spawns.

## 3.1

* I removed the swapping out of enchantable items by default, custom enchanting works in recent CB builds. Change "wand_item_enchantable" in
  config.yml back if you still need this functionality.

* The auto_undo configuration option is deprecated, now that nearly all spells undo it will produce strange behavior. You
  will need to add "undo" to individual spells if you want auto-undo behavior .. sorry!

## 3.0

* I removed the legacy player data migration. If you still have old playername-based player data, you will need to
  use 2.9.9 to migrate, and the player must log in. Player data has been UUID-based since 2.9.0 so hopefully you're all migrated by now.

* I changed more spell parameters, mostly the material/brush related ones. Sorry, trying to get this settled for 3.0.

* I made the wood_hoe the default wand item, instead of blaze_rod. You can change this back in config.yml if you wish, however this
  was made to support the official Magic resource pack, which are free to use- simply add the following to your server.properties file:

```  
resource-pack=https://s3-us-west-2.amazonaws.com/elmakers/Magic/Magic-RP.zip
```

## 2.9.9

* I removed the block populator. It will be part of a separate plugin, MagicWorlds: http://dev.bukkit.org/bukkit-plugins/magicworlds/

* I have renamed many of the default spell parameters, if you've been scripting, plesae check SPELLS.md: https://github.com/elBukkit/MagicPlugin/blob/master/SPELLS.md

* I broke any Wolf House's you may have made already, sorry! (Related to the above- you can fix it by re-casting at the center of your control booth)

* I removed many of the default wands, trying to de-clutter. If you were using any of them or want them back,
simply copy+paste from here into your wands.yml file (in plugins/Magic):

https://raw.githubusercontent.com/elBukkit/MagicPlugin/330f6b0e3721471bc6e101e97109a695d17e09dd/src/main/resources/defaults/wands.defaults.yml

* I changed the "modifiable" flag in wand data to "locked" (and inverted it). If you had any un-modifiable wands in your worlds, they will
now be modifiable sorry about that, please let me know if it's a concern (I'm assuming it's not) and I can add in some migration code.

I don't think anyone was using locked wands, hopefully not.

* I renamed the bending wands- *TAKE CARE* if you were giving out the "air" "water" "earth" or "fire" wands- those are now
the Master wands, not the Student!

## Some general notes

At this point I will try and ensure that migration is generally hassle-free, and plugin updates are
drop-in. I am hesitant to integrate any kind of auto-updater, but if you wanted to automate pulling
from the latest devbukkit or DEV build, I will try to make sure that won't cause problems.

To be clear, *to make migration easy* going forward, generally do NOT copy and paste from the defaults
into your custom configuration file.

Proper customization involves only putting *what you want to change* into your spells.yml, wands.yml config.yml
or messages.yml (etc) files. The files in defaults/ are always loaded first, and what you've put in the
customized configuration files overrides them.

For instance, if you want to give "fireball" a 10-second cooldown, you would put the following in plugins/Magic/spells.yml

```
fireball:
    parameters:
        cooldown: 10000
```

Note that all duration values will be in *milliseconds* (not seconds or ticks).

If you copy and paste the entire defaults file (or even sections of it, like the definition of a whole
spell), migration may get trickier. I will sometimes change the class a spell uses, fix effects, tweak
icons, or something else that you probably don't really want to tweak. 
If you've overridden the defaults, my changes won't have
any effect on your server, and that spell might break (or not get fixed) on update.

Going forward I will assume that you are following this rule when customizing. I'm not going to mention
every tweak I make to the default config files, because if you haven't overridden it, it shouldn't matter.

If you have overridden something, I'm going to assume it was intentional. I will try to summarize all of this
in the "admin instructions" section on devbukkit.

## 2.9.4

### spells.yml

The definitions of "Disc" and "SuperDisc" spells have changed, y_max and y_min have changed to 
orient_dimension_max and orient_dimension_min to support different orientations.

## 2.9.3

### spells.yml

The "Recurse" spell is now RecurseSpell, not FillSpell.

## 2.9.2

### config.yml

If you are using right-click-to-cycle, change the following

right_click_cycles: false

to

default_wand_mode: cycle

## 2.9.1

### spells.yml

 - ShrunkenHeadSpell changed to ShrinkSpell
 - MineSpell changed to DropSpell (and "chop" variant added)

## 2.9.0

 BIG BIG ONE:
 
 The configuration files have been split up and reorganized, and magic.yml has been renamed to config.yml.
 The defaults have moved to a separate folder, as have the data files (which you generally shouldn't edit).
 
 Specific material list and block populator configurations have been moved into separate files for clarity.
 
 Now would be a really good time to start with a clean, fresh install of Magic, and then re-apply your customizations, if any.
 
 I am going to make a big effort from here out to keep the configuration format consistent, and only have a few things planned
 before 3.0. The new additive configuration system, if used properly (only change what you need to change) will make migration
 in the future much, much easier.
 
 Also:

 Chests and signs are no longer indestructible. The undo system will save their contents (though not across a reload, yet).
 You can modify the indestructible material list in magic.yml.
 
 Additionally, player data and image map data has changed, but this should be automatically migrated on first run.
 Make sure you run some version of Magic 2.9.X before 3.0, I plan on removing the migration in 3.0. (Only really
 important if you have image maps).
 
 Detailed notes:
 
 - Potion effects (boon, curse) parameters now prefixed with "effect_"
 - Arrow, Sniper and ArrowRain spells now use ProjectileSpell
 

## 2.8.9

 BIG ONE: All config files have had their root removed. So there is no "general" in magic.yml, it's all flat.
 
 Additional note: in magic.yml the material lists (destructible, indestructible, etc) have moved under a "materials" 
 node. 
 
 This should be easy enough to fix in your customized configurations, but otherwise they will be ignored- so make
 sure you catch this on upgrade.
 
 Note that now you only need to modify what you want to have different now. So it might be a good time to review your
 custom configurations!
 
 If you want to disable a wand or spell, set the "enabled" property to "false" (instead of removing it)
 
 I'm trying to get the config formats more or less solidified by 2.9.0, and I hope the overall format won't change at all
 after official release (3.0). 

## 2.8.8

### spells.yml

 All FillSpell variants need
 
            check_destructible: false
            
 To continue to be able to fill all materials.

### magic.yml

 This config file is now additive. See the notes in magic.defaults.yml
 You only need to set what you wish to change from the default in magic.yml

## 2.8.7

### magic.yml
 
 quiet changed to show_cast_messages
 silent changed to show_messages
 message_prefix and cast_message prefix added
 "size" parameter added to IterateSpell, though the default is the same
 "effect_color" in wands.yml changed to a string

## 2.8.5

### magic.yml

 - max_power_multiplier removed, replaced with the following:
   
    max_power_construction_multiplier: 5.0
    max_power_radius_multiplier: 1.5
    max_power_range_multiplier: 3.0

## 2.8.3

### spells.yml

 - Added earth and stream spells
 - lava spell needs updating in spells.yml: LavaSpell -> IterateSpell
 - properties and parameters have been combined into "parameters". This one might be a pain, sorry!
 - Reconfigured the gills spell to be a PotionEffectSpell. 
 
## 2.8.2
 
### spells.yml
 
  - The fireball and icbm spells need updating: FireballSpell -> ProjectileSpell

## 2.8.0

### messages.yml

 - All name and description nodes from spells.yml and wands.yml should be moved to messages.yml

## 2.7.0
 
### spells.yml
 
  - Casting costs changes to a flat list of "material|xp: value" key/value paits.
  
  
  