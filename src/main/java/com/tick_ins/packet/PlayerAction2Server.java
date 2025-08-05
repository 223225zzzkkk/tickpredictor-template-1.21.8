package com.tick_ins.packet;

import com.tick_ins.util.CText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerAction2Server {
    private static long lastSeq;
    private static final Lock lock = new ReentrantLock();
    private static final Condition condition = lock.newCondition();
    public static final Map<Integer, Long> mapSeq = new TreeMap<>();
    private static final BlockPos blockPos = new BlockPos(999, 999, 999);
    private static volatile int tickInterval;
    private static volatile long tickTiming;
    private static volatile boolean isTickDateReady = false;
    private static final ExecutorService tickTimingUpdateThread = Executors.newSingleThreadExecutor();
    private static long lastUpdatedTimestamp;
    private static volatile boolean isTaskRunning = false;

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
            send(sequence);
            mapSeq.put(sequence, 0L);
            if (i == times - 1) {
                lastSeq = sequence;
            }
            sequence++;//数据包序号增加

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

    public static void testTick() {
        PlayerAction2Server.send(997, 20, 25);
        if (waitSendCallBack(1)) {
            return;
        }
        TickTiming(Ping2Server.getRtt());
    }

    //用于接收和处理send引起的数据包
    //TODO 是否需要考虑丢包的情况
    public static boolean PlayerActionResponseTask(int seq) {
        Long l = mapSeq.get(seq);
        if (l != null) {
            long now = System.currentTimeMillis();
            mapSeq.put(seq, now);
            if (lastSeq == seq) {
                lastSeq = 0;
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

    public static void updateTickStamp() {
        if (isTaskRunning || MinecraftClient.getInstance().isPaused()) {
            return;
        }
        isTaskRunning = true;
        tickTimingUpdateThread.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                if (now - lastUpdatedTimestamp > 60000) {
                    PlayerAction2Server.send(1000, 6, 35);
                    if (waitSendCallBack(1)) {
                        return;
                    }
                    isTickDateReady = TickTiming(Ping2Server.getRtt());
                    //仅在更新方法内需要,保证数据线程安全
                } else {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(60000 - (now - lastUpdatedTimestamp)));
                }
            } finally {
                isTaskRunning = false;
            }


        });

    }

    //这个方法用于控制lastTime变量的超时时间
    public static boolean waitSendCallBack(int seconds) {
        if (lastSeq != 0) {
            lock.lock();
            try {
                boolean notTimeout = condition.await(seconds, TimeUnit.SECONDS);
                if (!notTimeout) {
                    // 超时逻辑
                    lastSeq = 0;

                    if (MinecraftClient.getInstance().isPaused()) {
                        mapSeq.clear();
                        tickTiming = System.currentTimeMillis();
                        tickInterval = 50;
                        return true;
                    }
                    CText.onGameMessage("tick超时");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }

        }
        return false;
    }

    //这个方法用于根据mapTree和rtt求出目标tick以及tick速率
    public static boolean TickTiming(long rtt) {
        long sumTickIntervals = 0;
        int TickCount = 0;
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
                sumTickIntervals += timeTick;
                TickCount++;
            }

            // 更新前一个有效条目
            previousEntry = currentEntry;
            iterator.remove();
        }
        if (previousEntry == null) {
            return false;//单人客户端处于暂停状态(按ESC后)，不会接收actionResponse(猜测,未验证)
        }//  public boolean isPaused() {return this.paused;}
        long startTickTiming = previousEntry.getValue() - rtt;//减去往返时间
        long avgTickInterval = 250;//这么高的tick真的还能玩吗(bushi
        if (TickCount == 0) {
            CText.onGameMessage("%s".formatted(previousEntry));
            CText.onGameMessage("Fail:tick too slow");
        } else {
            avgTickInterval = sumTickIntervals / TickCount;//tick的间隔
        }
//        CText.onGameMessage("%d-%d".formatted(TickCount, avgTickInterval));
        while (startTickTiming < System.currentTimeMillis()) {
            startTickTiming = startTickTiming + avgTickInterval;
        }
        tickTiming = startTickTiming;
        tickInterval = Math.toIntExact(avgTickInterval);
        lastUpdatedTimestamp = System.currentTimeMillis();
        return true;
    }

    public static long getTickTiming() {
        return tickTiming;
    }

    public static int getTickInterval() {
        return tickInterval;
    }

    public static boolean isTickDateReady() {
        return isTickDateReady;
    }

    public static void consumedTickData() {
        isTickDateReady = false;
    }
}
