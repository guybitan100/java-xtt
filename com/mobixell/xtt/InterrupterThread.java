package com.mobixell.xtt;

public class InterrupterThread extends Thread
{
    private Thread threadToInterrupt = null;
    private int timeout = 0;
    private boolean active = true;

    public InterrupterThread(Thread theThread, int timeout)
    {
        threadToInterrupt = theThread;
        this.timeout = timeout;
        active = true;
    }

    public void run()
    {
        while((active)&&(timeout>0))
        {
            try
            {
                sleep(timeout);
                //System.out.println("Done sleeping, going to interrupt");
                
                //If you reach the end of the wait, interrupt the thread.
                threadToInterrupt.interrupt();
                //We've done our job. Just exit.
                active = false;
            }
            catch(InterruptedException ie)
            {
                //This is caused by the reset or cancel below.
            }
        }
    }

    public void reset()
    {
        //Reset the internal wait.
        this.interrupt();
    }

    public void cancel()
    {
        active = false;
        this.interrupt();
    }
}