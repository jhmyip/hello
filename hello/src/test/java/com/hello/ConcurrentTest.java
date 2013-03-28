package com.hello;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import junit.framework.Assert;

import org.junit.Test;

public class ConcurrentTest {
	
	class TaskResult implements Comparable<TaskResult> {
		public int i;
		public int result;
		public TaskResult(int i, int result) { this.i=i; this.result=result; }
		
		@Override
		public int compareTo(TaskResult o) {
			if (i==o.i) return 0;
			else if (i>o.i) return 1;
			else return -1;
		}

		@Override
		public String toString() {
			return "TaskResult [i=" + i + ", result=" + result + "]";
		}
	}
	
	class Calculator implements Callable<TaskResult> {
		int i;
		public Calculator(int x) { i=x; }

		@Override
		public TaskResult call() throws Exception {
			System.out.println(new Date() + "::processing " + i + " by " + Thread.currentThread().getName());
			int sum = 1;
			for (int j=1; j<=(i%100); j++) {
				sum += j*j;
			}
			return new TaskResult(i, sum);
		}
	}
	
	@Test
	public void testCompletionService() throws InterruptedException, ExecutionException {
		
		long start = System.currentTimeMillis();

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-1);
		ExecutorCompletionService<TaskResult> ecs = new ExecutorCompletionService<TaskResult>(executor);
		for (int i=1; i<=10000; i++) {
			ecs.submit(new Calculator(i));
		}
		System.out.println("t1 " + (System.currentTimeMillis()-start));
		
		executor.shutdown();
		
		System.out.println("t2 " + (System.currentTimeMillis()-start));

