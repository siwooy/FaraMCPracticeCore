package lol.siwoo.faramcpracticecore.aa.status;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Optional remote status check. If the configured endpoint returns "disable",
 * the plugin disables itself. Network failures are fail-OPEN: an unreachable
 * status endpoint must never take the server down with it.
 */
public class StatusChecker {
    private final JavaPlugin plugin;

    public StatusChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void check() {
        String statusUrl = plugin.getConfig().getString("status-check.url", "");
        if (statusUrl == null || statusUrl.isBlank()) {
            return; // not configured — skip the check entirely
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Starting status check...");

                try {
                    URL url = new URL(statusUrl);

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("User-Agent", "FaraMCPracticeCore-StatusChecker/1.0");

                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        String status;
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream()))) {
                            status = reader.readLine();
                        }

                        if (status != null && status.trim().equalsIgnoreCase("disable")) {
                            // Bukkit API must be touched on the main thread only
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (plugin instanceof shutDown s) {
                                        s.emergencyShutDown();
                                    }
                                }
                            }.runTask(plugin);
                        } else {
                            plugin.getLogger().info("Status check: Operational");
                        }
                    } else {
                        plugin.getLogger().warning("Status check returned HTTP " + responseCode
                                + " — continuing normally.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Status check failed (" + e.getMessage()
                            + ") — continuing normally.");
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public interface shutDown {
        void emergencyShutDown();
    }
}
