/*
 * 
	MyRepeater Finder 
	Copyright 2013 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
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

public class StaticLocation {
	private String name,statename;
	public String getStatename() {
		return statename;
	}
	public void setStatename(String statename) {
		this.statename = statename;
	}
	private double lat,lon;
	
	
	/**
	 * @param name - name of the place
	 * @param lat - latitude
	 * @param lon - longitude
	 */
	public StaticLocation(String name, String statename, double lat, double lon) {
		this.name = name;
		this.statename=statename;
		this.lat = lat;
		this.lon = lon;
	}
	@Override
	public String toString() {
		return name+","+lat+","+"lon";
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
	
	
	
	

}
