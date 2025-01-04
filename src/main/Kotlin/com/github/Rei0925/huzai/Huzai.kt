package com.github.Rei0925.huzai

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Huzai : JavaPlugin() {

    private val configVersionKey = "config-version"

    private val RESET = "\u001B[0m"
    private val RED = "\u001B[31m"
    private val GREEN = "\u001B[32m"
    private val YELLOW = "\u001B[33m"
    private val BLUE = "\u001B[34m"
    private val CYAN = "\u001B[36m"

    override fun onEnable() {
        try {
            saveDefaultConfig()
            checkAndUpdateConfig()

            // コマンドの登録
            val huzaiCommand = getCommand("huzai")
            if (huzaiCommand != null) {
                huzaiCommand.setExecutor(DebugCommand(this))
                huzaiCommand.tabCompleter = HuzaiTabCompleter()
            } else {
                logColored(YELLOW, "コマンド 'huzai' の登録に失敗しました。")
            }

            logColored(GREEN, "Huzaiプラグインが正常に有効化されました！")
        } catch (ex: Exception) {
            logColored(RED, "Huzaiプラグインの有効化中にエラーが発生しました: ${ex.message}")
            ex.printStackTrace()
        }
    }

    override fun onDisable() {
        try {
            logColored(YELLOW, "Huzaiプラグインが正常に無効化されました。")
        } catch (ex: Exception) {
            logColored(RED, "Huzaiプラグインの無効化中にエラーが発生しました: ${ex.message}")
            ex.printStackTrace()
        }
    }

    private fun checkAndUpdateConfig() {
        val pluginVersion = description.version
        val configFile = File(dataFolder, "config.yml")

        val currentConfigVersion = config.getString(configVersionKey, "0.0")

        if (currentConfigVersion != pluginVersion) {
            logColored(CYAN, "設定ファイルのバージョン ($currentConfigVersion) がプラグインバージョン ($pluginVersion) と異なるため、新しい設定に更新します。")

            if (configFile.exists()) {
                val backupFile = File(dataFolder, "config-backup-$currentConfigVersion.yml")
                configFile.copyTo(backupFile, overwrite = true)
                logColored(BLUE, "古い設定ファイルをバックアップしました: ${backupFile.name}")
            }

            saveResource("config.yml", true)

            reloadConfig()
            config.set(configVersionKey, pluginVersion)
            saveConfig()
        }
    }

    private fun logColored(color: String, message: String) {
        logger.info("$color$message$RESET")
    }
}
