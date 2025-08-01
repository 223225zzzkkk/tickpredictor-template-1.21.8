package com.tick_ins.tick;

import oshi.util.tuples.Pair;

/**
 * 这个类代表的是脚本操作,flag标志位可以用于玩家转向这种单tick只能生效一次并且需要等待的操作
 * 这个类的实例应该被有序地存入缓存，来保证task链的连续
 */
public class RunnableWithFlag {
    private final boolean Flag;
    //Flag作为tick中断标志位，当为true时当前tick不再执行后续task
    private final Runnable Task;
    private final Pair<Float, Float> YawAndPitch;
    public RunnableWithFlag(Runnable task, boolean flag, Pair<Float, Float> yawAndPitch){
        Flag=flag;
        Task=task;
        YawAndPitch = yawAndPitch;
    }
    public RunnableWithFlag(Runnable task, boolean flag){
        Flag=flag;
        Task=task;
        YawAndPitch = null;
    }
    public RunnableWithFlag(Runnable task){
        YawAndPitch = null;
        Flag=false;
        Task=task;
    }

    public Runnable getTask() {
        return Task;
    }

    public boolean isFlag() {
        return Flag;
    }
    public Pair<Float, Float> getYawAndPitch(){return YawAndPitch;}
}
