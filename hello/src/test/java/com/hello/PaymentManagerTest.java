package com.hello;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.stubbing.OngoingStubbing;

import com.hello.PaymentManager.PaymentEntry;
import com.hello.PaymentManager.LineFormatException;

public class PaymentManagerTest {

    @Before  
    public void init() {
    	Assert.assertEquals(0, PaymentBook.getInstance().size());
		PaymentBook.getCurrencySet().parse("USD RMB JPY HKD EUR GBP");
    }
    
    @After  
    public void cleanup() {  
    	PaymentBook.getInstance().clear();
    }
    
    private BufferedReader getSamplePaymentReader(String[] payments) {
    	StringBuilder sb = new StringBuilder();
    	for (String p : payments) {
    		sb.append(p).append("\n");
    	}
    	return new BufferedReader(new StringReader(sb.toString()));
    }

	@Test
	public void testGetPaymentEntry() {
		try {
			PaymentEntry entry = PaymentManager.getPaymentEntry("USD 1000");
			Assert.assertEquals("USD", entry.getCurrency().getCode());
			Assert.assertEquals(new BigDecimal("1000"), entry.getAmount());
		} catch (Exception e) {
			Assert.fail("shouldn't get exception");
		}

		try {
			PaymentEntry entry = PaymentManager.getPaymentEntry("HKD   -1000");
			Assert.assertEquals("HKD", entry.getCurrency().getCode());
			Assert.assertEquals(new BigDecimal("-1000"), entry.getAmount());
		} catch (Exception e) {
			Assert.fail("shouldn't get exception");
		}

		try {
			PaymentEntry entry = PaymentManager.getPaymentEntry("RMB		-1000 ");
			Assert.assertEquals("RMB", entry.getCurrency().getCode());
			Assert.assertEquals(new BigDecimal("-1000"), entry.getAmount());
		} catch (Exception e) {
			Assert.fail("shouldn't get exception");
		}

		try {
			PaymentManager.getPaymentEntry("HKD   -1000m");
			Assert.fail("should get NumberFormatException");
		} catch (NumberFormatException e) {
		} catch (Exception e) {
			Assert.fail("should get NumberFormatException");
		}

		try {
			PaymentManager.getPaymentEntry("HKD x  -1000");
			Assert.fail("should get LineFormatException");
		} catch (LineFormatException e) {
		} catch (Exception e) {
			Assert.fail("should get LineFormatException");
		}
	}

    final String[] samplePayments = new String[] {
    		"USD 1000",
    		"HKD 100",
    		"USD -100",
    		"RMB 2000",
    		"HKD 200",
    		"JPY 200000",
    		"JPY 100000",
    		"JPY -300000",
    };

	@Test
	public void testProcessPayments() throws Exception {
		BufferedReader reader = getSamplePaymentReader(samplePayments);
		
		PaymentManager processor = PaymentManager.getInstance();
		
		processor.processPayments(reader);
		
		Assert.assertEquals(4, PaymentBook.getInstance().size());
		Assert.assertEquals(new BigDecimal("900"), PaymentBook.getInstance().get("USD"));
		Assert.assertEquals(new BigDecimal("300"), PaymentBook.getInstance().get("HKD"));
		Assert.assertEquals(new BigDecimal("2000"), PaymentBook.getInstance().get("RMB"));
		Assert.assertEquals(BigDecimal.ZERO, PaymentBook.getInstance().get("JPY"));
		
		String summary = PaymentBook.getInstance().getSummary(";");
		
		Assert.assertTrue(summary.contains("USD 900;"));
		Assert.assertTrue(summary.contains("HKD 300;"));
		Assert.assertTrue(summary.contains("RMB 2000;"));
		
		Assert.assertEquals("", summary.replaceFirst("USD 900;", "").replaceFirst("HKD 300;", "").replaceFirst("RMB 2000;", ""));
	}

