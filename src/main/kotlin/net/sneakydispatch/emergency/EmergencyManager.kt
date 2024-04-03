package net.sneakydispatch.emergency

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import net.sneakydispatch.SneakyDispatch
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Manages emergency categories and their configurations.
 */
class EmergencyManager {
    
    private val emergencyCategories: MutableMap<String, EmergencyCategory> = mutableMapOf()

    /**
     * Loads emergency categories from the configuration file on initialization.
     */
    init {
        loadEmergencyCategories()
    }

    /**
     * Loads emergency categories from the configuration file.
     * If an error occurs during loading, it's logged and the categories are cleared.
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
                val dispatchCap = emergencySection.getInt("$key.dispatch-cap")
                val dispatchPar = emergencySection.getInt("$key.dispatch-par")
                val durationMillis = emergencySection.getInt("$key.duration-millis")

                emergencyCategories[key] = EmergencyCategory(
                    name,
                    description,
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
    val dispatchCap: Int,
    val dispatchPar: Int,
    val durationMillis: Int
)

data class Emergency(
    val category: EmergencyCategory,
    var dispatched: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val reportingPlayer: Player,
    val reportingLocation: Location,
    var description: String = if (SneakyDispatch.isPapiActive()) {
        PlaceholderAPI.setPlaceholders(reportingPlayer, category.description).replace("none", "Dinky Dank")
    } else {
        category.description
    }
) {
    fun incrementDispatched() {
        dispatched++
    }

    fun isExpired(): Boolean {
        return (System.currentTimeMillis() >= startTime + category.durationMillis)
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

    fun getDispatchCap(): Int {
        return category.dispatchCap
    }

    fun report() {

    }
    
}
