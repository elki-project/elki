/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht;

import java.io.Serializable;

/**
 * A predicate class that can for example be used to filter query results before they are returned.
 *
 * This interface needs to be serializable because in the distributed version of the PhTree, it is send
 * from the client machine to the server machine.
 *
 * @author ztilmann
 */
public interface PhPredicate extends Serializable {

	PhPredicate ACCEPT_ALL = new PhPredicate() {
    
    /**  */
    private static final long serialVersionUID = 1L;

    @Override
    public boolean test(long[] point) {
      return true;
    }
  };

	boolean test(long[] point);
	
}