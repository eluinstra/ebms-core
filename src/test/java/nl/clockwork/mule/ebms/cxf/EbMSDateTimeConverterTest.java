package nl.clockwork.mule.ebms.cxf;

import org.junit.Test;

public class EbMSDateTimeConverterTest {
    @Test
    public void testConcurrency() {
        for (int i = 0; i < 1000; i++) {
            String dateString = createDateString();
            Runnable converter = new DateConverterRunner(dateString);
            new Thread(converter).start();
        }
    }
    
    private String createDateString() {
        return "2001-12-31T12:00:00";
    }
    
    private class DateConverterRunner implements Runnable {

        private final String dateString;
        
        DateConverterRunner(final String dateString) {
            this.dateString = dateString;
        }
        
        @Override
        public void run() {
            EbMSDateTimeConverter.parseDateTime(dateString);
        }
    }
}