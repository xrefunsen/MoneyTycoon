package dev.xdev.moneytycoon.util;

import dev.xdev.moneytycoon.MoneyTycoon;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class MessageUtil {

    private static final LegacyComponentSerializer AMP_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    private MessageUtil() {}

    public static void send(Player player, String messageKey, Map<String, String> placeholders) {
        MoneyTycoon plugin = MoneyTycoon.getInstance();
        String prefix = plugin.getConfig().getString("messages.prefix", "&6[MoneyTycoon] &r");
        String message = plugin.getConfig().getString("messages." + messageKey, messageKey);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        player.sendMessage(AMP_SERIALIZER.deserialize(prefix + message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(SECTION_SERIALIZER.deserialize(message));
    }
}
