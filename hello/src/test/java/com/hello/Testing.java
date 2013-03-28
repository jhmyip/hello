package com.hello;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.internal.util.ArrayUtils;

public class Testing {
	public static class A {
		public static void m() {
			System.out.println("A");
		}
		public void m1() {
			System.out.println("A1");
		}
	}
	
	public static class B extends A {
		public static void m() {
			System.out.println("B");
		}
		@Override
		public void m1() {
			System.out.println("B1");
		}
	}
	
	@Test
	public void test1() {
		A a = new A();
		B b = new B();
		A ab = (A)b;
		a.m();
		b.m();
		ab.m();
		
		a.m1();
		b.m1();
		ab.m1();
	}
	
	@Test
	public void test2() {
		String regex = "A.*B";

		String sample1 = "AXXBAXXBAXXBAXXCAXXBAXXBAXXB";
		String sample2 = "AXXXAXXBAXXBAXXBAXXBAXXBAXXY";

		int N = 5000000;

		long start = System.currentTimeMillis();
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher("");
		for (int i=0; i<N; i++) {
			Assert.assertTrue(m.reset(sample1).matches());
			Assert.assertFalse(m.reset(sample2).matches());
		}
		System.out.println("time0 " + (System.currentTimeMillis()-start));

		start = System.currentTimeMillis();
		for (int i=0; i<N; i++) {
			Assert.assertTrue(p.matcher(sample1).matches());
			Assert.assertFalse(p.matcher(sample2).matches());
		}
		System.out.println("time1 " + (System.currentTimeMillis()-start));

		start = System.currentTimeMillis();
		for (int i=0; i<N; i++) {
			Assert.assertTrue(Pattern.matches(regex, sample1));
			Assert.assertFalse(Pattern.matches(regex, sample2));
		}
		System.out.println("time2 " + (System.currentTimeMillis()-start));

		start = System.currentTimeMillis();
		for (int i=0; i<N; i++) {
			Assert.assertTrue(sample1.matches(regex));
			Assert.assertFalse(sample2.matches(regex));
		}
		System.out.println("time3 " + (System.currentTimeMillis()-start));
	}
	
	@Test
	public void test3() {
		int N = 5000000;

		long start = System.currentTimeMillis();
		long sum = 0;
		for (int i=0; i<N; i++) {
			if (i>0)
				sum += i;
		}
		System.out.println("time0 " + (System.currentTimeMillis()-start));
		Assert.assertTrue(sum>0);
		System.out.println(sum);
		
		start = System.currentTimeMillis();
		Long sum1 = 0l;
		for (Integer i=0; i<N; i++) {
			if (i>0)
				sum1 += i;
		}
		System.out.println("time1 " + (System.currentTimeMillis()-start));
		System.out.println(sum1);

		start = System.currentTimeMillis();
		BigInteger sum2 = BigInteger.ZERO;
		for (int i=0; i<N; i++) {
			if (i>0)
				sum2 = sum2.add(BigInteger.valueOf(i));
		}
		System.out.println("time2 " + (System.currentTimeMillis()-start));
		System.out.println(sum2);
		
		Assert.assertEquals(sum, sum1.longValue());
		Assert.assertEquals(sum, sum2.longValue());
	}
	
	@Test
	public void testEuler1() {
		int m3 = 0;
		int m5 = 0;
		int sum=0;
		int N = 1000;
		
		while (m3 < N-3) {
			m3 += 3;
			sum += m3;
		}

		while (m5 < N - 5) {
			m5 += 5;
			if (m5 % 3 != 0) {
				sum += m5;
			}
		}
		
		System.out.println(sum);
	}

	@Test
	public void testEuler2() {
		int i1 = 1;
		int i2 = 2;
		int sum = 0;
		int N = 4000000;
		
		while (i2 < N) {
			if (i2 % 2 == 0)
				sum += i2;
			int t = i2;
			i2 += i1;
			i1 = t;
		}
		System.out.println(sum);
	}
	
	@Test
	public void testEuler3() {
		long N = 6857l; //600851475143l;
		long n = N; //;
		long p = 1;
		long f=1;
		for (int i = 2; n > i; i++) {
			if (n % i == 0) {
				n = n / i;
				System.out.println(i);
				p = p * i;
				f = i;
			}
		}
		System.out.println(n);
		System.out.println("::" + f);
		
		Assert.assertEquals(N, p * n);
	}
	
}
