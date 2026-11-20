package com.immersive_nature.fluids.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Translates the /flowing_fluids command texts into the player's language.
 * The commands build their messages from English pieces on the server, so each English piece
 * (the "immersive_nature.fluids.text.*" entries of en_us.json) is swapped for the same entry of the
 * player's language file. All texts live in assets/immersive_nature/lang like every other translation.
 */
public final class FFLang {
    private static final String PREFIX = "immersive_nature.fluids.text.";
    private static final Map<String, Map<String, String>> LANGS = new ConcurrentHashMap<>();
    private static volatile List<Map.Entry<String, String>> english;

    private FFLang() {
    }

    public static String translate(String text, @Nullable ServerPlayer player) {
        String code = player == null ? null : player.getLanguage();
        if (code == null || code.isEmpty() || code.equalsIgnoreCase("en_us")) return text;
        Map<String, String> lang = load(code.toLowerCase());
        if (lang.isEmpty()) return text;
        String out = text;
        List<String> found = new ArrayList<>();
        // longest pieces first; translated pieces are parked behind markers so nothing is translated twice
        for (Map.Entry<String, String> e : english()) {
            String translated = lang.get(e.getKey());
            if (translated == null || !out.contains(e.getValue())) continue;
            out = out.replace(e.getValue(), "\u0000" + found.size() + "\u0001");
            found.add(translated);
        }
        for (int i = 0; i < found.size(); i++) out = out.replace("\u0000" + i + "\u0001", found.get(i));
        return out;
    }

    private static List<Map.Entry<String, String>> english() {
        if (english == null) {
            List<Map.Entry<String, String>> list = new ArrayList<>(load("en_us").entrySet());
            list.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getValue().length()).reversed());
            english = list;
        }
        return english;
    }

    private static Map<String, String> load(String code) {
        return LANGS.computeIfAbsent(code, c -> {
            Map<String, String> result = new HashMap<>();
            try (InputStream in = FFLang.class.getResourceAsStream("/assets/immersive_nature/lang/" + c + ".json")) {
                if (in == null) return result;
                Map<String, String> all = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                        new TypeToken<Map<String, String>>() {}.getType());
                all.forEach((k, v) -> {
                    if (k.startsWith(PREFIX)) result.put(k, v);
                });
            } catch (Exception ignored) {
            }
            return result;
        });
    }
}
