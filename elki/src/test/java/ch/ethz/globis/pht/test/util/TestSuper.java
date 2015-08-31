/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.test.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class TestSuper {

	@SuppressWarnings("deprecation")
  @BeforeClass
	public static void setUpClass() {
		TestUtil.beforeClass();
	}

	@SuppressWarnings("deprecation")
  @AfterClass
	public static void tearDownClass() {
		TestUtil.afterClass();
	}

	@Before
	public void setUp() {
		TestUtil.beforeTest();
	}

	@SuppressWarnings("deprecation")
  @After
	public void tearDown() {
		TestUtil.afterTest();
	}

}