    final String[] samplePaymentsWithError = new String[] {
    		"USD 1000",
    		"HKD 100",
    		"JPY 1000 0",
    		"USD -100",
    		"RMB 2000",
    		"XXX 1234",
    		"HKD 100m",
    		"HKD -100",
    };

	@Test
	public void testProcessPaymentsWithErrInputs() throws Exception {
		BufferedReader reader = getSamplePaymentReader(samplePaymentsWithError);
		
		PaymentManager processor = PaymentManager.getInstance();
		
		processor.processPayments(reader);
		
		Assert.assertEquals(3, PaymentBook.getInstance().size());
		Assert.assertEquals(new BigDecimal("900"), PaymentBook.getInstance().get("USD"));
		Assert.assertEquals(new BigDecimal("2000"), PaymentBook.getInstance().get("RMB"));
		Assert.assertEquals(BigDecimal.ZERO, PaymentBook.getInstance().get("HKD"));
		Assert.assertNull(PaymentBook.getInstance().get("JPY"));
		
		String summary = PaymentBook.getInstance().getSummary(";");
		Assert.assertTrue(summary.contains("USD 900"));
		Assert.assertTrue(summary.contains("RMB 2000"));
		Assert.assertFalse(summary.contains("HKD"));
		Assert.assertFalse(summary.contains("JPY"));
	}
	
	@Test
	public void testProcessPaymentsConcurrent() throws Exception {
		final PaymentManager processor = PaymentManager.getInstance();

		final int REPEAT = 1000;
		
		for (int i=0; i<REPEAT; i++) {
			BufferedReader reader = getSamplePaymentReader(samplePayments);
			processor.processPaymentsConcurrent(reader);
		}

		new Timer().schedule(
				new TimerTask() {
					@Override
					public void run() {
						// kill all threads when timeout
						processor.killAllProcessingThreads();
					}
				}, 
				1000
			);

		processor.joinAllProcessingThreads();
		
		Assert.assertEquals(4, PaymentBook.getInstance().size());
		BigDecimal repeat = new BigDecimal(REPEAT);
		Assert.assertEquals(new BigDecimal("900").multiply(repeat), PaymentBook.getInstance().get("USD"));
		Assert.assertEquals(new BigDecimal("300").multiply(repeat), PaymentBook.getInstance().get("HKD"));
		Assert.assertEquals(new BigDecimal("2000").multiply(repeat), PaymentBook.getInstance().get("RMB"));
		Assert.assertEquals(BigDecimal.ZERO, PaymentBook.getInstance().get("JPY"));
	}
	
	@Test
	public void testSchedulePaymentSummary() throws Exception {
		BufferedReader reader = mock(BufferedReader.class);
		BufferedWriter writer = mock(BufferedWriter.class);

		PaymentManager processor = PaymentManager.getInstance();
		
		OngoingStubbing<String> stub = when(reader.readLine());
		
		for (String input : samplePayments) {
			stub = stub.thenReturn(input);
		}
		
		stub.thenReturn("quit");

		processor.processPaymentsConcurrent(reader);
		
		final long delay = 50;
		final long period = 100;
		final int REPEAT = 10;
		
		processor.schedulePaymentSummary(delay, period, writer);
		
		Thread.sleep(delay + REPEAT * period + period/3);  

		processor.cancelSchedule();
		
		verify(writer, times(REPEAT)).write(argThat(
				new ArgumentMatcher<String>() {
					@Override
					public boolean matches(Object argument) {
						if (argument instanceof String) {
							String str = (String)argument;
							if (str.contains("Summary"))
								return true;
						}
						return false;
					}
				}
			));

		verify(writer, times(REPEAT)).write(argThat(
				new ArgumentMatcher<String>() {
					@Override
					public boolean matches(Object argument) {
						if (argument instanceof String) {
							String str = (String)argument;
							if (str.contains("USD 900") && str.contains("HKD 300") && str.contains("RMB 2000"))
								return true;
						}
						return false;
					}
				}
			));

	}
}
