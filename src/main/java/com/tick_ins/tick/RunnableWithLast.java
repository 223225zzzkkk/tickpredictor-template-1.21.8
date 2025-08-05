package com.tick_ins.tick;

import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 这个类代表的是脚本操作,flag标志位可以用于玩家转向这种单tick只能生效一次并且需要等待的操作
 * 这个类的实例应该被有序地存入缓存，来保证task链的连续
 */
public class RunnableWithLast {
    //Flag作为tick中断标志位，当为true时当前tick不再执行后续task
    private final Runnable Task;
    private final Pair<Float, Float> YawAndPitch;
    private final List<Runnable> cacheTask = new ArrayList<>();
    public RunnableWithLast(Runnable task, Pair<Float, Float> yawAndPitch){
        Task=task;
        YawAndPitch = yawAndPitch;

    }
    public RunnableWithLast(Runnable task, Pair<Float, Float> yawAndPitch,Runnable cacheTask){
        Task=task;
        YawAndPitch = yawAndPitch;
        this.cacheTask.add(cacheTask);

    }
    public RunnableWithLast(Runnable task, Pair<Float, Float> yawAndPitch,List<Runnable> cacheTask){
        Task=task;
        YawAndPitch = yawAndPitch;
        this.cacheTask.addAll(cacheTask);
    }
    public RunnableWithLast(Runnable task){
        Task=task;
        YawAndPitch = null;
    }

    public Runnable getTask() {
        return Task;
    }

    public Pair<Float, Float> getYawAndPitch(){return YawAndPitch;}

    public List<Runnable> getCacheTask() {
        return cacheTask;
    }

    public static class Builder {
        //Flag作为tick中断标志位，当为true时当前tick不再执行后续task
        private Runnable task;
        private  Pair<Float, Float> yawAndPitch;
        private final List<Runnable> cacheTask = new ArrayList<>();
        public Builder setTask(Runnable runnable){
            this.task =runnable;
            return this;
        }
        public Builder setYawAndPitch(Pair<Float, Float> yawAndPitch){
            this.yawAndPitch =yawAndPitch;
            return this;
        }
        public Builder cache(Runnable runnable){
            cacheTask.add(runnable);
            return this;
        }
        public Builder cache(Runnable ... runnableList){
            Collections.addAll(cacheTask, runnableList);
            return this;
        }
        public RunnableWithLast build(){
            return new RunnableWithLast(this.task,this.yawAndPitch,cacheTask);
        }
    }
}
