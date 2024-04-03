package net.sneakydispatch.emergency

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import net.sneakydispatch.SneakyDispatch

class EmergencyManager() {

    init {
        try {
            val emergencyCategories = loadEmergencyCategories()
            emergencyCategories.forEach { category ->
                SneakyDispatch.log("Emergency Category: ${category.name}, Dispatch Cap: ${category.dispatchCap}, Dispatch Par: ${category.dispatchPar}")
            }
        } catch (e: Exception) {
            SneakyDispatch.log("An error occurred: ${e.message}")
        }
    }

    public fun loadEmergencyCategories(): List<EmergencyCategory> {
        val configFile = SneakyDispatch.getConfigFile()
        if (!configFile.exists()) {
            throw IllegalStateException("config.yml not found")
        }

        val config = YamlConfiguration.loadConfiguration(configFile)
        val emergencySection = config.getConfigurationSection("emergencies") ?: return emptyList()

        return emergencySection.getKeys(false).map { categoryName ->
            val dispatchCap = emergencySection.getInt("$categoryName.dispatch-cap")
            val dispatchPar = emergencySection.getInt("$categoryName.dispatch-par")
            EmergencyCategory(categoryName, dispatchCap, dispatchPar)
        }
    }
}

data class EmergencyCategory(
    val name: String,
    val dispatchCap: Int,
    val dispatchPar: Int
)

data class Emergency(
    val category: EmergencyCategory
)