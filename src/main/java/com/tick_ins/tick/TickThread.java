package com.tick_ins.tick;

import com.tick_ins.packet.Ping2Server;
import com.tick_ins.packet.PlayerAction2Server;
import com.tick_ins.util.CText;
import net.minecraft.util.Pair;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public  class  TickThread {
    //定义一个单线程来运行tick模拟
    private static  ScheduledExecutorService scheduler;
    private static final Queue<RunnableWithFlag> safeInputQueue = new ConcurrentLinkedQueue<>();
    private static boolean isStart=false;
    public static void start() {
        isStart=true;
        scheduler= Executors.newScheduledThreadPool(1);
        //在开始执行tick之前应该先将延迟，延迟波动，丢包率和服务器tick都测试出来
        scheduler.execute(() -> {
            Pair<Long, Long> delayAndDiff = Ping2Server.testPing();//获取延迟和波动
            Pair<Long, Long> startTimeAndTickDealy = PlayerAction2Server.testTick(delayAndDiff);//获取延迟和波动
            CText.onGameMessage("延迟波动目前没利用，波动为%.2fs".formatted(delayAndDiff.getRight() / 1000.0));
            tick(delayAndDiff, startTimeAndTickDealy);
        });
    }

    private static void tick(Pair<Long, Long> delayAndDiff, Pair<Long, Long> startTimeAndTickDealy) {
        while (startTimeAndTickDealy.getLeft() < System.currentTimeMillis()) {
            startTimeAndTickDealy.setLeft(startTimeAndTickDealy.getLeft() + startTimeAndTickDealy.getRight());
        }
        long targetTime = startTimeAndTickDealy.getLeft();
        long tickDealy = startTimeAndTickDealy.getRight();
        while (!scheduler.isShutdown()) {
            try {
                long now = System.currentTimeMillis();
                if (now < targetTime) {
                    Thread.sleep(targetTime - now);
                } else {
                    targetTime = targetTime + tickDealy;//TODO 目前还没有写动态调整的代码，根据实时的tick和往返延迟
                    RunnableWithFlag runnable;//TODO 没法调试打断点 因为依赖当前时钟
                    while ((runnable = safeInputQueue.poll()) != null) {
                        runnable.getTask().run();
                        if (runnable.isFlag()) {
                            break;
                        }
                    }

                }


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
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
        CText.onGameMessage("shotDown1");
        scheduler.shutdown();
        safeInputQueue.clear();//因为是静态变量，所以需要手动清除缓存
        isStart=false;
        CText.onGameMessage("shotDown2");
    }
    public static boolean isStart(){
        return isStart;
    }

}
