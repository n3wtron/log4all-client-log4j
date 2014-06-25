package net.log4all.log4j;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.util.Date;

/**
 * Created by igor on 05/06/14.
 */
public class Log4AllAppenderTest {
    private Logger logger;
    private int seq=0;
    @BeforeTest
    public void initLog4j(){
        DOMConfigurator.configure(getClass().getClassLoader().getResource("log4j.xml"));
        logger=Logger.getLogger(getClass());
    }

    @Test(threadPoolSize = 10, invocationCount = 100)
    public void longTest(){
        try{
            throw new Exception("test exception",new FileNotFoundException("test filenotfoundException"));
        }catch(Exception e) {
            logger.warn("test message very long very long  very long  very long  very long  very long  very long " +
                    " very long  very long  very long  very long  very long  very long  very long " +
                    " very long  very long  very long  very long  very long  very long  very long  very long " +
                    " very long  very long  very long  very long  very long  very long  very long  very long " +
                    "#application:log4j-test #rand:"+new Date().getSeconds(),e);
        }
    }

    @Test(invocationCount = 100)
    public void sequenceTest() throws InterruptedException {
       logger.info("Sequence test #test:sequenceTest #+seq:"+seq);
       seq++;
    }

    @Test
    public void basicTest() throws InterruptedException {
        try{
            throw new Exception("test exception",new FileNotFoundException("test filenotfoundException"));
        }catch(Exception e) {
            logger.info("test message #application:log4j-test2",e);
        }
        Thread.sleep(10000);
    }
}
