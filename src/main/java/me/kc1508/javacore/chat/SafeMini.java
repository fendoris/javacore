package me.kc1508.javacore.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class SafeMini {
    private SafeMini() {
    }

    public static Component renderPlayerMessage(MiniMessage mini, String template, Component playerNameComponent, String playerNameRaw, String messageRaw) {
        String t = normalize(template);
        TagResolver resolver = TagResolver.resolver(Placeholder.component("player", playerNameComponent), Placeholder.unparsed("playername", playerNameRaw), Placeholder.unparsed("message", messageRaw));
        return mini.deserialize(t, resolver);
    }

    public static Component renderPm(MiniMessage mini, String template, Component playerNameComponent, String playerNameRaw, Component targetNameComponent, String targetNameRaw, String messageRaw) {
        String t = normalize(template);
        TagResolver resolver = TagResolver.resolver(Placeholder.component("player", playerNameComponent), Placeholder.unparsed("playername", playerNameRaw), Placeholder.component("target", targetNameComponent), Placeholder.unparsed("targetname", targetNameRaw), Placeholder.unparsed("message", messageRaw));
        return mini.deserialize(t, resolver);
    }

    private static String normalize(String template) {
        if (template == null) template = "";
        return template
                // component placeholders
                .replace("%player%", "<player>").replace("{player}", "<player>").replace("%target%", "<target>").replace("{target}", "<target>")
                // raw-string placeholders (safe in attributes like click:suggest_command)
                .replace("%playername%", "<playername>").replace("{playername}", "<playername>").replace("%targetname%", "<targetname>").replace("{targetname}", "<targetname>")
                // message placeholder (raw text)
                .replace("%message%", "<message>").replace("{message}", "<message>");
    }
}
