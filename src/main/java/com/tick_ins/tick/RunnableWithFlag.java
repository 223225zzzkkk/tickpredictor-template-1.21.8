package com.tick_ins.tick;

/**
 * 这个类代表的是脚本操作,flag标志位可以用于玩家转向这种单tick只能生效一次并且需要等待的操作
 * 这个类的实例应该被有序地存入缓存，来保证task链的连续
 */
public class RunnableWithFlag {
    private final boolean Flag;
    //Flag作为tick中断标志位，当为true时当前tick不再执行后续task
    private final Runnable Task;
    public RunnableWithFlag(Runnable task,boolean flag){
        Flag=flag;
        Task=task;
    }
    public RunnableWithFlag(Runnable task){
        Flag=false;
        Task=task;
    }

    public Runnable getTask() {
        return Task;
    }

    public boolean isFlag() {
        return Flag;
    }
}
