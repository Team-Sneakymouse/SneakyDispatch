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

class EmergencyInventoryHolder() : InventoryHolder {
    private lateinit var inventory: Inventory

    override fun getInventory(): Inventory {
        return inventory
    }

    fun populateInventory() {
        SneakyDispatch.getDispatchManager().cleanup()
        val emergencies = SneakyDispatch.getDispatchManager().getEmergencies()

        // Count delayed emergencies
        val delayedEmergenciesCount = emergencies.count { it.delay > 0 }

        // Immediate emergencies take up the first 3 rows, calculate additional rows needed for
        // delayed ones
        val immediateRowCount =
                if (delayedEmergenciesCount > 0) 3 else ((emergencies.size - 1) / 9 + 1)
        val delayedRows =
                (delayedEmergenciesCount / 9) + if (delayedEmergenciesCount % 9 > 0) 1 else 0

        // Total rows = immediate rows + rows needed for delayed emergencies
        val totalRows = immediateRowCount + delayedRows

        // Ensure inventory size is within Minecraft constraints
        val size = (totalRows * 9).coerceAtLeast(9).coerceAtMost(54)
        inventory = Bukkit.createInventory(this, size, TextUtility.convertToComponent("&eDispatch"))

        var immediateIndex = 0 // Index for immediate emergencies
        var delayedIndex = immediateRowCount * 9 // Index for delayed emergencies

        for (emergency in emergencies) {
            val iconItem = emergency.getIconItem()

            // Determine where to place the emergency based on delay
            if (emergency.delay > 0) {
                // Check if there's room for more delayed emergencies
                if (delayedIndex < inventory.size) {
                    inventory.setItem(delayedIndex++, iconItem)
                }
            } else {
                // Place immediate emergencies in the first available slot within the first 3 rows
                if (immediateIndex < immediateRowCount * 9) {
                    inventory.setItem(immediateIndex++, iconItem)
                }
            }

            // Stop if inventory is full
            if (immediateIndex >= immediateRowCount * 9 && delayedIndex >= inventory.size) {
                break
            }
        }
    }

    fun clickedItem(clickedItem: ItemStack, player: Player) {
        val meta = clickedItem.itemMeta
        val uuid =
                meta.getPersistentDataContainer()
                        .get(SneakyDispatch.getEmergencyManager().IDKEY, PersistentDataType.STRING)

        if (uuid == null || uuid.isEmpty()) return

        player.closeInventory()

        SneakyDispatch.getDispatchManager().dispatch(uuid, player)
    }
}

class EmergencyInventoryListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clickedInventory = event.clickedInventory ?: return

        val topInventoryHolder = event.view.topInventory.holder
        if (topInventoryHolder is EmergencyInventoryHolder) {
            event.isCancelled = true
        }

        if (clickedInventory.holder !is EmergencyInventoryHolder) return

        val clickedItem = event.currentItem ?: return

        when (event.click) {
            ClickType.LEFT -> {
                val holder = clickedInventory.holder as? EmergencyInventoryHolder ?: return
                val player = event.whoClicked as? Player ?: return
                holder.clickedItem(clickedItem, player)
            }
            else -> {}
        }
    }

    @EventHandler
    fun onInventoryInteract(event: InventoryInteractEvent) {
        val topInventoryHolder = event.view.topInventory.holder
        if (topInventoryHolder is EmergencyInventoryHolder) {
            event.isCancelled = true
        }
    }
}
