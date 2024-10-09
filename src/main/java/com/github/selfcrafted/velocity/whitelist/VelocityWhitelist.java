package com.github.selfcrafted.velocity.whitelist;

import com.google.inject.Inject;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
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
        authors = {"offby0point5, AikoMastboom"})
public class VelocityWhitelist {
    private static final File WHITELIST_FILE = new File("whitelist.txt");

    private static Set<UUID> WHITELIST = Set.of();
    private static final ResultedEvent.ComponentResult ALLOWED = ResultedEvent.ComponentResult.allowed();
    private static final TextComponent deniedReason = Component.text()
            .content("Stuur een email naar ")
            .color(NamedTextColor.RED)
            .append(Component.text().content("info@meinland.nl")
                    .color(NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.openUrl(
                    "mailto:info@meinland.nl?subject=Verzoek%20toegang%20meinland.nl&body=Hoi%2C%0A%0AZou%20je%20mijn%20Minecraft%20naam%20%22JOUW_MINECRAFT_NAAM%22%20toegang%20willen%20geven%20tot%20meinland.nl.%20%0A%0AIk%20wil%20de%20server%20graag%20proberen.%0A%0AGroetjes%2C%0A%0AJOUW_ECHTE_NAAM%0A"
                    ))
            )
            .append(Component.text(" met je verzoek tot toegang, tot gauw.", NamedTextColor.RED))
            .build();
    private static final ResultedEvent.ComponentResult DENIED = ResultedEvent.ComponentResult.denied(deniedReason);


    @Inject
    private Logger logger;
    @Inject
    private ProxyServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server
                .getScheduler()
                .buildTask(this, this::reloadWhitelist)
                .repeat(Duration.ofSeconds(60))
                .schedule();
    }

    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        if(event.getPlayer() instanceof Player player) {
            if (WHITELIST.contains(player.getUniqueId())) {
                event.setResult(ALLOWED);
            } else {
                logger.warn("Player not yet whitelisted: {} - {}", player.getUsername(), player.getUniqueId());
                event.setResult(DENIED);
            }
        } else {
            event.setResult(DENIED);
        }
    }

    private void reloadWhitelist() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(WHITELIST_FILE));
            Set<UUID> uuidSet = new HashSet<>();
            String uuidLine;
            while ((uuidLine = reader.readLine()) != null) {
                if (uuidLine.isBlank()
                        || uuidLine.startsWith("#")
                        || uuidLine.startsWith("//")
                ) {
                    continue;
                }
                try {
                    uuidSet.add(UUID.fromString(uuidLine.stripIndent()));
                } catch (IllegalArgumentException e) {
                    logger.warn("Line is not a valid UUID: \"{}\" - Comment lines start with a '#'.", uuidLine);
                }
            }
            WHITELIST = Set.copyOf(uuidSet);
        } catch (FileNotFoundException fnfe) {
            try {
                if (WHITELIST_FILE.createNewFile()) {
                    logger.info("Created whitelist file");
                }
            } catch (IOException ioe) {
                logger.warn("Couldn't create whitelist file: " + ioe.getMessage(), ioe);
            }
        } catch (IOException ioe) {
            logger.error("Couldn't read whitelist file " + ioe.getMessage(), ioe);
        }
    }
}
