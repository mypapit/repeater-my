package net.mypapit.mobile.myrepeater.mapinfo;

import net.mypapit.mobile.myrepeater.Repeater;

public class RepeaterMapInfo extends MapInfoObject {

    public RepeaterMapInfo(Repeater rpt) {
        isRepeater = true;
    }

    public RepeaterMapInfo(Repeater rpt, int index) {
        this(rpt);
        m_index = index;
    }

    public void setIndex(int index) {

        m_index = index;
    }


}
