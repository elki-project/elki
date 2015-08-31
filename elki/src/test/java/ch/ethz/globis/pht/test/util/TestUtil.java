/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.test.util;

import ch.ethz.globis.pht.PhTree;

public abstract class TestUtil {

	private static TestUtilAPI INSTANCE;
	
	private static TestUtilAPI getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new TestUtilInMemory();
		}
		return INSTANCE;
	}
	
	/**
	 * Explicitly set the TestUtil implementation.
	 * @param util
	 */
	public static void setTestUtil(TestUtilAPI util) {
		INSTANCE = util;
	}

	public static <T> PhTree<T> newTree(int dim, int depth) {
		return getInstance().newTreeV(dim, depth);
	}
	
	public static <T> void close(PhTree<T> tree) {
		getInstance().close(tree);
	}
	
	/**
	 * Creates a ZooKeeper if none exists.
	 */
	public static void beforeTest() {
		getInstance().beforeTest();
	}
	
	/**
	 * Creates a special ZooKeeper.
	 */
	public static void beforeTest(Object[] args) {
		getInstance().beforeTest(args);
	}
	
	/**
	 * Do we need this?
	 * @deprecated
	 */
	public static void afterTest() {
		getInstance().afterTest();
	}

	
	/**
	 * Do we need this?
	 * @deprecated
	 */
	public static void beforeClass() {
		getInstance().beforeClass();
	}

	/**
	 * Do we need this?
	 * @deprecated
	 */
	public static void afterClass() {
		getInstance().afterClass();
	}

	
	/**
	 * Do we need this?
	 * @deprecated
	 */
	public static void beforeSuite() {
		getInstance().beforeSuite();
	}

	/**
	 * For example for shutting down ZooKeeper.
	 */
	public static void afterSuite() {
		getInstance().afterSuite();
	}
}
