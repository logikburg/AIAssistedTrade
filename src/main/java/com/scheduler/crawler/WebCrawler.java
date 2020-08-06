package com.scheduler.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.scheduler.model.NewsItem;
import com.scheduler.repository.NewsItemRepository;
import com.scheduler.service.NewsItemMongoService;

public class WebCrawler {

    private static Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    private final CloseableHttpClient client;
    private final String baseUrl;
    private final ExecutorService executorService;
    private final Object lock = new Object();
    private ConcurrentHashMap<String, Boolean> seen = new ConcurrentHashMap<String, Boolean>();
    private AtomicInteger pending = new AtomicInteger(0);

    private static int countWoker = 0;

    private NewsItemRepository newsItemRepository;
    
    private List<NewsItem> entites = new ArrayList<>();

    public WebCrawler(String baseUrl, int numOfThreads, NewsItemRepository newsItemRepository) {
	this.client = HttpClientBuilder.create().build();
	this.baseUrl = baseUrl;
	this.newsItemRepository = newsItemRepository;
	this.executorService = Executors.newFixedThreadPool(numOfThreads, new ThreadFactory() {
	    public Thread newThread(Runnable r) {
		return new Thread(r, "Crawler-Worker" + ++countWoker);
	    }
	});
    }

    public int getSeenLinks() {
	return seen.size();
    }

    private void handle(final String link) {
//	if (seen.containsKey(link))
//	    return;
//	seen.put(link, true);

	logger.info("handle");
	List<?> items = getLinksFromRSSUrl(link);
	logger.info("get rss items number {}", items.size());
//	Element el = (Element) items.get(0);
//	Elements elLinks = el.getElementsByTag("link");
//	System.out.println(elLinks.get(0).text());

	// news item objct creation
	NewsItem ni = new NewsItem();

	for (Element elLink : (Elements) items) {
	    logger.info("elLink.getElementsByTag(link).text() > " + elLink.getElementsByTag("link").text());

	    pending.incrementAndGet();
	    logger.info("pending.incrementAndGet");

	    // news item object creation
	    ni = new NewsItem();
	    ni.setGuid(elLink.getElementsByTag("guid").text());
	    ni.setHasSentiments(false);

	    // news publisher
	    ni.setLink(elLink.getElementsByTag("link").text());
	    ni.setSource(elLink.getElementsByTag("source").text());

	    String title = elLink.getElementsByTag("title").text().replace("- " + ni.getSource(), "");
	    ni.setTitle(title);

	    // another thread for fetching and storing the data.
	    handleElementLink(ni);

	    // save a couple of news Item
	    // newsItemRepository.save(ni);
	    entites.add(ni);
	}
    }

    private void handleElementLink(NewsItem ni) {
	logger.info("inside handleElementLink");

	executorService.execute(new Runnable() {
	    public void run() {
		Document doc = null;
		try {
		    String link = ni.getLink();
		    doc = Jsoup.connect(link).userAgent("Mozilla").get();
		    ni.setContent(doc.html());
		} catch (IOException e) {
		    e.printStackTrace();
		}finally {
		    pending.decrementAndGet();
		    logger.info("pending tasks after decrementAndGet {}", pending);
		}
		// Document doc = Jsoup.parse(getDataFromUrl(link));

		// fetch all customers
//		logger.info("NewsItem() found with findAll():");
//		logger.info("-------------------------------");
//		for (NewsItem _ni : newsItemRepository.findAll()) {
//			System.out.println(_ni);
//		}
		
		if (pending.get() == 0) {
		    synchronized (lock) {
			lock.notify();
		    }
		}
	    }
	});
	
    }

    private List<?> getLinksFromRSSUrl(final String url) {
	logger.info("getLinksFromUrl");
	Elements items = null;
	try {
	    Document parseDoc = Jsoup.connect(url).parser(Parser.xmlParser()).get();
	    items = parseDoc.select("item");
//	    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
//	    DefaultHandler handler = new XmlTagHandler();
//	    parser.parse(url, handler);
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return items;
    }

    private List<String> getLinksFromUrl(final String url) {
	logger.info("getLinksFromUrl");
	Document doc = Jsoup.parse(getDataFromUrl(url));
	Elements re = doc.select("a");
	ArrayList<String> list = new ArrayList<String>(re.size());
	for (Element element : re) {
	    String link = element.attributes().get("href");
	    list.add(link);
	}
	return list;
    }

    private String getDataFromUrl(String url) {
	BufferedReader rd = null;
	try {
	    HttpGet request = new HttpGet(url);
	    HttpResponse response = client.execute(request);
	    rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	    String line;
	    StringBuilder sb = new StringBuilder();
	    while ((line = rd.readLine()) != null) {
		sb.append(line);
	    }
	    return sb.toString();
	} catch (Throwable t) {
	    t.printStackTrace();
	    throw new RuntimeException("Could not fetch data from " + url);
	} finally {
	    if (rd != null)
		try {
		    rd.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
    }

    public void start() {
	handle(baseUrl);
    }

    public void join() {
	logger.info("before lock inside join");
	synchronized (this.lock) {
	    logger.info("lock wait()");
	    try {
		// Remember: Here main thread is waiting.
		this.lock.wait();
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
	logger.info("after lock inside join");
	
	newsItemRepository.saveAll(entites);
	logger.info("news repository entities are stored");
    }

    public void shutdown() {
	logger.info("shutdown");
	if (client != null) {
	    try {
		client.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}

	executorService.shutdown();
	try {
	    executorService.awaitTermination(5, TimeUnit.MINUTES);
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }
}

class XmlTagHandler extends DefaultHandler {
    boolean newItem = false;
    String title = null;

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
	// System.out.println("Start Element :" + qName);
	if (qName.equals("item"))
	    newItem = true;
	else if (qName.equals("title") && newItem)
	    title = "";
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
	// System.out.println("End Element :" + qName);
	if (qName.equals("title") && newItem) {
	    System.out.println(title);
	    title = null;
	}
    }

    public void characters(char ch[], int start, int length) throws SAXException {
	if (title != null)
	    title += new String(ch, start, length);
    }
}