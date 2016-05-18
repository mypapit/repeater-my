package net.mypapit.mobile.myrepeater;

import net.mypapit.mobile.myrepeater.RepeaterAdapter.ViewHolder;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NearbyOperatorAdapter extends BaseAdapter {

	private static LayoutInflater inflater = null;
	private RepeaterList data, realdata;
	private int mLastPosition = -1;
	private Activity activity;
	private HamOperatorList hol;

	public NearbyOperatorAdapter(Activity activity, HamOperatorList hol) {
		this.activity = activity;
		this.hol = hol;

		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return hol.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return hol.get(position);
	}
	
	public HamOperator getHamOperator(int position){
		
		return hol.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.nearby_operator_row, parent, false);
			holder = new ViewHolder();
			holder.tvCallsign = (TextView) convertView.findViewById(R.id.tvNearbyHamCallsign);
			holder.tvHandle = (TextView) convertView.findViewById(R.id.tvNearbyHamHandle);
			holder.tvActiveCheckins = (TextView) convertView.findViewById(R.id.tvActiveCheckins);
			holder.tvLastSeen = (TextView) convertView.findViewById(R.id.tvNearbyHamLastSeen);
			holder.tvVerified = (TextView) convertView.findViewById(R.id.tvNearbyHamValid);
			
			
			convertView.setTag(holder);
			
			

			
			
			
		} else {
			
			this.mLastPosition = position;
			holder = (ViewHolder) convertView.getTag();
					
		}
		
		HamOperator operator = hol.get(position);
		
		holder.tvCallsign.setText(operator.getCallsign());
		holder.tvHandle.setText(operator.getHandle());
		holder.tvLastSeen.setText(operator.getLastSeen());
		holder.tvVerified.setText(operator.isValid() ? "(v)": "");
		
		holder.tvActiveCheckins.setText("Checkins: " + operator.getActiveNo());
		
		
		return convertView;
	}

	static class ViewHolder {
		private TextView tvCallsign, tvHandle, tvLastSeen, tvVerified, tvActiveCheckins;

	}

}
