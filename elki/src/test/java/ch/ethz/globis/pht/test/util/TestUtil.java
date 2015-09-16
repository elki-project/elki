package ch.ethz.globis.pht.test.util;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011-2015
Eidgenössische Technische Hochschule Zürich (ETH Zurich)
Institute for Information Systems
GlobIS Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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
