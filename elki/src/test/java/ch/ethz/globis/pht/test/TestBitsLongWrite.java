/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 * 
 * Author: Tilmann Zaeschke
 */
package ch.ethz.globis.pht.test;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.util.BitsLong;

/**
 * 
 * @author ztilmann
 *
 */
public class TestBitsLongWrite {

	private static final int BITS = 64;
	
	@Test
	public void testCopy1() {
		long[] t = new long[2];
		BitsLong.writeArray(t, 0, 64, 0x0000FFFF0000FFFFL);
		check(t, 0x0000FFFF0000FFFFL, 0);
	}
	
	
	@Test
	public void testCopy2() {
		long[] t = new long[2];
		BitsLong.writeArray(t, 64, 64, 0x00000F0F0000F0F0L);
		check(t, 0, 0x00000F0F0000F0F0L);
	}
	
	
	@Test
	public void testCopy3() {
		//int[] s = newBA(0x0F, 0xF0000000);
		long[] t = new long[2];
		//BitsLong.copyBitsLeft(s, 28, t, 24, 32);
		BitsLong.writeArray(t, 56, 32, 0xFF000000L);
		check(t, 0xFF, 0);
	}
	
	
	@Test
	public void testCopy4() {
		//int[] s = newBA(0x0F, 0xF0000000);
		long[] t = new long[2];
		//BitsLong.copyBitsLeft(s, 28, t, 32, 32);
		BitsLong.writeArray(t, 64, 32, 0xFF000000L);
		check(t, 0, 0xFF00000000000000L);
	}

	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopy5() {
		//int[] s = newBA(0x00, 0x00);
		long[] t = newBA(0xFF, 0xFF00000000000000L);
		//BitsLong.copyBitsLeft(s, 28, t, 28, 8);
		BitsLong.writeArray(t, 60, 8, 0x0L);
		check(t, 0xF0, 0x0F00000000000000L);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopy6() {
		//int[] s = newBA(0xF0, 0x0F000000);
		long[] t = newBA(0x0F, 0xF000000000000000L);
		//BitsLong.copyBitsLeft(s, 28, t, 28, 8);
		BitsLong.writeArray(t, 60, 8, 0x0F00F000000L);
		check(t, 0x00, 0x00);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle1() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		//BitsLong.copyBitsLeft(s, 28, s, 27, 1);
		BitsLong.writeArray(s, 59, 1, 0x1L);
		check(s, 0xAAAAAAAAAAAAAABAL, 0xAAAAAAAAAAAAAAAAL);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle2() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		//BitsLong.copyBitsLeft(s, 27, s, 28, 1);
		BitsLong.writeArray(s, 60, 1, 0x0L);
		check(s, 0xAAAAAAAAAAAAAAA2L, 0xAAAAAAAAAAAAAAAAL);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle3a() {
		//int[] s = newBA(0x0008, 0x00);
		long[] t = newBA(0x00, 0x00);
		//BitsLong.copyBitsLeft(s, 28, t, 27, 1);
		BitsLong.writeArray(t, 59, 1, 0x1L);
		check(t, 0x0010, 0x00);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle3b() {
		//int[] s = newBA(0xFFFFFFF7, 0xFFFFFFFF);
		long[] t = newBA(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		//BitsLong.copyBitsLeft(s, 28, t, 27, 1);
		BitsLong.writeArray(t, 59, 1, 0x0L);
		check(t, 0xFFFFFFFFFFFFFFEFL, 0xFFFFFFFFFFFFFFFFL);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle4a() {
		//int[] s = newBA(0x0010, 0x00);
		long[] t = newBA(0x00, 0x00);
		//BitsLong.copyBitsLeft(s, 27, t, 28, 1);
		BitsLong.writeArray(t, 60, 1, 0x1L);
		check(t, 0x08, 0x00);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle4b() {
		//int[] s = newBA(0xFFFFFFEF, 0xFFFFFFFF);
		long[] t = newBA(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		//BitsLong.copyBitsLeft(s, 27, t, 28, 1);
		BitsLong.writeArray(t, 60, 1, 0x0L);
		check(t, 0xFFFFFFFFFFFFFFF7L, 0xFFFFFFFFFFFFFFFFL);
	}
	
	@Test
	public void testCopyShift1_OneByteToTwoByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0x0AAAAAAAAAAAAAAAL);
		//BitsLong.copyBitsLeft(s, 28, s, 30, 4);
		BitsLong.writeArray(s, 62, 4, 0xAL);
		check(s, 0xAAAAAAAAAAAAAAAAL, 0x8AAAAAAAAAAAAAAAL);
	}
	
	@Test
	public void testCopyShift1_TwoByteToOneByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xF5AAAAAAAAAAAAAAL);
		//BitsLong.copyBitsLeft(s, 30, s, 32, 4);
		BitsLong.writeArray(s, 64, 4, 0xBL);
		check(s, 0xAAAAAAAAAAAAAAAAL, 0xB5AAAAAAAAAAAAAAL);
	}
	
//	@Test
//	public void testCopyLong1() {
//		int[] s = newBA(0x000A, 0xAAAAAAAA, 0xAAAAAAAA, 0xAAAAAAAA);
//		BitsLong.copyBitsLeft(s, 28, s, 33, 65);
//		check(s, 0x000A, 0xD5555555, 0x55555555, 0x6AAAAAAA);
//	}
	
	@Test
	public void testCopySplitForwardA() {
		//int[] s = newBA(0x000F, 0xF0000000);
		long[] t = newBA(0x0000, 0x00000000);
		//BitsLong.copyBitsLeft(s, 28, t, 29, 8);
		BitsLong.writeArray(t, 61, 8, 0xFFL);
		check(t, 0x0007, 0xF800000000000000L);
	}
	
	@Test
	public void testCopySplitForwardB() {
		//int[] s = newBA(0xFFFFFFF0, 0x0FFFFFFF);
		long[] t = newBA(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		//BitsLong.copyBitsLeft(s, 28, t, 29, 8);
		BitsLong.writeArray(t, 61, 8, 0x00L);
		check(t, 0xFFFFFFFFFFFFFFF8L, 0x07FFFFFFFFFFFFFFL);
	}
	
	@Test
	public void testCopySplitBackwardA() {
		//int[] s = newBA(0x000F, 0xF0000000);
		long[] t = newBA(0x0000, 0x00000000);
		//BitsLong.copyBitsLeft(s, 28, t, 27, 8);
		BitsLong.writeArray(t, 59, 8, 0xFFL);
		check(t, 0x001F, 0xE000000000000000L);
	}
	
	@Test
	public void testCopySplitBackwardB() {
		//int[] s = newBA(0xFFFFFFF0, 0x0FFFFFFF);
		long[] t = newBA(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		//BitsLong.copyBitsLeft(s, 28, t, 27, 8);
		BitsLong.writeArray(t, 59, 8, 0x00L);
		check(t, 0xFFFFFFFFFFFFFFE0L, 0x1FFFFFFFFFFFFFFFL);
	}
	
	
	@Test
	public void testCopyLeftA() {
		long[] s = newBA(0xFFFFFFFFFFFFFFFFL, 0x00, 0x00);
		//BitsLong.copyBitsLeft(s, 32, s, 30, 62);
		BitsLong.writeArray(s, 62, 62, 0x00L);
		check(s, 0xFFFFFFFFFFFFFFFCL, 0x00, 0x00);
	}
	
	
	@Test
	public void testCopyLeftB() {
		long[] s = newBA(0x00, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		//BitsLong.copyBitsLeft(s, 32, s, 30, 62);
		BitsLong.writeArray(s, 62, 62, 0xFFFFFFFFFFFFFFFFL);
		check(s, 0x03, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
	}
	
//	@Test
//	public void testCopyLeftBug1() {
//		short[] s = newBA(-27705, 31758, -32768, 0x00);
//		short[] t = newBA(-28416, 0x00, 0x00, 0x00);
//		System.out.println("l=" + BitsLong.toBinary(1327362106));
//		System.out.println("src=" + BitsLong.toBinary(s));
//		System.out.println("trg=" + BitsLong.toBinary(new long[]{-28416, 0x00, 0x1C77, 0xC0E8}, 16));
//		BitsLong.copyBitsLeft(s, 7, t, 35, 27);
//		System.out.println("trg=" + BitsLong.toBinary(t));
//		check(t, -28416, 0x00, 0x1C77, 0xC0E8);
//	}
	
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopyRSingle2() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		//BitsLong.copyBitsRight(s, 27, s, 28, 1);
		BitsLong.writeArray(s, 60, 1, 0x0L);
		check(s, 0xAAAAAAAAAAAAAAA2L, 0xAAAAAAAAAAAAAAAAL);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopyRSingle4a() {
		//int[] s = newBA(0x0010, 0x0000);
		long[] t = newBA(0x0000, 0x0000);
		//BitsLong.copyBitsRight(s, 27, t, 28, 1);
		BitsLong.writeArray(t, 60, 1, 0x1L);
		check(t, 0x0008, 0x0000);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopyRSingle4b() {
		//int[] s = newBA(0xFFFFFFEF, 0xFFFFFFFF);
		long[] t = newBA(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		//BitsLong.copyBitsRight(s, 27, t, 28, 1);
		BitsLong.writeArray(t, 60, 1, 0x0L);
		check(t, 0xFFFFFFFFFFFFFFF7L, 0xFFFFFFFFFFFFFFFFL);
	}
	
	@Test
	public void testCopyRShift1_OneByteToTwoByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0x0AAAAAAAAAAAAAAAL);
		//BitsLong.copyBitsRight(s, 28, s, 30, 4);
		BitsLong.writeArray(s, 62, 4, 0xAL);
		check(s, 0xAAAAAAAAAAAAAAAAL, 0x8AAAAAAAAAAAAAAAL);
	}
	
	@Test
	public void testCopyRShift1_TwoByteToOneByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xF5AAAAAAAAAAAAAAL);
		//BitsLong.copyBitsRight(s, 30, s, 32, 4);
		BitsLong.writeArray(s, 64, 4, 0xBL);
		check(s, 0xAAAAAAAAAAAAAAAAL, 0xB5AAAAAAAAAAAAAAL);
	}
	

	@Test
	public void testInsert1_OneByteToTwoByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAA0000000000000L);
		BitsLong.insertBits(s, 60, 5);
		check(60, 5, s, 0xAAAAAAAAAAAAAAAAL, 0xD555000000000000L);
	}
	
	@Test
	public void testInsert1_TwoByteToOneByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAA0000000000000L);
		BitsLong.insertBits(s, 60, 3);
		check(60, 3, s, 0xAAAAAAAAAAAAAAABL, 0x5554000000000000L);
	}
	
	@Test
	public void testInsert1_OneByteToTwoByteBIG() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xCCCCCCCCCCCCCCCCL, 0xCCCCCCCCCCCCCCCCL, 
				0xAAAAAAAAAAA00000L);
		long[] s2 = s.clone();
		BitsLong.insertBits(s, 60, 5+128);
		insertBitsSlow(s2, 60, 5+128);
		check(60, 5+128, s, s2);
	}
	
	@Test
	public void testInsert1_TwoByteToOneByteBIG() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xCCCCCCCCCCCCCCCCL, 0xCCCCCCCCCCCCCCCCL, 
				0xAAAAAAAAAAA00000L);
		long[] s2 = s.clone();
		BitsLong.insertBits(s, 60, 3+128);
		insertBitsSlow(s2, 60, 3+128);
		check(60, 3+128, s, s2);
	}
	
	@Test
	public void testInsert2() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAA0000000000000L);
		BitsLong.insertBits(s, 64, 1);
		check(s, 0xAAAAAAAAAAAAAAAAL, 0xD550000000000000L);
	}
	
	@Test
	public void testInsert3() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAA0000000000000L);
		BitsLong.insertBits(s, 63, 1);
		check(s, 0xAAAAAAAAAAAAAAAAL, 0x5550000000000000L);
	}
	
	@Test
	public void testInsert4() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0x5555555555555555L);
		BitsLong.insertBits(s, 0, 64);
		check(0, 32, s, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
	}
	
	@Test
	public void testInsert5() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAA55000000000000L);
		BitsLong.insertBits(s, 64, 64);
		check(64, 64, s, 0xAAAAAAAAAAAAAAAAL, 0xAA55000000000000L);
	}
	
	@Test
	public void testInsert_Bug1() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 
				0xAAAAAAAAAAAAAAAAL);
		BitsLong.insertBits(s, 193, 5);
		check(s, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 
				0xA955555555555555L);
	}
	
	@Test
	public void testInsert_Bug2() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 
				0xAAAAAAAAAAAAAAAAL);
		BitsLong.insertBits(s, 128, 67);
		check(s, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 
				0xB555555555555555L);
	}
	
	@Test
	public void testInsert_Bug2b() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0x0000);
		BitsLong.insertBits(s, 0, 67);
		check(s, 0xAAAAAAAAAAAAAAAAL, 0x1555555555555555L);
	}
	
	@Test
	public void testInsert_Bug2c() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL);
		BitsLong.insertBits(s, 0, 3);
		check(s, 0xB555555555555555L);
	}
	
	@Test
	public void testInsertRandom() {
		Random r = new Random(0);
		for (int i = 0; i < 1000000; i++) {
			int LEN = r.nextInt(4)+1;
			long[] s = newBaPattern(LEN, r);
			long[] x = s.clone();
			int start = r.nextInt(LEN*BITS);
			int maxIns = LEN*BITS-start;
			int ins = r.nextInt(maxIns+1);
			BitsLong.insertBits(s, start, ins);
			BitsLong.insertBits1(x, start, ins);
//			System.out.println("s=" + start + " i=" + ins);
//			System.out.println("x=" + BitsLong.toBinary(x));
//			System.out.println("s=" + BitsLong.toBinary(s));
			check(x, s);
		}
	}
	
	@Test
	public void testWriteRandom() {
		Random r = new Random(0);
		for (int i = 0; i < 1000000; i++) {
			int LEN = r.nextInt(4)+2;
			long[] s = newBaPattern(LEN, r);
			long[] t1 = s.clone();
			long[] t2 = s.clone();
			int start = r.nextInt((LEN-1)*BITS);
			int len = r.nextInt(BITS+1); //0..64
			long val = r.nextLong();
//			System.out.println("s=" + start + " len=" + len + "/" + s.length);
			BitsLong.writeArray(t1, start, len, val);
			writeArraySlow(t2, start, len, val);
//			System.out.println("v  =" + BitsLong.toBinary(val));
//			System.out.println("s  =" + BitsLong.toBinary(s));
//			System.out.println("t1 =" + BitsLong.toBinary(t1));
//			System.out.println("t2 =" + BitsLong.toBinary(t2));
			check(t2, t1);
		}
	}
	
	@Test
	public void testReadRandom() {
		Random r = new Random(0);
		for (int i = 0; i < 1000000; i++) {
			int LEN = r.nextInt(4)+2;
			long[] s = newBaPattern(LEN, r);
			int start = r.nextInt((LEN-1)*BITS);
			int len = r.nextInt(BITS+1); //0..64
//			System.out.println("s=" + start + " len=" + len + "/" + s.length);
			long v1 = BitsLong.readArray(s, start, len);
			long v2 = readArraySlow(s, start, len);
//			System.out.println("s  =" + BitsLong.toBinary(s));
//			System.out.println("v1 =" + BitsLong.toBinary(v1));
//			System.out.println("v2 =" + BitsLong.toBinary(v2));
			assertEquals(v2, v1);
		}
	}
	
	@Test
	public void testInsert_BugI1() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 
				0xAAAAAAAAAAAAAAAAL);
		long[] t = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 
				0xAAAAAAAAAAAAAAAAL);
		BitsLong.insertBits(s, 54, 4);
		BitsLong.insertBits1(t, 54, 4);
		check(s, t);
	}
	

	
	private void insertBitsSlow(long[] ba, int start, int nBits) {
		int bitsToShift = ba.length*BITS - start - nBits;
		for (int i = 0; i < bitsToShift; i++) {
			int srcBit = ba.length*BITS - nBits - i - 1;
			int trgBit = ba.length*BITS - i - 1;
			BitsLong.setBit(ba, trgBit, BitsLong.getBit(ba, srcBit));
		}

	}
	
	private void writeArraySlow(long[] ba, int posTrg, int len, long v) {
		for (int i = 0; i < len; i++) {
			int srcBit = BITS - len + i;
			int trgBit = posTrg + i;
			BitsLong.setBit(ba, trgBit, getBit(v, srcBit));
		}

	}
	
	private long readArraySlow(long[] ba, int posTrg, int len) {
		long r = 0;
		for (int i = 0; i < len; i++) {
			int trgBit = BITS - len + i;
			int srcBit = posTrg + i;
			r = setBit(r, trgBit, BitsLong.getBit(ba, srcBit));
		}
		return r;
	}
	
    public static boolean getBit(long l, int posBit) {
        return (l & (0x8000000000000000L >>> posBit)) != 0;
	}

    public static long setBit(long ba, int posBit, boolean b) {
        if (b) {
            return ba | (0x8000000000000000L >>> posBit);
        } else {
            return ba & (~(0x8000000000000000L >>> posBit));
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

	private void check(int posIgnore, int lenIgnore, long[] t, long ... expected) {
		for (int i = 0; i < expected.length; i++) {
			if (posIgnore / 64 <= i && i <= (posIgnore + lenIgnore)/64) {
				long mask = -1L;
				if (posIgnore / 64 == i) {
					mask = (mask<<(posIgnore%64)) >>> (posIgnore%64);
				}
				if (i == (posIgnore + lenIgnore)/64) {
					int end = (posIgnore+lenIgnore) % 64;
					mask = (mask >>> (64-end)) << (64-end);
				}
				mask = ~mask;
				//System.out.println("c-mask: "+ Bits.toBinary(mask));
				t[i] &= mask;
				expected[i] &= mask;
			}
//			System.out.println("i=" + i + " \nex= " + BitsLong.toBinary(expected) + " \nac= " + 
//					BitsLong.toBinary(t));
			assertEquals("i=" + i + " | " + BitsLong.toBinary(expected) + " / " + 
					BitsLong.toBinary(t), expected[i], t[i]);
		}
	}

	private long[] newBA(long...ints) {
		long[] ba = new long[ints.length];
		for (int i = 0; i < ints.length; i++) {
			ba[i] = ints[i];
		}
		return ba;
	}

	private long[] newBaPattern(int n, Random R) {
		long[] ba = new long[n];
		for (int i = 0; i < n; i++) {
			ba[i] = R.nextLong();
		}
		return ba;
	}
}
