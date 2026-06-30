package com.goidacraft.goidachat;

import com.goidacraft.goidachat.commands.CommandRegistrar;
import com.goidacraft.goidachat.config.GoidaChatConfig;
import com.goidacraft.goidachat.data.BanAttemptStorage;
import com.goidacraft.goidachat.data.IgnoreStorage;
import com.goidacraft.goidachat.data.LegacyPluginImport;
import com.goidacraft.goidachat.data.PlayerHistory;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.goidacraft.goidachat.data.TrustedAccounts;
import com.goidacraft.goidachat.data.ViolationStorage;
import com.goidacraft.goidachat.chat.EscalationManager;
import com.goidacraft.goidachat.events.ChatEventHandler;
import com.goidacraft.goidachat.events.PlayerEventHandler;
import com.goidacraft.goidachat.logging.ChatLogger;
import com.goidacraft.goidachat.util.HwidLookup;
import com.goidacraft.goidachat.vote.VoteMuteManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.nio.file.Path;

@Mod("goidachat")
public class GoidaChat {

    public static final String MOD_ID = "goidachat";

    private static Boolean voteLoaded;
    private static VoteMuteManager voteMuteManager;

    public GoidaChat(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, GoidaChatConfig.SPEC);

        modEventBus.addListener(this::setup);

        NeoForge.EVENT_BUS.register(new ChatEventHandler());
        NeoForge.EVENT_BUS.register(new PlayerEventHandler());
        NeoForge.EVENT_BUS.register(new CommandRegistrar());
        NeoForge.EVENT_BUS.register(this);
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path gameDir   = FMLPaths.GAMEDIR.get();

            // Однократный перенос данных старого плагина GoidaChat в config/goidachat (до init).
            LegacyPluginImport.run(configDir, gameDir);

            PunishmentStorage.init(configDir);
            BanAttemptStorage.init(configDir);
            IgnoreStorage.init(configDir);
            PlayerHistory.init(configDir);
            TrustedAccounts.init(configDir);
            ViolationStorage.init(configDir);
            EscalationManager.init(configDir);
            ChatLogger.init(gameDir);
            HwidLookup.init(gameDir);
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ChatLogger.cleanOldLogs();
        PunishmentStorage.purgeExpired();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // GoidaVote loads before us (see mods.toml ordering), so its poll API is ready here.
        if (voteLoaded()) {
            voteMuteManager = new VoteMuteManager(event.getServer());
        }
    }

    /** Whether GoidaVote is installed — gates all references to its (compile-only) classes. */
    public static boolean voteLoaded() {
        if (voteLoaded == null) voteLoaded = ModList.get().isLoaded("goidavote");
        return voteLoaded;
    }

    /** The vote-mute manager, or {@code null} if GoidaVote is absent or the server isn't started. */
    public static VoteMuteManager voteManager() {
        return voteMuteManager;
    }
}
