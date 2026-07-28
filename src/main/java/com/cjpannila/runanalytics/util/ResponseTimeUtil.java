package com.cjpannila.runanalytics.util;

import org.slf4j.Logger;
import org.springframework.util.StopWatch;

public class ResponseTimeUtil {
    //Create new StopWatch instance and start
    public static StopWatch getStopWatchAndStart() {
        StopWatch watch = new StopWatch();
        watch.start();
        return watch;
    }

    //Stop the StopWatch and log the response time with the passed in Logger and apiName
    public static void stopAndLogResponseTime(Logger logger, String apiName, StopWatch watch) {
        watch.stop();
        logger.info("{} API execution time: {} seconds", apiName, String.format("%.2f", watch.getTotalTimeSeconds()));
    }
}
