package com.hello;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.hello.PaymentManager.PaymentEntry;

public class PaymentBookTest {
    @Before  
    public void init() {
    	PaymentBook.getInstance().clear();    	
		PaymentBook.getCurrencySet().parse("USD RMB JPY HKD EUR GBP");
    }
    
    @After  
    public void cleanup() {  
    	PaymentBook.getInstance().clear();
    }
    
    String[] samplePayments = new String[] {
    		"USD 1000",
    		"HKD 100",
    		"USD -100",
    		"RMB 2000",
    		"HKD 200.0",
    		"JPY 200000",
    		"JPY 100000",
    		"JPY -300000",
    };
    
    String[] samplePaymentsWithInvalidEntry = new String[] {
    		"USD 1000",
    		"HKD 100",
    		"USD -100",
    		"RMB 2000",
    		"HKD 200",
    		"USD 200m",
    		"HKD 1000 00",
    };

    private void processPayments(String [] payments) {
    	for (String line : payments) {
    		try {
    			PaymentEntry entry = PaymentManager.getPaymentEntry(line);
    			PaymentBook.getInstance().add(entry.getCurrency(), entry.getAmount());
    		} catch (Exception e) {
    			System.err.println(e.getMessage());
    		}
    	}
    }
    
    @Test
    public void testSingleton() {
    	Assert.assertEquals(PaymentBook.getInstance(), PaymentBook.getInstance());
    	Assert.assertSame(PaymentBook.getInstance(), PaymentBook.getInstance());
    }
    
	@Test
	public void testAddSingleEntry() throws Exception {
		String[] payment = { samplePayments[0] };
		processPayments(payment);
		
		Assert.assertEquals(1, PaymentBook.getInstance().size());
		Assert.assertEquals(new BigDecimal("1000"), PaymentBook.getInstance().get("USD"));
		Assert.assertNull(PaymentBook.getInstance().get("HKD"));
		
		Assert.assertEquals(samplePayments[0] + "\n", PaymentBook.getInstance().getSummary());
	}

	@Test
	public void testAddMultiEntries() throws Exception {
		PaymentBook.getCurrencySet().get("HKD").setUsdRate(new BigDecimal(1/7.8));
		PaymentBook.getCurrencySet().get("RMB").setUsdRate(new BigDecimal(1/6.0));

		processPayments(samplePayments);
		
		Assert.assertEquals(4, PaymentBook.getInstance().size());
		Assert.assertEquals(new BigDecimal("900"), PaymentBook.getInstance().get("USD"));
		Assert.assertEquals(new BigDecimal("300.0"), PaymentBook.getInstance().get("HKD"));
		Assert.assertEquals(new BigDecimal("2000"), PaymentBook.getInstance().get("RMB"));
		Assert.assertEquals(BigDecimal.ZERO, PaymentBook.getInstance().get("JPY"));
		
		String summary = PaymentBook.getInstance().getSummary(";");

		Assert.assertTrue(summary.contains("USD 900;"));
		Assert.assertTrue(summary.contains("HKD 300.0 (USD 38.46);"));
		Assert.assertTrue(summary.contains("RMB 2000 (USD 333.33);"));

		Assert.assertEquals("", summary.replaceFirst("USD 900;", "").replaceFirst("HKD 300.0 .USD 38.46.;", "").replaceFirst("RMB 2000 .USD 333.33.;", ""));
	}

	@Test
	public void testAddWithInvalidEntries() throws Exception {
		processPayments(samplePaymentsWithInvalidEntry);
		
		Assert.assertEquals(3, PaymentBook.getInstance().size());
		Assert.assertEquals(new BigDecimal("900"), PaymentBook.getInstance().get("USD"));
		Assert.assertEquals(new BigDecimal("300"), PaymentBook.getInstance().get("HKD"));
		Assert.assertEquals(new BigDecimal("2000"), PaymentBook.getInstance().get("RMB"));
	}

	@Test
	public void testAddMultiReaders() throws Exception {
		processPayments(samplePayments);
		processPayments(samplePaymentsWithInvalidEntry);
		
		Assert.assertEquals(4, PaymentBook.getInstance().size());
		Assert.assertEquals(new BigDecimal("1800"), PaymentBook.getInstance().get("USD"));
		Assert.assertEquals(new BigDecimal("600.0"), PaymentBook.getInstance().get("HKD"));
		Assert.assertEquals(new BigDecimal("4000"), PaymentBook.getInstance().get("RMB"));
		Assert.assertEquals(BigDecimal.ZERO, PaymentBook.getInstance().get("JPY"));
	}
}
