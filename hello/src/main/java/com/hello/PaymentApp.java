package com.hello;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.Properties;

import com.hello.CurrencySet.Currency;

public class PaymentApp {
	private long periodInMS = 60*1000;
	private long delayInMS = 100;

	public PaymentApp() {
	}

	public long getPeriodInMS() {
		return periodInMS;
	}
	public void setPeriodInMS(long periodInMS) {
		this.periodInMS = periodInMS;
	}
	public long getDelayInMS() {
		return delayInMS;
	}
	public void setDelayInMS(long delayInMS) {
		this.delayInMS = delayInMS;
	}

	public static void main(String[] args) {
		PaymentApp app = new PaymentApp();

		app.initialize("currency.txt");
		app.processing(args, new BufferedReader(new InputStreamReader(System.in)), new BufferedWriter(new OutputStreamWriter(System.out)));
		
		System.exit(0);
	}
	
	public void initialize(String configFile) {
		System.out.println("loading properties from " + configFile);
		Properties prop = new Properties();
		try {
			prop.load(getClass().getClassLoader().getResourceAsStream(configFile));
		} catch (IOException e) {
			return;
		}
		
		String currencyList = prop.getProperty("supported.codes");
		if (currencyList!=null) {
			System.out.println("supported.codes: " + currencyList);
			PaymentBook.getCurrencySet().parse(currencyList);
		}
		
		// setting usd exchange rate
		for (Currency curr : PaymentBook.getCurrencySet().getCurrencies()) {
			String fxCode = curr.getCode()+"/USD";
			String rate = prop.getProperty(fxCode);
			if (rate != null) {
				try {
					curr.setUsdRate(new BigDecimal(rate));
					System.out.println(fxCode + "=" + rate);
				} catch (NumberFormatException e) {
					System.err.println(e.getMessage());
				}
			}
		}
	}
	
	public void processing(String[] args, BufferedReader reader, BufferedWriter writer) {
		
		PaymentManager processor = PaymentManager.getInstance();

		for (String filename : args) {
			System.out.println("processing inputs from " + filename);
			File f = new File(filename);
			try {
				processor.processPaymentsConcurrent(new BufferedReader(new FileReader(f)));
			} catch (FileNotFoundException e) {
				System.err.println(e.getMessage());
			}
		}
		
		processor.processPaymentsConcurrent(reader);
		
		processor.schedulePaymentSummary(delayInMS, periodInMS, writer);
		
		processor.joinAllProcessingThreads();
		
		processor.cancelSchedule();

		try {
			writer.write("Bye\n");
			writer.write(PaymentBook.getInstance().getSummary());
			writer.flush();
		} catch (IOException e) {
		}
	}
}
