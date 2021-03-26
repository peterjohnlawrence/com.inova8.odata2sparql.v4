/*
 * inova8 2020
 */

package com.inova8.odata2sparql.Exception;

/**
 * The Class OData2SparqlException.
 */
public class OData2SparqlException  extends Exception{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

    /** Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public OData2SparqlException() {
        super();
    }

    /**
     *  Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the message
     */
    public OData2SparqlException(String message) {
        super(message);
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the message
     * @param cause the cause
     * @since  1.4
     */
    public OData2SparqlException(String message, Throwable cause) {
        super(message, cause);
    }



}