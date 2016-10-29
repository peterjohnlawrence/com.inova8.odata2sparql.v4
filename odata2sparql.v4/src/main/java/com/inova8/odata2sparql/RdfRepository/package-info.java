/**
 * Provides management of the RdfRepository instances, where a RdfRepository provides and interface between odata2sparql and the underlying triple store.
 * In the case of a triple store that is accessed via a SPARQL endpoint then the RdfRepository declares at least a data implementation, and optionally a model implementation.
 * A SPARQL implementation will define the query endpoint, and optionally the update endpoint.
 */
/**
 * @author Peter Lawrence
 *
 */
package com.inova8.odata2sparql.RdfRepository;