package com.inova8.odata2sparql.Utils;

import java.util.Comparator;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;

public class RdfNodeComparator implements Comparator<RdfNode>{ 
    @Override
	public int compare(RdfNode o1, RdfNode o2) {
		return o1.toString().compareTo(o2.toString());
	}
}
