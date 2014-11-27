package TweetMap.workers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import twitter4j.conf.ConfigurationBuilder;

import com.alchemyapi.api.AlchemyAPI;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class GetSentiment implements Runnable{

	private AmazonDynamoDBClient client;
    private AmazonSNSClient snsClient;
	
    
    private String TweetId;
    
    public GetSentiment(String pTweetId){
    	TweetId = pTweetId;
    	AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        if (client == null) {
            client = new AmazonDynamoDBClient(credentialsProvider);
        }
        
	    snsClient = new AmazonSNSClient(new ClasspathPropertiesFileCredentialsProvider());		                           
	    snsClient.setRegion(Region.getRegion(Regions.US_EAST_1));
	    
	    
    }
    
    public void run(){
        
        /*ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(Url);
        List<Message> messages = sqs.receiveMessage(receiveMessageRequest.withMessageAttributeNames("GetSentitment")).getMessages();
        for (Message message : messages) {
            System.out.println("  Message");
            System.out.println("    MessageId:     " + message.getMessageId());
            System.out.println("    ReceiptHandle: " + message.getReceiptHandle());
            System.out.println("    MD5OfBody:     " + message.getMD5OfBody());
            System.out.println("    Body:          " + message.getBody());
            for (Entry<String, MessageAttributeValue> entry : message.getMessageAttributes().entrySet()) {
                //System.out.println("  Attribute " + message.getMessageAttributes());
                //System.out.println("  Attribute " + message.getAttributes());
                System.out.println("    Name:  " + entry.getKey());
                System.out.println("    Value: " + entry.getValue());
            }
            
        String messageRecieptHandle = message.getReceiptHandle();
        sqs.deleteMessage(new DeleteMessageRequest(Url, messageRecieptHandle));
        System.out.println("    MessageId:     " + message.getMessageId() + "deleted");
        }*/	
    	Map<String,AttributeValue> key = new HashMap<String,AttributeValue>();
    	key.put("TweetId", new AttributeValue().withS(TweetId));
    	GetItemRequest getItemRequest = new GetItemRequest().withKey(key).withTableName("Tweet2");
    	Map<String,AttributeValue> Tweet = client.getItem(getItemRequest).getItem();
    	String MessageText = Tweet.get("Message").getS();
    	String Latitude = Tweet.get("Latitude").getS();
    	String Longitude = Tweet.get("Longtitude").getS();
    	
        AlchemyAPI alchemy = AlchemyAPI.GetInstanceFromString("fdb634555cadc5742bf86742dadc035d39b3153a");
        Document doc;
		try {
			doc = alchemy.TextGetTextSentiment(MessageText);
			String score = getStringFromDocument(doc);
	    	Tweet.replace("Score", new AttributeValue().withS(score));
			System.out.println("score: " +  score);
			
			String topicArn = "arn:aws:sns:us-east-1:505059448688:MyNewTopic";
			PublishRequest publishRequest = new PublishRequest(topicArn, Latitude + "," + Longitude + "," + score);
			PublishResult publishResult = snsClient.publish(publishRequest);
		} catch (XPathExpressionException | IOException | SAXException
				| ParserConfigurationException e) {
			// TODO Auto-generated catch block
			System.out.println("Alchemy parsing error: " + e.toString());
		}
    }
    
 // utility method
 	private static String getStringFromDocument(Document doc) {
 	//try {
 	//DOMSource domSource = new DOMSource(doc);
 	
 		NodeList nodeList = doc.getDocumentElement().getChildNodes();
 		for (int i = 0; i < nodeList.getLength(); i++) {
 			Node node = nodeList.item(i);
 			if (node.getNodeName().equals("docSentiment")) {
 				NodeList nodes = node.getChildNodes();
 				for (int j = 0; j < nodes.getLength(); j++) {
 					Node tag = nodes.item(j);
 					if (tag.getNodeName().equals("score")) {
 						return tag.getTextContent();
 					}
 				}
 			}
 		}
 		return "0";
 	/*StringWriter writer = new StringWriter();
 	StreamResult result = new StreamResult(writer);
 	TransformerFactory tf = TransformerFactory.newInstance();
 	Transformer transformer = tf.newTransformer();
 	transformer.transform(domSource, result);
 	return writer.toString();
 	} catch (TransformerException ex) {
 	ex.printStackTrace();
 	return null;
 	}*/
 	}
}
