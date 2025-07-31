package com.tick_ins.mixin;

import com.tick_ins.packet.Ping2Server;
import com.tick_ins.packet.PlayerAction2Server;
import com.tick_ins.util.CText;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.ServerList;
import net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
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
    //服务器会主动发送每个玩家的延迟 但是准确度因服务器而异，将他存储为备用rtt
    @Inject(method = "handlePlayerListAction(Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket$Action;Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket$Entry;Lnet/minecraft/client/network/PlayerListEntry;)V",
    at = @At(value = "HEAD"))
    private void list(PlayerListS2CPacket.Action action, PlayerListS2CPacket.Entry receivedEntry, PlayerListEntry currentEntry, CallbackInfo ci){
        Ping2Server.SetBackUpRtt(receivedEntry,currentEntry);
//        CText.onGameMessage("%s-%d".formatted(receivedEntry.profile().getName(),receivedEntry.latency()));
    }


    //TODO 不知道在什么时候启动tick
}
