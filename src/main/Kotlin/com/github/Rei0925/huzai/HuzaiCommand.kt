package com.github.Rei0925.huzai

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import org.bukkit.entity.Player
import java.util.*
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.OfflinePlayer


class DebugCommand(private val plugin: JavaPlugin) : CommandExecutor {

    private val absenceFile: File = File(plugin.dataFolder, "absences.yml")
    private var absences: FileConfiguration = YamlConfiguration.loadConfiguration(absenceFile)
    private val deleteRequests = mutableMapOf<String, String>() // UUID とプレイヤー名のペアマップ


    init {
        if (!absenceFile.exists()) {
            plugin.saveResource("absences.yml", false)
        }
        plugin.saveDefaultConfig()
        loadAbsences()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {

            sender.sendMessage(getMessage("messages.invalidcommand"))
            return true
        }



        when (args[0].lowercase()) {
            "add" -> {
                if (sender.hasPermission("huzai.add")) {
                    handleAdd(sender, args)
                } else {
                    sender.sendMessage(getMessage("messages.nopermission"))
                }
            }
            "check" -> {
                if (sender.hasPermission("huzai.check")) {
                    handleCheck(sender, args)
                } else {
                    sender.sendMessage(getMessage("messages.nopermission"))
                }
            }
            "del" -> {
                if (args.size > 1 && args[1].equals("all", ignoreCase = true)) {
                    if (sender.hasPermission("huzai.del")) {
                        handleDeleteAll(sender)
                    } else {
                        sender.sendMessage(getMessage("messages.nopermission"))
                    }
                } else if (sender.hasPermission("huzai.del")) {
                    handleDelete(sender, args)
                } else {
                    sender.sendMessage(getMessage("messages.nopermission"))
                }
            }
            "list" -> {
                if (sender.hasPermission("huzai.list")) {
                    handleList(sender)
                } else {
                    sender.sendMessage(getMessage("messages.nopermission"))
                }
            }

            else -> sender.sendMessage(getMessage("messages.invalidcommand"))
        }

        return true
    }

    private fun handleDeleteAll(sender: CommandSender) {
        if (absences.getKeys(false).isEmpty()) {
            sender.sendMessage(getMessage("messages.no_absentees"))
            return
        }

        absences.getKeys(false).forEach { uuid ->
            absences.set(uuid, null)
        }
        saveAbsences()

        sender.sendMessage(getMessage("messages.all_deleted"))
    }

    private fun handleList(sender: CommandSender) {
        val absenteeKeys = absences.getKeys(false)
        if (absenteeKeys.isEmpty()) {
            sender.sendMessage(getMessage("messages.no_absentees"))
            return
        }

        sender.sendMessage(getMessage("messages.absentee_list_header"))

        absenteeKeys.forEach { key ->
            try {
                val uuid = UUID.fromString(key) // UUID形式を確認
                val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
                val playerName = offlinePlayer.name ?: "Unknown"
                val deadline = absences.getString(key) ?: "Unknown"

                sender.sendMessage(
                    getMessage("messages.absentee_entry", mapOf("player" to playerName, "deadline" to deadline))
                )
            } catch (e: IllegalArgumentException) {
                // 無効なUUIDキーをスキップ
                plugin.logger.warning("無効なキーをスキップしました: $key")
            }
        }
    }


