package com.tick_ins.packet;

import com.tick_ins.util.CText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.TreeMap;

public class Ping2Server {
    private static final Map<Long, Long> mapPing = new TreeMap<>();
    private static long lastTime;

    //ping包将被用来计算延迟
    public static void send() {
        MinecraftClient.getInstance().getNetworkHandler().
                sendPacket(new QueryPingC2SPacket(Util.getMeasuringTimeMs()));

    }

    // 带延迟的重载方法
    public static void send(int times, long delayMillis) {
        for (int i = 0; i < times; i++) {
            long startTime = Util.getMeasuringTimeMs();
            MinecraftClient.getInstance().getNetworkHandler().
                    sendPacket(new QueryPingC2SPacket(startTime));
            //将当前时间存入map中作为键
            mapPing.put(startTime, 0L);
            if (i == times - 1) {
                lastTime = startTime;
                break;
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

    public static Pair<Long, Long> testPing() {
        int pingTimes=10;
        Ping2Server.send(pingTimes, 50);
        while (lastTime != 0) {
            //检测超时 1秒
            if ((Util.getMeasuringTimeMs() - lastTime) > 1000) {
                lastTime=0;
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {

            }
        }
        int losePing = 0;
        long sumRtt = 0;
        long sumAbsDiff = 0;
        int rttTimes = 0;
        int diffTimes=0;
        long currentDealy=0;
        for (Map.Entry<Long, Long> entry : mapPing.entrySet()) {
            Long pingTime = entry.getKey();
            Long pongTime = entry.getValue();
            if (pongTime == 0) {
                losePing++;//丢包数
                continue;
            }
            long preDealy=pongTime - pingTime;//单次往返延迟
            sumRtt += preDealy;//往返延迟的和
            rttTimes++;//往返次数
            if (currentDealy!=0){
                sumAbsDiff+=Math.abs(preDealy-currentDealy);//延迟波动的和
                diffTimes++;
            }
            currentDealy=preDealy;
        }
        long dealy = sumRtt/rttTimes;
        long Diff =sumAbsDiff/diffTimes;


         if (losePing==0){
             double result = (double)losePing/pingTimes;
             CText.onGameMessage("丢包率%.2f".formatted(result));
         }
         mapPing.clear();
        return new Pair<Long, Long>(dealy,Diff);
    }

    //用于接收和处理send引起的数据包
    //Ping发送过去的startTime将会被服务器Pong返回
    public static boolean PongTask(Long startTime) {
        //获取当前时间戳
        Long l2 = mapPing.get(startTime);//从map中根据startTime获取value
        if (l2 != null) {
            long currentTime = Util.getMeasuringTimeMs();
            mapPing.put(startTime, currentTime);
            if (startTime == lastTime) {
                lastTime = 0L;
            }
            return true;
        }

        return false;
    }
}
