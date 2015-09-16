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

import ch.ethz.globis.pht.util.BitsLong;

/**
 * 
 * @author ztilmann
 *
 */
public class TestBitsLongRemove {

	private static final int BITS = 64;
	
	@Test
	public void testCopy1() {
		long[] s = newBA(0xFFFF, 0xFFFF);
		BitsLong.removeBits(s, 0, 128);
		check(s, 0xFFFF, 0xFFFF);
	}
	
	
	@Test
	public void testCopy2() {
		long[] s = newBA(0x0F0F, 0xF0F0);
		BitsLong.removeBits(s, 0, 128);
		checkIgnoreTrailing(128, s, 0x0F0F, 0xF0F0);
	}
	
	
	@Test
	public void testCopy3() {
		long[] s = newBA(0x0F, 0xF000000000000000L);
		BitsLong.removeBits(s, 56, 4);
		check(s, 0xFF, 0);
	}
	
	
	@Test
	public void testCopy4() {
		long[] s = newBA(0x0F, 0xFF00000000000000L);
		BitsLong.removeBits(s, 64, 4);
		checkIgnoreTrailing(4, s, 0xF, 0xF000000000000000L);
	}

	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopy5() {
		long[] s = newBA(0xFF, 0xFF00000000000000L);
		BitsLong.removeBits(s, 60, 8);
		check(s, 0xFF, 0x0000000000000000L);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopy6() {
		long[] s = newBA(0x0F, 0xF000000000000000L);
		BitsLong.removeBits(s, 60, 8);
		check(s, 0x00, 0x00);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle1() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		BitsLong.removeBits(s, 59, 1);
		check(s, 0xAAAAAAAAAAAAAAB5L, 0x5555555555555554L);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle2() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		BitsLong.removeBits(s, 60, 1);
		check(s, 0xAAAAAAAAAAAAAAA5L, 0x5555555555555554L);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle3a() {
		long[] s = newBA(0x0008, 0x00);
		BitsLong.removeBits(s, 59, 1);
		check(s, 0x0010, 0x00);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle3b() {
		long[] s = newBA(0xFFFFFFFFFFFFFFF7L, 0xFFFFFFFFFFFFFFFFL);
		BitsLong.removeBits(s, 59, 1);
		checkIgnoreTrailing(1, s, 0xFFFFFFFFFFFFFFEFL, 0xFFFFFFFFFFFFFFFEL);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle4a() {
		long[] s = newBA(0x0010, 0x00);
		BitsLong.removeBits(s, 60, 1);
		check(s, 0x010, 0x00);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle4b() {
		long[] s = newBA(0xFFFFFFFFFFFFFFEFL, 0xFFFFFFFFFFFFFFFFL);
		BitsLong.removeBits(s, 59, 1);
		checkIgnoreTrailing(1, s, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFEL);
	}
	
	@Test
	public void testCopyShift1_OneByteToTwoByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0x0AAAAAAAAAAAAAAAL);
		BitsLong.removeBits(s, 60, 4);
		checkIgnoreTrailing(4, s, 0xAAAAAAAAAAAAAAA0L, 0xAAAAAAAAAAAAAAAAL);
	}
	
	@Test
	public void testCopyShift1_TwoByteToOneByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xF5AAAAAAAAAAAAAAL);
		BitsLong.removeBits(s, 62, 4);
		checkIgnoreTrailing(4, s, 0xAAAAAAAAAAAAAAABL, 0x5AAAAAAAAAAAAAAAL);
	}
	
	@Test
	public void testCopyLong1() {
		long[] s = newBA(0x000A, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		BitsLong.removeBits(s, 60, 5);
		checkIgnoreTrailing(5, s, 0x0005, 0x5555555555555555L, 0x5555555555555555L, 
				0x5555555555555555L);
	}
	
	@Test
	public void testCopySplitForwardA() {
		long[] s = newBA(0x000F, 0xF000000000000000L);
		BitsLong.removeBits(s, 61, 1);
		check(s, 0x000F, 0xE000000000000000L);
	}
	
	@Test
	public void testCopySplitForwardB() {
		long[] s = newBA(0xFFFFFFFFFFFFFFF0L, 0x0FFFFFFFFFFFFFFFL);
		BitsLong.removeBits(s, 61, 1);
		checkIgnoreTrailing(1, s, 0xFFFFFFFFFFFFFFF0L, 0x1FFFFFFFFFFFFFFFL);
	}
	
	@Test
	public void testCopySplitBackwardA() {
		long[] s = newBA(0x000F, 0xF000000000000000L);
		BitsLong.removeBits(s, 58, 1);
		check(s, 0x001F, 0xE000000000000000L);
	}
	
	@Test
	public void testCopySplitBackwardB() {
		long[] s = newBA(0xFFFFFFFFFFFFFFF0L, 0x0FFFFFFFFFFFFFFFL);
		BitsLong.removeBits(s, 58, 1);
		checkIgnoreTrailing(1, s, 0xFFFFFFFFFFFFFFE0L, 0x1FFFFFFFFFFFFFFFL);
	}
	
	
	@Test
	public void testCopyLeftA() {
		long[] s = newBA(0xFFFFFFFFFFFFFFFFL, 0x00, 0x00);
		BitsLong.removeBits(s, 62, 2);
		checkIgnoreTrailing(2, s, 0xFFFFFFFFFFFFFFFCL, 0x00, 0x00);
	}
	
	
	@Test
	public void testCopyLeftB() {
		long[] s = newBA(0x00, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		BitsLong.removeBits(s, 62, 2);
		checkIgnoreTrailing(2, s, 0x03, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
	}
	
	@Test
	public void testRemoveRandom() {
		Random r = new Random(0);
		int N = 1000*1000;
		long[][] data = new long[N][];
		long[][] data2 = new long[N][];
		int[] start = new int[N];
		int[] del = new int[N];
//		long t1 = System.currentTimeMillis();
		for (int i = 0; i < N; i++) {
			int LEN = r.nextInt(14)+1;
			data[i] = newBaPattern(LEN, r);
			data2[i] = data[i].clone();
			start[i] = r.nextInt(LEN*BITS);
			int maxIns = LEN*BITS-start[i];
			del[i] = r.nextInt(maxIns+1);
		}
//		long t2 = System.currentTimeMillis();
//		System.out.println("prepare: " + (t2-t1));
//		t1 = System.currentTimeMillis();
		for (int i = 0; i < N; i++) {
			BitsLong.removeBits(data[i], start[i], del[i]);
			//This is the old version
			removetBitsSlow(data2[i], start[i], del[i]);
			//BitsLong.removeBits0(data2[i], start[i], del[i]);
//			System.out.println("s=" + start[i] + " i=" + i + "  del=" + del[i] + "  len=" + data[i].length);
//			System.out.println("d2=" + BitsLong.toBinary(data2[i]));
//			System.out.println("d1=" + BitsLong.toBinary(data[i]));
			checkIgnoreTrailingBits(del[i], data2[i], data[i]);
		}
//		t2 = System.currentTimeMillis();
//		System.out.println("shift: " + (t2-t1));
//		System.out.println("n=" + BitsLong.getStats());
	}
	
	private void removetBitsSlow(long[] ba, int start, int nBits) {
		int bitsToShift = ba.length*BITS - start - (nBits);
		for (int i = 0; i < bitsToShift; i++) {
			int srcBit = start + (nBits) + i;
			int trgBit = start + i;
			BitsLong.setBit(ba, trgBit, BitsLong.getBit(ba, srcBit));
		}
	}
	
	private void check(long[] t, long ... expected) {
		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != t[i]) {
				assertEquals("i=" + i + " | " + BitsLong.toBinary(expected) + " / " + 
						BitsLong.toBinary(t), expected[i], t[i]);
			}
		}
	}

	private void checkIgnoreTrailing(int nTrailingBits, long[] t, long ... expected) {
		for (int i = 0; i < expected.length; i++) {
			if (i == expected.length-1) {
				long mask = (-1L) << nTrailingBits;
//				System.out.println("c-mask: "+ Bits.toBinary(mask));
				t[i] &= mask;
				expected[i] &= mask;
			}
//			System.out.println("i=" + i + " \nex= " + BitsLong.toBinary(expected, 32) + " \nac= " + 
//					BitsLong.toBinary(t));
			assertEquals("i=" + i + " | " + BitsLong.toBinary(expected, BITS) + " / " + 
					BitsLong.toBinary(t), expected[i], t[i]);
		}
	}

	private void checkIgnoreTrailingBits(int nTrailingBits, long[] t, long[] s) {
		int bytesToTest = s.length-nTrailingBits/BITS;
		for (int i = 0; i < bytesToTest; i++) {
			//this makes it much faster!
			if (s[i] != t[i]) {
				if (i == bytesToTest-1) {
					//ignore trailing bits
					int localBits = nTrailingBits%BITS;
					long mask = (-1L)<<localBits;
					if ((s[i]&mask) == (t[i]&mask)) {
						//ignore
						continue;
					}
				}
				assertEquals("i=" + i + " | " + BitsLong.toBinary(s) + " / " + 
						BitsLong.toBinary(t), s[i], t[i]);
			}
		}
	}

	private long[] newBA(long...ints) {
		long[] ba = new long[ints.length];
		for (int i = 0; i < ints.length; i++) {
			ba[i] = ints[i];
		}
		return ba;
	}
	
	private long[] newBaPattern(int n, Random r) {
		long[] ba = new long[n];
		for (int i = 0; i < n; i++) {
			ba[i] = r.nextLong();
		}
		return ba;
	}
}
