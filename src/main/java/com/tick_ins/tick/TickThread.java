package com.tick_ins.tick;

import com.tick_ins.packet.Ping2Server;
import com.tick_ins.packet.PlayerAction2Server;
import com.tick_ins.util.CText;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class TickThread {
    //定义一个单线程来运行tick模拟
    private static ScheduledExecutorService scheduler;
    private static final Queue<RunnableWithFlag> safeInputQueue = new ConcurrentLinkedQueue<>();
    private static volatile boolean isStart = false;

    public static void start() {
        isStart = true;
        scheduler = Executors.newScheduledThreadPool(1);
        //在开始执行tick之前应该先将延迟，延迟波动，丢包率和服务器tick都测试出来
        scheduler.execute(() -> {
            Ping2Server.testPing();//获取延迟和波动
            PlayerAction2Server.testTick();//获取延迟和波动
            tick();
        });
    }

    private static void tick() {
        long targetTimeStamp = PlayerAction2Server.getTickTiming();
        int tickInterval = PlayerAction2Server.getTickInterval();
        while (!scheduler.isShutdown()) {
            long now = System.currentTimeMillis();
            if (now < targetTimeStamp) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(targetTimeStamp - now));
                if (PlayerAction2Server.isTickReady()) {
                    targetTimeStamp = PlayerAction2Server.getTickTiming();
                    tickInterval = PlayerAction2Server.getTickInterval();
//                    CText.onGameMessage("1tick时间:%d".formatted(tickInterval));
                    PlayerAction2Server.consumedTickData();//表示tick数据已被消耗
                }
                Ping2Server.updateRtt(); // ping和tick由各自的类维护 tick负责调用更新即可
                PlayerAction2Server.updateTickStamp();
            } else {
                targetTimeStamp = targetTimeStamp + tickInterval;//TODO 单机状态下拒绝接收数据包问题
                RunnableWithFlag runnable;
                while ((runnable = safeInputQueue.poll()) != null) {
                    runnable.getTask().run();
                    if (runnable.isFlag()) {
                        break;
                    }
                }

            }


        }
    }

    //添加task缓存
    public static void addTask(RunnableWithFlag runnableList) {
        safeInputQueue.add(runnableList);
    }

    //重载
    public static void addTask(RunnableWithFlag... runnableList) {
        safeInputQueue.addAll(Arrays.asList(runnableList));
    }

    public static void shotDown() {
//        CText.onGameMessage("shotDown1");
        scheduler.shutdown();
        safeInputQueue.clear();//因为是静态变量，所以需要手动清除缓存
        isStart = false;
        PlayerAction2Server.consumedTickData();//在初始化时不会被赋值
//        CText.onGameMessage("shotDown2");
    }

    public static boolean isStart() {
        return isStart;
    }

}
