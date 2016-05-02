/*
 * 
 * This is the file for duplicating Repeater class functionality because of
 * pesky protected Location class  
 * This application requires (and has been compiled with) Google Play Service rev. 13
 *  
	MyRepeater Finder 
	Copyright 2014,2016 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
	http://blog.mypapit.net/
	http://repeater-my.googlecode.com/

	This file is part of MyRepeater Finder.

    MyRepeater Finder is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MyRepeater Finder is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MyRepeater Finder.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.mypapit.mobile.myrepeater;

import java.io.Serializable;
import java.text.DecimalFormat;

public class FauxRepeater implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8465110382781503697L;
	private String callsign, club, location, link, notes, provider;
	private double lat, lon, distance;
	private double downlink, shift, tone;

	public FauxRepeater() {
		// TODO Auto-generated constructor stub
	}

	public FauxRepeater(Repeater rpt) {
		this.callsign = rpt.getCallsign();
		this.club = rpt.getClub();
		this.location = rpt.getLocation();
		this.link = rpt.getLink();
		this.notes = rpt.getNotes();
		this.provider = rpt.getProvider();
		this.lat = rpt.getLatitude();
		this.lon = rpt.getLongitude();
		this.downlink = rpt.getDownlink();
		this.shift = rpt.getShift();
		this.tone = rpt.getTone();
		this.distance = 0.0;

	}

	public FauxRepeater(String provider, String callsign, String club, String location, String link, double lat,
			double lon, double downlink, double shift, double tone) {

		this.provider = provider;

		this.callsign = callsign;
		this.club = club;
		this.location = location;
		this.link = link;
		this.notes = "";
		this.lat = lat;
		this.lon = lon;
		this.downlink = downlink;
		this.shift = shift;
		this.tone = tone;

		this.setLatitude(lat);
		this.setLongitude(lon);

	}

	public void setLatitude(double lat) {
		this.lat = lat;
	}

	public void setLongitude(double lon) {
		this.lon = lon;
	}

	public String getCallsign() {
		return callsign;
	}

	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}

	public String getClub() {
		return club;
	}

	public void setClub(String club) {
		this.club = club;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public double getDownlink() {
		return downlink;
	}

	public void setDownlink(double downlink) {
		this.downlink = downlink;
	}

	public double getShift() {
		return shift;
	}

	public void setShift(double shift) {
		this.shift = shift;
	}

	public double getTone() {
		return tone;
	}

	public void setTone(double tone) {
		this.tone = tone;
	}

	public double getDistance() {

		return 0.0;

	}

	public String[] toArrayString() {

		DecimalFormat nf = new DecimalFormat("#.00");
		String distance = nf.format(this.getDistance() / 1000);

		String array[] = { this.getCallsign(), this.getClub(), "" + this.getDownlink(), "" + this.getShift(),
				"" + this.getLocation(), "" + this.getTone(), distance };

		return array;

	}

	public Repeater getRepeater() {
		Repeater repeater = new Repeater("", callsign, club, location, link, lat, lon, downlink, shift, tone);

		return repeater;

	}

}
