package com.hello;
public class Euler69Simple {
	public static void main(String[] args) {
		double maxR = 0;
		int maxN = 0;
		for (int i=2; i<=1000000; i++) {
			double r = i/(double)relPrimes(i);
			if (r>maxR) {
				maxR=r; maxN=i;
			}
		}
		System.out.println("result n=" + maxN + " ratio=" + maxR);
	}

	static int gcd(int k, int m) {
		while (m != 0) {
			int r = k % m;
			k = m; m = r;
		}
		return k;
	}
	
	static int relPrimes(int n) {
		int count=1;
		for (int i=2; i<n; i++)
			if (gcd(n, i)==1)
				count++;
		return count;
	}
}