package org.sypherphoenix.playerLeash // FIX: Make sure this matches your file path

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*

class PlayerLeashPlugin : JavaPlugin(), Listener {

    private val mm = MiniMessage.miniMessage()

    private var leashDistanceMax: Double = 12.0
    private var leashDistanceTug: Double = 8.0
    private var tugStrength: Double = 0.4
    private var breakOnDeath: Boolean = true

    private val leashedPlayers = mutableMapOf<UUID, UUID>()
    private var leashUpdateTask: BukkitTask? = null

    override fun onEnable() {
        saveDefaultConfig()
        loadConfigValues() // FIX: Call the function to load the config

        server.pluginManager.registerEvents(this, this)
        leashUpdateTask = startLeashTask()

        logger.info("PlayerLeashPlugin has been enabled.")
    }

    override fun onDisable() {
        leashUpdateTask?.cancel()
        leashedPlayers.keys.toSet().forEach { leashedId ->
            val holderId = leashedPlayers[leashedId]
            unleashPlayers(holderId?.let { Bukkit.getPlayer(it) }, Bukkit.getPlayer(leashedId), silent = true)
        }
        leashedPlayers.clear()
        logger.info("PlayerLeashPlugin has been disabled.")
    }

    private fun loadConfigValues() {
        config.options().copyDefaults(true)
        saveConfig()
        leashDistanceMax = config.getDouble("leash.distance.max", 12.0)
        leashDistanceTug = config.getDouble("leash.distance.tug", 8.0)
        tugStrength = config.getDouble("leash.strength", 0.4)
        breakOnDeath = config.getBoolean("leash.break-on-death", true)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return

        val holder = event.player
        val leashed = event.rightClicked as? Player ?: return

        if (!holder.hasPermission("playerleash.use")) {
            holder.sendMessage(Component.text("You do not have permission to use player leashes.", NamedTextColor.RED))
            return
        }

        if (holder.inventory.itemInMainHand.type != Material.LEAD) return
        event.isCancelled = true

        if (holder.uniqueId == leashed.uniqueId) {
            holder.sendMessage(mm.deserialize("<red>You cannot leash yourself.</red>"))
            return
        }

        if (leashedPlayers.containsKey(leashed.uniqueId) || leashedPlayers.containsValue(holder.uniqueId)) {
            if (leashedPlayers[leashed.uniqueId] == holder.uniqueId) {
                unleashPlayers(holder, leashed)
            } else {
                holder.sendMessage(mm.deserialize("<red>One of you is already part of a leash.</red>"))
            }
            return
        }

        leashedPlayers[leashed.uniqueId] = holder.uniqueId
        holder.sendMessage(mm.deserialize("<green>You have leashed <white>${leashed.name}</white>.</green>"))
        leashed.sendMessage(mm.deserialize("<green>You have been leashed by <white>${holder.name}</white>.</green>"))
    }

    // FIX: Correctly implement onPlayerQuit with the event parameter
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (leashedPlayers.containsValue(player.uniqueId)) { // Player was a holder
            val leashedId = leashedPlayers.entries.find { it.value == player.uniqueId }?.key
            leashedId?.let {
                val leashed = Bukkit.getPlayer(it)
                leashed?.sendMessage(mm.deserialize("<gold>Your leash holder has disconnected. You are now free.</gold>"))
                unleashPlayers(player, leashed)
            }
        } else if (leashedPlayers.containsKey(player.uniqueId)) { // Player was leashed
            val holderId = leashedPlayers[player.uniqueId]
            val holder = holderId?.let { Bukkit.getPlayer(it) }
            holder?.sendMessage(mm.deserialize("<gold>The player you had leashed has disconnected. The leash is broken.</gold>"))
            unleashPlayers(holder, player)
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!breakOnDeath) return
        val player = event.entity
        // This logic is very similar to onPlayerQuit
        if (leashedPlayers.containsValue(player.uniqueId)) {
            val leashedId = leashedPlayers.entries.find { it.value == player.uniqueId }?.key
            leashedId?.let {
                unleashPlayers(player, Bukkit.getPlayer(it))
            }
        } else if (leashedPlayers.containsKey(player.uniqueId)) {
            val holderId = leashedPlayers[player.uniqueId]
            unleashPlayers(holderId?.let { Bukkit.getPlayer(it) }, player)
        }
    }

    private fun unleashPlayers(holder: Player?, leashed: Player?, silent: Boolean = false) {
        val leashedId = leashed?.uniqueId ?: return
        if (leashedPlayers.remove(leashedId) == null) return

        if (!silent) {
            holder?.sendMessage(mm.deserialize("<gold>You have unleashed ${leashed.name}.</gold>"))
            leashed.sendMessage(mm.deserialize("<gold>You have been unleashed by ${holder?.name ?: "an offline player"}.</gold>"))
        }
    }

    private fun startLeashTask(): BukkitTask {
        return server.scheduler.runTaskTimer(this, Runnable {
            val toRemove = mutableListOf<UUID>() // Define before the loop

            // FIX: The entire logic block is now INSIDE the for-loop, solving the scope issues.
            for ((leashedId, holderId) in leashedPlayers) {
                val holder = Bukkit.getPlayer(holderId)
                val leashed = Bukkit.getPlayer(leashedId)

                if (holder == null || leashed == null || !holder.isOnline || !leashed.isOnline || holder.world != leashed.world) {
                    toRemove.add(leashedId)
                    continue // Skip to the next pair
                }

                val distance = holder.location.distance(leashed.location)

                when {
                    distance > leashDistanceMax -> {
                        holder.sendMessage(mm.deserialize("<red>The leash broke! You were too far from ${leashed.name}.</red>"))
                        leashed.sendMessage(mm.deserialize("<red>The leash broke! You were too far from ${holder.name}.</red>"))
                        toRemove.add(leashedId)
                    }
                    distance > leashDistanceTug -> {
                        val toHolder = holder.location.toVector().subtract(leashed.location.toVector()).normalize()
                        val toLeashed = leashed.location.toVector().subtract(holder.location.toVector()).normalize()
                        leashed.velocity = leashed.velocity.add(toHolder.multiply(tugStrength))
                        holder.velocity = holder.velocity.add(toLeashed.multiply(tugStrength))
                    }
                }
            }

            toRemove.forEach { leashedId ->
                unleashPlayers(leashedPlayers[leashedId]?.let { Bukkit.getPlayer(it) }, Bukkit.getPlayer(leashedId), silent = true)
            }
        }, 0L, 1L)
    }
}