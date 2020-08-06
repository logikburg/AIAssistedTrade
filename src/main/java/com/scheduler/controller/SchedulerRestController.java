package com.scheduler.controller;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scheduler.crawler.WebCrawler;
import com.scheduler.jobs.SchedulerJob;
import com.scheduler.repository.NewsItemRepository;

/**
 *
 * @author Sandeep Mogla
 */
@RestController
public class SchedulerRestController {

    @Autowired
    private Scheduler scheduler;

    //@Autowired
    //private NewsItemMongoService newsItemMongoService;

    private final NewsItemRepository newsItemRepository;

    public SchedulerRestController(NewsItemRepository newsItemRepository) {
	this.newsItemRepository = newsItemRepository;
    }

    @GetMapping("/schedule")
    public void schedule() throws SchedulerException {
	System.out.println("***********************Schedule");

	// define the job and tie it to our MyJob class
	JobDetail job = newJob(SchedulerJob.class).withIdentity("job1", "group1").build();

	/*
	 * //Trigger using cron expressions Trigger trigger = TriggerBuilder
	 * .newTrigger() .withIdentity("dummyTriggerName", "group1") .withSchedule(
	 * CronScheduleBuilder.cronSchedule("0/5 * * * * ?")) .build();
	 */
	/*
	 * //Trigger that will fire only once trigger = (SimpleTrigger) newTrigger()
	 * .withIdentity("trigger5", "group1") .startAt(futureDate(5,
	 * IntervalUnit.MINUTE)) // use DateBuilder to create a date in the future
	 * .forJob(myJobKey) // identify job with its JobKey .build();
	 */

	// Trigger the job to run now, and then repeat every 10 seconds
	Trigger trigger = newTrigger().withIdentity("trigger1", "group1").startNow()
		.withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();

	// Tell quartz to schedule the job using our trigger
	scheduler.scheduleJob(job, trigger);

    }

    @GetMapping("/scheduleStatus")
    public ResponseEntity<Object> getScheduleStatus() throws SchedulerException {
	Trigger trigger = newTrigger().withIdentity("trigger1", "group1").build();
	TriggerState ts = scheduler.getTriggerState(trigger.getKey());
	System.out.println("***********************TriggerState ");
	System.out.println(ts);
	return new ResponseEntity<>(ts, HttpStatus.OK);
    }

    @GetMapping("/startFetchingData")
    public void startFetchData() {
	WebCrawler webcrawler = new WebCrawler(
		"https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx6TVdZU0FtVnVHZ0pWVXlnQVAB?hl=en-US&gl=US&ceid=US:en",
		3, this.newsItemRepository);
	long start = System.currentTimeMillis();
	webcrawler.start();
	webcrawler.join();
	long totalTimeInMS = System.currentTimeMillis() - start;
	webcrawler.shutdown();
	System.out.println(webcrawler.getSeenLinks() + " links processed in " + (double) totalTimeInMS / 1000 + "s");
    }

}
