package net.mypapit.mobile.myrepeater.mapinfo;

import net.mypapit.mobile.myrepeater.Repeater;

public class RepeaterMapInfo extends MapInfoObject {
	
	public RepeaterMapInfo(Repeater rpt){
		isRepeater = true;
		m_callsign = rpt.getCallsign();
		m_location_handle = rpt.getLocation();
		m_lat = rpt.getLat();
		m_lng = rpt.getLon();
		m_top = rpt.getDownlink()+" MHz";
		m_middle=""+rpt.getShift();
		m_bottom=rpt.getTone()+"Hz";
		
		
		
		
	}
	
	public void setIndex(int index){
		
		m_index = index;
	}
	
	

}
