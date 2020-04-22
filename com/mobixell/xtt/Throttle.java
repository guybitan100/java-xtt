package com.mobixell.xtt;

/**
 * Implements a throttle that can be used to limit throughput to a fixed
 * number of operations per second. Callers of the "pause" method that
 * attempt to exceed the specified rate will wait for the next available
 * slot. The maximum number of threads that are allowed to wait can be
 * specified on the constructor and an exception is thrown when this
 * would be exceeded.
 *
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: 724 Solutions Inc.</p>
 * @author Andrew Schofield
 * @version $Id: Throttle.java,v 1.3 2006/08/24 07:34:39 gcattell Exp $
 */

public class Throttle
{
    private final static long nanosPerMilli = 1000000;
    private String name;
    private int maxWaiters = -1;
    private int nrWaiters;
    private long delayNanos;
    private long interval = 1000; //1000 milliseconds
    private long nextSlotNanos = System.currentTimeMillis() * nanosPerMilli;
    
    /**
     * Constructor.
     *
     * @param name                 is the throttle name, used for diagnosics.
     * @param operationsPerInterval  is the allowable number of operations per second.
     */
    public Throttle(String name, int operationsPerInterval) {
        this.name = name;
        this.maxWaiters = -1;
        
        this.delayNanos = (nanosPerMilli * interval) / operationsPerInterval;
    }
    
    /**
     * Constructor specifying the interval.
     *
     * @param name                 the throttle name, used for diagnosics.
     * @param operationsPerInterval  the allowable number of operations per second.
     * @param interval  the interval over which to limit.
     */    
    public Throttle(String name, int operationsPerInterval, int interval) {
        this.name = name;
        this.maxWaiters = -1;
        this.interval = interval;
        
        this.delayNanos = (nanosPerMilli * interval) / operationsPerInterval;
    }
      
    /**
     * Holds the calling thread until an execution slot is available.
     *
     * @throws TooManyWaitingThreadsException
     */
    public synchronized void pause() throws TooManyWaitingThreadsException
    {
        if (nrWaiters == maxWaiters)
        {
            throw new TooManyWaitingThreadsException("Max allowed waiters on throttle '" + name + "' is " +maxWaiters);
        }
        long nowNanos = System.currentTimeMillis() * nanosPerMilli;
        long waitNanos = nextSlotNanos - nowNanos;
        if (waitNanos <= 0)
        {
            nextSlotNanos = nowNanos + delayNanos;
        }
        else
        {
            nextSlotNanos += delayNanos;
            nrWaiters++;
            try
            {
                  wait(waitNanos / nanosPerMilli, (int) (waitNanos % nanosPerMilli));
            }
            catch (InterruptedException e)
            {
                // just let it go anyway
            }
           nrWaiters--;
        }
    }
    
    /**
     * Too many calling threads
     */
    public class TooManyWaitingThreadsException extends java.lang.Exception
    {
        TooManyWaitingThreadsException(String message)
        {
            super(message);
        }
    }
}