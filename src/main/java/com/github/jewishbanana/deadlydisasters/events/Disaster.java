package com.github.jewishbanana.deadlydisasters.enums;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.jewishbanana.deadlydisasters.Utils;

public enum Disaster {
	CAVEIN("&7Cave In"),
	PLAGUE("&0Black Plague"),
	EXTREMEWINDS("&fExtreme Winds"),
	BLIZZARD("&9Blizzard"),
	SANDSTORM("&eSandstorm"),
	EARTHQUAKE("&8Earthquake"),
	PURGE("&8Purge"),
	BANDITRAID("&8Bandit Raid"),
	INFESTATION("&2Infestation"),
	DEATHPARADE("&5Death Parade"),
	CUSTOM("&fCustom");

	private String displayName;
	private String configKey;
	private int delayTicks;
	private double chance;
	private int minHeight;
	private String tip;

	private static JavaPlugin plugin;
	private static Map<String, Disaster> disasterMap = new HashMap<>();

	private static Disaster[] copyOfValues = Disaster.values();

	Disaster(String displayName) {
		this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getConfigKey() {
		return configKey;
	}

	public int getDelayTicks() {
		return delayTicks;
	}

	public double getChance() {
		return chance;
	}

	public int getMinHeight() {
		return minHeight;
	}

	public String getTip() {
		return tip;
	}

	public static void setPlugin(JavaPlugin plugin) {
		Disaster.plugin = plugin;
		Arrays.stream(Disaster.values())
				.forEach(disaster -> disasterMap.put(disaster.name(), disaster));
	}

	public static void reload() {
		for (Disaster obj : copyOfValues) {
			String configKey = obj.name().toLowerCase();

			if (!configKey.equals("custom")) {
                if (plugin.getConfig().contains(configKey + ".delay_ticks"))
                    obj.delayTicks = plugin.getConfig().getInt(configKey + ".delay_ticks");
                else
                    obj.delayTicks = plugin.getConfig().getInt("purge.delay_ticks");
                if (plugin.getConfig().contains(configKey + ".chance"))
                    obj.chance = plugin.getConfig().getDouble(configKey + ".chance");
                else
                    obj.chance = plugin.getConfig().getDouble("purge.chance");
                if (plugin.getConfig().contains(configKey + ".min_height"))
                    obj.minHeight = plugin.getConfig().getInt(configKey + ".min_height");
                else
                    obj.minHeight = plugin.getConfig().getInt("purge.min_height");
                if (Languages.langFile.contains("tips." + configKey))
                    obj.setTip(Utils.convertString("&7&o") + Languages.langFile.getString("tips." + configKey));
                else if (Languages.langFile.contains("tips.purge"))
                    obj.setTip(Utils.convertString("&7&o") + Languages.langFile.getString("tips.purge"));
			}
		}
	}

	public static Disaster forName(String name) {
		return disasterMap.get(name);
	}
}