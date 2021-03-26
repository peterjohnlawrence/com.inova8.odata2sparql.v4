/*
 * inova8 2020
 */
package com.inova8.odata2sparql.Utils;

import java.util.Comparator;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;

/**
 * The Class RdfNodeComparator.
 */
public class RdfNodeComparator implements Comparator<RdfNode>{ 
    
    /**
     * Compare.
     *
     * @param o1 the o 1
     * @param o2 the o 2
     * @return the int
     */
    @Override
	public int compare(RdfNode o1, RdfNode o2) {
		return o1.toString().compareTo(o2.toString());
	}
}
