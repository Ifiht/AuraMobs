package dev.aurelium.auramobs.listeners;

import dev.aurelium.auramobs.AuraMobs;
import dev.aurelium.auramobs.util.ColorUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public class MobDamage implements Listener {

    private final AuraMobs plugin;

    public MobDamage(AuraMobs plugin){
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMobDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (!plugin.isAuraMob(entity)) {
            return;
        }

        int level = entity.getPersistentDataContainer().getOrDefault(plugin.getMobKey(), PersistentDataType.INTEGER, 1);
        double resHealth = entity.getHealth() - e.getFinalDamage();
        resHealth = Math.max(resHealth, 0.0);
        String formattedHealth = plugin.getFormatter().format(resHealth);
        try {
            entity.setCustomName(ColorUtils.colorMessage(plugin.optionString("custom_name.format")
                    .replace("{mob}", plugin.getMsg("mobs." + entity.getType().name().toLowerCase(Locale.ROOT)))
                    .replace("{lvl}", Integer.toString(level))
                    .replace("{health}", formattedHealth)
            ));
        } catch (NullPointerException ex){
            entity.setCustomName(ColorUtils.colorMessage(plugin.optionString("custom_name.format")
                    .replace("{mob}", entity.getType().name())
                    .replace("{lvl}", Integer.toString(level))
                    .replace("{health}", formattedHealth)
            ));
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onArrowHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Projectile p)) {
            return;
        }

        if (!(p.getShooter() instanceof LivingEntity entity)) {
            return;
        }

        if (!plugin.isAuraMob(entity)) {
            return;
        }

        e.setDamage(entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue());
    }

}
