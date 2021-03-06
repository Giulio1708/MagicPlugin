package com.elmakers.mine.bukkit.magic.listener;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.Messages;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.block.MaterialAndData;
import com.elmakers.mine.bukkit.magic.MagicController;
import com.elmakers.mine.bukkit.utility.NMSUtils;
import com.elmakers.mine.bukkit.wand.Wand;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerController implements Listener {
    private final MagicController controller;
    private int clickCooldown = 150;
    private boolean enableCreativeModeEjecting = true;
    private MaterialAndData enchantBlockMaterial;
    private String enchantClickSpell = "spellshop";
    private String enchantSneakClickSpell = "upgrades";

    public PlayerController(MagicController controller) {
        this.controller = controller;
    }

    public void loadProperties(ConfigurationSection properties) {
        clickCooldown = properties.getInt("click_cooldown", 0);
        enableCreativeModeEjecting = properties.getBoolean("enable_creative_mode_ejecting", false);
        enchantBlockMaterial = new MaterialAndData(properties.getString("enchant_block", "enchantment_table"));
        enchantClickSpell = properties.getString("enchant_click");
        enchantSneakClickSpell = properties.getString("enchant_sneak_click");
    }

    public void setCreativeModeEjecting(boolean eject) {
        enableCreativeModeEjecting = eject;
    }

    @EventHandler
    public void onPlayerEquip(PlayerItemHeldEvent event)
    {
        if (!controller.isLoaded()) return;

        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack next = inventory.getItem(event.getNewSlot());

        Mage apiMage = controller.getMage(player);
        if (!(apiMage instanceof com.elmakers.mine.bukkit.magic.Mage)) return;
        com.elmakers.mine.bukkit.magic.Mage mage = (com.elmakers.mine.bukkit.magic.Mage)apiMage;

        // Check for self-destructing and temporary items
        if (Wand.isSelfDestructWand(next)) {
            mage.sendMessageKey("wand.self_destruct");
            inventory.setItem(event.getNewSlot(), null);
            mage.checkWand();
            return;
        }

        if (NMSUtils.isTemporary(next)) {
            inventory.setItem(event.getNewSlot(), null);
            mage.checkWand();
            return;
        }

        Wand activeWand = mage.getActiveWand();
        boolean isSkill = Wand.isSkill(next);
        boolean isQuickCast = activeWand != null && activeWand.isQuickCast() && activeWand.isInventoryOpen();
        if (isSkill || isQuickCast)
        {
            Spell spell = mage.getSpell(Wand.getSpell(next));
            if (spell != null) {
                if (activeWand != null) {
                    activeWand.cast(spell);
                } else {
                    spell.cast();
                }
            }
            event.setCancelled(true);
            return;
        }

        // Check for active Wand
        if (activeWand != null && activeWand.isInventoryOpen()) {
            // If the wand inventory is open, we're going to let them select a spell or material
            if (!Wand.isWand(next)) {
                controller.onPlayerActivateIcon(mage, activeWand, next);

                // Make sure we have the current wand item in the player's inventory so the
                // item text gets updated on hotbar item selection "bounce"
                int previousSlot = event.getPreviousSlot();
                ItemStack previous = inventory.getItem(previousSlot);
                String previousId = Wand.getWandId(previous);
                if (previousId != null && previousId.equals(activeWand.getId())) {
                    player.getInventory().setItem(previousSlot, activeWand.getItem());
                }
            }

            event.setCancelled(true);
        } else {
            // Check to see if we've switched to/from a wand
            mage.checkWand(next);
        }

        // Check for map selection if no wand is active
        activeWand = mage.getActiveWand();
        if (activeWand == null && next != null) {
            if (next.getType() == Material.MAP) {
                mage.setLastHeldMapId(next.getDurability());
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        final Player player = event.getPlayer();
        Mage apiMage = controller.getRegisteredMage(player);
        if (apiMage == null || !(apiMage instanceof com.elmakers.mine.bukkit.magic.Mage)) return;
        final com.elmakers.mine.bukkit.magic.Mage mage = (com.elmakers.mine.bukkit.magic.Mage)apiMage;

        // Catch lag-related glitches dropping items from GUIs
        if (mage.getActiveGUI() != null) {
            event.setCancelled(true);
            return;
        }

        final Wand activeWand = mage.getActiveWand();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        boolean cancelEvent = false;
        String droppedId = Wand.getWandId(droppedItem);
        boolean droppedWand = droppedId != null && activeWand != null && activeWand.getId().equals(droppedId);
        if (droppedWand && activeWand.isUndroppable()) {
            // Postpone cycling until after this event unwinds
            Bukkit.getScheduler().scheduleSyncDelayedTask(controller.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    if (activeWand.isDropToggle()) {
                        controller.onToggleInventory(mage, activeWand);
                    } else if (activeWand.getHotbarCount() > 1) {
                        activeWand.cycleHotbar(1);
                    } else if (activeWand.isInventoryOpen()) {
                        activeWand.closeInventory();
                    }
                }
            });
            cancelEvent = true;
        } else if (activeWand != null) {
            if (droppedWand) {
                activeWand.deactivate();
                // Clear after inventory restore (potentially with deactivate), since that will put the wand back
                if (Wand.hasActiveWand(player)) {
                    String activeId = Wand.getWandId(player.getItemInHand());
                    if (activeId != null && activeWand.getId().equals(activeId))
                    {
                        player.setItemInHand(new ItemStack(Material.AIR, 1));
                    }
                }
            } else if (activeWand.isInventoryOpen()) {
                if (!controller.isSpellDroppingEnabled()) {
                    cancelEvent = true;
                } else {
                    // The item is already removed from the wand's inventory, but that should be ok
                    controller.removeItemFromWand(activeWand, droppedItem);
                }
            }
        } else if (Wand.Undroppable && Wand.isWand(droppedItem) && !player.hasPermission("Magic.wand.override_drop")) {
            Wand wand = new Wand(controller, droppedItem);
            if (wand.isUndroppable()) {
                cancelEvent = true;
            }
        }

        if (cancelEvent) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Mage apiMage = controller.getRegisteredMage(event.getPlayer());
        if (apiMage == null || !(apiMage instanceof com.elmakers.mine.bukkit.magic.Mage)) return;
        com.elmakers.mine.bukkit.magic.Mage mage = (com.elmakers.mine.bukkit.magic.Mage)apiMage;
        mage.restoreRespawnInventories();
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteractArmorStand(PlayerArmorStandManipulateEvent event)
    {
        Player player = event.getPlayer();
        Mage mage = controller.getRegisteredMage(player);
        if (mage == null) return;
        com.elmakers.mine.bukkit.api.wand.Wand wand = mage.checkWand();
        if (wand != null) {
            if (wand.isUndroppable()) {
                event.setCancelled(true);
                return;
            } else {
                wand.deactivate();
            }
        }
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Mage apiMage = controller.getRegisteredMage(player);
        if (apiMage == null || !(apiMage instanceof com.elmakers.mine.bukkit.magic.Mage)) return;
        com.elmakers.mine.bukkit.magic.Mage mage = (com.elmakers.mine.bukkit.magic.Mage)apiMage;
        Wand wand = mage.checkWand();

        // Check for a player placing a wand in an item frame
        Entity clickedEntity = event.getRightClicked();

        // Don't think this ever fires for ArmorStand - see above
        boolean isPlaceable = clickedEntity instanceof ItemFrame || clickedEntity instanceof ArmorStand;
        if (wand != null && isPlaceable) {
            if (wand.isUndroppable()) {
                event.setCancelled(true);
                return;
            } else {
                wand.deactivate();
            }
        }

        // Check for clicking on a Citizens NPC, in case
        // this hasn't been cancelled yet
        if (controller.isNPC(event.getRightClicked())) {
            if (wand != null) {
                wand.closeInventory();
            }

            // Don't let it re-open right away
            mage.checkLastClick(0);
        } else {
            // Don't allow interacting while holding spells, brushes or upgrades
            ItemStack itemInHand = player.getItemInHand();
            if (Wand.isSpell(itemInHand) || Wand.isBrush(itemInHand) || Wand.isUpgrade(itemInHand)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (!controller.isLoaded()) return;
        // Note that an interact on air event will arrive pre-cancelled
        // So this is kind of useless. :\
        //if (event.isCancelled()) return;

        // Block block = event.getClickedBlock();
        // controller.getLogger().info("INTERACT: " + event.getAction() + " on " + (block == null ? "NOTHING" : block.getType()));

        Player player = event.getPlayer();

        // Don't allow interacting while holding spells, brushes or upgrades
        ItemStack itemInHand = player.getItemInHand();
        if (Wand.isSpell(itemInHand) || Wand.isBrush(itemInHand) || Wand.isUpgrade(itemInHand)) {
            event.setCancelled(true);
            return;
        }

        Mage apiMage = controller.getMage(player);
        if (!(apiMage instanceof com.elmakers.mine.bukkit.magic.Mage)) return;
        com.elmakers.mine.bukkit.magic.Mage mage = (com.elmakers.mine.bukkit.magic.Mage)apiMage;

        Wand wand = mage.checkWand();

        // Reset indestructible wand durability
        if (wand != null && wand.isIndestructible())
        {
            ItemStack item = wand.getItem();
            if (item.getType().getMaxDurability() > 0)
            {
                wand.getItem().setDurability((short)0);
            }
        }

        // Check for wearing via right-click
        Action action = event.getAction();
        if (itemInHand != null
                && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && controller.isWearable(itemInHand))
        {
            if (wand != null)
            {
                wand.deactivate();
            }
            controller.onArmorUpdated(mage);
            return;
        }

        if (wand == null) return;

        Messages messages = controller.getMessages();
        if (!controller.hasWandPermission(player))
        {
            return;
        }

        // Check for region or wand-specific permissions
        if (!controller.hasWandPermission(player, wand))
        {
            wand.deactivate();
            mage.sendMessage(messages.get("wand.no_permission").replace("$wand", wand.getName()));
            return;
        }

        // Check for enchantment table click
        Block clickedBlock = event.getClickedBlock();
        if (wand.hasSpellProgression() && controller.isSPEnabled() && clickedBlock != null && clickedBlock.getType() != Material.AIR && enchantBlockMaterial != null && enchantBlockMaterial.is(clickedBlock))
        {
            Spell spell = null;
            if (player.isSneaking())
            {
                spell = enchantSneakClickSpell != null ? mage.getSpell(enchantSneakClickSpell) : null;
            }
            else
            {
                spell = enchantClickSpell != null ? mage.getSpell(enchantClickSpell) : null;
            }
            if (spell != null)
            {
                spell.cast();
                event.setCancelled(true);
                return;
            }
        }

        if (!mage.checkLastClick(clickCooldown)) {
            event.setCancelled(true);
            return;
        }

        boolean isSwing = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;

        if (isSwing) {
            wand.playEffects("swing");
        }

        if (isSwing && !wand.isUpgrade() && !wand.isQuickCast())
        {
            wand.cast();
            event.setCancelled(true);
            return;
        }

        boolean toggleInventory = (action == Action.RIGHT_CLICK_AIR);
        if (!toggleInventory && action == Action.RIGHT_CLICK_BLOCK) {
            Material material = event.getClickedBlock().getType();
            toggleInventory = !controller.isInteractable(event.getClickedBlock());

            // This is to prevent Essentials signs from giving you an item in your wand inventory.
            if (material== Material.SIGN_POST || material == Material.WALL_SIGN) {
                wand.closeInventory();
            }
        }
        if (toggleInventory && !wand.isDropToggle())
        {
            controller.onToggleInventory(mage, wand);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        // Automatically re-activate mages.
        controller.getMage(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerKick(PlayerKickEvent event)
    {
        handlePlayerQuitEvent(event);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        handlePlayerQuitEvent(event);
    }

    protected void handlePlayerQuitEvent(PlayerEvent event) {
        Player player = event.getPlayer();
        Mage mage = controller.getRegisteredMage(player);
        if (mage != null) {
            if (mage instanceof com.elmakers.mine.bukkit.magic.Mage)
            {
                ((com.elmakers.mine.bukkit.magic.Mage)mage).onPlayerQuit(event);
            }
            controller.playerQuit(mage);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event)
    {
        Player player = event.getPlayer();
        Mage mage = controller.getMage(player);
        mage.checkWand();
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
    {
        if (event.getNewGameMode() == GameMode.CREATIVE) {
            Player player = event.getPlayer();
            Mage mage = controller.getMage(player);
            com.elmakers.mine.bukkit.api.wand.Wand activeWand = mage.getActiveWand();
            if (activeWand != null) {
                activeWand.closeInventory();
            }

            if (enableCreativeModeEjecting) {
                boolean ejected = false;
                if (activeWand != null) {
                    activeWand.deactivate();
                }
                Inventory inventory = player.getInventory();
                ItemStack[] contents = inventory.getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    if (Wand.isWand(item)) {
                        ejected = true;
                        inventory.setItem(i, null);
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
                if (ejected) {
                    mage.sendMessage("Ejecting wands, creative mode will destroy them!");
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event)
    {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Mage apiMage = controller.getMage(player);

        if (!(apiMage instanceof com.elmakers.mine.bukkit.magic.Mage)) return;
        com.elmakers.mine.bukkit.magic.Mage mage = (com.elmakers.mine.bukkit.magic.Mage)apiMage;

        Item item = event.getItem();
        ItemStack pickup = item.getItemStack();
        if (NMSUtils.isTemporary(pickup) || item.hasMetadata("temporary"))
        {
            item.remove();
            event.setCancelled(true);
            return;
        }

        boolean isWand = Wand.isWand(pickup);

        // Creative mode inventory hacky work-around :\
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE && isWand && enableCreativeModeEjecting) {
            event.setCancelled(true);
            return;
        }

        // Remove lost wands from records
        Messages messages = controller.getMessages();
        if (isWand) {
            Wand wand = new Wand(controller, pickup);
            if (!wand.canUse(player)) {
                mage.sendMessage(messages.get("wand.bound").replace("$name", wand.getOwner()));
                event.setCancelled(true);
                Item droppedItem = event.getItem();
                org.bukkit.util.Vector velocity = droppedItem.getVelocity();
                velocity.setY(velocity.getY() * 2 + 1);
                droppedItem.setVelocity(velocity);
                return;
            }

            if (controller.removeLostWand(wand.getId())) {
                controller.info("Player " + mage.getName() + " picked up wand " + wand.getName() + ", id " + wand.getId());
            }
        }

        // Wands will absorb spells and upgrade items
        Wand activeWand = mage.getActiveWand();
        if (activeWand != null
                && activeWand.isModifiable()
                && (Wand.isSpell(pickup) || Wand.isBrush(pickup) || Wand.isUpgrade(pickup) || Wand.isSP(pickup))
                && activeWand.addItem(pickup)) {
            event.getItem().remove();
            event.setCancelled(true);
            return;
        }

        // If a wand's inventory is active, add the item there
        if (mage.hasStoredInventory()) {
            event.setCancelled(true);
            if (mage.addToStoredInventory(event.getItem().getItemStack())) {
                event.getItem().remove();
            }
        } else {
            // Hackiness needed because we don't get an equip event for this!
            PlayerInventory inventory = event.getPlayer().getInventory();
            ItemStack inHand = inventory.getItemInHand();
            if (isWand && (inHand == null || inHand.getType() == Material.AIR)) {
                Wand wand = new Wand(controller, pickup);
                event.setCancelled(true);
                event.getItem().remove();
                inventory.setItem(inventory.getHeldItemSlot(), pickup);
                wand.activate(mage);
            }
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event)
    {
        // We don't care about exp loss events
        if (event.getAmount() <= 0) return;

        Player player = event.getPlayer();
        Mage apiMage = controller.getRegisteredMage(player);

        if (apiMage != null && !(apiMage instanceof com.elmakers.mine.bukkit.magic.Mage)) return;
        com.elmakers.mine.bukkit.magic.Mage mage = (com.elmakers.mine.bukkit.magic.Mage)apiMage;

        Wand wand = mage.getActiveWand();
        if (wand != null) {
            wand.onPlayerExpChange(event);
        }
    }
}
