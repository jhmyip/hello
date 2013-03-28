package com.hello.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AlarmClock {
	  /** Run the example. */
	  public static void main(String... aArgs) throws InterruptedException {
	    log("Main started.");
	    AlarmClock alarmClock = new AlarmClock(3, 1, 20);
	    alarmClock.activateAlarmThenStop();
	    /*
	    To start the alarm at a specific date in the future, the initial delay
	    needs to be calculated relative to the current time, as in : 
	    Date futureDate = ...
	    long startTime = futureDate.getTime() - System.currentTimeMillis();
	    AlarmClock alarm = new AlarmClock(startTime, 1, 20);
	    This works only if the system clock isn't reset.
	    */
	    log("Main ended.");
	  }
	  
	  AlarmClock(long aInitialDelay, long aDelayBetweenBeeps, long aStopAfter){
	    fInitialDelay = aInitialDelay;
	    fDelayBetweenRuns = aDelayBetweenBeeps;
	    fShutdownAfter = aStopAfter;
	    fScheduler = Executors.newScheduledThreadPool(NUM_THREADS);    
	  }
	  
	  /** Sound the alarm for a few seconds, then stop. */
	  void activateAlarmThenStop(){
	    Runnable soundAlarmTask = new SoundAlarmTask();
	    ScheduledFuture<?> soundAlarmFuture = fScheduler.scheduleWithFixedDelay(
	      soundAlarmTask, fInitialDelay, fDelayBetweenRuns, TimeUnit.SECONDS
	    );
	    Runnable stopAlarm = new StopAlarmTask(soundAlarmFuture);
	    fScheduler.schedule(stopAlarm, fShutdownAfter, TimeUnit.SECONDS);
	  }

	  // PRIVATE 
	  private final ScheduledExecutorService fScheduler;
	  private final long fInitialDelay;
	  private final long fDelayBetweenRuns;
	  private final long fShutdownAfter;
	  
	  /** If invocations might overlap, you can specify more than a single thread.*/ 
	  private static final int NUM_THREADS = 1;
	  private static final boolean DONT_INTERRUPT_IF_RUNNING = false;
	  
	  private static final class SoundAlarmTask implements Runnable {
	    public void run() {
	      ++fCount;
	      log("beep " + fCount);
	    }
	    private int fCount;
	  }
	  
	  private final class StopAlarmTask implements Runnable {
	    StopAlarmTask(ScheduledFuture<?> aSchedFuture){
	      fSchedFuture = aSchedFuture;
	    }
	    public void run() {
	      log("Stopping alarm.");
	      fSchedFuture.cancel(DONT_INTERRUPT_IF_RUNNING);
	      /* 
	       Note that this Task also performs cleanup, by asking the 
	       scheduler to shutdown gracefully. 
	      */
	      fScheduler.shutdown();
	    }
	    private ScheduledFuture<?> fSchedFuture;
	  }
	  
	  private static void log(String aMsg){
	    System.out.println(aMsg);
	  }
}
