/*
 * 
	MyRepeater Finder 
	Copyright 2013 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
	http://blog.mypapit.net/
	https://github.com/mypapit/repeater-my

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

import android.location.Location;
import android.os.Parcelable;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ListIterator;

public class Repeater extends Location implements Comparable<Repeater>, Parcelable, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 8465110382781503697L;
    private String callsign, club, location, link, notes;
    private double lat, lon, distance;
    private double downlink, shift, tone;

    /**
     * @param lat
     * @param lon
     */

    public Repeater(String provider, double lat, double lon) {
        super(provider);
        this.setProvider(provider);
        this.setLatitude(lat);
        this.setLongitude(lon);

    }

    public Repeater(String provider, String callsign, String club, String location, String link, double lat,
                    double lon, double downlink, double shift, double tone) {

        super(provider);

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

    public Repeater(String provider, Double[] coordinates) {
        // TODO Auto-generated constructor stub
        super(provider);
        this.setProvider(provider);
        this.setLatitude(coordinates[0]);
        this.setLongitude(coordinates[1]);

    }

    public Repeater(Location location) {
        // TODO Auto-generated constructor stub
        super(location);

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

    public String getDownlinkShift() {



        return new StringBuilder(Double.toString(getDownlink())).append("MHz (").append(getShift()).append(")").toString();
    }

	/*
     * public void setDistance(double lat, double lon){ distance =
	 * Distance.distance(this.lat, this.lon, lat, lon);
	 * 
	 * }
	 */

    public ListIterator<Repeater> calcDistanceAll(RepeaterList rl) {
        ListIterator<Repeater> venum = rl.listIterator();
        Repeater temp;


        while (venum.hasNext()) {
            temp = venum.next();
            temp.distanceTo(this);
            venum.set(temp);

        }
        return venum;

    }

	/*
     * calculate and set distance (non-Javadoc)
	 * 
	 * @see android.location.Location#distanceTo(android.location.Location)
	 * Accepts: Location Returns: the distance between Repeater and Location l;
	 */

    public float distanceTo(Location l) {
        this.distance = super.distanceTo(l);

        return (float) distance;

    }

    @Override
    public int compareTo(Repeater repeater) {

        if (this.distance < repeater.distance) {
            return -1;
        } else if (this.distance > repeater.distance) {
            return 1;
        }

        return 0;
    }

    public double getDistance() {

        return this.distance;

    }

    public String[] toArrayString() {

        DecimalFormat nf = new DecimalFormat("#.00");
        String distance = nf.format(this.getDistance() / 1000);

        String array[] = {this.getCallsign(), this.getClub(), "" + this.getDownlink(), "" + this.getShift(),
                "" + this.getLocation(), "" + this.getTone(), distance, this.getLatitude() + "",
                this.getLongitude() + "", "0.0", "0.0"};

        return array;

    }

}
