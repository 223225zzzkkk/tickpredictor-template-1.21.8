package com.tick_ins.packet;

import com.tick_ins.util.CText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class PlayerAction2Server {
    private static long lastTime;

    public static final Map<Integer, Long> mapSeq = new TreeMap<>();
    private static final BlockPos blockPos = new BlockPos(999, 999, 999);
    //blockPos创建一个较大值，让服务器抛出该操作
    public static void send(int sequence) {
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket
                        (
                                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                                blockPos,
                                Direction.DOWN,
                                sequence
                        )
        );
    }

    // 带延迟的重载方法
    public static void send(int sequence, int times, long delayMillis) {
        for (int i = 0; i < times; i++) {
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(
                    new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                            blockPos,
                            Direction.DOWN,
                            sequence
                    )
            );
            mapSeq.put(sequence, 0L);
            sequence++;//数据包序号增加
            if (i == times - 1 ){
                lastTime =System.currentTimeMillis();
            }
            if (delayMillis > 0) { // 不是最后一次且延迟>0
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
        //TODO 目前mod的主要逻辑之一
    public static Pair<Long, Long> testTick(Pair<Long, Long> pair){
        CText.onGameMessage("tick测试开始");
        PlayerAction2Server.send(997, 10, 25);
        while (lastTime !=0){
            //检测超时 1秒
            if ((System.currentTimeMillis() - lastTime) > 1000) {
                lastTime =0;
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {

            }

        }
        long  sumTick=0;
        int TickTimes=0;
        Iterator<Map.Entry<Integer, Long>> iterator = mapSeq.entrySet().iterator();
        Map.Entry<Integer, Long> previousEntry = null;
        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> currentEntry = iterator.next();

            // 跳过值为0的条目
            if (currentEntry.getValue() == 0L) {
                iterator.remove();
                continue;
            }

            // 如果有前一个有效条目，计算时间差
            if (previousEntry != null) {
                long timeTick = currentEntry.getValue() - previousEntry.getValue();
                sumTick+= timeTick;
                TickTimes++;
            }

            // 更新前一个有效条目
            previousEntry = currentEntry;
            iterator.remove();
        }
        long startTick = previousEntry.getValue()-pair.getLeft();//减去往返时间
        long tickDealy =sumTick/TickTimes;//tick的间隔
        CText.onGameMessage("tick测试结束"+tickDealy+"ms");
        return new Pair<>(startTick,tickDealy);
    }

        //用于接收和处理send引起的数据包
    //TODO 是否需要考虑丢包的情况
    public static boolean PlayerActionResponseTask(int seq) {
        Long l = mapSeq.get(seq);
        if (l != null) {
            mapSeq.put(seq, System.currentTimeMillis());
            return true;
        }
        return false;
    }
}
