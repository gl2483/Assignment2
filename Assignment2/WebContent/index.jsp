<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="com.amazonaws.*" %>
<%@ page import="com.amazonaws.auth.*" %>
<%@ page import="com.amazonaws.services.ec2.*" %>
<%@ page import="com.amazonaws.services.ec2.model.*" %>
<%@ page import="com.amazonaws.services.s3.*" %>
<%@ page import="com.amazonaws.services.s3.model.*" %>
<%@ page import="com.amazonaws.services.dynamodbv2.*" %>
<%@ page import="com.amazonaws.services.dynamodbv2.model.*" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.PrintWriter"%>
<%@ page import="java.text.DateFormat"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Arrays"%>
<%@ page import="java.util.Date"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Map"%>

<%! // Share the client objects across threads to
    // avoid creating new clients for each web request
    private AmazonEC2         ec2;
    private AmazonS3           s3;
    private AmazonDynamoDB dynamo;
 %>

<%
    /*
     * AWS Elastic Beanstalk checks your application's health by periodically
     * sending an HTTP HEAD request to a resource in your application. By
     * default, this is the root or default resource in your application,
     * but can be configured for each environment.
     *
     * Here, we report success as long as the app server is up, but skip
     * generating the whole page since this is a HEAD request only. You
     * can employ more sophisticated health checks in your application.
     */
    if (request.getMethod().equals("HEAD")) return;
    		 
    		 String date = request.getParameter("date");
    			String key = request.getParameter("key");
%>

<%
	String pDate = "";
	String pKey = "";
	if(date != null)
		pDate = date;
	if(key != null)
		pKey = key;
    if (ec2 == null) {
        AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        ec2    = new AmazonEC2Client(credentialsProvider);
        s3     = new AmazonS3Client(credentialsProvider);
        dynamo = new AmazonDynamoDBClient(credentialsProvider);
    }
	
	DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
	String currDate = df.format(new Date());
	String str = "Hardcoded text";
	if(pDate.length() == 0)
		pDate = currDate;
	
	Condition keyCondition = new Condition().withComparisonOperator(ComparisonOperator.CONTAINS.toString()).withAttributeValueList(new AttributeValue().withS(pKey));
	Condition dateCondition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString()).withAttributeValueList(new AttributeValue().withS(pDate));
	Map<String,Condition> scanCondition = new HashMap<String,Condition>();
	if(pKey.length() != 0)
	scanCondition.put("Message", keyCondition);
	if(pDate.length() != 0)
	scanCondition.put("Date", dateCondition);
	ArrayList<String> arr = new ArrayList<String>();
	//HashMap<String, AttributeValue> expression = new HashMap<String, AttributeValue>();
	//expression.put("KeyString", new AttributeValue().withS("NFL"));
	//key.put("KeyString", new AttributeValue().withS("Hahaha"));
	ScanRequest scanRequest = new ScanRequest()
	.withTableName("Tweet2").withAttributesToGet(new String []{"Date","Longtitude","Latitude","Score"}).withScanFilter(scanCondition);
	//.withFilterExpression("KeyString equal NFL");
	
	ScanResult result = dynamo.scan(scanRequest);
	for(Map<String, AttributeValue> item : result.getItems())
	{
	if(item.values().size() == 4)
	arr.add(item.values().toString().replace("'", "").replace("{S: ", "'").replace(",}", "'"));
	}

	//ArrayList<String> arr = ((ArrayList<String>)request.getAttribute("arr"));
	//String arr = (String)request.getAttribute("arr");
	//Condition keyCondition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString()).withAttributeValueList(new AttributeValue().withS(pKey));
	//Condition dateCondition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString()).withAttributeValueList(new AttributeValue().withS(pDate));
	//Map<String,Condition> scanCondition = new HashMap<String,Condition>();
	//if(pKey.length() != 0)
		//scanCondition.put("KeyString", keyCondition);
	//if(pDate.length() != 0)
		//scanCondition.put("Date", dateCondition);
	//ArrayList<String> arr = new ArrayList<String>();
	//arr.add("40.7127837,-74.0059413");
	//HashMap<String, AttributeValue> expression = new HashMap<String, AttributeValue>();
	//expression.put("KeyString", new AttributeValue().withS("NFL"));
	//key.put("KeyString", new AttributeValue().withS("Hahaha"));
	//ScanRequest scanRequest = new ScanRequest()
	//.withTableName("TweetInfo").withAttributesToGet(new String []{"KeyString","Date","Longtitude","Latitude"}).withScanFilter(scanCondition);
	//.withFilterExpression("KeyString equal NFL");
	
	//ScanResult result = dynamo.scan(scanRequest);
	//for(Map<String, AttributeValue> item : result.getItems())
	//{
		//if(item.values().size() == 4)
		//arr.add(item.values().toString().replace("'", "").replace("{S: ", "'").replace(",}", "'"));
	//}
