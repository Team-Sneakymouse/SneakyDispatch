package net.sneakydispatch.emergency

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class EmergencyManager {
    fun main() {
        try {
            val emergencyCategories = loadEmergencyCategories()
            emergencyCategories.forEach { category ->
                println("Emergency Category: ${category.name}, Dispatch Cap: ${category.dispatchCap}")
            }
        } catch (e: Exception) {
            println("An error occurred: ${e.message}")
        }
    }

    private fun loadEmergencyCategories(): List<EmergencyCategory> {
        val configFile = File("config.yml")
        if (!configFile.exists()) {
            throw IllegalStateException("config.yml not found")
        }

        val config = YamlConfiguration.loadConfiguration(configFile)
        val emergencySection = config.getConfigurationSection("emergencies") ?: return emptyList()

        return emergencySection.getKeys(false).map { categoryName ->
            val dispatchCap = emergencySection.getInt("$categoryName.dispatch-cap")
            EmergencyCategory(categoryName, dispatchCap)
        }
    }
}
