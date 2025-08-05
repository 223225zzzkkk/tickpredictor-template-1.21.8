package com.tick_ins.packet;

import com.tick_ins.util.CText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class Ping2Server {
    private static final Map<Long, Long> mapPing = new TreeMap<>();
    private static long lastTime;
    private static final Lock lock = new ReentrantLock();
    private static final Condition condition = lock.newCondition();
    private static long backupRtt;     // 备用 RTT 值
    private static volatile int rtt;
    private static final ExecutorService rttUpdateThread = Executors.newSingleThreadExecutor();
        private static long lastPingTimeStamp ;

    //ping包将被用来计算延迟
    public static void send(long pingTime) {
        MinecraftClient.getInstance().getNetworkHandler().
                sendPacket(new QueryPingC2SPacket(pingTime));

    }

    // 带延迟的重载方法
    public static void send(int count, long delayMillis) {
        for (int i = 0; i < count; i++) {
            long pingTime = Util.getMeasuringTimeMs();//这里用的是客户端本身的实现，获取的是相对时间，仅用来测试ping和pong的间隔
            send(pingTime);
            //将当前时间存入map中作为键
            mapPing.put(pingTime, 0L);
            if (i == count - 1) {
                lastTime = pingTime;
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

    public static void updateRtt() {
        rttUpdateThread.execute(() -> {
            long now = System.currentTimeMillis();
            if (now - lastPingTimeStamp > 15000) {
                //----初始化操作 ----
                lastTime = 0;
                //----初始化操作 ----
                Ping2Server.send(3, 50);
                waitSendCallBack(300, TimeUnit.MILLISECONDS);
                long sumRtt = 0;//往返延迟和
                int rttCount = 0;
                for (Map.Entry<Long, Long> entry : mapPing.entrySet()) {
                    Long pingTimestamp = entry.getKey();
                    Long pongTimestamp = entry.getValue();
                    long rtt = pongTimestamp - pingTimestamp;//单次往返延迟
                    sumRtt += rtt;//往返延迟的和
                    rttCount++;//往返次数
                }
                long avgRtt;
                if (rttCount != 0) {
                    avgRtt = sumRtt / rttCount;
                } else {
                    CText.onGameMessage("全部丢包,无法得出延迟,使用备用rtt%d".formatted(backupRtt));
                    avgRtt = backupRtt;
                }
                rtt = Math.toIntExact(avgRtt);
                mapPing.clear();
//                CText.onGameMessage("ping测试结束" + avgRtt + "ms");
                lastPingTimeStamp=System.currentTimeMillis();

            } else {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos( 15000-(now-lastPingTimeStamp)));
            }


        });


    }

    public static void testPing() {
        //----初始化操作 ----
        lastTime = 0;
        //----初始化操作 ----

        int pingCount = 10;
        Ping2Server.send(pingCount, 50);
        waitSendCallBack(1, TimeUnit.SECONDS);
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

            previousRtt = rtt;// 如果rtt始终为0(本地),会导致除0错误（已经改为-1）
        }
//        CText.onGameMessage("开始除%d===%d".formatted(rttCount, jitterCount));
        long avgRtt;
        if (rttCount != 0) {
            avgRtt = sumRtt / rttCount;
        } else {
            CText.onGameMessage("全部丢包,无法得出延迟,使用备用rtt%d".formatted(backupRtt));
            avgRtt = backupRtt;
        }

//        long avgJitter;
//        if (jitterCount != 0) {
//            avgJitter = sumjitter / jitterCount;
//            CText.onGameMessage("avgJitter=>%dms".formatted(avgJitter));
//        } else {
//            CText.onGameMessage("样本为1,无法得出波动");
//        }


        if (lostCount != 0) {
            double result = (double) lostCount / pingCount;
            CText.onGameMessage("packetLossRate:%.2f".formatted(result));
        }
        mapPing.clear();
//        CText.onGameMessage("avgRtt=>" + avgRtt + "ms");
        rtt = Math.toIntExact(avgRtt);
        lastPingTimeStamp=System.currentTimeMillis();
    }

    //用于接收和处理send引起的数据包
    //Ping发送过去的startTime将会被服务器Pong返回
    public static boolean PongTask(Long pingTime) {
        //获取当前时间戳
        Long l2 = mapPing.get(pingTime);//从map中根据pingTime获取value
        if (l2 != null) {
            if (lastTime == -1) {
                lastTime = 0;
                CText.onGameMessage("pongTask任务因滞后关闭");
                return true;//互相之间可以关闭,防止多线程操作mapTree数组
            }
            long currentTime = Util.getMeasuringTimeMs();
            mapPing.put(pingTime, currentTime);
            if (pingTime == lastTime) {
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

    public static void SetBackUpRtt(PlayerListS2CPacket.Entry receivedEntry, PlayerListEntry currentEntry) {
        if (receivedEntry.profileId().equals(currentEntry.getProfile().getId()) ) {
//            CText.onGameMessage("服务器发送的你的延迟是%d".formatted(receivedEntry.latency()));
            backupRtt = receivedEntry.latency();
        }
    }

    //这个方法用于控制lastTime变量的超时时间
    public static void waitSendCallBack(int timeOut, TimeUnit unit) {
        if (lastTime != 0) {
            lock.lock();
            try {
                boolean notTimeout = condition.await(timeOut, unit);
                if (!notTimeout) {
                    // 超时逻辑
                    lastTime = -1;
                    CText.onGameMessage("ping超时");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }

        }

    }

    public static int getRtt() {
        return rtt;
    }
}
