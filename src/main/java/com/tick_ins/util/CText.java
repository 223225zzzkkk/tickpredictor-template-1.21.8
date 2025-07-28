package com.tick_ins.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class CText {
    public static void onGameMessage(String s){
        MinecraftClient.getInstance().getMessageHandler().onGameMessage(
                Text.of(s),false
        );
    }
}
