package net.mypapit.mobile.myrepeater;

import android.app.Activity;
import android.view.View;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class CallsignInfoWindowAdapter implements InfoWindowAdapter {
	
	private final View myContentsView;
	
	public CallsignInfoWindowAdapter(Activity displayMap) {
		
		
		myContentsView = displayMap.getLayoutInflater().inflate(R.layout.callsigninfowindow, null);
		
	}

	@Override
	public View getInfoContents(Marker marker) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public View getInfoWindow(Marker marker) {
		// TODO Auto-generated method stub
		return null;
	}

}
