This project named "hello" consists of below source files

pom.xml
src/main/java/com/hello/CurrencySet.java
src/main/java/com/hello/PaymentApp.java
src/main/java/com/hello/PaymentBook.java
src/main/java/com/hello/PaymentManager.java
src/test/java/com/hello/CurrencySetTest.java
src/test/java/com/hello/PaymentAppTest.java
src/test/java/com/hello/PaymentBookTest.java
src/test/java/com/hello/PaymentManagerTest.java
src/main/resources/currency.txt
src/main/resources/readme.txt

Files under src/main are production source and those under src/test contain unit testing cases.


Design
======

PaymentApp contains the main method. It loads config and accepts payment input from files and console input.

PaymentManager and PaymentBook are both singleton classes.

PaymentManager processes input entries and add valid one to PaymentBook. 
It handles multiple input steams (files and console) concurrently.
It also schedules console output of net payment position in regular interval via a timer thread. 

PaymentBook stores the payment records in concurrentHashMap which support concurrent read / write. 
It allows concurrent input of payment by multiple threads via methods PaymentBook.add(PaymentEntry) which is implemented using atomic methods to ensure thread safe. 

CurrencySet is for storing the valid currency codes.

Two nested classes PaymentEntry and Currency are defined under PaymentBook and CurrencySet.  
PaymentEntry represents a payment entry of form "CurrencyCode Amount".
Currency represents a currency plus its exchange rate to USD.


JUnit and Mockito are employed to develop the unit test cases. 


How to build the program
========================

Please build the program using maven 2 by running below command under the folder "hello".

mvn clean install

The output jar file will be created as target/hello-0.0.1-SNAPSHOT.jar


Configuration
=============

The currency codes are configurable in the file "currency.txt". 

The exchange rates are also stored in the same file "currency.txt".

This config file can be placed anywhere on classpath. 

A sample src/main/resources/currency.txt is included in the project.

User input with currency code not defined in the config file will be rejected with error message display.


How to run the program
======================

Please run the program under the folder "hello" with below command.

java -cp target\hello-0.0.1-SNAPSHOT.jar com.hello.PaymentApp <paymentfile1> <paymentfile2> ...

The program accepts one or multiple files on command line.

User can input payment entries from console. 

Input entries with be ignored with error message in following cases.
- incorrect input format, i.e. not in the format of CurrencyCode Amount
- undefined currency code
- invalid number format of Amount   

The net position of the payments is output to console once per minute.


Sample output
=============

java -cp target\hello-0.0.1-SNAPSHOT.jar com.hello.PaymentApp c:\tmp\curr.txt c:\tmp\curr2.txt
loading properties from currency.txt
supported.codes: USD, HKD, JPY, RMB, EUR, GBP, NZD, AUD
JPY/USD=0.0123456
EUR/USD=1.3
HKD/USD=0.128205
RMB/USD=0.166666
GBP/USD=1.6
processing c:\tmp\curr.txt
processing c:\tmp\curr2.txt
Summary:
USD 246
EUR 56666 (USD 73665.80)
JPY 251780 (USD 3108.38)
HKD 2690 (USD 344.87)

HKD 1234.1
RMB 9000.0
Summary:
USD 246
EUR 56666 (USD 73665.80)
JPY 251780 (USD 3108.38)
RMB 9000.0 (USD 1499.99)
HKD 3924.1 (USD 503.09)

quit
Bye
USD 246
EUR 56666 (USD 73665.80)
JPY 251780 (USD 3108.38)
RMB 9000.0 (USD 1499.99)
HKD 3924.1 (USD 503.09)