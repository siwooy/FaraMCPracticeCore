package lol.siwoo.faramcpracticecore.util;

import com.eduardomcb.discord.webhook.WebhookClient;
import com.eduardomcb.discord.webhook.WebhookManager;
import com.eduardomcb.discord.webhook.models.Message;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class WebhookMessage {
    public static void statusMessage(String status) {
        FaraMCPracticeCore plugin = JavaPlugin.getPlugin(FaraMCPracticeCore.class);

        // The webhook URL is a secret (anyone holding it can post to the Discord
        // channel) — it must come from the server's local config, never the jar.
        String webhookUrl = plugin.getConfig().getString("discord.status-webhook-url", "");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        Message message = new Message()
                .setAvatarUrl("https://siwoo.lol/")
                .setUsername("Server Status")
                .setContent("**Practice** is Currently " + status);

        WebhookManager webhookManager = new WebhookManager()
                .setChannelUrl(webhookUrl)
                .setMessage(message);

        webhookManager.setListener(new WebhookClient.Callback() {
            @Override
            public void onSuccess(String response) {
                plugin.getLogger().info("Status message sent to webhook.");
            }

            @Override
            public void onFailure(int statusCode, String errorMessage) {
                plugin.getLogger().warning(
                        "Failed to send status webhook (code: " + statusCode + ", error: " + errorMessage + ")");
            }
        });

        // Plain thread instead of the Bukkit scheduler: this also runs from
        // onDisable, where scheduling new tasks is rejected.
        Thread sender = new Thread(() -> {
            try {
                webhookManager.exec();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send status webhook", e);
            }
        }, "FaraMC-StatusWebhook");
        sender.setDaemon(true);
        sender.start();
    }
}
