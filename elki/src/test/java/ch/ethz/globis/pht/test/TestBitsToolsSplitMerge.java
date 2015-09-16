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

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.util.BitTools;


public class TestBitsToolsSplitMerge {

	@Test
	public void testSplitMerge32() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			long l = Math.abs(rnd.nextInt());
			BitTools.split(l, a, 0, 1, 32);
			long l2 = BitTools.merge(a, 0, 1, 32);
			assertEquals(l, l2);
		}
	}

	@Test
	public void testSplitMerge63() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			long l = rnd.nextLong()>>>1;
			BitTools.split(l, a, 0, 1, 63);
			long l2 = BitTools.merge(a, 0, 1, 63);
			assertEquals(l, l2);
		}
	}

	@Test
	public void testSplitMerge64() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			long l = rnd.nextLong();
			BitTools.split(l, a, 0, 1, 64);
			long l2 = BitTools.merge(a, 0, 1, 64);
			assertEquals(l, l2);
		}
	}
	
	@Test
	public void testSplitMergeFloat() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			float f = rnd.nextFloat();
			long l = BitTools.toSortableLong(f);
			BitTools.split(l, a, 0, 1, 32);
			long l2 = BitTools.merge(a, 0, 1, 32);
			assertEquals(l, l2);
			float f2 = BitTools.toFloat(l2);
			assertEquals(f, f2, 0.0);
		}
	}

	
	@Test
	public void testSplitMergeDouble() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			double d = rnd.nextDouble();
			long l = BitTools.toSortableLong(d);
			//System.err.println("ARGGHHH 63!!!!");
			BitTools.split(l, a, 0, 1, 63);
			long l2 = BitTools.merge(a, 0, 1, 63);
			assertEquals(l, l2);
			double d2 = BitTools.toDouble(l2);
			assertEquals(d, d2, 0.0);
		}
	}
}