%>

<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-type" content="text/html; charset=utf-8">
    <title>Tweet Map</title>
    <link rel="stylesheet" href="styles/styles.css" type="text/css" media="screen">
    <link rel="stylesheet" href="//code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css">
    <SCRIPT LANGUAGE=javascript src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCbq08SqMSglrKcKeJWfc1ySZ6gyI2P6vc"></SCRIPT>
    <script src="//code.jquery.com/jquery-1.10.2.js"></script>
    <script src="//code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
    <script type="text/javascript" >
    	var locations = new Array(); 
    	var mapOptions;
    	var map;
    	var i = 1;
    	var locations = <%= arr %>
		function initialize() {
				        mapOptions = {
				          center: { lat: 40.7127837, lng: -74.0059413},
				          zoom: 8
				        };
				        map = new google.maps.Map(document.getElementById('map-canvas'),
				            mapOptions);
				        
				        /*for (var i = 0; i < locations.length; i++) {
				        	var a = locations[i][0];
				        	var marker = new google.maps.Marker({
				        		position: new google.maps.LatLng(locations[i][0], locations[i][1]),
				        		map: map
				        	});
				        }*/
					}
		google.maps.event.addDomListener(window, 'load', initialize);
		function add(){
			var marker = new google.maps.Marker({
	    		position: new google.maps.LatLng(40.7127837, -74.0059413)
	    	});
			marker.setMap(map);	
		}
		
		$(function() {
			$( "#date" ).datepicker();
			$( "#key" ).selectmenu();
			if (typeof(EventSource) !== "undefined") {
		        var eventSource = new EventSource("index");
		         
		        eventSource.onmessage = function(event) {
		        	document.getElementById('foo').innerHTML = event.data + ' ' + i + "</br>";
		        	i = i+1;
		        	var str = event.data.split(",");
		        	var lat = Number(str[0]);
		        	var lng = Number(str[1]);
		        	var marker = new google.maps.Marker({
			    		position: new google.maps.LatLng(Number(str[0]), Number(str[1]))
			    	});
					marker.setMap(map);	
		        };
			} else {
	        	document.getElementById('foo').innerHTML = "Sorry, Server-Sent Event is not supported in your browser";
	        }
		});
		

		/*function start() {
			if (typeof(EventSource) !== "undefined") {
	        var eventSource = new EventSource("index");
	         
	        eventSource.onmessage = function(event) {
	         	string = event.data.replace(/[\[]/g,'').replace(/[\]]/g,'').replace(/'/g,'').split(',');
	         	//locations = string
	         	for(var i = 0; i < string.length; i=i+4){
	         		var item = new Array();
	         		for(var j = 0; j < 4; j++){
	         			item[j] = string[i+j];
	         		}
	         		locations[i/4] = item;
	         	}
	         	initialize();
	            //document.getElementById('foo').innerHTML = locations + "</br>";
	            //document.getElementById('array').innerHTML = event.array + "</br>";
	        };
	        } else {
	        	document.getElementById('foo').innerHTML = "Sorry, Server-Sent Event is not supported in your browser";
	        }
	         
	    }
		window.addEventListener("load", start);*/
	</script>
	<style>fieldset {border: 0;}label {display: block;margin: 30px 0 0 0;}select {width: 200px;}.overflow {height: 200px;}</style>
</head>
<body>
Time: <span id="foo"></span>
array: <button onclick = 'add()'>add</button>
<table>
	<tr>
		<button action="index" method="post">post</button>
		<td>
			<form action="index" method="post">
			<fieldset><select name="key" id="key"><option selected="selected"> </option><option>NFL</option><option>Baseball</option><option>New Arrivals</option><option>Coffee</option><option>Believe</option></select></fieldset>
			<input type="text" id="date" name="date" value="<%=pDate %>">
			<input type="submit" value="Update">
			</form>
		</td>
		<td>
        	<div id="map-canvas" style="height:500px; width:800px"></div>
        </td>
    </tr>
</table>
</body>
</html>