package com.mobixell.xtt;

public interface BillingWorker
{

    public void setStop();
    public void join() throws java.lang.InterruptedException;
    public int getWorkerId();
}