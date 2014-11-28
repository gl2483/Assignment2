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
	.withTableName("Tweet2").withAttributesToGet(new String []{"Date","Latitude","Longtitude","Score"}).withScanFilter(scanCondition);
	//.withFilterExpression("KeyString equal NFL");
	
	ScanResult result = dynamo.scan(scanRequest);
	for(Map<String, AttributeValue> item : result.getItems())
	{
	if(item.values().size() == 4)
	arr.add(item.values().toString().replace("'", "").replace("{S: ", "'").replace(",}", "'"));
	}
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
    <script src="https://maps.googleapis.com/maps/api/js?v=3.exp&libraries=visualization"></script>
    <script type="text/javascript" >
    	//var locations = new Array(); 
    	var mapOptions;
    	var map;
    	var i = 1;
        var positive = 'http://s18.postimg.org/8lskwd11h/positive_small.png';
        var neutraul = 'http://s29.postimg.org/z714fof5f/neutrual_small.png';
        var negative = 'http://s28.postimg.org/ktomb2gh5/negative_small.png';
        //var positive = 'images/positive_small.png';
        //var neutraul = 'images/neutrual_small.png';
        //var negative = 'images/negative_small.png';
    	var locations = <%= arr %>
    	var heatData = [];
    	var heatmap;
		function initialize() {
				        mapOptions = {
				          center: { lat: 40.46366700000001, lng: -3.74922},
				          zoom: 2
				        };
				        map = new google.maps.Map(document.getElementById('map-canvas'),
				            mapOptions);
				        for (var i = 0; i < locations.length; i++) {
				        	var score = locations[i][0];
				        	var image;
				        	if(Number(score) == 0)
				        		image = neutraul;
				        	else if(Number(score) > 0)
				        		image = positive;
				        	else
				        		image = negative;
				        	heatData.push(new google.maps.LatLng(locations[i][2], locations[i][1]));
				        	var marker = new google.maps.Marker({
				        		position: new google.maps.LatLng(locations[i][2], locations[i][1]),
				        		map: map,
				        		icon: image
				        	});
				        }/**/
				        
				        var pointArray = new google.maps.MVCArray(heatData);

				        heatmap = new google.maps.visualization.HeatmapLayer({
				          data: pointArray
				        });

				        heatmap.setMap(map);
				        heatmap.set('radius', heatmap.get('radius') ? null : 50);
					}
		
		
		
		google.maps.event.addDomListener(window, 'load', initialize);
		
		
		
		
		
		function add(){
			if(heatmap.getMap())
				heatmap.setMap(null);
			else
			{
				var pointArray = new google.maps.MVCArray(heatData);
	
		        heatmap = new google.maps.visualization.HeatmapLayer({
		          data: pointArray
		        });
		        heatmap.setMap(map);
		        heatmap.set('radius', heatmap.get('radius') ? null : 50);/**/
			}
	        
			//heatmap.setMap(heatmap.getMap() ? null : map);
		}
		
		$(function() {
			$( "#date" ).datepicker();
			$( "#key" ).selectmenu();
			if (typeof(EventSource) !== "undefined") {
		        var eventSource = new EventSource("index");
		         
		        eventSource.onmessage = function(event) {
		        	document.getElementById('foo').innerHTML = event.data + '  count: ' + i + "</br>";
		        	i = i+1;
		        	var str = event.data.split(",");
		        	var lat = Number(str[0]);
		        	var lng = Number(str[1]);
		        	var s = Number(str[2]);
		        	heatData.push(new google.maps.LatLng(lat, lng));
		        	var image;
		        	if(s == 0)
		        		image = neutraul;
		        	else if(s > 0)
		        		image = positive;
		        	else
		        		image = negative;
		        	var marker = new google.maps.Marker({
		        		animation: google.maps.Animation.DROP,
						position: new google.maps.LatLng(Number(str[0]), Number(str[1])),
		        		icon: image
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
<table>
<tr>
	<td>
		New point: <span id="foo"></span>
	</td>
</tr>
<tr>
	<td>	
		<button onclick = 'add()'>Toggle HeatMap</button>
	</td>
</tr>
</table>
<table>
	<tr>
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