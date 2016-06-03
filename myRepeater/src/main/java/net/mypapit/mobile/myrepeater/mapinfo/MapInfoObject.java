package net.mypapit.mobile.myrepeater.mapinfo;

public abstract class MapInfoObject {


    protected String m_callsign = "", m_location_handle = "", m_top = "", m_bottom = "", m_middle = "", m_status = "None", m_phoneno = "", m_client = "";
    protected int m_index = -100;
    protected String m_deviceid = "";
    protected boolean isRepeater = true;
    protected boolean m_verified = false;
    double m_lat = 0.0;
    double m_lng = 0.0;


    public String getCallsign() {
        return m_callsign;

    }

    public String getVerifiedFreq() {
        if (isRepeater) {
            return m_top;
        } else {

            if (m_verified) {
                return "Verified";


            } else {
                return "Unverified";
            }

        }

    }

    public String getDetailTop() {
        return m_top;
    }

    public String getDetailMiddle() {
        return m_middle;
    }


    public String getDetailBottom() {
        return m_bottom;
    }

    public String getLocationHandle() {
        return m_location_handle;

    }

    public int getIndex() {
        if (isRepeater) {
            return m_index;
        } else {
            return -32768;
        }

    }

    public String getID() {
        if (isRepeater) {
            return "repeater";
        } else {
            return m_deviceid;
        }

    }

    public double getLat() {
        return m_lat;


    }

    public double getLng() {
        return m_lng;

    }

    public String getStatus() {

        return m_status;
    }

    public boolean getIsRepeater() {
        return isRepeater;

    }


}
