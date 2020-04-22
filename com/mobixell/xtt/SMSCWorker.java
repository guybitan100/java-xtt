package com.mobixell.xtt;

public interface SMSCWorker
{
    public void setStop();
    // This Function is here so it's possible to access
    // the join function on Thread (which all the Workers
    // have to implement) directly on the SMSCWorker without
    // the need to ClassCast it into a Thread first
    public void join() throws java.lang.InterruptedException;
    public void join(long millis) throws java.lang.InterruptedException;
    public int getWorkerId();
    public int getInstances();
}
