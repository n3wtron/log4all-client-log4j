package net.log4all.log4j;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Created by igor on 05/06/14.
 */
public class Log4AllAppenderTest {
    private Logger logger;

    @BeforeTest
    public void initLog4j(){
        DOMConfigurator.configure(getClass().getClassLoader().getResource("log4j.xml"));
        logger=Logger.getLogger(getClass());
    }

    @Test
    public void basicTest(){
        logger.info("test messaggio");

    }
}
