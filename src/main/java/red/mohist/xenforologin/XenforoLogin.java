package red.mohist.xenforologin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import red.mohist.xenforologin.enums.ResultType;
import red.mohist.xenforologin.enums.StatusType;
import red.mohist.xenforologin.forums.ForumSystems;
import red.mohist.xenforologin.interfaces.BukkitAPIListener;
import red.mohist.xenforologin.listeners.protocollib.ListenerProtocolEvent;
import red.mohist.xenforologin.utils.ResultTypeUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.bukkit.Bukkit.*;

public final class XenforoLogin extends JavaPlugin implements Listener {

    public static XenforoLogin instance;
    public ConcurrentMap<String, StatusType> logged_in;
    public FileConfiguration config;
    public FileConfiguration location_data;
    public File location_file;
    public Location default_location;
    private ListenerProtocolEvent listenerProtocolEvent;

    @Override
    public void onEnable() {
        getLogger().info("Hello, XenforoLogin!");
        instance = this;
        logged_in = new ConcurrentHashMap<>();
        saveDefaultConfig();
        loadConfig();

        ForumSystems.reloadConfig();

        hookProtocolLib();

        registerListeners();
    }

    private void registerListeners() {
        {
            int unavailableCount = 0;
            Set<Class<? extends BukkitAPIListener>> classes = new Reflections("red.mohist.xenforologin.listeners")
                    .getSubTypesOf(BukkitAPIListener.class);
            for (Class<? extends BukkitAPIListener> clazz : classes) {
                BukkitAPIListener listener;
                try {
                    listener = clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    getLogger().warning(clazz.getName() + " is not available.");
                    unavailableCount++;
                    continue;
                }
                if (!listener.isAvailable()) {
                    getLogger().warning(clazz.getName() + " is not available.");
                    unavailableCount++;
                    continue;
                }
                Bukkit.getPluginManager().registerEvents(listener, this);
            }
            if (unavailableCount > 0) {
                getLogger().warning("Warning: Some features in this plugin is not available on this version of bukkit");
                getLogger().warning("If your encountered errors, do NOT report to XenforoLogin.");
                getLogger().warning("Error count: " + unavailableCount);
            }
        }
        getPluginManager().registerEvents(this, this);
    }

