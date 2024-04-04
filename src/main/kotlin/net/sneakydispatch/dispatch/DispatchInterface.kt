package net.sneakydispatch.dispatch

import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.ChatUtility
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
        val size = (((emergencies.size + 8) / 9) * 9).coerceAtLeast(9).coerceAtMost(54)
        inventory = Bukkit.createInventory(this, size, ChatUtility.convertToComponent("&eDispatch"))

        for (emergency in emergencies) {
            val iconItem = emergency.getIconItem()
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(iconItem)
            } else {
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
