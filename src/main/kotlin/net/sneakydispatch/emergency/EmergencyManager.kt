package net.sneakydispatch.emergency

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import net.sneakydispatch.SneakyDispatch

class EmergencyManager {
    
    private val emergencyCategories: MutableMap<String, EmergencyCategory> = mutableMapOf()

    init {
        loadEmergencyCategories()
    }

    private fun loadEmergencyCategories() {
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
    val startTime: Long = System.currentTimeMillis()
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

    fun getDescription(): String {
        return category.description
    }

    fun getDispatchCap(): Int {
        return category.dispatchPar
    }
}
