package com.atiqm.Musik.commands;

import com.atiqm.Musik.lavaplayer.GuildMusicManager;
import com.atiqm.Musik.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Recommendations;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.browse.GetRecommendationsRequest;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class CommandManager extends ListenerAdapter {
    //Spotify API connection
    private static final Dotenv config = Dotenv.configure().load();
    //TODO: create webhooks for formatting output to make it look even nicer
    private static final String clientId = config.get("CLIENT_ID");
    private static final String clientSecret = config.get("CLIENT_SECRET");
    private static final URI redirectUri = SpotifyHttpManager.makeUri("https://accounts.spotify.com/authorize");
    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRedirectUri(redirectUri)
            .build();
    private static ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
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
    //Channel for daily song output
    private static long dailyAlertChannelId;
    //backticks for formatted output
    private static final String blockQuote = ">>> ";
    private static final String bold = "**";
    public static void clientCredentials() {
        try {
            ClientCredentials clientCredentials = clientCredentialsRequest.execute();
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
            case "recommend" -> {
                try {
                    recommend(event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            case "play" -> play(event);
            case "skip" -> skip(event);
            case "pause" -> pause(event);
            case "resume" -> resume(event);
            case "clear" -> clear(event);
            case "np" -> nowPlaying(event);
            case "queue" -> queue(event);
            case "loop" -> loop(event);
            case "sac" -> setAlertsChannel(event);
        }
    }
    //Registering commands-------------------------------------------------------------------------------
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();
        //recommend command
//        OptionData genre = new OptionData(OptionType.STRING, "genre", "The genre of music you want to be recommended or enter 'random' for a random recommendation", true);
//        commandData.add(Commands.slash("recommend", "get recommended music from a random or a specific genre")
//                .addOptions(genre));
        //play command
        OptionData track = new OptionData(OptionType.STRING, "song", "URL, link, or name of the song you wish to play", true);
        commandData.add(Commands.slash("play", "play a song")
                .addOptions(track));
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
        //sac (set alerts channel) command
        List<TextChannel> channels = event.getGuild().getTextChannels();
        List<Command.Choice> choices = new ArrayList<>();
        for(TextChannel c : channels){
            choices.add(new Command.Choice(c.getName(), c.getId()));
        }
        OptionData channel = new OptionData(OptionType.STRING, "channel", "channel you would like Musik to send all his daily alerts in.", true)
                .addChoices(choices);
        commandData.add(Commands.slash("sac", "Pick the channel to have Musik output his daily recommendations.")
                .addOptions(channel));
        //adding commands to bot
        event.getGuild().updateCommands().addCommands(commandData).queue();
        //spotify api connection
        clientCredentials();
        //assigning default channel for daily song event
        dailyAlertChannelId = event.getGuild().getDefaultChannel().getIdLong();
        //Daily event for sending the song of the day
//        Timer timer = new Timer();
//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                dailySong(event);
//            }
//        };
//        Date today = new Date();
//        timer.scheduleAtFixedRate(task, new Date(today.getYear(), today.getMonth(), today.getDate(), 9, 0), 86400000);
    }

    //Command functions-----------------------------------------------------------------------------------------
    public void recommend(SlashCommandInteractionEvent event){
        //switch case to check if user picked a genre
        OptionMapping option = event.getOption("genre");
        String genre = option.getAsString();
        if(genre.trim().equals("")){
            genre = "random";
        }
        String genreApiCall = getRecs(genre.trim().toLowerCase());
        if(genreApiCall.equals("invalid genre")){
            event.reply( blockQuote + bold + "Invalid genre entered" + bold).setEphemeral(true).queue();
        }
        else{
            event.reply(blockQuote + bold + genreApiCall + bold).queue();
        }
    }

    public void play(SlashCommandInteractionEvent event){
        //check to make sure member is in a voice channel before running the command
        Member member = event.getMember();
        GuildVoiceState memberVoiceState = member.getVoiceState();
        if(!memberVoiceState.inAudioChannel()){
            event.reply(blockQuote + "You need to be in a voice channel to run this command").setEphemeral(true).queue();
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
                event.reply(blockQuote + "You need to be in the same channel as the bot").setEphemeral(true).queue();
                return;
            }
        }
        //conditions checked, play the song provided by user
        PlayerManager playerManager = PlayerManager.get();
        playerManager.play(event.getGuild(), event.getOption("song").getAsString());
        event.reply(blockQuote + ":arrow_forward: Added your song to the queue").queue();
    }

    public void skip(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        //skipping the song
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        guildMusicManager.getTrackScheduler().getPlayer().stopTrack();
        event.reply(blockQuote + ":track_next: Song skipped").queue();
    }

    public void pause(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        guildMusicManager.getTrackScheduler().getPlayer().setPaused(true);
        event.reply(blockQuote + ":pause_button: Song paused").queue();
    }

    public void resume(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        guildMusicManager.getTrackScheduler().getPlayer().setPaused(false);
        event.reply(blockQuote + ":play_pause: Song resumed").queue();
    }
    public void clear(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        guildMusicManager.getTrackScheduler().setRepeating(false);
        guildMusicManager.getTrackScheduler().getQueue().clear();
        guildMusicManager.getTrackScheduler().getPlayer().stopTrack();
        event.reply(blockQuote + "the queue has been cleared").queue();
    }

    public void nowPlaying(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        AudioTrackInfo info = guildMusicManager.getTrackScheduler().getPlayer().getPlayingTrack().getInfo();
        event.reply(blockQuote + "Now playing " + bold + info.title + bold).queue();
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
            event.reply(blockQuote + "No songs are queued up").queue();
        }
        else{
            event.reply(blockQuote + "Coming up next: \n" + bold + reply + bold).queue();
        }
    }
    public void loop(SlashCommandInteractionEvent event){
        checkVoiceState(event);
        GuildMusicManager guildMusicManager = PlayerManager.get().getGuildMusicManager(event.getGuild());
        boolean looping = !guildMusicManager.getTrackScheduler().isRepeating();
        guildMusicManager.getTrackScheduler().setRepeating(looping);
        if(looping){
            event.reply(blockQuote + ":repeat: Looping").queue();
        }
        else{
            event.reply(blockQuote + ":x: Stopped loop").queue();
        }
    }

    public void checkVoiceState(SlashCommandInteractionEvent event){
        //check to make sure member is in a voice channel before running the command
        Member member = event.getMember();
        GuildVoiceState memberVoiceState = member.getVoiceState();
        if(!memberVoiceState.inAudioChannel()){
            event.reply(blockQuote + "You need to be in a voice channel to run this command.").setEphemeral(true).queue();
            return;
        }
        //checking bots state of seeing if it is in a voice channel or not
        Member self = event.getGuild().getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();
        if(!selfVoiceState.inAudioChannel()){
            event.reply(blockQuote + "I am not currently in a voice channel.").setEphemeral(true).queue();
            return;
        }
        //check to ensure bot is in same channel as user
        if(memberVoiceState.getChannel() != selfVoiceState.getChannel()){
            event.reply(blockQuote + "Please join the same voice channel as me to use this command.").setEphemeral(true).queue();
        }
    }

    public void setAlertsChannel(SlashCommandInteractionEvent event){
        dailyAlertChannelId = event.getOption("channel").getAsLong();
        event.reply(blockQuote + "Musik will now send all his daily alerts in " + event.getGuild().getTextChannelById(dailyAlertChannelId).getName()).queue();
    }

    //Spotify API Calls-----------------------------------------------------------------------------------------
    public String getRecs(String genre) {
        //TODO: check to see if token is expired

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
            if(recs.length == 0){
                return "Get Recommendations Command Length Error";
            }
            for (int x = 0; x < recs[0].getArtists().length; x++) {
                artists += recs[0].getArtists()[x].getName() + " ";
            }
            return recs[0].getName() + " by " + artists;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return "Get Recommendations Command API Call Error";
    }

    //Other Helper Methods / Functions-----------------------------------------------------------------------
    public void dailySong(GuildReadyEvent event) {
        event.getGuild().getTextChannelById(dailyAlertChannelId).sendMessage(blockQuote + "Good Morning Gamers!\nToday's Jammer: "
                + getRecs("random")).queue();
    }
}
