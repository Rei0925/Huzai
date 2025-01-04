package com.github.Rei0925.huzai

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class HuzaiTabCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        return when (args.size) {
            1 -> listOf("add", "check", "del", "list").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].equals("add", ignoreCase = true) ||
                args[0].equals("check", ignoreCase = true) ||
                args[0].equals("del", ignoreCase = true)
            ) {
                // サジェストするプレイヤー名リスト
                getRegisteredPlayers().filter { it.startsWith(args[1], ignoreCase = true) }
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }

    private fun getRegisteredPlayers(): List<String> {
        return org.bukkit.Bukkit.getOnlinePlayers().map { it.name }
    }
}
