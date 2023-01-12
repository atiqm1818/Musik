package com.atiqm.Musik.listeners;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivityOrderEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListener extends ListenerAdapter {
    @Override //TODO: find way to check if user is listening to spotify / has a listening activity going on
    public void onUserActivityStart(UserActivityStartEvent event) {
        System.out.println(event.getMember().getUser().getAsTag() + " is jamming out");
    }
}
