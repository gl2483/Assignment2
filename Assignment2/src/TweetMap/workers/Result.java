package TweetMap.workers;

import org.codehaus.jackson.annotate.JsonIgnore;


public class Result {
 
 private String formatted_address;
 
 private boolean partial_match;
 
 private Geometry geometry;
 
 @JsonIgnore
 private Object address_components;
 
 @JsonIgnore
 private Object types;

 public String getFormatted_address() {
  return formatted_address;
 }

 public void setFormatted_address(String formatted_address) {
  this.formatted_address = formatted_address;
 }

 public boolean isPartial_match() {
  return partial_match;
 }

 public void setPartial_match(boolean partial_match) {
  this.partial_match = partial_match;
 }

 public Geometry getGeometry() {
  return geometry;
 }

 public void setGeometry(Geometry geometry) {
  this.geometry = geometry;
 }

 public Object getAddress_components() {
  return address_components;
 }

 public void setAddress_components(Object address_components) {
  this.address_components = address_components;
 }

 public Object getTypes() {
  return types;
 }

 public void setTypes(Object types) {
  this.types = types;
 }
 
 
 public static class Geometry {

	 private Location location ;
	 
	 private String location_type;
	 
	 @JsonIgnore
	 private Object bounds;
	 
	 @JsonIgnore
	 
	 private Object viewport;

	 public Location getLocation() {
	  return location;
	 }

	 public void setLocation(Location location) {
	  this.location = location;
	 }

	 public String getLocation_type() {
	  return location_type;
	 }

	 public void setLocation_type(String location_type) {
	  this.location_type = location_type;
	 }

	 public Object getBounds() {
	  return bounds;
	 }

	 public void setBounds(Object bounds) {
	  this.bounds = bounds;
	 }

	 public Object getViewport() {
	  return viewport;
	 }

	 public void setViewport(Object viewport) {
	  this.viewport = viewport;
	 }
	 
	 
	 
	 
	}
 
 public static class Location {
	 
	 private String lat;
	 
	 private String lng;

	 public String getLat() {
	  return lat;
	 }

	 public void setLat(String lat) {
	  this.lat = lat;
	 }

	 public String getLng() {
	  return lng;
	 }

	 public void setLng(String lng) {
	  this.lng = lng;
	 }
	 
	 

	}
 
 
 
}