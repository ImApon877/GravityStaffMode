package it.gravitymc.gravitystaffmode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * YAML files always read and written as UTF-8. On 1.8 Bukkit uses the system charset
 * (cp1252 on Windows), which turns characters like » into Â» and loses symbols in saved item names.
 */
public final class Utf8Yaml {
    private Utf8Yaml() {
    }

    public static YamlConfiguration load(File file) {
        YamlConfiguration configuration = new YamlConfiguration();
        if (!file.exists()) {
            return configuration;
        }
        try {
            configuration.loadFromString(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Cannot load " + file.getName() + ": " + exception.getMessage(), exception);
        }
        return configuration;
    }

    public static YamlConfiguration load(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            bytes.write(buffer, 0, read);
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
        return configuration;
    }

    /**
     * Repairs text saved by older versions on 1.8 under Windows: UTF-8 read as cp1252 turned » into Â»
     * and ✦ into âœ¦. Such strings are turned back into UTF-8. Returns true if something was repaired.
     */
    public static boolean repairMojibake(YamlConfiguration configuration) {
        Charset windows = Charset.forName("windows-1252");
        boolean repaired = false;
        for (String key : configuration.getKeys(true)) {
            Object value = configuration.get(key, null);
            if (!(value instanceof String) || !configuration.isSet(key)) {
                continue;
            }
            String text = (String) value;
            if (text.indexOf('\u00C2') < 0 && text.indexOf('\u00C3') < 0 && text.indexOf('\u00E2') < 0) {
                continue;
            }
            String fixed = new String(text.getBytes(windows), StandardCharsets.UTF_8);
            if (fixed.indexOf('\uFFFD') < 0 && !fixed.equals(text)) {
                configuration.set(key, fixed);
                repaired = true;
            }
        }
        return repaired;
    }

    public static void save(YamlConfiguration configuration, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        Files.write(file.toPath(), configuration.saveToString().getBytes(StandardCharsets.UTF_8));
    }
}
