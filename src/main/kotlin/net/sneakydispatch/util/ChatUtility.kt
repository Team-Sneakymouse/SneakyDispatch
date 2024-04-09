package net.sneakydispatch.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage

object ChatUtility {

    fun convertToComponent(message: String): Component {
        var convertedMessage =
                message.replace("&1", "<dark_blue>")
                        .replace("&2", "<dark_green>")
                        .replace("&3", "<dark_aqua>")
                        .replace("&4", "<dark_red>")
                        .replace("&5", "<dark_purple>")
                        .replace("&6", "<gold>")
                        .replace("&7", "<gray>")
                        .replace("&8", "<dark_gray>")
                        .replace("&9", "<blue>")
                        .replace("&0", "<black>")
                        .replace("&a", "<green>")
                        .replace("&b", "<aqua>")
                        .replace("&c", "<red>")
                        .replace("&d", "<light_purple>")
                        .replace("&e", "<yellow>")
                        .replace("&f", "<white>")
                        .replace("&k", "<obf>")
                        .replace("&l", "<b>")
                        .replace("&m", "<st>")
                        .replace("&n", "<u>")
                        .replace("&o", "<i>")
                        .replace("&r", "<reset>")
                        .replace("&#([A-Fa-f0-9]{6})".toRegex(), "<color:#$1>")

        return MiniMessage.miniMessage()
                .deserialize(convertedMessage)
                .decoration(TextDecoration.ITALIC, false)
    }

    fun splitIntoLines(text: String, maxLineLength: Int): List<String> {
        val words = text.split("\\s+".toRegex())
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else if (currentLine.length + word.length + 1 <= maxLineLength) {
                currentLine.append(" ").append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
    }
}
