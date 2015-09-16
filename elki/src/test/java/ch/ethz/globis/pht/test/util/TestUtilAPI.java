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
