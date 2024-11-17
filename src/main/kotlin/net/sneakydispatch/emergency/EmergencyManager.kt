package net.sneakydispatch.emergency

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
import java.util.*

/**
 * Manages emergency categories and their configurations. This class is responsible for
 * loading, storing, and accessing emergency categories defined in the configuration file.
 */
class EmergencyManager {

    /** A unique key used for identifying emergencies in persistent data. */
    val IDKEY: NamespacedKey = NamespacedKey(SneakyDispatch.getInstance(), "id")

    /** A map storing emergency categories, where keys are category names and values are [EmergencyCategory] objects. */
    private val emergencyCategories: MutableMap<String, EmergencyCategory> = mutableMapOf()

    /**
     * Loads emergency categories from the configuration file during the initialization of this manager.
     */
    init {
        loadEmergencyCategories()
    }

    /**
     * Loads emergency categories from the config.yml file. Clears existing categories before loading new ones.
     * If any error occurs during the loading process, an error message is logged, and categories are cleared.
     */
    private fun loadEmergencyCategories() {
        try {
            val configFile = SneakyDispatch.getConfigFile()
            if (!configFile.exists()) {
                throw IllegalStateException("config.yml not found")
            }

            val config = YamlConfiguration.loadConfiguration(configFile)
            val emergencySection = config.getConfigurationSection("emergencies") ?: return

            emergencyCategories.clear()

            // Iterate over emergency categories in the config and load them.
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
                val durationMillis = emergencySection.getInt("$key.duration-millis")

                emergencyCategories[key] = EmergencyCategory(
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
     * @return A map of emergency category keys to their corresponding [EmergencyCategory] objects.
     */
    fun getEmergencyCategories(): Map<String, EmergencyCategory> {
        return emergencyCategories
    }
}

/**
 * Represents an emergency category with its associated properties.
 *
 * @param name The name of the emergency category.
 * @param description The description of the emergency category.
 * @param iconMaterial The material used for the emergency's icon.
 * @param iconCustomModelData The custom model data for the icon.
 * @param dispatchCap The maximum number of responders that can be dispatched to this emergency.
 * @param dispatchPar The recommended number of responders for this emergency.
 * @param durationMillis The duration of the emergency in milliseconds.
 */
data class EmergencyCategory(
    val name: String,
    val description: String,
    val iconMaterial: Material,
    val iconCustomModelData: Int,
    val dispatchCap: Int,
    val dispatchPar: Int,
    val durationMillis: Int
)

/**
 * Represents an individual emergency associated with a player.
 *
 * @param category The category of the emergency.
 * @param player The player who reported the emergency.
 */
data class Emergency(val category: EmergencyCategory, val player: Player) {
    val uuid: String = UUID.randomUUID().toString()
    val location: Location = player.location

    /** The description of the emergency, optionally with PlaceholderAPI replacements. */
    private var description: String = if (SneakyDispatch.isPapiActive()) {
        PlaceholderAPI.setPlaceholders(player, category.description).replace("none", "Dinky Dank")
    } else {
        category.description
    }

    /** Delay before the emergency becomes active. */
    var delay: Long = 0
        set(value) {
            startTime -= delay - value
            field = value

            if (value > 0) {
                var desc = category.description

                val config = SneakyDispatch.getInstance().config
                val replacements = config.getConfigurationSection("delayed-tooltip-text-replacements")
                if (replacements != null) {
                    for (key in replacements.getKeys(false)) {
                        val replacementList = replacements.getStringList(key)
                        if (replacementList.isNotEmpty()) {
                            val replacement = replacementList.random()
                            desc = desc.replace(key, replacement)
                        }
                    }
                }

                description = if (SneakyDispatch.isPapiActive()) {
                    PlaceholderAPI.setPlaceholders(player, desc).replace("none", "Dinky Dank")
                } else {
                    desc
                }
            } else {
                description = if (SneakyDispatch.isPapiActive()) {
                    PlaceholderAPI.setPlaceholders(player, category.description).replace("none", "Dinky Dank")
                } else {
                    category.description
                }
            }
        }

    /** The start time of the emergency. */
    private var startTime: Long = System.currentTimeMillis() + delay

    /** The number of players dispatched to handle this emergency. */
    var dispatched: Int = 0

    /** The paladins that this event was assigned to. */
    val paladins = mutableListOf<Player>();

    /** Checks if the emergency has expired based on its duration. */
    fun isExpired(): Boolean {
        return (System.currentTimeMillis() >= startTime + category.durationMillis)
    }

    /** Increments the count of dispatched responders. */
    fun incrementDispatched() {
        dispatched++
    }

    /** Gets the maximum number of responders allowed for this emergency. */
    fun getDispatchCap(): Int {
        return category.dispatchCap
    }

    /** Checks if the dispatch cap has been fulfilled. */
    private fun isCapFulfilled(): Boolean {
        return (dispatched >= category.dispatchCap)
    }

    /** Checks if the dispatch par (recommended responders) has been fulfilled. */
    fun isParFulfilled(): Boolean {
        return (dispatched >= category.dispatchPar)
    }

    /** Retrieves the name of the emergency. */
    fun getName(): String {
        return category.name
    }

    /**
     * Generates an item representing the emergency, which includes custom metadata, lore,
     * and dispatch information.
     * @return The [ItemStack] representing the emergency.
     */
    fun getIconItem(): ItemStack {
        val itemStack: ItemStack
        val customModelData: Int
        val dispatchColorCode: String

        if (!isCapFulfilled()) {
            itemStack = ItemStack(category.iconMaterial)
            customModelData = category.iconCustomModelData
            dispatchColorCode = "&b"
        } else {
            val iconMaterialString =
                SneakyDispatch.getInstance().config.getString("cap-icon-material") ?: "red_wool"
            var iconMaterial = Material.matchMaterial(iconMaterialString)
            if (iconMaterial == null) {
                SneakyDispatch.log(
                    "Invalid material '$iconMaterialString' specified for dispatch cap. Using default."
                )
                iconMaterial = Material.RED_WOOL
            }

            itemStack = ItemStack(iconMaterial)
            customModelData = SneakyDispatch.getInstance().config.getInt("cap-icon-custom-model-data")
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
            SneakyDispatch.getEmergencyManager().IDKEY, PersistentDataType.STRING, uuid
        )

        itemStack.itemMeta = meta
        return itemStack
    }
}