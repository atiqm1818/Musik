package com.atiqm.Musik;

import com.atiqm.Musik.commands.CommandManager;
import com.atiqm.Musik.listeners.EventListener;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import javax.security.auth.login.LoginException;
/**
 * Musik is an interactive music bot that can give you recommendations and keeps
 * a server lively with daily song recommendations.It uses the JDA Java Wrapper
 * class along with Spotify's API to do so.
 *
 * @author  Muhammad Atiq
 */
public class Musik{
    private final ShardManager shardManager;
    private final Dotenv config;
    public Musik() throws LoginException {
        config = Dotenv.configure()
                .directory("src/main/assets")
                .filename(".env")
                .load();
        String token = config.get("TEST_BOT_TOKEN");
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.listening("some jammers"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES);
        shardManager = builder.build();

        shardManager.addEventListener(new EventListener());
        shardManager.addEventListener(new CommandManager());
    }
    public ShardManager getShardManager(){return shardManager;}
    public Dotenv getConfig(){return config;}
    public static void main(String[] args){
        try{
            Musik bot = new Musik();
        }
        catch (LoginException e){
            System.out.println("ERROR: Bot token invalid");
        }
    }
}