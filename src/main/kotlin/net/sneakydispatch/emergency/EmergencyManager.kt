package net.sneakydispatch.emergency

import java.util.UUID
import me.clip.placeholderapi.PlaceholderAPI
import net.sneakydispatch.SneakyDispatch
import net.sneakydispatch.util.TextUtility
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** Manages emergency categories and their configurations. */
class EmergencyManager {

    public val IDKEY: NamespacedKey = NamespacedKey(SneakyDispatch.getInstance(), "id")
    private val emergencyCategories: MutableMap<String, EmergencyCategory> = mutableMapOf()

    /** Loads emergency categories from the configuration file on initialization. */
    init {
        loadEmergencyCategories()
    }

    /**
     * Loads emergency categories from the configuration file. If an error occurs during loading,
     * it's logged and the categories are cleared.
     */
    public fun loadEmergencyCategories() {
        try {
            val configFile = SneakyDispatch.getConfigFile()
            if (!configFile.exists()) {
                throw IllegalStateException("config.yml not found")
            }

            val config = YamlConfiguration.loadConfiguration(configFile)
            val emergencySection = config.getConfigurationSection("emergencies") ?: return

            emergencyCategories.clear()

            emergencySection.getKeys(false).forEach { key ->
                val name = emergencySection.getString("$key.name") ?: key
                val description = emergencySection.getString("$key.description") ?: key
                val iconMaterialString = emergencySection.getString("$key.icon-material") ?: ""

                var iconMaterial = Material.matchMaterial(iconMaterialString)
                if (iconMaterial == null) {
                    SneakyDispatch.log(
                            "Invalid material '$iconMaterialString' specified for emergency '$key'. Using default."
                    )
                    iconMaterial = Material.MUSIC_DISC_CAT
                }
                val iconCustomModelData = emergencySection.getInt("$key.icon-custom-model-data")
                val dispatchCap = emergencySection.getInt("$key.dispatch-cap")
                val dispatchPar = emergencySection.getInt("$key.dispatch-par")
                val durationMillis = emergencySection.getInt("$key.duration-milis")

                emergencyCategories[key] =
                        EmergencyCategory(
                                name,
                                description,
                                iconMaterial,
                                iconCustomModelData,
                                if (dispatchCap > 0) dispatchCap else 1,
                                dispatchPar,
                                if (durationMillis > 0) durationMillis else 600000
                        )
            }
        } catch (e: Exception) {
            SneakyDispatch.log("An error occurred while loading emergency categories: ${e.message}")
        }
    }

    /**
     * Retrieves a read-only map of emergency categories.
     * @return A map of emergency category keys to their corresponding EmergencyCategory objects.
     */
    fun getEmergencyCategories(): Map<String, EmergencyCategory> {
        return emergencyCategories
    }
}

data class EmergencyCategory(
        val name: String,
        val description: String,
        val iconMaterial: Material,
        val iconCustomModelData: Int,
        val dispatchCap: Int,
        val dispatchPar: Int,
        val durationMillis: Int
)

data class Emergency(val category: EmergencyCategory, val player: Player) {
    val uuid: String = UUID.randomUUID().toString()
    val location: Location = player.location
    var description: String =
            if (SneakyDispatch.isPapiActive()) {
                PlaceholderAPI.setPlaceholders(player, category.description)
                        .replace("none", "Dinky Dank")
            } else {
                category.description
            }
    var delay: Long = 0
        set(value) {
            startTime -= delay - value
            field = value

            if (value > 0) {
                var desc_ = category.description

                val config = SneakyDispatch.getInstance().getConfig()
                val replacements =
                        config.getConfigurationSection("delayed-tooltip-text-replacements")
                if (replacements != null) {
                    for (key in replacements.getKeys(false)) {
                        val replacementList = replacements.getStringList(key)
                        if (replacementList.isNotEmpty()) {
                            val replacement = replacementList.random()
                            desc_ = desc_.replace(key, replacement)
                        }
                    }
                }

                if (SneakyDispatch.isPapiActive()) {
                    description =
                            PlaceholderAPI.setPlaceholders(player, desc_)
                                    .replace("none", "Dinky Dank")
                } else {
                    description = desc_
                }
            } else {
                if (SneakyDispatch.isPapiActive()) {
                    description =
                            PlaceholderAPI.setPlaceholders(player, category.description)
                                    .replace("none", "Dinky Dank")
                } else {
                    description = category.description
                }
            }
        }
    var startTime: Long = System.currentTimeMillis() + delay
    var dispatched: Int = 0

    fun isExpired(): Boolean {
        return (System.currentTimeMillis() >= startTime + category.durationMillis)
    }

    fun incrementDispatched() {
        dispatched++
    }

    fun getDispatchCap(): Int {
        return category.dispatchCap
    }

    fun isCapFulfilled(): Boolean {
        return (dispatched >= category.dispatchCap)
    }

    fun isParFulfilled(): Boolean {
        return (dispatched >= category.dispatchPar)
    }

    fun getName(): String {
        return category.name
    }

    fun getIconItem(): ItemStack {
        var itemStack: ItemStack
        var customModelData: Int
        var dispatchColorCode: String

        if (!isCapFulfilled()) {
            itemStack = ItemStack(category.iconMaterial)
            customModelData = category.iconCustomModelData
            dispatchColorCode = "&b"
        } else {
            val iconMaterialString =
                    SneakyDispatch.getInstance().getConfig().getString("cap-icon-material")
                            ?: "red_wool"
            var iconMaterial = Material.matchMaterial(iconMaterialString)
            if (iconMaterial == null) {
                SneakyDispatch.log(
                        "Invalid material '$iconMaterialString' specified for dispatch cap. Using default."
                )
                iconMaterial = Material.RED_WOOL
            }

            itemStack = ItemStack(iconMaterial)
            customModelData =
                    SneakyDispatch.getInstance().getConfig().getInt("cap-icon-custom-model-data")
            dispatchColorCode = "&4"
        }

        val meta = itemStack.itemMeta

        // Set custom model data, display name, and lore.
        meta.setCustomModelData(customModelData)
        meta.displayName(
                TextUtility.convertToComponent(
                        "&a${if (delay > 0) "Local Report: " else ""}${category.name}"
                )
        )

        val lore = mutableListOf<String>()

        // Split the description into lines of a maximum length
        val descriptionLines = TextUtility.splitIntoLines(description, 30)

        // Add each line of the description to the lore
        for (line in descriptionLines) {
            lore.add("&e$line")
        }

        // Add the dispatched information to the lore
        lore.add("${dispatchColorCode}Dispatched: [ $dispatched / ${category.dispatchCap} ]")

        meta.lore(lore.map { TextUtility.convertToComponent(it) })

        // Set persistent data.
        val persistentData = meta.persistentDataContainer
        persistentData.set(
                SneakyDispatch.getEmergencyManager().IDKEY,
                PersistentDataType.STRING,
                uuid
        )

        itemStack.itemMeta = meta
        return itemStack
    }
}
