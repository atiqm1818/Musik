package com.atiqm.Musik.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Recommendations;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.browse.GetRecommendationsRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.*;

public class CommandManager extends ListenerAdapter {
    //Spotify API connection
    private static final String clientId = "c9d85716b5e94020b4955f1d66fa9cb4";
    private static final String clientSecret = "43dbd8882b7f489cac80dee728b996b2";
    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .build();
    private static final ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
            .build();
    public static void clientCredentials(){
        try{

            final ClientCredentials clientCredentials = clientCredentialsRequest.execute();
            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());

            System.out.println("Expires in: " + clientCredentials.getExpiresIn());
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    //Command pickup-------------------------------------------------------------------------------------
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName().toLowerCase();
        switch (cmd) {
            case "recommend":
                try {
                    recommend(event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case "rate":
                rateBars(event);
                break;
        }
    }
    //Registering commands-------------------------------------------------------------------------------
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandData =new ArrayList<>();
        //recommend command
        OptionData genre = new OptionData(OptionType.STRING, "genre", "The genre of music you want to be recommended")
                .addChoice("Hip-Hop", "hip-hop")
                .addChoice("Pop", "pop")
                .addChoice("K-pop", "k-pop")
                .addChoice("Rock", "rock")
                .addChoice("EDM", "edm")
                .addChoice("Classical", "classical");
        commandData.add(Commands.slash("recommend", "get recommended music from a random or a specific genre")
                .addOptions(genre));
        //rap battle command
        OptionData lyrics = new OptionData(OptionType.STRING, "lyrics", "The bars you wrote");
        commandData.add(Commands.slash("rate", "Have Musik rate your bars")
                .addOptions(lyrics));
        //adding commands to bot
        event.getGuild().updateCommands().addCommands(commandData).queue();
        //Daily event for sending the song of the day
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                dailySong(event);
            }
        };
        Date today = new Date();
        timer.scheduleAtFixedRate(task,new Date(today.getYear(), today.getMonth(), today.getDate(),9,0),86400000);
        //spotify api connection
        clientCredentials();
    }
    //Command functions-----------------------------------------------------------------------------------------
    public void recommend(SlashCommandInteractionEvent event) throws Exception{
        //switch case to check if user picked a genre
        OptionMapping option = event.getOption("genre");
        String genre = option.getAsString();
        event.reply(getRecs(genre)).queue();
    }
    //TODO: FIGURE OUT WUDAHELL WE DOIN FOR THIS
    public void rateBars(SlashCommandInteractionEvent event){
        event.reply("moms spaghetti").queue();
    }
    //Spotify API Calls-----------------------------------------------------------------------------------------
    public String getRecs(String genre){
        GetRecommendationsRequest getRecommendationsRequest = spotifyApi.getRecommendations()
                .seed_genres(genre)
                .limit(1)
                .build();
        try {
            final Recommendations recommendations = getRecommendationsRequest.execute();

            TrackSimplified[] recs = recommendations.getTracks();
            String artists = "";
            for(int x = 0; x < recs[0].getArtists().length; x++){
                artists += recs[0].getArtists()[x].getName() + " ";
            }
                return recs[0].getName() + " by " + artists;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return "it did nto freaking work";
    }
    //Other Helper Methods / Functions-----------------------------------------------------------------------
    public void dailySong(GuildReadyEvent event){
        event.getGuild().getDefaultChannel().asTextChannel().sendMessage("Good Morning Gamers!\nToday's Jammer: "
                + "song");
    }
}

