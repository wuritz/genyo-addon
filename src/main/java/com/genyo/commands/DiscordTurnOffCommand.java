package com.genyo.commands;

import com.genyo.systems.config.GenyoConfig;
import com.genyo.utils.GenyoChatUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

public class DiscordTurnOffCommand extends Command {

    public DiscordTurnOffCommand() {
        super("imapussy", "Turns off Genyo Discord. It's like a cheat code.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            GenyoConfig.get().makeDiscordVisible();
            GenyoChatUtils.sendInfo("Sorry to hear that. :( You can now turn off the GenyoDiscord module.");

            return SINGLE_SUCCESS;
        });
    }
}
