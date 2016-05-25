package net.mypapit.mobile.myrepeater.mapinfo;

public class CallsignMapInfo extends MapInfoObject {
	
	
	
	public CallsignMapInfo() 
	{
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
	
	public void setIndex(int index){
		m_index=index;
		
	}
	
	public int getIndex(){
		return m_index;
		
	}
	
	

}
