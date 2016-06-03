package net.mypapit.mobile.myrepeater;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NearbyOperatorAdapter extends BaseAdapter {

    private static LayoutInflater inflater = null;



    private HamOperatorList hol;

    public NearbyOperatorAdapter(Context context, HamOperatorList hol) {

        this.hol = hol;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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

    public HamOperator getHamOperator(int position) {

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

            //this.mLastPosition = position;
            holder = (ViewHolder) convertView.getTag();

        }

        HamOperator operator = hol.get(position);

        holder.tvCallsign.setText(truncate(operator.getCallsign().toUpperCase(), 10));
        holder.tvHandle.setText(truncate(operator.getHandle(), 25));
        holder.tvLastSeen.setText(operator.getLastSeen());
        holder.tvVerified.setText(operator.isValid() ? "(v)" : "");

        holder.tvActiveCheckins.setText("Checkins: " + operator.getActiveNo());


        return convertView;
    }

    public static String truncate(final String text, int length) {
        // The letters [iIl1] are slim enough to only count as half a character.
        length += Math.ceil(text.replaceAll("[^iIl]", "").length() / 2.0d);

        if (text.length() > length) {
            return text.substring(0, length - 1) + "\u2026";
        }

        return text;
    }


    static class ViewHolder {
        private TextView tvCallsign, tvHandle, tvLastSeen, tvVerified, tvActiveCheckins;

    }


}
