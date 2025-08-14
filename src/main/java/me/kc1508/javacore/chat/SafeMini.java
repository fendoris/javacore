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
     * Render a template with %player% and %message% safely (no tag injection).
     */
    public static Component renderPlayerMessage(MiniMessage mini, String template,
                                                Component playerName, String message) {
        String t = normalize(template);
        TagResolver resolver = TagResolver.resolver(
                Placeholder.component("player", playerName),
                Placeholder.unparsed("message", message) // user's text stays unparsed
        );
        return mini.deserialize(t, resolver);
    }

    /**
     * Render a PM template with %player%, %target%, and %message% safely.
     */
    public static Component renderPm(MiniMessage mini, String template,
                                     Component playerName, Component targetName,
                                     String message) {
        String t = normalize(template);
        TagResolver resolver = TagResolver.resolver(
                Placeholder.component("player", playerName),
                Placeholder.component("target", targetName),
                Placeholder.unparsed("message", message)
        );
        return mini.deserialize(t, resolver);
    }

    /**
     * Map config placeholders to MiniMessage placeholder tags.
     */
    private static String normalize(String template) {
        if (template == null) template = "";
        return template
                .replace("%player%", "<player>").replace("{player}", "<player>")
                .replace("%message%", "<message>").replace("{message}", "<message>")
                .replace("%target%", "<target>").replace("{target}", "<target>");
    }
}
