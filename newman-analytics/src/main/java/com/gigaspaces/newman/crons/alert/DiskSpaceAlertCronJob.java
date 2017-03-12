package com.gigaspaces.newman.crons.alert;

import com.gigaspaces.newman.analytics.CronJob;
import com.gigaspaces.newman.smtp.Mailman;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

import javax.mail.MessagingException;

/**
 * Created by moran on 2/22/17.
 */
public class DiskSpaceAlertCronJob implements CronJob {
    private static final Logger logger = LoggerFactory.getLogger(DiskSpaceAlertCronJob.class);
    public static final String CAPACITY_THRESHOLD = "crons.alert.capacityThreshold";

    @Override
    public void run(Properties properties) {
        try {
            checkDiskSpace(properties);
        } catch (Exception e) {
            logger.warn(e.toString(), e);
        }
    }

    private void checkDiskSpace(Properties properties) throws MessagingException {
        File file = new File("/home");
        long freeSpace = file.getFreeSpace();
        long totalSpace = file.getTotalSpace();
        double percentageFree = (100.*freeSpace/totalSpace);
        int capacity = 100 - (int)percentageFree;

        String alert = "Size: " + humanReadableByteCount(totalSpace)
                + "    Used: " + humanReadableByteCount(totalSpace - freeSpace)
                + "    Available: " + humanReadableByteCount(freeSpace)
                + "    Capacity: " + capacity + "%";

        logger.info(alert);

        String threshold = properties.getProperty(CAPACITY_THRESHOLD, "70");
        if (capacity > Long.valueOf(threshold)) {
            logger.warn("Trigger capacity alert, usage " + capacity +"% > " + threshold +"%");
            Mailman mailman = new Mailman(properties.getProperty(Mailman.MAIL_ACCOUNT_USERNAME),
                    properties.getProperty(Mailman.MAIL_ACCOUNT_PASSWORD));

            mailman.compose(properties.getProperty(Mailman.MAIL_MESSAGE_RECIPIENTS),
                    "Alert! Disk capacity on xap-newman is at " + capacity +"%", alert, Mailman.Format.HTML);

        }
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = String.valueOf("kMGTPE".charAt(exp-1));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static void main(String[] args) throws MessagingException {
        DiskSpaceAlertCronJob j = new DiskSpaceAlertCronJob();
        j.checkDiskSpace(new Properties());
    }
}
