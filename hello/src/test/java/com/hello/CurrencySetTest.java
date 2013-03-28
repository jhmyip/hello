package com.hello;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import com.hello.CurrencySet.Currency;
import com.hello.CurrencySet.InvalidCurrencyException;

public class CurrencySetTest {

	@SuppressWarnings("unused")
	@Test
	public void test() throws Exception {
		CurrencySet currencies = new CurrencySet();
    	currencies.parse("USD RMB JPY HKD EUR GBP");

    	Currency usd = currencies.get("USD");
    	Currency rmb = currencies.get("RMB");
    	Currency jpy = currencies.get("JPY");
    	Currency hkd = currencies.get("HKD");
    	Currency eur = currencies.get("EUR");
    	Currency gbp = currencies.get("gbp");
    	
    	Assert.assertNotNull(hkd);
    	Assert.assertEquals("HKD", hkd.getCode());
    	
    	Currency hkd2 = currencies.get("HKD");
    	Assert.assertEquals(hkd, hkd2);
    	Assert.assertNotSame(hkd, usd);
    	Assert.assertTrue(!hkd.equals(usd));
    	
    	try {
    		currencies.get("XXX");
    		Assert.fail("failed to catch undefined currency");
    	} catch (InvalidCurrencyException e) {
    	}
    	
    	jpy.setUsdRate(new BigDecimal(1/80.0));
    	
    	Assert.assertTrue(Math.abs(jpy.getUsdRate().doubleValue()-1/80.0)<0.01);
	}
}
