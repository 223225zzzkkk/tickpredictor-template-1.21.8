package com.tick_ins.mixin;

import com.tick_ins.packet.Ping2Server;
import com.tick_ins.packet.PlayerAction2Server;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "onPlayerActionResponse(Lnet/minecraft/network/packet/s2c/play/PlayerActionResponseS2CPacket;)V",
            at = @At(value = "HEAD"),
            cancellable = true)
    private void task(PlayerActionResponseS2CPacket packet, CallbackInfo ci) {
        if (PlayerAction2Server.PlayerActionResponseTask(packet.sequence())) {
            ci.cancel();
        }
//此处用于接收发送playerAction后服务器每个tick开始发送的sequence响应,如果是对应seq就取消原版客户端逻辑
    }

    @Inject(method = "onPingResult(Lnet/minecraft/network/packet/s2c/query/PingResultS2CPacket;)V",
    at = @At(value = "HEAD"),
    cancellable = true)
    private void pongTask(PingResultS2CPacket packet, CallbackInfo ci){
        if (Ping2Server.PongTask(packet.startTime())){
            ci.cancel();
        }
    }
    //TODO 不知道在什么时候启动tick
}
