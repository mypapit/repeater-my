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
