package dev.aurelium.auramobs.listeners;

import dev.aurelium.auramobs.AuraMobs;
import dev.aurelium.auramobs.api.WorldGuardHook;
import dev.aurelium.auramobs.entities.AureliumMob;
import dev.aurelium.auramobs.util.MessageUtils;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MobSpawn implements Listener {

    private final AuraMobs plugin;
    private final Random random = new Random();

    public MobSpawn(AuraMobs plugin){
        this.plugin = plugin;
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSpawn(CreatureSpawnEvent e) {
        try {
            boolean valid = false;
            boolean spawner = false;
            for (String s : plugin.optionList("spawn_reasons")) {
                if (e.getSpawnReason().name().equalsIgnoreCase(s)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) return;
            if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
                spawner = true;
            }

            if (plugin.isInvalidEntity(e.getEntity())) {
                return;
            }

            LivingEntity entity = e.getEntity();

            if (!plugin.optionBoolean("bosses.enabled") && plugin.isBossMob(entity)) {
                return;
            }

            if (!passWorld(e.getEntity().getWorld())) return;

            if (plugin.getWorldGuard() != null) {
                if (!(plugin.getWorldGuard().mobsEnabled(e.getLocation()))) {
                    return;
                }
            }

            List<String> mobs = plugin.optionList("mob_replacements.list");
            String type = plugin.optionString("mob_replacements.type");

            if (type.equalsIgnoreCase("blacklist") && (mobs.contains(e.getEntity().getType().name()) || mobs.contains("*"))) {
                return;
            } else if (type.equalsIgnoreCase("whitelist") && (!mobs.contains(e.getEntity().getType().name().toUpperCase(Locale.ROOT)) && !mobs.contains("*"))) {
                return;
            }

            if (!plugin.optionBoolean("custom_name.allow_override")) {
                if (e.getEntity().getCustomName() != null) {
                    return;
                }
            }

            changeMob(entity, spawner).runTask(plugin);
        } catch (NullPointerException ex) {
            plugin.getLogger().severe(ex.getMessage());
        }
    }


    public boolean passWorld(World world) {
        if (plugin.isWorldWhitelist()) {
            if (plugin.getEnabledWorlds().contains("*")) return true;
            for (String enabledworld : plugin.getEnabledWorlds()) {
                if (world.getName().equalsIgnoreCase(enabledworld) || world.getName().startsWith(enabledworld.replace("*", ""))) return true;
            }
            return false;
        } else {
            if (plugin.getEnabledWorlds().contains("*")) return false;
            for (String enabledworld : plugin.getEnabledWorlds()) {
                if (world.getName().equalsIgnoreCase(enabledworld) || world.getName().startsWith(enabledworld.replace("*", ""))) return false;
            }
            return true;
        }
    }

    public BukkitRunnable changeMob(LivingEntity entity, boolean spawner) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid()) {
                    return;
                }
                int sumlevel = 0;
                int maxlevel = Integer.MIN_VALUE;
                int minlevel = Integer.MAX_VALUE;
                Location mobloc = entity.getLocation();
                Location spawnpoint = entity.getWorld().getSpawnLocation();
                double distance = mobloc.distance(spawnpoint);
                int level;

                int overrideLevel = getMetadataLevel(entity);
                if (overrideLevel != 0) {
                    level = overrideLevel;
                } else if (spawner) {
                    level = 1;
                } else {
                    level = getCalculatedLevel(entity, distance, maxlevel, minlevel, sumlevel);
                }
                new AureliumMob(entity, correctLevel(entity.getLocation(), level), plugin);
            }
        };
    }

    private int getCalculatedLevel(LivingEntity entity, double distance, int maxlevel, int minlevel, int sumlevel) {
        int level;
        String pformula = "{sumlevel_global} / 1.5 + {distance} * 0.004 + {random_int}";
        String lformula;
        String prefix = plugin.isBossMob(entity) ? "bosses.level." : "mob_level.";
        int globalOnline = plugin.getServer().getOnlinePlayers().size();
        lformula = MessageUtils.setPlaceholders(null, pformula
              .replace("{distance}", Double.toString(distance))
              .replace("{sumlevel_global}", Integer.toString(plugin.getGlobalLevel()))
              .replace("{random_int}", String.valueOf(random.nextInt(4)))
        );
        level = (int) new ExpressionBuilder(lformula).build().evaluate();
        level = Math.min(level, plugin.optionInt(prefix + "max_level"));
        return level;
    }

    private int getMetadataLevel(Entity entity) {
        int overrideLevel = 0;
        List<MetadataValue> meta = entity.getMetadata("auraskills_level");
        if (!meta.isEmpty()) {
            for (MetadataValue val : meta) {
                Plugin owning = val.getOwningPlugin();
                if (owning == null) continue;

                if (owning.getName().equals("AuraSkills")) {
                    overrideLevel = val.asInt();
                    break;
                }
            }
        }
        return overrideLevel;
    }

    public int correctLevel(Location loc, int level) {
        WorldGuardHook wg = plugin.getWorldGuard();
        if (wg == null) {
            return level;
        }

        if (level < wg.getMinLevel(loc)) {
            return wg.getMinLevel(loc);
        } else return Math.min(level, wg.getMaxLevel(loc));
    }

}
