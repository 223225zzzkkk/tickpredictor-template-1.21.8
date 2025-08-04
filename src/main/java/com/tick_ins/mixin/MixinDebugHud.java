package com.tick_ins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tick_ins.packet.Ping2Server;
import com.tick_ins.packet.PlayerAction2Server;
import com.tick_ins.tick.TickThread;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(DebugHud.class)
public class MixinDebugHud {
    @ModifyReturnValue(method = "getLeftText()Ljava/util/List;",
            at = @At(value = "RETURN"))
    private List<String> addRtt(List<String> original) {
        if (PlayerAction2Server.getTickInterval()!=0){
            original.set(2, original.get(2) + " [rtt: %d][tps: %d]".formatted(
                    Ping2Server.getRtt(),
                    1000 / PlayerAction2Server.getTickInterval()

            ));
        }

        return original;
    }
}
