package com.github.selfcrafted.velocity.whitelist;

import com.google.inject.Inject;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Plugin(id = "selfcrafted-velocity-whitelist", name = "&&name", version = "&&version",
        url = "https://github.com/self-crafted/velocity-whitelist",
        description = "Dead simple player whitelist for Velocity proxy",
        authors = {"offby0point5"})
public class VelocityWhitelist {
    private static final File WHITELIST_FILE = new File("whitelist.txt");

    private static Set<UUID> WHITELIST = Set.of();
    private static final ResultedEvent.ComponentResult ALLOWED = ResultedEvent.ComponentResult.allowed();
    private static final ResultedEvent.ComponentResult DENIED = ResultedEvent.ComponentResult.denied(
            Component.text("You're not invited to the party...", NamedTextColor.RED));

    @Inject
    private Logger logger;
    @Inject
    private ProxyServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getScheduler().buildTask(this, this::reloadWhitelist).repeat(Duration.ofSeconds(10)).schedule();
    }

    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        if (WHITELIST.contains(event.getPlayer().getUniqueId())) event.setResult(ALLOWED);
        else event.setResult(DENIED);
    }

    private void reloadWhitelist() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(WHITELIST_FILE));
            Set<UUID> uuidSet = new HashSet<>();
            String uuidLine;
            while ((uuidLine = reader.readLine()) != null) {
                if (uuidLine.startsWith("#") || uuidLine.startsWith("//") || uuidLine.isBlank()) continue;
                try {
                    uuidSet.add(UUID.fromString(uuidLine.stripIndent()));
                } catch (IllegalArgumentException e) {
                    logger.warn("Line is not a valid UUID: \"" + uuidLine + "\"\nComment lines start with a '#'.");
                }
            }
            WHITELIST = Set.copyOf(uuidSet);
        } catch (FileNotFoundException e) {
            try {
                if (WHITELIST_FILE.createNewFile())
                    logger.info("Created whitelist file");
            } catch (IOException e2) {
                logger.warn("Couldn't create whitelist file", e2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
