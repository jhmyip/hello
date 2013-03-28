package com.hello;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.hello.CurrencySet.Currency;
import com.hello.CurrencySet.InvalidCurrencyException;

public class PaymentBook {

	private final CurrencySet currencySet = new CurrencySet();
	private final ConcurrentMap<Currency, BigDecimal> book = new ConcurrentHashMap<Currency, BigDecimal>();

	private PaymentBook() {};
	
	private static final PaymentBook instance = new PaymentBook();
	
	public static PaymentBook getInstance() { 
		return instance; 
	}

	public static CurrencySet getCurrencySet() {
		return instance.currencySet;
	}

	public void add(Currency currency, BigDecimal amount) {
		while (amount!=null) {
			// the methods putIfAbsent and replace below are atomic
			BigDecimal oldValue = book.putIfAbsent(currency, amount);
			if (oldValue==null)
				return;
			if (book.replace(currency, oldValue, oldValue.add(amount)))
				return;
		}
	}

	public String getSummary() {
		return getSummary("\n");
	}
	
	public String getSummary(String entryDelimiter) {
		StringBuilder sb = new StringBuilder();
		for (Entry<Currency, BigDecimal> entry : book.entrySet()) {
			if (entry.getValue().compareTo(BigDecimal.ZERO)!=0) {
				sb.append(entry.getKey().getCode()).append(" ").append(entry.getValue());
				BigDecimal usdRate = entry.getKey().getUsdRate(); 
				if (usdRate!=null) {
					BigDecimal usd = entry.getValue().multiply(usdRate);
					sb.append(" (USD ").append(usd.setScale(2, BigDecimal.ROUND_HALF_UP)).append(")");
				}
				sb.append(entryDelimiter);
			}
		}
		return sb.toString();
	}
	
	public void clear() {
		book.clear();
		currencySet.clear();
	}
	
	public int size() {
		return book.size();
	}

	public BigDecimal get(String currencyCode) throws InvalidCurrencyException {
		return book.get(currencySet.get(currencyCode));
	}
	public BigDecimal get(Currency currency) {
		return book.get(currency);
	}

	@Override
	public String toString() {
		return "PaymentCalculator: " + getSummary("; ");
	}
}