    private void hookProtocolLib() {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null && config.getBoolean("secure.hide_inventory", true)) {
            listenerProtocolEvent = new ListenerProtocolEvent();
            getLogger().info("Found ProtocolLib, hooked into ProtocolLib to use \"hide_inventory\"");
        }
    }

    private void loadConfig() {
        config = getConfig();
        location_file = new File(getDataFolder(), "player_location.yml");
        if (!location_file.exists()) {
            try {
                if (!location_file.createNewFile()) {
                    throw new IOException("File can't be created.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        location_data = YamlConfiguration.loadConfiguration(location_file);
        Location spawn_location = Objects.requireNonNull(getWorld("world")).getSpawnLocation();
        default_location = new Location(
                getWorld(Objects.requireNonNull(config.getString("spawn.world", "world"))),
                config.getDouble("spawn.x", spawn_location.getX()),
                config.getDouble("spawn.y", spawn_location.getY()),
                config.getDouble("spawn.z", spawn_location.getZ())
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void Onjoin(AsyncPlayerPreLoginEvent event){
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL,langFile("errors.server"));
        if(logged_in.containsKey(event.getName())){
            return;
        }
        ResultType resultType = ForumSystems.getCurrentSystem()
                .join(event.getName())
                .shouldLogin(false);
        switch (resultType) {
            case OK:
                logged_in.put(event.getName(), StatusType.NEED_LOGIN);
                event.allow();
                break;
            case ERROR_NAME:
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        langFile("errors.name_incorrect",
                                resultType.getInheritedObject()));
                break;
            case NO_USER:
                if(config.getBoolean("api.register",false)){
                    event.allow();
                    logged_in.put(event.getName(), StatusType.NEED_REGISTER_EMAIL);
                }else{
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            langFile("errors.no_user"));
                }
                break;
            case UNKNOWN:
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        langFile("errors.unknown", resultType.getInheritedObject()));
                break;
            case SERVER_ERROR:
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        langFile("errors.server"));
                break;
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void OnJoin(PlayerJoinEvent event) {
        sendBlankInventoryPacket(event.getPlayer());
        if(!logged_in.containsKey(event.getPlayer().getName())){
            getLogger().warning("AsyncPlayerPreLoginEvent don't effect.It may cause some secure problem!");
            getLogger().warning("It's not a bug.DON'T report this.");
        }
        if (config.getBoolean("tp.tp_spawn_before_login", true)) {
            try {
                event.getPlayer().teleportAsync(default_location);
            } catch (NoSuchMethodError e) {
                XenforoLogin.instance.getLogger().warning("Cannot find method " + e.getMessage());
                XenforoLogin.instance.getLogger().warning("Using synchronized teleport");
                getLogger().warning("It's not a bug.DON'T report this.");
                Bukkit.getScheduler().runTask(XenforoLogin.instance, () ->
                        event.getPlayer().teleport(default_location));
            }
        }
        new Thread(() -> {
            if(logged_in.get(event.getPlayer().getName())!=StatusType.NEED_CHECK){
                boolean result = ResultTypeUtils.handle(event.getPlayer(),
                        ForumSystems.getCurrentSystem()
                                .join(event.getPlayer())
                                .shouldLogin(false));
                if (!result) {
                    XenforoLogin.instance.getLogger().warning(
                            event.getPlayer().getName() + " didn't pass AccountExists test");
                    return;
                }
                message(event.getPlayer());
            }
            sendBlankInventoryPacket(event.getPlayer());
            int f = 0;
            int s = config.getInt("secure.show_tips_time", 5);
            int t = config.getInt("secure.max_login_time", 30);
            while (true) {
                sendBlankInventoryPacket(event.getPlayer());
                switch (logged_in.get(event.getPlayer().getName())){
                    case NEED_LOGIN:
                        event.getPlayer().sendMessage(langFile("need_login"));
                        break;
                    case NEED_REGISTER_EMAIL:
                        event.getPlayer().sendMessage(langFile("register_email"));
                        break;
                    case NEED_REGISTER_PASSWORD:
                        event.getPlayer().sendMessage(langFile("register_password"));
                        break;
                    case NEED_REGISTER_CONFIRM:
                        event.getPlayer().sendMessage(langFile("register_password_confirm"));
                        break;
                }
                try {
                    Thread.sleep(s * 1000);
                    f += s;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (f > t && logged_in.get(event.getPlayer().getName())==StatusType.NEED_LOGIN) {
                    break;
                }
                if (!event.getPlayer().isOnline() || !needCancelled(event.getPlayer())) {
                    return;
                }
            }
            Bukkit.getScheduler().runTask(XenforoLogin.instance, () -> event.getPlayer()
                    .kickPlayer(langFile("errors.time_out")));

        }).start();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void OnQuit(PlayerQuitEvent event) throws IOException {
        Location leave_location = event.getPlayer().getLocation();
        if (!needCancelled(event.getPlayer())) {
            location_data.set(event.getPlayer().getName() + ".world", leave_location.getWorld().getUID().toString());
            location_data.set(event.getPlayer().getName() + ".x", leave_location.getX());
            location_data.set(event.getPlayer().getName() + ".y", leave_location.getY());
            location_data.set(event.getPlayer().getName() + ".z", leave_location.getZ());
            location_data.set(event.getPlayer().getName() + ".yaw", leave_location.getYaw());
            location_data.set(event.getPlayer().getName() + ".pitch", leave_location.getPitch());
            location_data.save(location_file);
        }
        logged_in.remove(event.getPlayer().getName());
    }

    public boolean needCancelled(Player player) {
        return !logged_in.getOrDefault(player.getName(), StatusType.NEED_LOGIN).equals(StatusType.LOGINED);
    }

    public String langFile(String key) {
        String result = config.getString("lang." + key);
        if (result == null) {
            return key;
        }
        return result;
    }

    public String langFile(String key, Map<String, String> data) {
        String result = config.getString("lang." + key);
        if (result == null) {
            StringBuilder resultBuilder = new StringBuilder(key);
            resultBuilder.append("\n");
            for (Map.Entry<String, String> entry : data.entrySet()) {
                resultBuilder.append(entry.getKey()).append(":").append(entry.getValue());
            }
            result = resultBuilder.toString();
            return result;
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result = result.replace("[" + entry.getKey() + "]", entry.getValue());
        }
        return result;
    }

    private void sendBlankInventoryPacket(Player player) {
        if (listenerProtocolEvent != null)
            listenerProtocolEvent.sendBlankInventoryPacket(player);
    }

    public void login(Player player) {
        try {
            if (config.getBoolean("event.tp_back_after_login", true)) {
                location_data.load(location_file);
                Location spawn_location = Objects.requireNonNull(getWorld("world")).getSpawnLocation();
                Location leave_location = new Location(
                        getWorld(UUID.fromString(Objects.requireNonNull(location_data.getString(
                                player.getName() + ".world",
                                spawn_location.getWorld().getUID().toString())))),
                        XenforoLogin.instance.location_data.getDouble(
                                player.getName() + ".x", spawn_location.getX()),
                        XenforoLogin.instance.location_data.getDouble(
                                player.getName() + ".y", spawn_location.getY()),
                        XenforoLogin.instance.location_data.getDouble(
                                player.getName() + ".z", spawn_location.getZ()),
                        (float) XenforoLogin.instance.location_data.getDouble(
                                player.getName() + ".yaw", spawn_location.getYaw()),
                        (float) XenforoLogin.instance.location_data.getDouble(
                                player.getName() + ".pitch", spawn_location.getPitch())
                );
                try {
                    player.teleportAsync(leave_location);
                } catch (NoSuchMethodError e) {
                    XenforoLogin.instance.getLogger().warning("Cannot find method " + e.getMessage());
                    XenforoLogin.instance.getLogger().warning("Using synchronized teleport");
                    Bukkit.getScheduler().runTask(XenforoLogin.instance, () -> player.teleport(leave_location));
                }
            }
            logged_in.put(player.getName(), StatusType.LOGINED);
            player.updateInventory();
            XenforoLogin.instance.getLogger().info("Logging in " + player.getName());
            player.sendMessage(XenforoLogin.instance.langFile("success"));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void message(Player player){
        switch (logged_in.get(player.getName())){
            case NEED_LOGIN:
                player.sendMessage(langFile("need_login"));
                break;
            case NEED_REGISTER_EMAIL:
                player.sendMessage(langFile("register_email"));
                break;
            case NEED_REGISTER_PASSWORD:
                player.sendMessage(langFile("register_password"));
                break;
            case NEED_REGISTER_CONFIRM:
                player.sendMessage(langFile("register_password_confirm"));
                break;
        }
    }
}

