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

package net.mypapit.mobile.myrepeater.mapinfo;

public class CallsignMapInfo extends MapInfoObject {


    public CallsignMapInfo(int index) {
        this();
        m_index = index;


    }


    public CallsignMapInfo() {
        isRepeater = false;


    }
    /*
    public CallsignMapInfo (String callsign, double lat, double lng)
	{
		this();
		m_callsign=callsign;
		m_lat = lat;
		m_lng = lng;
		
		
	}
	
	public void setCallsign(String callsign){
		m_callsign = callsign;
		
	}
	
	public void setHandle(String handle){
		m_location_handle = handle;
		
		
	}
	
	
	public void setDistance(String distance){
		m_middle = distance + " km";
		
	}
	public void setTime(String time){
		m_bottom = time;
		
	}
	
	public void setStatus(String status){
		m_status=status;	
	}
	
	public void setDeviceId(String deviceid){
		
		m_deviceid = deviceid;
	}
	
	public void setVerified(String valid){
		if (Integer.parseInt(valid)== 1) {
			m_verified = true;
		} else {
			m_verified = false;
		}
		
	}
	*/

    public void setIndex(int index) {
        m_index = index;

    }

    public int getIndex() {
        return m_index;

    }


}
