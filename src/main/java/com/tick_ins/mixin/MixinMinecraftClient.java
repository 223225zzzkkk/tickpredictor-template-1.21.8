package com.tick_ins.mixin;

import com.tick_ins.tick.TickThread;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Inject(method = "onDisconnected()V",
    at = @At(value = "HEAD"))
    private void disconnect(CallbackInfo ci){
        if (TickThread.isStart()){
            TickThread.shotDown();
        }
    }
}
