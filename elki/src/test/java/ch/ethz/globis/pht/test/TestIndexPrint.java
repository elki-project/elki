package ch.ethz.globis.pht.test;

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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.PhTree;
import ch.ethz.globis.pht.test.util.TestUtil;
import ch.ethz.globis.pht.util.Bits;

public class TestIndexPrint {

	private PhTree<long[]> create(int dim, int depth) {
		return TestUtil.newTree(dim, depth);
	}
	
	@Test
	public void testPrint() {
		final int N = 1000;
		final Random R = new Random(0);
		
		PhTree<long[]> ind = create(3, 32);
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt(), R.nextInt()};
			ind.put(v, v);
		}

		ArrayList<String> keys = new ArrayList<>();
		Iterator<long[]> it = ind.queryExtent();
		while (it.hasNext()) {
			long[] v = it.next();
			keys.add(Bits.toBinary(v, ind.getDEPTH()));
		}
	
		String out = ind.toStringPlain();
		for (String key: keys) {
			assertTrue("key=" + key, out.contains(key));
		}
	}

	@Test
	public void testPrintBug() {
		final int N = 5;
		final Random R = new Random(4);
		
		PhTree<long[]> ind = create(3, 32);
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt(), R.nextInt()};
			ind.put(v, v);
		}

		ArrayList<String> keys = new ArrayList<>();
		Iterator<long[]> it = ind.queryExtent();
		while (it.hasNext()) {
			long[] v = it.next();
			keys.add(Bits.toBinary(v, ind.getDEPTH()));
		}
	
		String out = ind.toStringPlain();
		for (String key: keys) {
			assertTrue("key=" + key, out.contains(key));
		}
	}

}
