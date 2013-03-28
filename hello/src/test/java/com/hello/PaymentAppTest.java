package com.hello;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.hello.CurrencySet.InvalidCurrencyException;

public class PaymentAppTest {
    @Before  
    public void init() {
    	PaymentBook.getInstance().clear();    	
    }
    
    @After  
    public void cleanup() {  
    	PaymentBook.getInstance().clear();
    }
    
	@Test
	public void testAppSummary() throws Exception {
		BufferedReader bufferedReader = mock(BufferedReader.class);
		BufferedWriter bufferedWriter = mock(BufferedWriter.class);
		
		final PaymentApp app = new PaymentApp();
		app.setDelayInMS(50);
		app.setPeriodInMS(100);

		app.initialize("currency.txt");

		try {
			Assert.assertNull(PaymentBook.getCurrencySet().get("USD").getUsdRate());
			Assert.assertNotNull(PaymentBook.getCurrencySet().get("HKD").getUsdRate());
		} catch (InvalidCurrencyException e) {
			Assert.fail("undefined currency code " + e.getMessage());
		}
		try {
			Assert.assertNull(PaymentBook.getCurrencySet().get("XXX"));
			Assert.fail("XXX is undefined and should get exception");
		} catch (InvalidCurrencyException e) {
		}
		
		final int REPEAT = 10;
		when(bufferedReader.readLine()).thenReturn("HKD 12345").thenReturn("USD 23456").thenReturn("HKD 20000.1").thenReturn("USD   -56").thenAnswer(
				new Answer<String>() {
					@Override
					public String answer(InvocationOnMock invocation) throws Throwable {
						// wait for REPEAT periods printing summary
						Thread.sleep(app.getDelayInMS() + app.getPeriodInMS() * REPEAT + app.getPeriodInMS()/3);
						return "quit";
					}
				}
			);

		app.processing(new String[] {}, bufferedReader, bufferedWriter);

		verify(bufferedWriter, times(REPEAT)).write(argThat(
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

		verify(bufferedWriter).write(argThat(
				new ArgumentMatcher<String>() {
					@Override
					public boolean matches(Object argument) {
						if (argument instanceof String) {
							String str = (String)argument;
							if (str.contains("Bye"))
								return true;
						}
						return false;
					}
				}
			));

		verify(bufferedWriter, times(REPEAT+1)).write(argThat(
				new ArgumentMatcher<String>() {
					@Override
					public boolean matches(Object argument) {
						if (argument instanceof String) {
							String str = (String)argument;
							if (str.contains("HKD 32345.1 (USD 4146.80)") && str.contains("USD 23400"))
								return true;
						}
						return false;
					}
				}
			));
	
	}
	
	@Test
	public void testAppQuit() throws Exception {
		BufferedReader bufferedReader = mock(BufferedReader.class);
		BufferedWriter bufferedWriter = mock(BufferedWriter.class);
		
		PaymentApp app = new PaymentApp();

		app.initialize("currency.txt");
		
		when(bufferedReader.readLine()).thenReturn("HKD 12345").thenReturn("USD 23456").thenReturn("HKD 20000.1").thenReturn("USD   -56").thenReturn("quit");

		app.processing(new String[] {}, bufferedReader, bufferedWriter);

		verify(bufferedWriter).write(argThat(
				new ArgumentMatcher<String>() {
					@Override
					public boolean matches(Object argument) {
						if (argument instanceof String) {
							String str = (String)argument;
							if (str.contains("Bye"))
								return true;
						}
						return false;
					}
				}
			));

		verify(bufferedWriter).write(argThat(
				new ArgumentMatcher<String>() {
					@Override
					public boolean matches(Object argument) {
						if (argument instanceof String) {
							String str = (String)argument;
							if (str.contains("HKD 32345.1 (USD 4146.80)") && str.contains("USD 23400"))
								return true;
						}
						return false;
					}
				}
			));
		
		verify(bufferedWriter).flush();
	}
}
