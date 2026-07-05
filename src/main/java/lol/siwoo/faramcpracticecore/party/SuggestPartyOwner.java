package lol.siwoo.faramcpracticecore.party;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.battlekit.BattleKit;
import lol.siwoo.faramcpracticecore.design.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class SuggestPartyOwner implements CommandExecutor {

    public static final String GUI_TITLE = ChatColor.DARK_AQUA + "Select Kit to Suggest";
    StrikePracticeAPI api = StrikePractice.getAPI();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(MessageStyle.error("This command can only be used by a player."));
            return true;
        }

        if (api.getParty(p) == null) {
            p.sendMessage(MessageStyle.error("You're not in a party."));
            return true;
        }

        // getOwner() returns the owner's name, not a Player
        if (api.getParty(p).getOwner().equals(p.getName())) {
            p.sendMessage(MessageStyle.error("You're the party owner. Start an event yourself."));
            return true;
        }

        Inventory kitSelectionGui = createKitSelectionGUI();
        if (kitSelectionGui == null) {
            p.sendMessage(MessageStyle.error("Could not load kits. Contact an admin."));
            return true;
        }
        p.openInventory(kitSelectionGui);

        return true;
    }

    private Inventory createKitSelectionGUI() {
        int kitCount = api.getKits().size();
        int inventorySize = Math.min(54, Math.max(9, (int) Math.ceil(kitCount / 9.0) * 9));

        Inventory gui = Bukkit.createInventory(null, inventorySize, GUI_TITLE);

        int slot = 0;
        for (BattleKit kit : api.getKits()) {
            if (slot >= inventorySize)
                break;

            ItemStack kitIcon = kit.getIcon().clone();

            ItemMeta meta = kitIcon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + kit.getName());

                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Click to suggest this kit to the party owner.",
                        "",
                        ChatColor.DARK_GRAY + "Kit ID: " + kit.getName()));
                kitIcon.setItemMeta(meta);
            }

            gui.setItem(slot++, kitIcon);
        }

        return gui;
    }
}
