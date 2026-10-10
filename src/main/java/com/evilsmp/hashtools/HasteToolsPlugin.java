package com.evilsmp.hashtools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class HasteToolsPlugin extends JavaPlugin {
    private NamespacedKey toolKey;
    private final Set<UUID> playersGivenEffect = new HashSet<>();
    private PotionEffectType hasteType;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        toolKey = new NamespacedKey(this, "haste_tool");
        hasteType = PotionEffectType.HASTE;
        long interval = Math.max(1L, getConfig().getLong("refresh-interval-ticks", 10L));
        Bukkit.getScheduler().runTaskTimer(this, this::updatePlayers, 1L, interval);
        getLogger().info("HasteTools enabled. Haste V is applied while a tagged tool is held in the main hand.");
    }

    @Override
    public void onDisable() {
        for (UUID id : new HashSet<>(playersGivenEffect)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) removeOurHaste(player);
        }
        playersGivenEffect.clear();
    }

    private void updatePlayers() {
        int amplifier = Math.max(0, getConfig().getInt("effect-amplifier", 4));
        int duration = Math.max(10, getConfig().getInt("effect-duration-ticks", 30));
        boolean particles = !getConfig().getBoolean("hide-particles", true);
        boolean icon = getConfig().getBoolean("hide-icon", false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isHasteTool(player.getInventory().getItemInMainHand())) {
                PotionEffect current = player.getPotionEffect(hasteType);
                // Do not overwrite a stronger Haste effect supplied by another source.
                if (current == null || current.getAmplifier() < amplifier) {
                    player.addPotionEffect(new PotionEffect(hasteType, duration, amplifier, true, particles, icon), true);
                    playersGivenEffect.add(player.getUniqueId());
                } else if (current.getAmplifier() == amplifier && playersGivenEffect.contains(player.getUniqueId())
                        && current.getDuration() <= duration / 2) {
                    player.addPotionEffect(new PotionEffect(hasteType, duration, amplifier, true, particles, icon), true);
                }
            } else if (playersGivenEffect.contains(player.getUniqueId())) {
                removeOurHaste(player);
            }
        }
    }

    private void removeOurHaste(Player player) {
        PotionEffect current = player.getPotionEffect(hasteType);
        int configuredAmplifier = Math.max(0, getConfig().getInt("effect-amplifier", 4));
        if (current != null && current.getAmplifier() == configuredAmplifier) {
            player.removePotionEffect(hasteType);
        }
        playersGivenEffect.remove(player.getUniqueId());
    }

    private boolean isHasteTool(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        String marker = item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        return "axe".equals(marker) || "pickaxe".equals(marker);
    }

    private ItemStack createTool(String tool) {
        String path = "items." + tool + ".";
        Material fallback = tool.equals("axe") ? Material.DIAMOND_AXE : Material.DIAMOND_PICKAXE;
        Material material = Material.matchMaterial(getConfig().getString(path + "material", fallback.name()));
        if (material == null || material.isAir() || !material.name().endsWith(tool.equals("axe") ? "_AXE" : "_PICKAXE")) {
            getLogger().warning("Invalid material in config for " + tool + "; using " + fallback + ".");
            material = fallback;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(getConfig().getString(path + "name", "&6Haste V " + tool)));
            List<String> lore = getConfig().getStringList(path + "lore");
            if (!lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) colored.add(color(line));
                meta.setLore(colored);
            }
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, tool);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    private String message(String key) {
        return color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, ""));
    }

    private void send(CommandSender sender, String key) {
        sender.sendMessage(message(key));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hashtools.admin")) {
            send(sender, "no-permission");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            send(sender, "reloaded");
            return true;
        }
        if (args.length < 1 || !(args[0].equalsIgnoreCase("axe") || args[0].equalsIgnoreCase("pickaxe"))) {
            send(sender, "usage");
            return true;
        }

        String tool = args[0].toLowerCase(Locale.ROOT);
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color("&8[&6HasteTools&8] &cThat player is not online.") );
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            send(sender, "player-only");
            return true;
        }

        target.getInventory().addItem(createTool(tool)).values().forEach(item ->
                target.getWorld().dropItemNaturally(target.getLocation(), item));
        String pretty = tool.equals("axe") ? "Axe" : "Pickaxe";
        sender.sendMessage(color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages.given", "")
                .replace("%tool%", pretty).replace("%player%", target.getName())));
        target.sendMessage(color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages.received", "")
                .replace("%tool%", pretty)));
        return true;
    }
}
