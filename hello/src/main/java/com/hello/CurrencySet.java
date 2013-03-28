package com.hello;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CurrencySet {
	public static class Currency {
		private String code;
		private BigDecimal usdRate;
		
		private Currency() {}
		private Currency(String code) { 
			this.code = code; 
		}
		public String getCode() {
			return code;
		}
		public BigDecimal getUsdRate() {
			return usdRate;
		}
		public void setUsdRate(BigDecimal rate) {
			usdRate = rate;
		}

		@Override
		public int hashCode() {
			return code.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Currency other = (Currency) obj;
			if (code == null)
				return (other.code == null);
			else 
				return code.equals(other.code);
		}
		@Override
		public String toString() {
			return "Currency [code=" + code + " usdRate=" + usdRate + "]";
		}
	}
	
	public static class InvalidCurrencyException extends Exception {
		private static final long serialVersionUID = 1L;
		public InvalidCurrencyException(String msg) { super(msg); }
	}

	private Map<String, Currency> currencies;
	
	public CurrencySet() { 
		currencies = new HashMap<String, Currency>(); 
	}
	
	public void parse(String list) {
		for (String code : list.split("[ \t,]+")) {
			String uCode = code.toUpperCase();
			currencies.put(uCode, new Currency(uCode));
		}
	}

	public Currency get(String currencyCode) throws InvalidCurrencyException {
		String uCode = currencyCode.toUpperCase();
		if (currencies.containsKey(uCode))
			return currencies.get(uCode);
		else
			throw new InvalidCurrencyException(currencyCode);
	}
	
	public Collection<Currency> getCurrencies() {
		return currencies.values();
	}
	
	public void clear() {
		currencies.clear();
	}

	@Override
	public String toString() {
		return "CurrencySet [codes=" + currencies + "]";
	}
}
