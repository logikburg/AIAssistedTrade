package com.scheduler.controller;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scheduler.crawler.WebCrawler;
import com.scheduler.jobs.SchedulerJob;
import com.scheduler.model.NewsItem;
import com.scheduler.repository.NewsItemRepository;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLPClient;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

/**
 *
 * @author Sandeep Mogla
 */
@RestController
public class SchedulerRestController {

    private static Logger logger = LoggerFactory.getLogger(SchedulerRestController.class);

    @Autowired
    private Scheduler scheduler;

    public static AtomicBoolean isCallInitiated = new AtomicBoolean();

    // @Autowired
    // private NewsItemMongoService newsItemMongoService;

    private final NewsItemRepository newsItemRepository;

    public SchedulerRestController(NewsItemRepository newsItemRepository) {
	this.newsItemRepository = newsItemRepository;
    }

    @GetMapping("/schedule")
    public ResponseEntity<Object> schedule() throws SchedulerException {

	synchronized (SchedulerRestController.isCallInitiated) {
	    System.out.println("*********************** synchronized");
	    System.out.println("SchedulerRestController.isCallInitiated" + SchedulerRestController.isCallInitiated);
	    if (SchedulerRestController.isCallInitiated.get()) {
		return new ResponseEntity<>("Aborted", HttpStatus.CONFLICT);
	    }
	    SchedulerRestController.isCallInitiated.set(true);
	}

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

	return new ResponseEntity<>("Success", HttpStatus.OK);

    }

    @GetMapping("/scheduleStatus")
    public ResponseEntity<Object> getScheduleStatus() throws SchedulerException {
	Trigger trigger = newTrigger().withIdentity("trigger1", "group1").build();
	TriggerState ts = scheduler.getTriggerState(trigger.getKey());
	System.out.println("***********************TriggerState ");
	System.out.println(ts);
	return new ResponseEntity<>(ts, HttpStatus.OK);
    }

    @GetMapping("/resetSchedule")
    public ResponseEntity<Object> resetSchedule() throws SchedulerException {
	SchedulerRestController.isCallInitiated.set(true);
	return new ResponseEntity<>("reset", HttpStatus.OK);
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

    @GetMapping("/findNamedEntities")
    public void findNamedEntities() {
	// fetch all customers
	logger.info("findNamedEntities");
	logger.info("-------------------------------");

	// creates a StanfordCoreNLP object with POS tagging, lemmatization, NER,
	// parsing, and coreference resolution
	Properties props = new Properties();
	props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, sentiment");
	StanfordCoreNLPClient pipeline = new StanfordCoreNLPClient(props, "http://localhost", 9000, 2);
	// read some text in the text variable
	String text = "US Bourses Lose Compass; Mkt Spotlight on Big Techs"; // Add your text here!

	// create an empty Annotation just with the given text
	// Annotation document = new Annotation(text);
	// // run all Annotators on this text
	// pipeline.annotate(document);
	// List sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

	// pipeline.annotate(document);

	for (NewsItem _ni : this.newsItemRepository.findAll()) {

	    logger.info("title " + _ni.getTitle());

	    Annotation annotation = pipeline.process(_ni.getTitle());
	    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

	    logger.info("---------");

	    for (CoreMap sentence : sentences) {
		String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
		// System.out.println(sentiment);
		logger.info(_ni.getTitle());
		logger.info(sentiment);
	    }

	    // NER
	    boolean inEntity = false;
	    String currentEntity = "";
	    String currentEntityType = "";

	    for (CoreMap sentence : sentences) {
		for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {

		    String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

		    if (!inEntity) {
			if (!"O".equals(ne)) {
			    inEntity = true;
			    currentEntity = "";
			    currentEntityType = ne;
			}
		    }
		    if (inEntity) {
			if ("O".equals(ne)) {
			    inEntity = false;
			    switch (currentEntityType) {
			    case "PERSON":
				System.out.println("Extracted Person - " + currentEntity.trim());
				break;
			    case "ORGANIZATION":
				System.out.println("Extracted Organization - " + currentEntity.trim());
				break;
			    case "LOCATION":
				System.out.println("Extracted Location - " + currentEntity.trim());
				break;
			    case "DATE":
				System.out.println("Extracted Date " + currentEntity.trim());
				break;
			    case "COUNTRY":
				System.out.println("Extracted Country " + currentEntity.trim());
				break;
			    case "MISC":
				System.out.println("Extracted MISC " + currentEntity.trim());
				break;
			    }
			} else {
			    currentEntity += " " + token.originalText();
			}

		    }
		}
	    }
	}

    }

}
