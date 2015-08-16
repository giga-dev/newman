package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.report.daily.DailyReport;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by moran on 8/13/15.
 */
public class NewmanReporter {

    private static final Logger logger = LoggerFactory.getLogger(NewmanReporter.class);

    private final NewmanReporterConfig config;
    private final Scheduler scheduler;

    public NewmanReporter(NewmanReporterConfig config) throws SchedulerException {
        this.config = config;
        this.scheduler = new StdSchedulerFactory().getScheduler();
    }

    public static void main(String[] args) throws Exception {

        NewmanReporterConfig config = new NewmanReporterConfig();
        NewmanReporter newmanReporter = new NewmanReporter(config);
        try {
            newmanReporter.start();
        }
        catch (Exception e) {
            logger.error("Caught unexpected exception, exiting", e);
            if (newmanReporter != null) {
                newmanReporter.close();
            }
        }
    }

    private void start() throws SchedulerException {
        this.scheduler.start();

        scheduleDailyReport();

    }

    private void scheduleDailyReport() throws SchedulerException {

        JobDetail job = newJob(DailyReport.class).withDescription(DailyReport.class.getName()).build();
        job.getJobDataMap().put("mutable.ref.build", new AtomicReference<Build>());
        job.getJobDataMap().put("immutable.newman.config", config);

        Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(cronSchedule("0 0 8 ? * SUN-THU")) //Fire at 08:00am every Sunday - Thursday
                .build();

        scheduler.scheduleJob(job, trigger);
    }


    private void close() throws SchedulerException {
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }
    }
}
