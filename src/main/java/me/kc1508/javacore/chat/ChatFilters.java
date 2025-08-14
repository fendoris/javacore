package me.kc1508.javacore.chat;

import me.kc1508.javacore.FendorisPlugin;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilters {
    private static final Pattern URL_PATTERN = Pattern.compile("\\b((https?://)?([A-Za-z0-9.-]+)\\.[A-Za-z]{2,})(/[\\w\\-./%?=&]*)?", Pattern.CASE_INSENSITIVE);

    public static String filter(FendorisPlugin plugin, String message) {
        String out = message;
        out = filterUrls(plugin, out);
        out = filterProfanity(plugin, out);
        return out;
    }

    private static String filterUrls(FendorisPlugin plugin, String msg) {
        String replacement = plugin.getConfig().getString("chat.urls.replacement", "***");
        List<String> allow = plugin.getConfig().getStringList("chat.urls.allowlist");
        if (allow == null) allow = Collections.emptyList();

        Matcher m = URL_PATTERN.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String full = m.group(0);
            String hostAndPath = "";
            try {
                String toParse = full.startsWith("http") ? full : "http://" + full;
                URI uri = new URI(toParse);
                String host = uri.getHost() == null ? "" : uri.getHost();
                String path = uri.getRawPath() == null ? "" : uri.getRawPath();
                hostAndPath = (host + path).toLowerCase(Locale.ROOT);
            } catch (Exception ignored) {
            }

            boolean allowed = false;
            for (String a : allow) {
                String al = a.toLowerCase(Locale.ROOT);
                if (al.contains("/")) {
                    if (hostAndPath.startsWith(al)) {
                        allowed = true;
                        break;
                    }
                } else {
                    if (hostAndPath.equals(al)) {
                        allowed = true;
                        break;
                    }
                }
            }
            if (allowed) {
                m.appendReplacement(sb, Matcher.quoteReplacement(full));
            } else {
                String mask = replacement.equals("***") ? "*".repeat(full.length()) : replacement;
                m.appendReplacement(sb, Matcher.quoteReplacement(mask));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String filterProfanity(FendorisPlugin plugin, String msg) {
        boolean enabled = plugin.getConfig().getBoolean("chat.profanity.enabled", true);
        if (!enabled) return msg;
        List<String> words = plugin.getConfig().getStringList("chat.profanity.words");
        if (words == null || words.isEmpty()) return msg;

        StringBuilder out = new StringBuilder();
        Matcher m = Pattern.compile("\\w+|\\W+").matcher(msg);
        while (m.find()) {
            String token = m.group();
            if (token.chars().anyMatch(Character::isLetterOrDigit)) {
                String norm = normalize(token);
                boolean bad = false;
                for (String w : words) {
                    if (norm.contains(w.toLowerCase(Locale.ROOT))) {
                        bad = true;
                        break;
                    }
                }
                out.append(bad ? "*".repeat(token.length()) : token);
            } else {
                out.append(token);
            }
        }
        return out.toString();
    }

    private static String normalize(String s) {
        String t = s.toLowerCase(Locale.ROOT);
        t = t.replace('0', 'o').replace('1', 'i').replace('!', 'i').replace('3', 'e').replace('4', 'a').replace('@', 'a').replace('5', 's').replace('$', 's').replace('7', 't');
        t = t.replaceAll("[^a-z]", "");        // strip non-letters
        t = t.replaceAll("(.)\\1{2,}", "$1$1"); // collapse long repeats
        return t;
    }
}
