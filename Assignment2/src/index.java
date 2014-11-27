

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;

import org.apache.commons.codec.binary.Base64;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import TweetMap.workers.GetTweets;
import TweetMap.workers.SNSHelper;
import TweetMap.workers.SNSMessage;

import com.amazonaws.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.auth.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet implementation class index
 */
@WebServlet(urlPatterns={"/index"}, asyncSupported=true)
public class index extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public index() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    private final Queue<AsyncContext> ongoingRequests = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService service;
    
    private AmazonEC2         ec2;
    private AmazonDynamoDB dynamo;
    private String pDate;
    private String pKeystring;
    private String pMessage = "";
    
    
	
	
    @Override
    public void init(ServletConfig config) throws ServletException {
    	final Runnable notifier = new Runnable() {
            @Override
            public void run() {
              final Iterator<AsyncContext> iterator = ongoingRequests.iterator();
              //not using for : in to allow removing items while iterating
              while (iterator.hasNext()) {
                AsyncContext ac = iterator.next();
                //Random random = new Random();
                final ServletResponse res = ac.getResponse();
                PrintWriter out;
                if(!pMessage.isEmpty())
                {
	                try {
	                  out = res.getWriter();
	                  Random randomGenerator = new Random();
	          		int pos = randomGenerator.nextInt(50);
	          		//String.valueOf(pos) + "," + String.valueOf(pos) + "," + String.valueOf(ongoingRequests.size()) + Thread.currentThread().getName()
	                  String next = "data: " + pMessage + "\n\n";
	                  out.write(next);
	                  pMessage = "";
	                  if (out.checkError()) { 
	                    iterator.remove();
	                  }
	                } catch (IOException ignored) {
	                  iterator.remove();
	                }
	                System.out.println("Msseage sent.");
                }
              }
            }
          };
          service = Executors.newScheduledThreadPool(1);
          service.scheduleAtFixedRate(notifier, 0, 1, TimeUnit.SECONDS);
          //ExecutorService executor = Executors.newFixedThreadPool(1);
          //executor.execute(notifier);
    }
    
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
    	response.setContentType("text/event-stream");
		response.setCharacterEncoding("UTF-8");
		request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
		//pDate = request.getParameter("date");
		final AsyncContext ac = request.startAsync();
		ac.setTimeout(0);
		ac.addListener(new AsyncListener() {
		      @Override public void onComplete(AsyncEvent event) throws 
		        IOException {ongoingRequests.remove(ac);/**/}
		      @Override public void onTimeout(AsyncEvent event) throws 
		        IOException {ongoingRequests.remove(ac);/**/}
		      @Override public void onError(AsyncEvent event) throws 
		        IOException {ongoingRequests.remove(ac);/**/}
		      @Override public void onStartAsync(AsyncEvent event) throws 
		         IOException {}
		    });
		//while(true){
		ongoingRequests.add(ac);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//String date = request.getParameter("date");
		
		AWSCredentialsProvider credentials = new ClasspathPropertiesFileCredentialsProvider();
		
		
		
		//Get the message type header.
		String messagetype = request.getHeader("x-amz-sns-message-type");
		//If message doesn't have the message type header, don't process it.
		if (messagetype == null)
			return;

		// Parse the JSON message in the message body
		// and hydrate a Message object with its contents 
		// so that we have easy access to the name/value pairs 
		// from the JSON message.
		Scanner scan = new Scanner(request.getInputStream());
		StringBuilder builder = new StringBuilder();
		while (scan.hasNextLine()) {
			builder.append(scan.nextLine());
		}

		SNSMessage msg = readMessageFromJson(builder.toString());

		// The signature is based on SignatureVersion 1. 
		// If the sig version is something other than 1, 
		// throw an exception.
		if (msg.getSignatureVersion().equals("1")) {
			// Check the signature and throw an exception if the signature verification fails.
			if (isMessageSignatureValid(msg))
				System.out.println(">>Signature verification succeeded");
			else {
				System.out.println(">>Signature verification failed");
				throw new SecurityException("Signature verification failed.");
			}
		}
		else {
			System.out.println(">>Unexpected signature version. Unable to verify signature.");
			throw new SecurityException("Unexpected signature version. Unable to verify signature.");
		}
		
		// Process the message based on type.
		if (messagetype.equals("Notification")) {
			//TODO: Do something with the Message and Subject.
			//Just log the subject (if it exists) and the message.
			String logMsgAndSubject = ">>Notification received from topic " + msg.getTopicArn();
			if (msg.getSubject() != null)
				logMsgAndSubject += " Subject: " + msg.getSubject();
			logMsgAndSubject += " Message: " + msg.getMessage();
			String mes = msg.getMessage();
			System.out.println(logMsgAndSubject);
			pMessage = msg.getMessage();
			
		} 
		else if (messagetype.equals("SubscriptionConfirmation"))
		{
			//TODO: You should make sure that this subscription is from the topic you expect. Compare topicARN to your list of topics 
			//that you want to enable to add this endpoint as a subscription.

			//Confirm the subscription by going to the subscribeURL location 
			//and capture the return value (XML message body as a string)
			Scanner sc = new Scanner(new URL(msg.getSubscribeURL()).openStream());
			StringBuilder sb = new StringBuilder();
			while (sc.hasNextLine()) {
				sb.append(sc.nextLine());
			}
			System.out.println(">>Subscription confirmation (" + msg.getSubscribeURL() +") Return value: " + sb.toString());
			//TODO: Process the return value to ensure the endpoint is subscribed.
			SNSHelper.INSTANCE.confirmTopicSubmission(msg);
		}
		else if (messagetype.equals("UnsubscribeConfirmation")) {
			//TODO: Handle UnsubscribeConfirmation message. 
			//For example, take action if unsubscribing should not have occurred.
			//You can read the SubscribeURL from this message and 
			//re-subscribe the endpoint.
			System.out.println(">>Unsubscribe confirmation: " + msg.getMessage());
		}
		else {
			//TODO: Handle unknown message type.
			System.out.println(">>Unknown message type.");
		}
		System.out.println(">>Done processing message: " + msg.getMessageId());
		
		
		
		
		
		
		
		pKeystring = request.getParameter("key");
		//request.setAttribute("date", date);
		pDate = request.getParameter("date");
		
		//final AsyncContext ac = request.startAsync();
		//ongoingRequests.add(ac);
		
		System.out.println("In doPost");
		//request.getRequestDispatcher("index.jsp").forward(request,response);
		//ArrayList<String> arr = new ArrayList<String>();
    	//arr.add("40.7127837,-74.0059413");
		//String arr = "some str";
    	//request.getSession().setAttribute("arr", pDate);
    	//request.getRequestDispatcher("index.jsp").forward(request,response);
		//doGet(request,response);
	}
	
	private boolean isMessageSignatureValid(SNSMessage msg) {

		try {
			URL url = new URL(msg.getSigningCertUrl());
			InputStream inStream = url.openStream();
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
			inStream.close();

			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(cert.getPublicKey());
			sig.update(getMessageBytesToSign(msg));
			return sig.verify(Base64.decodeBase64(msg.getSignature().getBytes()));
		}
		catch (Exception e) {
			throw new SecurityException("Verify method failed.", e);

		}
	}
	
	private byte[] getMessageBytesToSign(SNSMessage msg) {

		byte [] bytesToSign = null;
		if (msg.getType().equals("Notification"))
			bytesToSign = buildNotificationStringToSign(msg).getBytes();
		else if (msg.getType().equals("SubscriptionConfirmation") || msg.getType().equals("UnsubscribeConfirmation"))
			bytesToSign = buildSubscriptionStringToSign(msg).getBytes();
		return bytesToSign;
	}
	
	private static String buildNotificationStringToSign( SNSMessage msg) {
		String stringToSign = null;

		//Build the string to sign from the values in the message.
		//Name and values separated by newline characters
		//The name value pairs are sorted by name 
		//in byte sort order.
		stringToSign = "Message\n";
		stringToSign += msg.getMessage() + "\n";
		stringToSign += "MessageId\n";
		stringToSign += msg.getMessageId() + "\n";
		if (msg.getSubject() != null) {
			stringToSign += "Subject\n";
			stringToSign += msg.getSubject() + "\n";
		}
		stringToSign += "Timestamp\n";
		stringToSign += msg.getTimestamp() + "\n";
		stringToSign += "TopicArn\n";
		stringToSign += msg.getTopicArn() + "\n";
		stringToSign += "Type\n";
		stringToSign += msg.getType() + "\n";
		return stringToSign;
	}
	
	private static String buildSubscriptionStringToSign(SNSMessage msg) {
		String stringToSign = null;
		//Build the string to sign from the values in the message.
		//Name and values separated by newline characters
		//The name value pairs are sorted by name 
		//in byte sort order.
		stringToSign = "Message\n";
		stringToSign += msg.getMessage() + "\n";
		stringToSign += "MessageId\n";
		stringToSign += msg.getMessageId() + "\n";
		stringToSign += "SubscribeURL\n";
		stringToSign += msg.getSubscribeURL() + "\n";
		stringToSign += "Timestamp\n";
		stringToSign += msg.getTimestamp() + "\n";
		stringToSign += "Token\n";
		stringToSign += msg.getToken() + "\n";
		stringToSign += "TopicArn\n";
		stringToSign += msg.getTopicArn() + "\n";
		stringToSign += "Type\n";
		stringToSign += msg.getType() + "\n";
		return stringToSign;
	}
	
	private SNSMessage readMessageFromJson(String string) {
		ObjectMapper mapper = new ObjectMapper(); 
		SNSMessage message = null;
		try {
			message = mapper.readValue(string, SNSMessage.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return message;
	}

}
