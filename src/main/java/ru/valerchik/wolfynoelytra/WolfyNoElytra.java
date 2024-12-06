package ru.valerchik.wolfynoelytra;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WolfyNoElytra extends JavaPlugin implements Listener {

    public static final StateFlag ELYTRA_FLAG = new StateFlag("elytra", true);
    private boolean membersRegionAllow;

    @Override
    public void onLoad() {
        try {
            WorldGuard.getInstance().getFlagRegistry().register(ELYTRA_FLAG);
        } catch (Exception e) {
            getLogger().severe("Не удалось зарегистрировать флаг elytra!");
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("WolfyNoElytra успешно включен!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        membersRegionAllow = config.getBoolean("members-region-allow", true);
    }

    @EventHandler
    public void onPlayerToggleFlight(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (player.hasPermission("WolfyNoElytra.bypass")) {
            return;
        }

        if (event.isGliding()) {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            StateFlag.State elytraState = regions.queryState(WorldGuardPlugin.inst().wrapPlayer(player), ELYTRA_FLAG);

            if (elytraState == StateFlag.State.DENY && !isPlayerInOwnRegion(player, regions)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("elytra-deny-message", "&cПолет на элитрах запрещен в этом регионе!")));
            }
        }
    }

    private boolean isPlayerInOwnRegion(Player player, ApplicableRegionSet regions) {
        if (!membersRegionAllow) return false;

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        return regions.getRegions().stream().anyMatch(region ->
                region.isOwner(localPlayer) || region.isMember(localPlayer)
        );
    }
}
