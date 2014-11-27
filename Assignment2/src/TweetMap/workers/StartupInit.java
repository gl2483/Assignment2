package TweetMap.workers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;

public class StartupInit implements ServletContextListener {

	private AmazonSQS sqs;
	private String Url;
	//private ThreadPool pool;
	
    public void contextInitialized(ServletContextEvent event) {
        // Webapp startup.
    	/*System.out.println("Starting webapp init");
    	AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
    	if(sqs == null){
	    	sqs = new AmazonSQSClient(credentialsProvider);
	    	Region usEast1 = Region.getRegion(Regions.US_EAST_1);
	    	sqs.setRegion(usEast1);
	    }
    	try{
			ListQueuesResult queue = sqs.listQueues("TweetQueue");
			Url = queue.getQueueUrls().get(0);
		} catch (IndexOutOfBoundsException ase) {
			CreateQueueRequest createQueueRequest = new CreateQueueRequest("TweetQueue");
	        Url = sqs.createQueue(createQueueRequest).getQueueUrl();
		}*/
    	System.out.println("Stating 5 threads");
    	Runnable pool = new ThreadPool(5);
    	ExecutorService executor = Executors.newFixedThreadPool(2);
    	Runnable GetTweets = new GetTweets();
    	executor.execute(GetTweets);
    	executor.execute(pool);
    	//System.out.println("Starting servlet index");
    }

    public void contextDestroyed(ServletContextEvent event) {
        // Webapp shutdown.
    	//pool.shutdown();
    	System.out.println("Stopping webapp");
    }


}
