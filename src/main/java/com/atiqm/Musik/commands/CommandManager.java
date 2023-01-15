package com.atiqm.Musik.commands;

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

public class CommandManager extends ListenerAdapter {
    //Command pickup-------------------------------------------------------------------------------------
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName().toLowerCase();
        switch (cmd) {

            case "recommend":
                recommend(event);
                break;

            case "battle":
                rapBattle(event);
                break;
        }
    }
    //Registering commands-------------------------------------------------------------------------------
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandData =new ArrayList<>();
        //recommend command
        OptionData genre = new OptionData(OptionType.STRING, "genre", "The genre of music you want to be recommended")
                .addChoice("Rap", "rap")
                .addChoice("Pop", "pop")
                .addChoice("K-pop", "kpop")
                .addChoice("Lo-Fi", "lofi")
                .addChoice("Rock", "rock")
                .addChoice("EDM", "edm")
                .addChoice("Classical", "classic");
        commandData.add(Commands.slash("recommend", "get recommended music from a random or a specific genre")
                .addOptions(genre));
        //rap battle command
        commandData.add(Commands.slash("battle", "rap battle Musik, 8 mile style"));
        //adding commands to bot
        event.getGuild().updateCommands().addCommands(commandData).queue();
    }
    //Command functions----------------------------------------------------------------------------------
    //TODO: Slash command for music recommendation
    public void recommend(SlashCommandInteractionEvent event){
        //switch case to check if user picked a genre
        //TODO: Research on shazam api / other music apis and learn to link it to my app
        OptionMapping option = event.getOption("genre");
        String genre = option.getAsString();
        switch(genre){
            case "rap":
                //rap rec
                break;
            case "pop":
                //pop rec
                break;
            case "kpop":
                //kpop rec
                break;
            case "lofi":
                //lofi rec
                break;
            case "rock":
                //rock rec
                break;
            case "edm":
                //edm rec
                break;
            case "classic":
                //classical rec
                break;
            default:
                break;
        }
        event.reply("Its a lofi kinda day").queue();
    }
    //TODO: Slash command for rap battle
    public void rapBattle(SlashCommandInteractionEvent event){
        event.reply("moms spaghetti").queue();
    }
}
