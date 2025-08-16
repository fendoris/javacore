package me.kc1508.javacore.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Utilities to render MiniMessage templates without letting user text inject tags.
 */
public final class SafeMini {
    private SafeMini() {
    }

    /**
     * Global chat: %player% (styled), %playername% (raw string for attributes), %message% (unparsed).
     */
    public static Component renderPlayerMessage(MiniMessage mini, String template, Component playerName, String playerNameRaw, String message) {
        String t = normalize(template);
        TagResolver resolver = TagResolver.resolver(Placeholder.component("player", playerName), Placeholder.unparsed("playername", playerNameRaw), Placeholder.unparsed("message", message));
        return mini.deserialize(t, resolver);
    }

    /**
     * PMs: %player%, %playername%, %target%, %targetname%, %message% (all safe).
     */
    public static Component renderPm(MiniMessage mini, String template, Component playerName, String playerNameRaw, Component targetName, String targetNameRaw, String message) {
        String t = normalize(template);
        TagResolver resolver = TagResolver.resolver(Placeholder.component("player", playerName), Placeholder.unparsed("playername", playerNameRaw), Placeholder.component("target", targetName), Placeholder.unparsed("targetname", targetNameRaw), Placeholder.unparsed("message", message));
        return mini.deserialize(t, resolver);
    }

    /**
     * Map config placeholders to MiniMessage placeholder tags.
     */
    private static String normalize(String template) {
        if (template == null) template = "";
        return template.replace("%player%", "<player>").replace("{player}", "<player>").replace("%playername%", "<playername>").replace("{playername}", "<playername>").replace("%target%", "<target>").replace("{target}", "<target>").replace("%targetname%", "<targetname>").replace("{targetname}", "<targetname>").replace("%message%", "<message>").replace("{message}", "<message>");
    }
}
