package com.scheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author Sandeep Mogla
 */
public class SchedulerJob implements Job {

    public SchedulerJob() {

    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
	System.err.println("Sample Job Executing");
    }
}
