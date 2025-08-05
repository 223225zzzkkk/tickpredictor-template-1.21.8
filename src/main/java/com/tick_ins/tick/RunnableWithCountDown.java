package com.tick_ins.tick;

public class RunnableWithCountDown {
    private final Runnable task;
    private int count;
    public RunnableWithCountDown(Runnable task,int count) {
        this.task = task;
        this.count=count;
    }

    public int getCount() {
        count=count-1;
        return count;
    }

    public Runnable getTask() {
        return task;
    }

    public static class Builder{
        private Runnable  task;
        private int count=1;


        public Builder setCount(int count){
            this.count =count;
            return this;
        }
        public RunnableWithCountDown build(Runnable task) {
            if (count<1){
                count=1;
            }
            return new RunnableWithCountDown(task,count) ;
        }
    }
}
