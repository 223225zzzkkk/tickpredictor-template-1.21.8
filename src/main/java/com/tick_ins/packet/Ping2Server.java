package com.tick_ins.packet;

import com.tick_ins.util.CText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ping2Server {
    private static final Map<Long, Long> mapPing = new TreeMap<>();
    private static long lastTime;
    private static final Lock lock = new ReentrantLock();
    private static final Condition condition = lock.newCondition();

    //ping包将被用来计算延迟
    public static void send(long startTime) {
        MinecraftClient.getInstance().getNetworkHandler().
                sendPacket(new QueryPingC2SPacket(Util.getMeasuringTimeMs()));

    }

    // 带延迟的重载方法
    public static void send(int times, long delayMillis) {
        for (int i = 0; i < times; i++) {
            long startTime = Util.getMeasuringTimeMs();
            send(startTime);
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
                    CText.onGameMessage("报错");
                    break;
                }
            }

        }
    }

    public static Pair<Long, Long> testPing() {
        CText.onGameMessage("ping测试开始");
        int pingTimes = 10;
        CText.onGameMessage("发送");
        Ping2Server.send(pingTimes, 50);
        CText.onGameMessage("结束发送");
        if (lastTime != 0) {
            lock.lock();
            try {
                boolean notTimeout = condition.await(1, TimeUnit.SECONDS);
                if (!notTimeout) {
                    // 超时逻辑
                    CText.onGameMessage("超时");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }

        CText.onGameMessage("开始计算");
        int lostCount = 0;//丢包数
        long sumRtt = 0;//往返延迟和
        long sumjitter = 0;//延迟波动差的和
        int rttCount = 0;
        int jitterCount = 0;
        long previousRtt = -1;//前一次往返延迟
        //从ping为key,pong为value的map中取出元素
        for (Map.Entry<Long, Long> entry : mapPing.entrySet()) {
            Long pingTimestamp = entry.getKey();
            Long pongTimestamp = entry.getValue();
            if (pongTimestamp == 0) {
                lostCount++;//丢包数
                continue;
            }
            long rtt = pongTimestamp - pingTimestamp;//单次往返延迟
            sumRtt += rtt;//往返延迟的和
            rttCount++;//往返次数
            //-------------------波动延迟---------------
            if (previousRtt != -1) {
                sumjitter += Math.abs(rtt - previousRtt);//延迟波动的和
                jitterCount++;
            }
            //-------------------波动延迟---------------

            previousRtt = rtt;//TODO 如果rtt始终为0(本地),会导致除0错误
        }
        CText.onGameMessage("开始除%d===%d".formatted(rttCount,jitterCount));

        long avgRtt = sumRtt / rttCount;
        long avgJitter = sumjitter / jitterCount;


        if (lostCount != 0) {
            double result = (double) lostCount / pingTimes;
            CText.onGameMessage("丢包率%.2f".formatted(result));
        }
        mapPing.clear();
        CText.onGameMessage("ping测试结束" + avgRtt + "ms");
        return new Pair<Long, Long>(avgRtt, avgJitter);
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
                lock.lock(); // 必须先加锁
                try {
                    condition.signal(); // 现在可以安全调用
                } finally {
                    lock.unlock();
                }
            }
            return true;
        }

        return false;
    }
}
