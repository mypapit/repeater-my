package net.mypapit.mobile.myrepeater;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import net.sf.jfuzzydate.FuzzyDateFormat;
import net.sf.jfuzzydate.FuzzyDateFormatter;

import java.io.Serializable;
import java.util.Date;

public class HamOperator extends Location implements Comparable<HamOperator>, Parcelable, Serializable {


    private String callsign, handle, status, phoneno, locality, client, deviceid, qsx, strDate;
    private Date date;
    private double lat, lng, distance;
    private int activeNo;
    private boolean valid;



    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(callsign);
        dest.writeString(handle);
        dest.writeString(status);
        dest.writeString(phoneno);
        dest.writeString(locality);
        dest.writeString(client);
        dest.writeString(deviceid);
        dest.writeString(qsx);
        dest.writeString(strDate);
        dest.writeDouble(lat);
        dest.writeDouble(lng);
        dest.writeDouble(distance);
        dest.writeInt(activeNo);
        dest.writeByte((byte) (valid ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }



    public String getHandle() {
        return handle;
    }

    public String getCallsign() {
        return callsign;
    }

    public String getStatus() {
        return status;
    }

    public String getPhoneno() {
        return phoneno;
    }

    public String getLocality() {
        return locality;
    }

    public String getClient() {
        return client;
    }

    public String getDeviceid() {
        return deviceid;
    }

    public String getQsx() {
        return qsx;
    }

    public Date getDate() {
        return date;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getDistance() {
        return distance;
    }

    public boolean isValid() {
        return valid;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getLastSeen() {


        FuzzyDateFormatter fuzzydate = FuzzyDateFormat.getInstance();


        return fuzzydate.formatDistance(date);
    }

    public int getActiveNo() {
        return this.activeNo;

    }

    public void setActiveNo(int num) {
        this.activeNo = num;

    }

    public String getStrDate() {

        return this.strDate;
    }


    /**
     * @param handle
     * @param status
     * @param phoneno
     * @param locality
     * @param client
     * @param deviceid
     * @param qsx
     * @param date
     * @param lat
     * @param lng
     * @param valid
     */
    public HamOperator(String callsign, String handle, String status, String phoneno, String locality, String client,
                       String deviceid, String qsx, Date date, String strDate, double lat, double lng, boolean valid) {
        super("Simulated");
        this.callsign = callsign;
        this.handle = handle;
        this.status = status;
        this.phoneno = phoneno;
        this.locality = locality;
        this.client = client;
        this.deviceid = deviceid;
        this.qsx = qsx;
        this.date = date;
        this.lat = lat;
        this.lng = lng;
        this.strDate = strDate;

        this.valid = valid;


        this.valid = valid;

        this.activeNo = 0;
    }


    /**
     *
     */
    private static final long serialVersionUID = 6532664149391248677L;

    public HamOperator(String provider) {
        super(provider);
        // TODO Auto-generated constructor stub
    }

    public HamOperator(Location l) {
        super(l);
        // TODO Auto-generated constructor stub
    }

    @Override
    public int compareTo(HamOperator hamoperator) {


        if (this.distance < hamoperator.distance) {
            return -1;
        } else if (this.distance > hamoperator.distance) {
            return 1;
        }

        return 0;
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


}
