package com.hello;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Euler69 {
	
	public static void main(String[] args) throws Exception {
		new Euler69().go();
	}

	final int SIZE = 1000000;
	final int PROCESSORS = 6;
	int[][] primes = new int[SIZE][];

	private void primes(int n) {
		if (primes[n] == null) {
			int m=n;
			Set<Integer> s = new HashSet<Integer>();
			for (int i=2; m>i; i++) {
				if (m%i==0) {
					m /= i;
					s.add(i);
				}
			}
			s.add(m);
			primes[n] = new int[s.size()];
			int i=0;
			for (Integer v : s) {
				primes[n][i++] = v;
			}
		}
	}

	private boolean relPrime(int m, int n) { // m < n
		for (int p : primes[m]) {
			if (n % p == 0)
				return false;
		}
		return true;
	}
	
	public int relPrimes(int n) {
		int count=1;
		for (int i=2; i<n; i++) {
			if (n%i==0)
				continue;
			primes(i); 
			if (relPrime(i, n)) {
				count++;
			}
		}
		return count;
	}

	public int relPrimesParallel(int n, int processors) throws ExecutionException, InterruptedException {
		ExecutorService es = Executors.newFixedThreadPool(processors);
		CompletionService<Boolean> compService = new ExecutorCompletionService<Boolean>(es);
		
		int count=1;
		final int nn = n;
		for (int i=2; i<n; i++) {
			final int ii = i;
			compService.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					primes(ii);
					return relPrime(ii, nn);
				}
			});
		}
		
		for (int i=2; i<n; i++) {
			if (compService.take().get())
				count++;
		}

		es.shutdown();
		return count;
	}
	
	class Task69Result {
		public double ratio;
		public int i;
		public Task69Result(int i, double r) {
			this.i = i;
			ratio = r;
		} 
		
		public boolean bigger(Task69Result r2) {
			return ratio > r2.ratio;
		}

		@Override
		public String toString() {
			return "Task69Result [i=" + i + ", r=" + ratio + "]";
		}
	}
	
	public void go() throws InterruptedException, ExecutionException {
		long start0 = System.currentTimeMillis();
		// initialize primes 
		Task69Result result = new Task69Result(SIZE, SIZE / (double)relPrimesParallel(SIZE, PROCESSORS));
		System.out.println(result);
		long start1 = System.currentTimeMillis();
		System.out.println("time1 " + (start1-start0));		
		start0 = start1;

		ExecutorService es = Executors.newFixedThreadPool(PROCESSORS);
		CompletionService<Task69Result> executor = new ExecutorCompletionService<Task69Result>(es);

		for (int i=SIZE/2; i<SIZE; i++) {
			final int ii=i;
			executor.submit(new Callable<Task69Result>() {
				@Override
				public Task69Result call() {
					double r = ii/(double)relPrimes(ii);
					return new Task69Result(ii, r);
				}
			});
		}
		start1 = System.currentTimeMillis();
		System.out.println("time2 " + (start1-start0));		
		start0 = start1;
		es.shutdown();

		for (int i=SIZE/2; i<SIZE; i++) {
			Future<Task69Result> f = executor.take();
			Task69Result r = f.get();
			if (r.bigger(result)) {
				result = r;
				start1 = System.currentTimeMillis();
				System.out.println((start1-start0) + ": " + result);
				start0 = start1;
			} else if (i % 5000 == 0) {
				start1 = System.currentTimeMillis();
				System.out.println((start1-start0) + ": " + r.i + ": " + result);
				start0 = start1;
			}
		}

		start1 = System.currentTimeMillis();
		System.out.println("time3 " + (start1-start0));		
		start0 = start1;
		System.out.println(result);
	}

}
