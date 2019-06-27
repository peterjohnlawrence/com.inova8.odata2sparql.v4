package com.inova8.odata2sparql.Utils;

import java.util.Comparator;
import org.apache.olingo.commons.api.edm.FullQualifiedName;


public class NavigationPropertyComparator implements Comparator<FullQualifiedName>{ 
    @Override
	public int compare(FullQualifiedName o1, FullQualifiedName o2) {
		return o1.getName().compareTo(o2.getName());
	}
}
