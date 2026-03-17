package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Ghoul;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Psyco;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Scarecrow;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Vampire;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.LostSoul;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.SkeletonKnight;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.ZombieKnight;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class DeathParade extends DestructionDisaster {
	
	private boolean custom,showBar,running = true;
	private UUID playerUUID;
	private int max,spawnDistance,despawnSpeed,spawnSpeed;
	private BossBar bar;
	private Random rand;
	private String barTitle;
	private BarColor barColor;
	private String endMessage;
	private int[] mobProbabilities;
	private int sum;
	private World world;
	
	private Queue<UUID> entities = new ArrayDeque<>();
	
	public static Set<UUID> targetedPlayers = new HashSet<>();

	public DeathParade(int level, World world) {
		super(level, world);
		this.rand = plugin.random;
		custom = CustomEntityType.mobsEnabled;
		max = configFile.getInt("purge.horde_size.level " + this.level);
		showBar = true;
		barTitle = "&5&lDeath Parade";
		barColor = BarColor.PURPLE;
		spawnDistance = 25;
		despawnSpeed = 40;
		endMessage = Utils.convertString("&aThe parade has moved on. You survived!");
		volume = configFile.getDouble("purge.volume");
		spawnSpeed = 120 - (8 * (level - 1));
		
		mobProbabilities = new int[] {
			10, //zombie
			11, //skeleton
			3, //skeleton knight
			2, //lost soul
			3, //zombie knight
			5, //ghoul
			2, //psyco
			2, //scarecrow
			2 //vampire
		};
		for (int i = 0; i < mobProbabilities.length; i++)
			sum += mobProbabilities[i];
		
		this.type = Disaster.PURGE;
	}
	@Override
	public void start(Location loc, Player p) {
		this.world = p.getWorld();
		this.playerUUID = p.getUniqueId();
		if (!targetedPlayers.contains(playerUUID))
			targetedPlayers.add(playerUUID);
		if (showBar) {
			bar = Bukkit.createBossBar(Utils.convertString(barTitle), barColor, BarStyle.SOLID, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);
			bar.addPlayer(p);
		}
		p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, (float) (100 * volume), 0.1f);
		double decrement = 1.0 / max;
		FixedMetadataValue fixdata = new FixedMetadataValue(plugin, "protected");
		new RepeatingTask(plugin, 0, spawnSpeed) {
			public void run() {
				Player player = Bukkit.getPlayer(playerUUID);
				if (player == null)
					return;
				if (Utils.isPlayerImmune(player))
					max = 0;
				Iterator<UUID> it = entities.iterator();
				while (it.hasNext()) {
					LivingEntity e = (LivingEntity) Bukkit.getEntity(it.next());
					if (e == null)
						it.remove();
					else if (e.isDead()) {
						max--;
						if (showBar && bar != null) bar.setProgress(Math.max(bar.getProgress()-decrement, 0));
						it.remove();
					}
				}
				if (max <= 0 || !targetedPlayers.contains(playerUUID) || !world.equals(player.getWorld())) {
					if (showBar && bar != null) bar.removeAll();
					targetedPlayers.remove(playerUUID);
					player.sendMessage(Utils.convertString(endMessage));
					for (Entity e : player.getNearbyEntities(30, 30, 30))
						if (e instanceof Player)
							e.sendMessage(endMessage);
					new RepeatingTask(plugin, 0, despawnSpeed) {
						public void run() {
							UUID uuid = entities.poll();
							if (uuid == null)
								return;
							LivingEntity temp = (LivingEntity) Bukkit.getEntity(uuid);
							if (temp != null)
								temp.remove();
							if (entities.isEmpty())
								cancel();
						}
					};
					for (UUID e : entities) {
						Entity entity = Bukkit.getEntity(e);
						if (entity != null)
							entity.removeMetadata("dd-purgemob", plugin);
					}
					cancel();
					running = false;
					return;
				}
				if (showBar && bar != null) {
					bar.removeAll();
					bar.addPlayer(player);
					for (Entity e : player.getNearbyEntities(30, 30, 30))
						if (e instanceof Player)
							bar.addPlayer((Player) e);
				}
				
				Location temp = Utils.findSmartYSpawn(player.getLocation(), Utils.getSpotInSquareRadius(player.getLocation(), spawnDistance), 2, 25);
				if (temp == null || temp.getBlock().getRelative(BlockFace.DOWN).isPassable())
					return;
				
				int r = rand.nextInt(sum);
				int cumalitive = 0;
				for (int i=0; i < mobProbabilities.length; i++) {
					cumalitive += mobProbabilities[i];
					if (r <= cumalitive) {
						r = i;
						break;
					}
				}
				Mob entity = null;
				switch (r) {
				default:
				case 0:
					entity = (Mob) temp.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
					entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);
					entity.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
					break;
				case 1:
					entity = (Mob) temp.getWorld().spawnEntity(temp, EntityType.SKELETON);
					entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);
					entity.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
					break;
				case 2:
					if (!custom)
						break;
					entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.SKELETON);
					CustomEntity.handler.addEntity(new SkeletonKnight((Skeleton) entity, plugin));
					break;
				case 3:
					if (!custom)
						break;
					entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
					CustomEntity.handler.addEntity(new LostSoul(entity, plugin, rand));
					break;
				case 4:
					if (!custom)
						break;
					entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
					CustomEntity.handler.addEntity(new ZombieKnight(entity, plugin));
					break;
				case 5:
					if (!custom)
						break;
					entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
					CustomEntity.handler.addEntity(new Ghoul((Zombie) entity, temp.getBlock().getRelative(BlockFace.DOWN), plugin, true));
					break;
				case 6:
					if (!custom)
						break;
					entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.SKELETON);
					CustomEntity.handler.addEntity(new Psyco(entity, plugin, rand));
					break;
				case 7:
					if (!custom)
						break;
					entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
					CustomEntity.handler.addEntity(new Scarecrow((Zombie) entity, plugin, rand));
					break;
				case 8:
					if (!custom)
						break;
					entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.EVOKER);
					CustomEntity.handler.addEntity(new Vampire(entity, plugin));
					break;
				}
				if (entity == null)
					return;
				entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(50);
				entity.setTarget(player);
				entity.setMetadata("dd-purgemob", fixdata);
				entities.add(entity.getUniqueId());
			}
		};
		new RepeatingTask(plugin, 0, 5) {
			public void run() {
				if (!running) {
					cancel();
					return;
				}
				Player player = Bukkit.getPlayer(playerUUID);
				if (player == null)
					return;
				for (UUID uuid : entities) {
					Mob entity = (Mob) Bukkit.getEntity(uuid);
					if (entity == null || entity.isDead() || entity.getTarget() != null)
						continue;
					entity.setTarget(player);
				}
			}
		};
	}
	public void clearEntities() {
		for (UUID e : entities)
			if (Bukkit.getEntity(e) != null)
				Bukkit.getEntity(e).remove();
	}
	public Location findApplicableLocation(Location temp, Player p) {
		if (temp.getBlockY() < type.getMinHeight())
			return null;
		if ((boolean) WorldObject.findWorldObject(temp.getWorld()).settings.get("event_broadcast"))
			broadcastMessage(p.getLocation(), p);
		return temp;
	}
	public void startAdjustment(Location loc, Player p) {
		start(loc, p);
	}
	public void clearBar() {
		if (bar != null) bar.removeAll();
	}
	public void broadcastMessage(Location location, Player p) {
		if ((boolean) WorldObject.findWorldObject(location.getWorld()).settings.get("event_broadcast")) {
			String str = plugin.getConfig().getString("messages.misc.purge.started");
			if (level == 1)
				str = "&a"+str;
			else if (level == 2)
				str = "&2"+str;
			else if (level == 3)
				str = "&b"+str;
			else if (level == 4)
				str = "&e"+str;
			else if (level == 5)
				str = "&c"+str;
			else if (level == 6)
				str = "&4"+str;
			str = Utils.convertString(str.replace("%level%", level+"").replace("%player%", p.getName()));
			if (plugin.getConfig().getBoolean("messages.disaster_tips"))
				str += "\n"+type.getTip();
			for (Player all : location.getWorld().getPlayers())
				all.sendMessage(str);
			Main.consoleSender.sendMessage(Languages.prefix+str+ChatColor.GREEN+" ("+location.getWorld().getName()+")");
		}
	}
	public boolean isShowBar() {
		return showBar;
	}
	public void setShowBar(boolean showBar) {
		this.showBar = showBar;
	}
	public int getMax() {
		return max;
	}
	public void setMax(int max) {
		this.max = max;
	}
	public int getSpawnDistance() {
		return spawnDistance;
	}
	public void setSpawnDistance(int spawnDistance) {
		this.spawnDistance = spawnDistance;
	}
	public int getDespawnSpeed() {
		return despawnSpeed;
	}
	public void setDespawnSpeed(int despawnSpeed) {
		this.despawnSpeed = despawnSpeed;
	}
	public String getBarTitle() {
		return barTitle;
	}
	public void setBarTitle(String barTitle) {
		this.barTitle = barTitle;
	}
	public BarColor getBarColor() {
		return barColor;
	}
	public void setBarColor(BarColor barColor) {
		this.barColor = barColor;
	}
	public String getEndMessage() {
		return endMessage;
	}
	public void setEndMessage(String endMessage) {
		this.endMessage = endMessage;
	}
	public int getSpawnSpeed() {
		return spawnSpeed;
	}
	public void setSpawnSpeed(int spawnSpeed) {
		this.spawnSpeed = spawnSpeed;
	}
}