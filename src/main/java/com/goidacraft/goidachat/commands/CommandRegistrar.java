package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.GoidaChat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class CommandRegistrar {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        // Убираем ванильные команды, чтобы наши одноимённые версии были основными.
        VanillaCommandUtil.remove(dispatcher,
                "msg", "tell", "w", "teammsg", "tm",
                "ban", "ban-ip", "banlist", "pardon", "pardon-ip", "kick");

        // Чат / личные сообщения
        MsgCommand.register(dispatcher);
        ReplyCommand.register(dispatcher);
        IgnoreCommand.register(dispatcher);
        UnignoreCommand.register(dispatcher);
        SocialSpyCommand.register(dispatcher);
        MentionSoundCommand.register(dispatcher);
        AdminChatCommand.register(dispatcher);
        AdminRestCommand.register(dispatcher);

        // Модерация
        MuteCommand.register(dispatcher);      // /mute + /unmute
        BanCommand.register(dispatcher);
        UnbanCommand.register(dispatcher);     // /unban + /pardon
        KickCommand.register(dispatcher);
        TrustCommand.register(dispatcher);
        UntrustCommand.register(dispatcher);
        BanListCommand.register(dispatcher);
        BanLogCommand.register(dispatcher);
        ViolationsCommand.register(dispatcher);
        AutoModCommand.register(dispatcher);

        // Голосование за мут — только если GoidaVote установлен.
        // Guard защищает от загрузки классов, ссылающихся на GoidaVote, в его отсутствие.
        if (GoidaChat.voteLoaded()) {
            VoteMuteCommand.register(dispatcher);
            VoteUnmuteCommand.register(dispatcher);
        }
    }
}
