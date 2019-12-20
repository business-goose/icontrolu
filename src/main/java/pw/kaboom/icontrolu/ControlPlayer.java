package pw.kaboom.icontrolu;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import pw.kaboom.icontrolu.utilities.PlayerList;

class Tick extends BukkitRunnable {
	@Override
	public void run() {
		for (Player target: Bukkit.getOnlinePlayers()) {
			final Player controller = PlayerList.getController(target.getUniqueId());

			if (controller != null) {
				for (int i = 0; i < controller.getInventory().getSize(); i++) {
					if (controller.getInventory().getItem(i) != null) {
						if (!controller.getInventory().getItem(i).equals(target.getInventory().getItem(i))) {
							target.getInventory().setItem(i, controller.getInventory().getItem(i));
						}
					} else {
						target.getInventory().setItem(i, null);
					}
				}

				if (target.getHealth() > 0) {
					target.teleportAsync(controller.getLocation());
				}

				AttributeInstance controllerMaxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				AttributeInstance targetMaxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				targetMaxHealth.setBaseValue(controllerMaxHealth.getBaseValue());

				target.setAllowFlight(controller.getAllowFlight());
				target.setExhaustion(controller.getExhaustion());
				target.setFlying(controller.isFlying());
				target.setFoodLevel(controller.getFoodLevel());
				target.setHealth(controller.getHealth());
				target.setLevel(controller.getLevel());
				target.setSneaking(controller.isSneaking());
				target.setSprinting(controller.isSprinting());

				for (Player player: Bukkit.getOnlinePlayers()) {
					player.hidePlayer(JavaPlugin.getPlugin(Main.class), controller);
				}

				final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				Team team = scoreboard.getTeam("icuCollision");

				if (team == null) {
					team = scoreboard.registerNewTeam("icuCollision");
				}

				team.setCanSeeFriendlyInvisibles(false);
				team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

				if (!team.hasEntry(controller.getName())) {
					team.addEntry(controller.getName());
				}

				final int duration = 99999;
				final int amplifier = 0;
				final boolean ambient = false;
				final boolean particles = false;

				controller.addPotionEffect(
					new PotionEffect(
						PotionEffectType.INVISIBILITY,
						duration,
						amplifier,
						ambient,
						particles
					)
				);
			}
		}
	}
}

class ControlPlayer implements Listener {
	@EventHandler
	private void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
		final Player player = event.getPlayer();

		if (PlayerList.getController(player.getUniqueId()) != null) {
			event.setCancelled(true);
		}

		if (PlayerList.getTarget(player.getUniqueId()) != null) {
			final Player target = PlayerList.getTarget(player.getUniqueId());

			target.chat(event.getMessage());
			event.setCancelled(true);
		}
	}

	@EventHandler
	private void onEntityDamage(final EntityDamageEvent event) {
		final Entity player = event.getEntity();

		if (PlayerList.getTarget(player.getUniqueId()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	private void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
		final Player player = event.getPlayer();

		if (PlayerList.getController(player.getUniqueId()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	private void onPlayerDropItem(final PlayerDropItemEvent event) {
		final Player player = event.getPlayer();

		if (PlayerList.getController(player.getUniqueId()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	private void onPlayerInteract(final PlayerInteractEvent event) {
		final Player player = event.getPlayer();

		if (PlayerList.getController(player.getUniqueId()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	private void onPlayerMove(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();

		if (PlayerList.getController(player.getUniqueId()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	private void onPlayerQuit(final PlayerQuitEvent event) {
		final Player player = event.getPlayer();

		for (Player otherPlayer: Bukkit.getOnlinePlayers()) {
			/* Target disconnects */
			if (PlayerList.getController(player.getUniqueId()) != null
					&& PlayerList.getController(player.getUniqueId()).equals(otherPlayer)) {
				PlayerList.removeTarget(otherPlayer.getUniqueId());
				PlayerList.removeController(player.getUniqueId());

				final Player controller = otherPlayer;
				final int tickDelay = 200;

				new BukkitRunnable() {
					@Override
					public void run() {
						for (Player allPlayers: Bukkit.getOnlinePlayers()) {
							allPlayers.showPlayer(JavaPlugin.getPlugin(Main.class), controller);
						}

						final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
						final Team team = scoreboard.getTeam("icuCollision");

						if (team != null
								&& team.hasEntry(controller.getName())) {
							team.removeEntry(controller.getName());
						}

						controller.removePotionEffect(PotionEffectType.INVISIBILITY);
						controller.sendMessage("You are now visible");
					}
				}.runTaskLater(JavaPlugin.getPlugin(Main.class), tickDelay);

				otherPlayer.sendMessage("The player you were controlling has disconnected. You are invisible for 10 seconds.");
			}

			/* Controller disconnects */
			if (PlayerList.getTarget(player.getUniqueId()) != null
					&& PlayerList.getTarget(player.getUniqueId()).equals(otherPlayer)) {
				PlayerList.removeTarget(player.getUniqueId());
				PlayerList.removeController(otherPlayer.getUniqueId());
			}
		}
	}

	@EventHandler
	private void onPlayerRespawn(final PlayerRespawnEvent event) {
		final Player player = event.getPlayer();

		if (PlayerList.getController(player.getUniqueId()) != null) {
			final Player controller = PlayerList.getController(player.getUniqueId());

			controller.teleportAsync(player.getLocation());
		}
	}
}
