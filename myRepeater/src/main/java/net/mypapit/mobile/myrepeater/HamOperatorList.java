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

package net.mypapit.mobile.myrepeater;

import java.util.ArrayList;
import java.util.Collection;

public class HamOperatorList extends ArrayList<HamOperator> {


    /**
     *
     */
    private static final long serialVersionUID = 2527128553372925079L;

    public HamOperatorList() {
        // TODO Auto-generated constructor stub
    }

    public HamOperatorList(int capacity) {
        super(capacity);

    }

    public HamOperatorList(Collection<? extends HamOperator> collection) {
        super(collection);

    }

}
