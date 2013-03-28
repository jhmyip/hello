package com.hello;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.hello.CurrencySet.Currency;
import com.hello.CurrencySet.InvalidCurrencyException;

public class PaymentManager {
	
	private List<Thread> threads;
	private Timer timer;
	
	private static final PaymentManager instance = new PaymentManager(); 
	public static PaymentManager getInstance() {
		return instance;
	}
	
	private PaymentManager() {
		threads = new ArrayList<Thread>();
	}
	
	public void addProcessingThread(Thread thread) {
		threads.add(thread);
	}
	
	public void joinAllProcessingThreads() {
		for (Thread t : threads) {
			try {
				t.join();
			} catch (Exception e) {
			}
		}
	}

	public void killAllProcessingThreads() {
		for (Thread t : threads) {
			try {
				if (t.isAlive())
					t.interrupt();
			} catch (Exception e) {
			}
		}
	}

	public void processPaymentsConcurrent(final BufferedReader reader) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				processPayments(reader);
			}
		};
		thread.start();
		
		addProcessingThread(thread);
	}
	
	public static class LineFormatException extends Exception {
		private static final long serialVersionUID = 1L;
		public LineFormatException(String msg) { super(msg); }
	}
	
	public static class PaymentEntry {
		private Currency currency;
		private BigDecimal amount;
		public PaymentEntry(Currency currency, BigDecimal amount) {
			this.currency = currency;
			this.amount = amount;
		}
		public Currency getCurrency() {
			return currency;
		}
		public BigDecimal getAmount() {
			return amount;
		}
	}
	
	static public PaymentEntry getPaymentEntry(String line) throws LineFormatException, NumberFormatException, InvalidCurrencyException {
		String[] part = line.split("[ \t]+");
		if (part.length != 2) {
			throw new LineFormatException("Line '" + line + "' format invalid");
		}
		try {
			Currency currency = PaymentBook.getCurrencySet().get(part[0].toUpperCase());
			BigDecimal amount = new BigDecimal(part[1]);
			
			return new PaymentEntry(currency, amount);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Line '" + line + "' number format invalid");
		}
	}

	public void processPayments(final BufferedReader reader) {
		while (true) {
			try {
				String line = reader.readLine();
				if (line==null || line.contains("quit"))
					break;

				PaymentEntry entry = getPaymentEntry(line);
				PaymentBook.getInstance().add(entry.getCurrency(), entry.getAmount());
			} catch (LineFormatException e) {
				System.err.println(e.getMessage());
			} catch (NumberFormatException e) {
				System.err.println(e.getMessage());
			} catch (InvalidCurrencyException e) {
				System.err.println(e.getMessage() + " is not valid currency " + PaymentBook.getCurrencySet());
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
			// ignore entry with exception
		}
	}
	
	public void schedulePaymentSummary(long delayInMS, long periodInMS, final BufferedWriter out) {
		timer = new Timer();
		timer.schedule(
			new TimerTask() {
				@Override
				public void run() { 
					try {
						out.write("Summary:");
						out.newLine();
						out.write(PaymentBook.getInstance().getSummary());
						out.newLine();
						out.flush();
					} catch (IOException e) {
						
					}
				}
			}, 
			delayInMS,
			periodInMS
		);
	}
	
	public void cancelSchedule() {
		timer.cancel();
	}
}
