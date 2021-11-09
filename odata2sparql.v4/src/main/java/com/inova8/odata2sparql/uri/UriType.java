/*
 * inova8 2020
 */
package com.inova8.odata2sparql.uri;

import java.util.ArrayList;

/**
 * The Enum UriType.
 */
public enum UriType {
	
	/** Service document. */
	URI0(SystemQueryOption.$format),
	
	/** Entity set. */
	URI1(SystemQueryOption.$format, SystemQueryOption.$filter, SystemQueryOption.$inlinecount,
			SystemQueryOption.$orderby, SystemQueryOption.$skiptoken, SystemQueryOption.$skip, SystemQueryOption.$top,
			SystemQueryOption.$expand, SystemQueryOption.$select),
	
	/** Entity set with key predicate. */
	URI2(SystemQueryOption.$format, SystemQueryOption.$filter, SystemQueryOption.$expand, SystemQueryOption.$select),
	
	/** Complex property of an entity. */
	URI3(SystemQueryOption.$format),
	
	/** Simple property of a complex property of an entity. */
	URI4(SystemQueryOption.$format),
	
	/** Simple property of an entity. */
	URI5(SystemQueryOption.$format),
	/**
	 * Navigation property of an entity with target multiplicity '1' or '0..1'
	 */
	URI6A(SystemQueryOption.$format, SystemQueryOption.$filter, SystemQueryOption.$expand, SystemQueryOption.$select),
	
	/** Navigation property of an entity with target multiplicity '*'. */
	URI6B(SystemQueryOption.$format, SystemQueryOption.$filter, SystemQueryOption.$inlinecount,
			SystemQueryOption.$orderby, SystemQueryOption.$skiptoken, SystemQueryOption.$skip, SystemQueryOption.$top,
			SystemQueryOption.$expand, SystemQueryOption.$select),
	
	/** Link to a single entity. */
	URI7A(SystemQueryOption.$format, SystemQueryOption.$filter),
	
	/** Link to multiple entities. */
	URI7B(SystemQueryOption.$format, SystemQueryOption.$filter, SystemQueryOption.$inlinecount,
			SystemQueryOption.$orderby, SystemQueryOption.$skiptoken, SystemQueryOption.$skip, SystemQueryOption.$top),
	
	/** Metadata document. */
	URI8(),
	
	/** Batch request. */
	URI9(),
	
	/** Function import returning a single instance of an entity type. */
	URI10(SystemQueryOption.$format),
	
	/** Function import returning a collection of complex-type instances. */
	URI11(SystemQueryOption.$format),
	
	/** Function import returning a single instance of a complex type. */
	URI12(SystemQueryOption.$format),
	
	/** Function import returning a collection of primitive-type instances. */
	URI13(SystemQueryOption.$format),
	
	/** Function import returning a single instance of a primitive type. */
	URI14(SystemQueryOption.$format),
	
	/** Count of an entity set. */
	URI15(SystemQueryOption.$filter, SystemQueryOption.$orderby, SystemQueryOption.$skip, SystemQueryOption.$top),
	
	/** Count of a single entity. */
	URI16(SystemQueryOption.$filter),
	
	/** Media resource of an entity. */
	URI17(SystemQueryOption.$format, SystemQueryOption.$filter),
	
	/** Count of link to a single entity. */
	URI50A(SystemQueryOption.$filter),
	
	/** Count of links to multiple entities. */
	URI50B(SystemQueryOption.$filter, SystemQueryOption.$orderby, SystemQueryOption.$skip, SystemQueryOption.$top);

	/** The white list. */
	private final ArrayList<SystemQueryOption> whiteList = new ArrayList<SystemQueryOption>();

	/**
	 * Instantiates a new uri type.
	 *
	 * @param compatibleQueryOptions the compatible query options
	 */
	private UriType(final SystemQueryOption... compatibleQueryOptions) {
		for (SystemQueryOption queryOption : compatibleQueryOptions) {
			whiteList.add(queryOption);
		}
	}
}