    private fun handleAdd(sender: CommandSender, args: Array<out String>) {
        if (args.size < 5) {
            sender.sendMessage(getMessage("messages.usage_add"))
            return
        }

        val playerName = args[1]
        val player = Bukkit.getOfflinePlayer(playerName) // オフラインプレイヤーを検索
        if (player == null || (!player.hasPlayedBefore() && !player.isOnline)) {
            sender.sendMessage(getMessage("messages.player_not_found", mapOf("player" to playerName)))
            return
        }

        val uuid = player.uniqueId.toString()
        val dateString = "${args[2]}/${args[3]}/${args[4]}"

        // 設定値を取得
        val maxDays = plugin.config.getInt("max-days", 365)

        // 日付の解析と比較
        try {
            val year = args[2].toInt()
            val month = args[3].toInt()
            val day = args[4].toInt()

            val deadlineDate = LocalDate.of(year, month, day)
            val currentDate = LocalDate.now()
            val maxAllowedDate = currentDate.plusDays(maxDays.toLong())

            // 過去の日付を許可するかチェック
            if (deadlineDate.isBefore(currentDate)) {
                sender.sendMessage(getMessage("messages.past_date"))
                return
            }

            // 最大未来日数をチェック
            if (deadlineDate.isAfter(maxAllowedDate)) {
                sender.sendMessage(getMessage("messages.invalid_date_range", mapOf("max_days" to maxDays.toString())))
                return
            }
        } catch (e: Exception) {
            sender.sendMessage(getMessage("messages.invalid_date"))
            return
        }

        // 有効な日付の場合、不在情報を保存
        absences.set(uuid, dateString)
        saveAbsences()

        sender.sendMessage(getMessage("messages.added", mapOf("player" to playerName)))
        sender.sendMessage(getMessage("messages.deadline", mapOf("deadline" to dateString)))
    }



    private fun handleCheck(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player && args.size < 2) {
            sender.sendMessage(getMessage("messages.usage_check"))
            return
        }

        val targetPlayer: OfflinePlayer = if (args.size >= 2) {
            Bukkit.getOfflinePlayer(args[1])
        } else if (sender is Player) {
            sender
        } else {
            sender.sendMessage(getMessage("messages.usage_check"))
            return
        }

        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
            val playerName = targetPlayer.name ?: "Unknown"
            sender.sendMessage(getMessage("messages.player_not_found", mapOf("player" to playerName)))
            return
        }

        val uuid = targetPlayer.uniqueId.toString()
        if (!absences.contains(uuid)) {
            val playerName = targetPlayer.name ?: "Unknown"
            val messageKey = if (sender is Player && sender.uniqueId == targetPlayer.uniqueId) {
                "messages.noabsence_self"
            } else {
                "messages.noabsence"
            }
            sender.sendMessage(getMessage(messageKey, mapOf("player" to playerName)))
            return
        }

        val deadline = absences.getString(uuid) ?: "Unknown"
        val playerName = targetPlayer.name ?: "Unknown"

        if (sender is Player && sender.uniqueId == targetPlayer.uniqueId) {
            sender.sendMessage(getMessage("messages.deadline1_self", mapOf("deadline" to deadline)))
        } else {
            sender.sendMessage(getMessage("messages.deadline1", mapOf("player" to playerName, "deadline" to deadline)))
        }
    }


    private fun handleDelete(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(getMessage("messages.usage_del"))
            return
        }

        val playerName = args[1]
        val player = Bukkit.getOfflinePlayer(playerName)
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(getMessage("messages.player_not_found", mapOf("player" to playerName)))
            return
        }

        val uuid = player.uniqueId.toString()
        if (absences.contains(uuid)) {
            absences.set(uuid, null)
            saveAbsences()
            sender.sendMessage(getMessage("messages.deleted", mapOf("player" to playerName)))
        } else {
            sender.sendMessage(getMessage("messages.notfound", mapOf("player" to playerName)))
        }
    }

    private fun loadAbsences() {
        if (absenceFile.exists()) {
            absences = YamlConfiguration.loadConfiguration(absenceFile)
        }
    }

    private fun saveAbsences() {
        absences.save(absenceFile)
    }

    private fun getMessage(path: String, replacements: Map<String, String> = emptyMap()): String {
        var message = plugin.config.getString(path, "§c§lメッセージが見つかりません: $path") ?: ""
        replacements.forEach { (key, value) ->
            message = message.replace("{$key}", value)
        }
        return message
    }
}

class HuzaiPlugin : JavaPlugin() {

    private val absenceFile = File(dataFolder, "absences.yml")
    private lateinit var absences: YamlConfiguration

    override fun onEnable() {
        // ファイルの存在確認とロード
        if (!absenceFile.exists()) {
            saveResource("absences.yml", false)
        }
        absences = YamlConfiguration.loadConfiguration(absenceFile)

        // デフォルトコンフィグ保存
        saveDefaultConfig()

        // コマンド登録
        getCommand("huzai")?.setExecutor(DebugCommand(this))

        // イベントリスナー登録

    }
}
