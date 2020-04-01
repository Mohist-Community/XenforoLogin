package red.mohist.xenforologin.bukkit.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import red.mohist.xenforologin.bukkit.BukkitLoader;
import red.mohist.xenforologin.bukkit.interfaces.BukkitAPIListener;
import red.mohist.xenforologin.core.XenforoLoginCore;

public class ListenerEntityDamageByEntityEvent implements BukkitAPIListener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void OnEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            if (XenforoLoginCore.instance.needCancelled(BukkitLoader.instance.player2info((Player) event.getEntity()))) {
                event.setCancelled(true);
            }
        }
        if (event.getDamager().getType() == EntityType.PLAYER) {
            if (XenforoLoginCore.instance.needCancelled(BukkitLoader.instance.player2info((Player) event.getDamager()))) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void eventClass() {
        EntityDamageByEntityEvent.class.getName();
    }
}