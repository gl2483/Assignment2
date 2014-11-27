package TweetMap.workers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class GetTweets implements Runnable{

	//private AWSCredentials credentials = null;
	private AmazonDynamoDBClient client;
	private AmazonSQS sqs;
    private ConfigurationBuilder cb = new ConfigurationBuilder();
    private String Url;
    private String lat;
    private String lon;
	
	public GetTweets(){
        /*try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (My Folder), and is in valid format.",
                    e);
        }*/
        AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        if (client == null) {
            client = new AmazonDynamoDBClient(credentialsProvider);
        }
        
	    cb.setDebugEnabled(true);
	    cb.setOAuthConsumerKey("OsKJ9ILJls70tmIEU2g2yULgu");
	    cb.setOAuthConsumerSecret("P9aArdYM9JMKE6eKk5cxTy5Nv9brWrSOEWBp8mf0I2Uk0m1KnZ");
	    cb.setOAuthAccessToken("2847059842-Am6r9tjdlZFJJZEPFbv3fE7aAnhoSoDEBq5JpbO");
	    cb.setOAuthAccessTokenSecret("556Lfrk9JQEx4o6gwDJWDZTfFcn1s5sjaZStw2AGzYRct");
	    
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
		}
		
	}
	
	public void run(){
		TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
		
		StatusListener listener = new StatusListener() {
			@Override
			public void onStatus(Status status) {
				try {
					//CreateQueueRequest createQueueRequest = new CreateQueueRequest("TweetQueue");
		            //String myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
		            // List queues
		            //ListQueuesRequest lqr = new ListQueuesRequest().withQueueNamePrefix("TweetQueue");
					
		            /*for (String queueUrl : sqs.listQueues().getQueueUrls()) {
		                System.out.println("  QueueUrl: " + queueUrl);
		            }*/
					//MessageAttributeValue mav = new MessageAttributeValue().withStringValue("GetTweets");
					//
					PutRecordDynamo(client, status);
					
		            Thread.sleep(2000);
		            
		        } catch (InterruptedException e) {
		            e.printStackTrace();
		        }
			}
			
			
			@Override
			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
			//System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
			}
			
			@Override
			public void onScrubGeo(long userId, long upToStatusId) {
			//System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
			}
			
			@Override
			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
			//System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
			}
			
			@Override
			public void onStallWarning(StallWarning warning) {
			//System.out.println("Got stall warning:" + warning);
			}
			
			@Override
			public void onException(Exception ex) {
			ex.printStackTrace();
			}
			
		};

		twitterStream.addListener(listener);
		twitterStream.sample();
	}
	

	private void PutRecordDynamo(AmazonDynamoDBClient pClient, Status pStatus) 
	{
		String tableName = "Tweet2";
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		if(pStatus.getGeoLocation() != null)
    	{
			if(!String.valueOf(pStatus.getGeoLocation().getLongitude()).isEmpty() && !String.valueOf(pStatus.getGeoLocation().getLatitude()).isEmpty())
			{
				setLat(String.valueOf(pStatus.getGeoLocation().getLatitude()));
				setLon(String.valueOf(pStatus.getGeoLocation().getLongitude()));
				item.put("TweetId", new AttributeValue().withS(String.valueOf(pStatus.getId())));
				//item.put("KeyString", new AttributeValue().withS(pKeyString));
				item.put("Date", new AttributeValue().withS(df.format(pStatus.getCreatedAt())));
				item.put("Message", new AttributeValue().withS(pStatus.getText()));
				item.put("Longtitude", new AttributeValue().withS(String.valueOf(pStatus.getGeoLocation().getLongitude()).isEmpty()?"EMPTY": String.valueOf(pStatus.getGeoLocation().getLongitude())));
				item.put("Latitude", new AttributeValue().withS(String.valueOf(pStatus.getGeoLocation().getLatitude()).isEmpty()?"EMPTY": String.valueOf(pStatus.getGeoLocation().getLatitude())));
				item.put("Score", new AttributeValue().withS("0"));
				//item.put("Location", new AttributeValue().withS(pStatus.getUser().getLocation().isEmpty()?"EMPTY":pStatus.getUser().getLocation()));
			}
    	}
		else if(!pStatus.getUser().getLocation().isEmpty())
		{
			String[] locations = pStatus.getUser().getLocation().split(",");
			if(locations.length == 2)
			{
				try{
					double latitude = Double.parseDouble(locations[0]);
					double longtitude = Double.parseDouble(locations[1]);
					setLat(String.valueOf(latitude));
					setLon(String.valueOf(longtitude));
					item.put("TweetId", new AttributeValue().withS(String.valueOf(pStatus.getId())));
					//item.put("KeyString", new AttributeValue().withS(pKeyString));
					item.put("Date", new AttributeValue().withS(df.format(pStatus.getCreatedAt())));
					item.put("Message", new AttributeValue().withS(pStatus.getText()));
					item.put("Longtitude", new AttributeValue().withS(String.valueOf(latitude)));
					item.put("Latitude", new AttributeValue().withS(String.valueOf(longtitude)));
					item.put("Score", new AttributeValue().withS("0"));
				}catch(Exception e) 
				{
					
				}
			}
			else
			{
				GoogleResponse res;
				try {
					res = new AddressConverter().convertToLatLong(pStatus.getUser().getLocation());
					if(res.getStatus().equals("OK"))
					{
						for(Result result : res.getResults())
						{
							//System.out.println("Lattitude of address is :"  +result.getGeometry().getLocation().getLat());
							//System.out.println("Longitude of address is :" + result.getGeometry().getLocation().getLng());
							setLat(String.valueOf(result.getGeometry().getLocation().getLat()));
							setLon(String.valueOf(result.getGeometry().getLocation().getLng()));
							item.put("TweetId", new AttributeValue().withS(String.valueOf(pStatus.getId())));
							//item.put("KeyString", new AttributeValue().withS(pKeyString));
							item.put("Date", new AttributeValue().withS(df.format(pStatus.getCreatedAt())));
							item.put("Message", new AttributeValue().withS(pStatus.getText()));
							item.put("Longtitude", new AttributeValue().withS(String.valueOf(lon)));
							item.put("Latitude", new AttributeValue().withS(String.valueOf(lat)));
							item.put("Score", new AttributeValue().withS("0"));
						}
					}
					else
					{
						System.out.println(res.getStatus());
						System.out.println("No Geo found.");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//item.put("TweetId", new AttributeValue().withS(String.valueOf(pStatus.getId())));
			//item.put("KeyString", new AttributeValue().withS(pKeyString));
			//item.put("Date", new AttributeValue().withS(df.format(pStatus.getCreatedAt())));
			//item.put("Message", new AttributeValue().withS(pStatus.getText()));
			//item.put("Longtitude", new AttributeValue().withS("EMPTY"));
			//item.put("Latitude", new AttributeValue().withS("EMPTY"));
			//item.put("Location", new AttributeValue().withS(pStatus.getUser().getLocation()));
		}
		
		if(item.size() > 0)
		{
			PutItemRequest putItemRequest = new PutItemRequest()
			  .withTableName(tableName)
			  .withItem(item);
			PutItemResult Putresult = pClient.putItem(putItemRequest);
			System.out.println("@" + pStatus.getUser().getScreenName() + " - " + pStatus.getText() + " - " + pStatus.getId());
			
					
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();
			messageAttributes.put("GetSentiment", new MessageAttributeValue()
	        .withDataType("String")
	        .withStringValue("GetSentiment"));
			sqs.sendMessage(new SendMessageRequest(Url, String.valueOf(pStatus.getId())).withMessageAttributes(messageAttributes));
					
		}
	}

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLon() {
		return lon;
	}

	public void setLon(String lon) {
		this.lon = lon;
	}
	
	
}
