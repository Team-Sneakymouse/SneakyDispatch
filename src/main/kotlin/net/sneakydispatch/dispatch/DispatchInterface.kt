package net.sneakydispatch.dispatch

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Custom inventory holder for managing the emergency dispatch GUI.
 * This class creates and populates the inventory with emergency information,
 * allowing players to interact with the inventory to dispatch themselves to emergencies.
 */
class EmergencyInventoryHolder : InventoryHolder {

    /** The inventory instance associated with this holder. */
    private lateinit var inventory: Inventory

    /**
     * Returns the custom inventory managed by this holder.
     * @return The [Inventory] instance managed by this holder.
     */
    override fun getInventory(): Inventory {
        return inventory
    }

    /**
     * Populates the inventory with emergency icons, sorting immediate emergencies
     * into the first rows and delayed emergencies into subsequent rows.
     * Ensures the inventory stays within Minecraft size constraints (9 to 54 slots).
     */
    fun populateInventory() {
        // Cleanup expired emergencies before populating.
        SneakyDispatch.getDispatchManager().cleanup()
        val emergencies = SneakyDispatch.getDispatchManager().getEmergencies()

        // Count the number of delayed emergencies.
        val delayedEmergenciesCount = emergencies.count { it.delay > 0 }

        // Calculate the number of rows needed for immediate emergencies.
        val immediateRowCount = if (delayedEmergenciesCount > 0) 3 else ((emergencies.size - 1) / 9 + 1)
        val delayedRows = (delayedEmergenciesCount / 9) + if (delayedEmergenciesCount % 9 > 0) 1 else 0

        // Total rows include immediate rows plus rows for delayed emergencies.
        val totalRows = immediateRowCount + delayedRows

        // Ensure the inventory size is between 9 and 54 slots.
        val size = (totalRows * 9).coerceAtLeast(9).coerceAtMost(54)
        inventory = Bukkit.createInventory(this, size, TextUtility.convertToComponent("&eDispatch"))

        var immediateIndex = 0  // Index for placing immediate emergencies.
        var delayedIndex = immediateRowCount * 9  // Index for placing delayed emergencies.

        for (emergency in emergencies) {
            val iconItem = emergency.getIconItem()

            // Determine the placement of the emergency based on its delay.
            if (emergency.delay > 0) {
                // Place delayed emergencies in subsequent rows.
                if (delayedIndex < inventory.size) {
                    inventory.setItem(delayedIndex++, iconItem)
                }
            } else {
                // Place immediate emergencies in the first rows.
                if (immediateIndex < immediateRowCount * 9) {
                    inventory.setItem(immediateIndex++, iconItem)
                }
            }

            // Stop populating if the inventory is full.
            if (immediateIndex >= immediateRowCount * 9 && delayedIndex >= inventory.size) {
                break
            }
        }
    }

    /**
     * Handles the action when a player clicks on an emergency item in the inventory.
     * The player will be dispatched to the selected emergency.
     *
     * @param clickedItem The item that was clicked in the inventory.
     * @param player The player who clicked the item.
     */
    fun clickedItem(clickedItem: ItemStack, player: Player) {
        val meta = clickedItem.itemMeta
        val uuid =
            meta?.persistentDataContainer?.get(SneakyDispatch.getEmergencyManager().IDKEY, PersistentDataType.STRING)

        // If no valid UUID is found, exit the function.
        if (uuid.isNullOrEmpty()) return

        // Close the inventory and dispatch the player to the emergency.
        player.closeInventory()
        SneakyDispatch.getDispatchManager().dispatch(uuid, player)
    }
}

/**
 * Listener class to handle inventory interactions related to emergency dispatching.
 * This class ensures that players can interact with the emergency dispatch inventory correctly,
 * and handles clicks on emergency items.
 */
class EmergencyInventoryListener : Listener {

    /**
     * Handles the event where a player clicks in an inventory. If the player is clicking
     * in an emergency dispatch inventory, the click is processed to dispatch the player.
     *
     * @param event The [InventoryClickEvent] triggered when a player clicks in an inventory.
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clickedInventory = event.clickedInventory ?: return

        // Check if the top inventory is an emergency dispatch inventory.
        val topInventoryHolder = event.view.topInventory.holder
        if (topInventoryHolder is EmergencyInventoryHolder) {
            // Cancel all interactions with the top inventory to prevent unauthorized actions.
            event.isCancelled = true
        }

        // If the clicked inventory is not an emergency dispatch inventory, exit.
        if (clickedInventory.holder !is EmergencyInventoryHolder) return

        val clickedItem = event.currentItem ?: return

        // Handle left-click to dispatch the player.
        when (event.click) {
            ClickType.LEFT -> {
                val holder = clickedInventory.holder as? EmergencyInventoryHolder ?: return
                val player = event.whoClicked as? Player ?: return
                holder.clickedItem(clickedItem, player)
            }

            // Other click types are ignored.
            else -> {}
        }
    }

    /**
     * Handles the event when a player interacts with an inventory, such as moving items.
     * If the inventory is an emergency dispatch inventory, the interaction is cancelled to
     * prevent unintended changes.
     *
     * @param event The [InventoryInteractEvent] triggered when a player interacts with an inventory.
     */
    @EventHandler
    fun onInventoryInteract(event: InventoryInteractEvent) {
        // Cancel any interaction with the emergency dispatch inventory.
        val topInventoryHolder = event.view.topInventory.holder
        if (topInventoryHolder is EmergencyInventoryHolder) {
            event.isCancelled = true
        }
    }
}