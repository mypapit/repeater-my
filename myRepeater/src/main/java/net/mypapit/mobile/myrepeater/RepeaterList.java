/*
 * 
	MyRepeater Finder 
	Copyright 2013,2017 Mohammad Hafiz bin Ismail <mypapit@gmail.com>
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

package net.mypapit.mobile.myrepeater;

import java.util.ArrayList;
import java.util.Collections;

class RepeaterList extends ArrayList<Repeater> {

    /**
     * Custom ArrayList version for Repeater, can filter and sort repeaters.
     */
    private static final long serialVersionUID = -8386681782482908119L;

    public RepeaterList() {
        super();
    }

    public RepeaterList(int capacity) {
        super(capacity);

    }


    public RepeaterList(ArrayList<Repeater> arraylist) {
        for (Repeater repeater : arraylist) {
            this.add(repeater);
        }

    }

    /*
     * Skips linked repeater
     */
    public RepeaterList filterLink(RepeaterList input, boolean excludeLink) {
        RepeaterList output = new RepeaterList(130);

        for (Repeater temp : input) {
            if ((temp.getLink().length() > 0) && excludeLink) {

            } else {
                output.add(temp);
            }

        }

        return output;
    }

    public void sort() {
        // sort repeater list according to distance
        Collections.sort(this);
    }

}