		int sum=0;
		for (int i=1; i<=10000; i++) {
			Future<TaskResult> f = ecs.take();
			TaskResult result = f.get();
			sum += result.result;
			System.out.println(">> " + System.currentTimeMillis() + " ::: " + result);
		}
		System.out.println("t3 " + (System.currentTimeMillis()-start));
		System.out.println("sum === " + sum);
	}

	@Test
	public void testInvokeAny() throws InterruptedException, ExecutionException {
		ExecutorService executor = Executors.newFixedThreadPool(4);

		Set<Callable<TaskResult>> tasks = new HashSet<Callable<TaskResult>>();
		
		for (int i=1; i<=100; i++) {
			tasks.add(new Calculator(i));
		}
		
		TaskResult result = executor.invokeAny(tasks);
		Assert.assertNotNull(result);
		executor.shutdown();

		System.out.println("result === " + result);
	}
	
	@Test
	public void testInvokeAll() throws InterruptedException, ExecutionException {
		ExecutorService executor = Executors.newFixedThreadPool(4);

		List<Callable<TaskResult>> tasks = new ArrayList<Callable<TaskResult>>();
		long start = System.currentTimeMillis();

		for (int i=1; i<=100; i++) {
			tasks.add(new Calculator(i));
		}
		System.out.println("t1 " + (System.currentTimeMillis()-start));

		List<Future<TaskResult>> results = executor.invokeAll(tasks);
		Assert.assertNotNull(results);
		System.out.println("t2 " + (System.currentTimeMillis()-start));
		executor.shutdown();

		for (Future<TaskResult> f : results) {
			System.out.println("result === " + f.get());
		}
		System.out.println("t3 " + (System.currentTimeMillis()-start));
	}

	@Test
	public void testScheduledExec() throws InterruptedException, ExecutionException {
		ScheduledExecutorService fScheduler = Executors.newScheduledThreadPool(4);
		
		List<ScheduledFuture<TaskResult>> results = new ArrayList<ScheduledFuture<TaskResult>>(); 

		long start = System.currentTimeMillis();

		for (int i=1; i<=100; i++) {
			results.add(fScheduler.schedule(new Calculator(i), i/5, TimeUnit.SECONDS));
		}
		System.out.println("t1 " + (System.currentTimeMillis()-start));
		fScheduler.shutdown();

		for (ScheduledFuture<TaskResult> f : results) {
			System.out.println("result === " + f.get() + " " + f.getDelay(TimeUnit.MILLISECONDS));
		}
		System.out.println("t3 " + (System.currentTimeMillis()-start));
	}
	
	@Test
	public void testCountDown() throws InterruptedException {
	     final CountDownLatch doneSignal = new CountDownLatch(4);

	     new Thread(new Runnable() {
			@Override
			public void run() {
			    System.out.println(Thread.currentThread().getName());
			    for (int i=0; i<4; i++) {
			    	try {
						Thread.sleep(1000);
						System.out.println(new Date());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			    	doneSignal.countDown();
			    }
			}
	     }).start();
	     
	     System.out.println("waiting");
	     doneSignal.await();
	     
	     System.out.println(new Date() + " done");
	}
	
	@Test
	public void testCountDown2() throws InterruptedException {
	     final CountDownLatch doneSignal = new CountDownLatch(1);

	     for (int i=0; i<4; i++)
		     new Thread(new Runnable() {
				@Override
				public void run() {
				    System.out.println(Thread.currentThread().getName() + " ready");
			    	try {
						doneSignal.await();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				    System.out.println(Thread.currentThread().getName() + " go");
				}
		     }).start();

	     System.out.println(new Date() + " go ");
	     
	     doneSignal.countDown();
	     
	     System.out.println(new Date() + " done");
	}

	@Test
	public void testPriorityQueue() throws Exception {
		final CountDownLatch startSignal = new CountDownLatch(1);
		final PriorityBlockingQueue<TaskResult> results = new PriorityBlockingQueue<TaskResult>();
		ExecutorService executor = Executors.newSingleThreadExecutor();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				for (int i=100; i>0; i--) {
					try {
						results.put(new Calculator(i).call());
					} catch (Exception e) {
						e.printStackTrace();
					}
					startSignal.countDown();
				}
			}
		});

		startSignal.await();
		
		for (int i=0; i<100; i++) {
			TaskResult r = results.take(); 
			System.out.println("result === " + r + " ::: " + results.size());
			if (i%10==0) {
				Thread.sleep(1);
			}
		}
	}
	
	@Test
	public void testCyclicBarrier() throws Exception {
		int N=5;
		final CyclicBarrier barrier = new CyclicBarrier(N, new Runnable() {
			@Override
			public void run() {
				System.out.println(Thread.currentThread().getName() + " go go go ...");				
			}
		});

		for (int i=N*3+1; i>0; i--) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println(Thread.currentThread().getName() + " ready ");
					try {
						int a = barrier.await();
						System.out.println(Thread.currentThread().getName() + " go " + a);
					} catch (Exception e) {
						System.out.println(Thread.currentThread().getName() + " " + e.toString());
						e.printStackTrace();
					}
				}
			}).start();
		}
		
		Thread.sleep(10);
		System.out.println("done !!");
		
		Assert.assertEquals(1, barrier.getNumberWaiting());
		
		barrier.reset();
	}
	
	@Test
	public void testExchanger() throws Exception {
		final Exchanger<TaskResult> exchanger = new Exchanger<TaskResult>();
		
		for (Integer i : new Integer[] {1, 2, 4, 5, 6} ) {
			final int ii = i;
//			System.out.println(ii + " >>>");
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						TaskResult a = exchanger.exchange(new Calculator(ii).call());
						System.out.println(ii + " " + a);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
		TaskResult a = exchanger.exchange(new Calculator(10).call());
		System.out.println(10 + " " + a);
	}
	
	@Test
	public void testSemaphore() throws Exception {
		final Semaphore available = new Semaphore(4, true);
		ExecutorService executor = Executors.newFixedThreadPool(6);

		for (int i=0; i<22; i++) {
			available.acquire();
			System.out.println(Thread.currentThread().getName() + " >>> " + available.availablePermits());
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						System.out.println(Thread.currentThread().getName() + " " + new Date());
						Thread.sleep(2000);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						available.release();
					}
				}
			});
		}
	}
	
	@Test
	public void testSemaphore2() throws Exception {
		final Semaphore available = new Semaphore(1, true);
		ExecutorService executor = Executors.newFixedThreadPool(6);

		System.out.println(Thread.currentThread().getName() + " avail " + available.availablePermits());

		for (int i=0; i<3; i++) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						available.release();
						System.out.println(Thread.currentThread().getName() + " release " + new Date());
						Thread.sleep(2000);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
					}
				}
			});
		}
			
		Thread.sleep(2000);

		System.out.println(Thread.currentThread().getName() + " avail " + available.availablePermits());

		for (int i=0; i<3; i++) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						available.acquire();
						System.out.println(Thread.currentThread().getName() + " acquire " + new Date());
						Thread.sleep(2000);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
					}
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		System.out.println(Thread.currentThread().getName() + " avail " + available.availablePermits());

	}

	@Test
	public void testLock() throws Exception {
		final ReentrantLock lock = new ReentrantLock();

		System.out.println(Thread.currentThread().getName() + " lock " + lock.getQueueLength() + " " + lock.hasQueuedThreads());

		for (int i=0; i<3; i++)
			new Thread(new Runnable() {
				@Override
				public void run() {
					lock.lock();
					System.out.println(Thread.currentThread().getName() + " lock " + lock.getHoldCount());
				}
			}).start();

		Thread.sleep(500);
		
		System.out.println(Thread.currentThread().getName() + " lock " + lock.getQueueLength() + " " + lock.hasQueuedThreads());
		Assert.assertEquals(2, lock.getQueueLength());
		
	}
	
	enum RWType { Reader, Writer }

	@Test
	public void testReadWriteLock() throws Exception {
		ReentrantReadWriteLock fLock = new ReentrantReadWriteLock();
		Lock fReadLock = fLock.readLock();
		Lock fWriteLock = fLock.writeLock();
		
		ExecutorService executor = Executors.newFixedThreadPool(5);

		class WriterReader implements Runnable {
			int n;
			Lock lock;
			WriterReader(Lock k, int i) { lock = k; n = i; }
			@Override
			public void run() {
				lock.lock();
				try {
					System.out.println(lock.getClass().getSimpleName() + n + " " + Thread.currentThread().getName() + " " + new Date());
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				lock.unlock();
			}
		} 
		
		for (int i=0; i<10; i++) {
			executor.execute(new WriterReader(fWriteLock, i));
			executor.execute(new WriterReader(fReadLock, i));
			executor.execute(new WriterReader(fReadLock, i));
			executor.execute(new WriterReader(fReadLock, i));
			executor.execute(new WriterReader(fReadLock, i));
		}
		
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		
		System.out.println(Thread.currentThread().getName() + " bye " + new Date() );
	}
	
	@Test
	public void testThreadLocal() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(6);
		final ThreadLocal<String> local = new ThreadLocal<String>();
		final Semaphore available = new Semaphore(3, true);

		for (int i=1; i<=10; i++) {
			final int ii = i;
			executor.submit(new Runnable() {
				@Override
				public void run() {
					String val = Thread.currentThread().getName() + ":" + ii + "::(" + local.get()+")"; 
					local.set(val);
					for (int j=0; j<5; j++) {
						try {
							available.acquire();
							Assert.assertEquals(val, local.get());
							Assert.assertTrue(val.contains(Thread.currentThread().getName()));
							System.out.println(Thread.currentThread().getName() + " " + local.get() + " " + j);
							Thread.sleep((int)(Math.random()*50));
							available.release();
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
				}
			});
		}
		
		executor.shutdown();
		executor.awaitTermination(1000, TimeUnit.SECONDS);
		
	}
	
	class MyFutureTask extends FutureTask<String> {
		int ii;
		public MyFutureTask(final int i) {
			super(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String result = "testing " + i;
					int r = (int)(Math.random()*10);
					System.out.println(result + " : " + r);
					Thread.sleep(r);
					return result;
				}
			});
			ii = i;
		}

		@Override
		protected void done() {
			super.done();
			System.out.println("DONE " + ii);
		}

		@Override
		public void run() {
			super.run();
			System.out.println("RUN " + ii);
		}
	}
	
	@Test
	public void testFutureTask() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(3);

		List<MyFutureTask> futures = new ArrayList<MyFutureTask>();

		for (int i=0; i<5; i++) {
			MyFutureTask task = new MyFutureTask(i);
			futures.add(task);
			executor.submit(task);
		}
		
		executor.shutdown();
		executor.awaitTermination(1000, TimeUnit.SECONDS);
		
		for (MyFutureTask f : futures) {
			System.out.println("Result " + f.isDone() + " " + f.get());
		}
	}

	boolean gogo=true;
	boolean flag=true;

	@Test
	public void testVolatile() throws Exception {
		int N = 2;
		ExecutorService executor = Executors.newFixedThreadPool(N);
		List<Future<Integer>> results = new ArrayList<Future<Integer>>();
		
		final int repeat=20000;
		for (int i=0; i<N; i++) {
			results.add(executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					int count=0;
					boolean oldflag = flag;
					while (gogo) {
						boolean currflag = flag;
						if (currflag!=oldflag)
							count++;
						oldflag = currflag;
					}
					System.out.println(Thread.currentThread().getName() + " count " + count);
					return count;
				}
			}));
		}
		
		Thread.sleep(50);

		for (int i=0; i<repeat; i++) {
			flag = !flag;
			double s=0;
			for (int j=0; j<5000; j++) {
				s += Math.log(j+i);
			}			
			Assert.assertTrue(s!=0);
		}
		
		Thread.sleep(50);
		gogo=false;

		executor.shutdown();
		executor.awaitTermination(1000, TimeUnit.SECONDS);

		System.out.println(Thread.currentThread().getName() + " count " + repeat);

		for (Future<Integer> f : results) {
			Assert.assertEquals(repeat, f.get().intValue());
		}
		
	}
	
	boolean running=true;
	long time1 = 0;

	@Test
	public void testVolatile2() throws Exception {
		
		Thread t1 = new Thread(new Runnable() {
		    public void run() {
		      int counter = 0;
		      double sum=0;
		      while (running) {
		    	  sum += Math.cos(counter);
		    	  counter++;
		      }
		      time1 = System.nanoTime();
		      System.out.println("Thread 1 finished. Counted up to " + counter);
		    }
		});
		
		t1.start();
		
		Thread.sleep(100);
		System.out.println("Thread 2 finishing");
		running = false;
		long time2 = System.nanoTime();
		
		System.out.println("delay " + (time1-time2));

		t1.join();
	}
	
	class Volatile3 {
		int i=0, j=0;
		void one() { i++; j++; }
		void two() { 
			System.out.println("i=" + i + " j=" + j); 
		}
		void diff() {
			System.out.println("j-i=" + (j-i));
		}
	}
	@Test
	public void testVolatile3() throws Exception {
		final Volatile3 v = new Volatile3();
		
		Thread t1 = new Thread(new Runnable() {
		    public void run() {
		    	while (running) {
		    		v.diff();
		    	}
		    }
		});
		t1.start();
		
		for (int i=0; i<10000; i++) {
			v.one();
//			v.diff();
		}
		
		running = false;
		t1.join();
	}
	
	@Test
	public void testThread() throws Exception {
		final Object obj1 = new Object();
		
		Thread t1 = new Thread() {
			@Override
			public void run() {
				try {
					for (int i=0; i<10; i++) {
						System.out.println("test1 " + i);
						synchronized (obj1) {
							System.out.println("test1xx " + i);
							if (i%2==0)
								obj1.wait();
							else
								obj1.notify();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					for (int i=0; i<10; i++) {
						System.out.println("test2 " + i);
						synchronized (obj1) {
							System.out.println("test2xx " + i);
							if (i%2!=0)
								obj1.wait();
							else
								obj1.notify();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		t1.start();
		t2.start();

		t1.join();
		t2.join();
	}
	
	@Test
	public void testJoin() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(5);

		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		t.start();
		
		for (int i=0; i<10; i++) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						System.out.println(Thread.currentThread().getName() + " a");
						t.join();
						System.out.println(Thread.currentThread().getName() + " b");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
		executor.shutdown();
		long a = System.currentTimeMillis();
		executor.awaitTermination(8, TimeUnit.SECONDS);
		System.out.println(Thread.currentThread().getName() + " bye " + (System.currentTimeMillis()-a));
	}
	
	
	class BlockingQueue0 {
		final int SIZE=10;
		int[] v = new int[SIZE];
		int head=0, tail=0;
		
		public synchronized void put(int a) throws InterruptedException {
			try {
				if ((head+1)%10==tail)
					wait();
				v[head] = a;
			} finally {
				head = (++head) % 10;
				notify();
			}
		}
		public synchronized int take() throws InterruptedException {
			try {
				if (head==tail)
					wait();
				return v[tail];
			} finally {
				tail = (++tail) % 10;
				notify();
			}
		}
	}
	
	class BlockingQueue {
		final int SIZE=10;
		int[] v = new int[SIZE];
		int head=0, tail=0;
		
		public synchronized void put(int a) throws InterruptedException {
			try {
				if ((head+1)%10==tail%10)
					wait();
				v[head++ % 10] = a;
			} finally {
				notify();
			}
		}
		public synchronized int take() throws InterruptedException {
			try {
				if (head%10==tail%10)
					wait();
				return v[tail++ % 10];
			} finally {
				notify();
			}
		}
	}

	
	@Test
	public void testThreadQueue() throws Exception {
		final BlockingQueue q = new BlockingQueue();
		
		new Thread() {
			@Override
			public void run() {
				for (int i=0; i<50; i++)
					try {
						q.put(i);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
		}.start();

//		Thread.sleep(1);
		
		for (int i=0; i<50; i++) {
			System.out.println(i + " pre  head " + q.head + " tail " + q.tail);
			System.out.println(q.take());
			System.out.println(i + " post head " + q.head + " tail " + q.tail);
		}
	}
}