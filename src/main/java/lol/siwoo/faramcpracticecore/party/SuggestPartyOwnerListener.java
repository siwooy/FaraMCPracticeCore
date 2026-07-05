package lol.siwoo.faramcpracticecore.party;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import lol.siwoo.faramcpracticecore.design.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static lol.siwoo.faramcpracticecore.party.SuggestPartyOwner.GUI_TITLE;

public class SuggestPartyOwnerListener implements Listener {

    StrikePracticeAPI api = StrikePractice.getAPI();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();

        if (!view.getTitle().equals(GUI_TITLE)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player p)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }

        List<String> lore = meta.getLore();
        String suggestedKitName = null;
        for (String line : lore) {
            String strippedLine = ChatColor.stripColor(line);
            if (strippedLine.startsWith("Kit ID: ")) {
                suggestedKitName = strippedLine.substring("Kit ID: ".length());
                break;
            }
        }

        if (suggestedKitName == null) {
            p.sendMessage(MessageStyle.error("Could not determine selected kit."));
            p.closeInventory();
            return;
        }

        if (api.getParty(p) == null) {
            p.sendMessage(MessageStyle.error("You're not in a party."));
            p.closeInventory();
            return;
        }

        String ownerName = api.getParty(p).getOwner();
        Player owner = Bukkit.getPlayerExact(ownerName);

        if (owner == null || !owner.isOnline()) {
            p.sendMessage(MessageStyle.errorWithHighlight("Party owner", ownerName, "is offline."));
            p.closeInventory();
            return;
        }

        p.sendMessage(MessageStyle.successWithHighlight("Suggested", suggestedKitName, "to the party owner."));
        owner.sendMessage(MessageStyle.infoFromPlayer(p.getName(), "suggested " + suggestedKitName));

        p.closeInventory();
    }
}
