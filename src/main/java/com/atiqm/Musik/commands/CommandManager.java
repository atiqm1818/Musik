package com.atiqm.Musik.commands;

import com.atiqm.Musik.lavaplayer.GuildMusicManager;
import com.atiqm.Musik.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
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
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class CommandManager extends ListenerAdapter {
    //Spotify API connection
    private static Dotenv config = Dotenv.configure().load();
    private static final String clientId = config.get("CLIENT_ID");
    private static final String clientSecret = config.get("CLIENT_SECRET");
    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .build();
    private static final ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
            .build();
    private static final List<String> spotifyGenres = new ArrayList<>(Arrays.asList("acoustic", "afrobeat", "alt-rock", "alternative", "ambient", "anime",
            "black-metal", "bluegrass", "blues", "bossanova", "brazil", "breakbeat", "british", "cantopop", "chicago-house",
            "children", "chill", "classical", "club", "comedy", "country", "dance", "dancehall", "death-metal", "deep-house",
            "detroit-techno", "disco", "disney", "drum-and-bass", "dub", "dubstep", "edm", "electro", "electronic",
            "emo", "folk", "forro", "french", "funk", "garage", "german", "gospel", "goth", "grindcore", "groove", "grunge",
            "guitar", "happy", "hard-rock", "hardcore", "hardstyle", "heavy-metal", "hip-hop", "holidays", "honky-tonk", "house",
            "idm", "indian", "indie", "indie-pop", "industrial", "iranian", "j-dance", "j-idol", "j-pop", "j-rock", "jazz", "k-pop",
            "kids", "latin", "latino", "malay", "mandopop", "metal", "metal-misc", "metalcore", "minimal-techno", "movies", "mpb", "new-age",
            "new-release", "opera", "pagode", "party", "philippines-opm", "piano", "pop", "pop-film", "post-dubstep", "power-pop",
            "progressive-house", "psych-rock", "punk", "punk-rock", "r-n-b", "rainy-day", "reggae",
            "reggaeton", "road-trip", "rock", "rock-n-roll", "rockabilly", "romance", "sad", "salsa", "samba", "sertanejo", "show-tunes",
            "singer-songwriter", "ska", "sleep", "songwriter", "soul", "soundtracks", "spanish", "study",
            "summer", "swedish", "synth-pop", "tango", "techno", "trance", "trip-hop", "turkish", "work-out", "world-music"));
    public static void clientCredentials() {
        try {

            final ClientCredentials clientCredentials = clientCredentialsRequest.execute();
            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());

            System.out.println("Expires in: " + clientCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
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
            case "play":
                play(event);
                break;
            case "skip":
                skip(event);
                break;
            case "pause":
                pause(event);
                break;
            case "resume":
                resume(event);
                break;
            case "clear":
                clear(event);
                break;
            case "np":
                nowPlaying(event);
                break;
            case "queue":
                queue(event);
                break;
            case "loop":
                loop(event);
                break;
        }
    }
    //Registering commands-------------------------------------------------------------------------------
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();
        //recommend command
        OptionData genre = new OptionData(OptionType.STRING, "genre", "The genre of music you want to be recommended or enter 'random' for a random recommendation", true);
        commandData.add(Commands.slash("recommend", "get recommended music from a random or a specific genre")
                .addOptions(genre));
        //play command
        OptionData track = new OptionData(OptionType.STRING, "song", "URL, link, or name of the song you wish to play", true);
        commandData.add(Commands.slash("play", "play a song").addOptions(track));
        //skip command
        commandData.add(Commands.slash("skip", "skip current track"));
        //pause command
        commandData.add(Commands.slash("pause", "pause current track"));
        //resume command
        commandData.add(Commands.slash("resume", "resume paused track"));
        //clear command
        commandData.add(Commands.slash("clear", "clear the current queue and stop the currently playing track"));
        //nowPlaying command
        commandData.add(Commands.slash("np", "view the currently playing song"));
        //queue command
        commandData.add(Commands.slash("queue", "view the upcoming tracks that are queued"));
        //loop command
        commandData.add(Commands.slash("loop", "loop the current track or stop the current loop"));
        //adding commands to bot
        event.getGuild().updateCommands().addCommands(commandData).queue();
        //spotify api connection
        clientCredentials();
        //Daily event for sending the song of the day
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                dailySong(event);
            }
        };
        Date today = new Date();
        timer.scheduleAtFixedRate(task, new Date(today.getYear(), today.getMonth(), today.getDate(), 9, 0), 86400000);
    }

    //Command functions-----------------------------------------------------------------------------------------
    //TODO: Clean up responses for each command, make them well formatted and organized
    public void recommend(SlashCommandInteractionEvent event) throws Exception {
        //switch case to check if user picked a genre
        OptionMapping option = event.getOption("genre");
        String genre = option.getAsString();
        if(genre.trim().equals("")){
            genre = "random";
        }
        String genreApiCall = getRecs(genre.trim().toLowerCase());
        if(genreApiCall.equals("invalid genre")){
            event.reply("Invalid genre entered").queue();
        }
        else{
            event.reply(genreApiCall).queue();
        }
    }

    public void play(SlashCommandInteractionEvent event){
        //check to make sure member is in a voice channel before running the command
        Member member = event.getMember();
        GuildVoiceState memberVoiceState = member.getVoiceState();
        if(!memberVoiceState.inAudioChannel()){
            event.reply("You need to be in a voice channel to run this command").queue();
            return;
        }
        //checking bots state of seeing if it is in a voice channel or not
        Member self = event.getGuild().getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();
        if(!selfVoiceState.inAudioChannel()){
            event.getGuild().getAudioManager().openAudioConnection(memberVoiceState.getChannel());
        }
        else{
            if(selfVoiceState.getChannel() != memberVoiceState.getChannel()){
                event.reply("need to be in the same channel as the bot").queue();
                return;
            }
        }
        //conditions checked, play the song provided by user
        PlayerManager playerManager = PlayerManager.get();
        playerManager.play(event.getGuild(), event.getOption("song").getAsString());
        event.reply("Added your song to the queue").queue();
    }

    public void skip(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        //skipping the song
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        guildMusicManager.getTrackScheduler().getPlayer().stopTrack();
        event.reply("Song skipped->").queue();
    }

    public void pause(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        guildMusicManager.getTrackScheduler().getPlayer().setPaused(true);
        event.reply("Song paused").queue();
    }

    public void resume(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        guildMusicManager.getTrackScheduler().getPlayer().setPaused(false);
        event.reply("Song resumed").queue();
    }
    public void clear(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        guildMusicManager.getTrackScheduler().getQueue().clear();
        guildMusicManager.getTrackScheduler().getPlayer().stopTrack();
        event.reply("the queue has been cleared").queue();
    }

    public void nowPlaying(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        AudioTrackInfo info = guildMusicManager.getTrackScheduler().getPlayer().getPlayingTrack().getInfo();
        event.reply("Now playing *" + info.title + "*").queue();
    }

    public void queue(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        String reply = "";
        int count = 1;
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        BlockingQueue<AudioTrack> list = guildMusicManager.getTrackScheduler().getQueue();
        for(AudioTrack track : list){
            reply += count + ". " + track.getInfo().title + "\n";
            count++;
        }
        if(reply.equals("")){
            event.reply("No songs are queued up").queue();
        }
        else{
            event.reply("Coming up next: \n" + reply).queue();
        }
    }
    public void loop(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        boolean looping = !guildMusicManager.getTrackScheduler().isRepeating();
        guildMusicManager.getTrackScheduler().setRepeating(looping);
        if(looping){
            event.reply(":repeat: Looping").queue();
        }
        else{
            event.reply(":x:Stopped loop").queue();
        }
    }

    public void checkVoiceState(SlashCommandInteractionEvent event){
        //check to make sure member is in a voice channel before running the command
        Member member = event.getMember();
        GuildVoiceState memberVoiceState = member.getVoiceState();
        if(!memberVoiceState.inAudioChannel()){
            event.reply("You need to be in a voice channel to run this command.").queue();
            return;
        }
        //checking bots state of seeing if it is in a voice channel or not
        Member self = event.getGuild().getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();
        if(!selfVoiceState.inAudioChannel()){
            event.reply("I am not currently in a voice channel.").queue();
            return;
        }
        //check to ensure bot is in same channel as user
        if(memberVoiceState.getChannel() != selfVoiceState.getChannel()){
            event.reply("Please join the same voice channel as me to use this command.").queue();
            return;
        }
        return;
    }

    //Spotify API Calls-----------------------------------------------------------------------------------------
    public String getRecs(String genre) {
        if(genre.equals("random")){
            genre = spotifyGenres.get(ThreadLocalRandom.current().nextInt(0, spotifyGenres.size()));
        }
        if(!spotifyGenres.contains(genre)){
            return "invalid genre";
        }
        GetRecommendationsRequest getRecommendationsRequest = spotifyApi.getRecommendations()
                .seed_genres(genre)
                .limit(1)
                .build();
        try {
            final Recommendations recommendations = getRecommendationsRequest.execute();
            TrackSimplified[] recs = recommendations.getTracks();
            String artists = "";
            for (int x = 0; x < recs[0].getArtists().length; x++) {
                artists += recs[0].getArtists()[x].getName() + " ";
            }
            return recs[0].getName() + " by " + artists;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return "Get Recommendations Command Error";
    }

    //Other Helper Methods / Functions-----------------------------------------------------------------------
    public void dailySong(GuildReadyEvent event) {

        event.getGuild().getTextChannelsByName("general", true).get(0).sendMessage("Good Morning Gamers!\nToday's Jammer: "
                + getRecs("random")).queue();
    }
}
