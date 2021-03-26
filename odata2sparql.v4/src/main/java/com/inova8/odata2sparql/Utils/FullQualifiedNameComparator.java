/*
 * inova8 2020
 */
package com.inova8.odata2sparql.Utils;

import java.util.Comparator;
import org.apache.olingo.commons.api.edm.FullQualifiedName;


/**
 * The Class FullQualifiedNameComparator.
 */
public class FullQualifiedNameComparator implements Comparator<FullQualifiedName>{ 
    
    /**
     * Compare.
     *
     * @param o1 the o 1
     * @param o2 the o 2
     * @return the int
     */
    @Override
	public int compare(FullQualifiedName o1, FullQualifiedName o2) {
		return o1.getFullQualifiedNameAsString().compareTo(o2.getFullQualifiedNameAsString());
	}
}
