/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.test.util;

import ch.ethz.globis.pht.PhTree;

public interface TestUtilAPI {

	public <T> PhTree<T> newTreeV(int dim, int depth);

	public <T> void close(PhTree<T> tree);
	public void beforeTest();
	public void beforeTest(Object[] args);
	public void afterTest();
	public void beforeSuite();
	public void afterSuite();
	public void beforeClass();
	public void afterClass();
}
