/*
 * Copyright 2011 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.util.BitsLong;

/**
 * 
 * @author ztilmann
 *
 */
public class TestBitsLong {

	private static final int BITS = 64;
	
	@Test
	public void testCopy1() {
		long[] s = newBA(0xFFFF, 0xFFFF);
		long[] t = new long[2];
		BitsLong.copyBitsLeft(s, 0, t, 0, 128);
		check(t, 0xFFFF, 0xFFFF);
	}
	
	
	@Test
	public void testCopy2() {
		long[] s = newBA(0x0F0F, 0xF0F0);
		long[] t = new long[2];
		BitsLong.copyBitsLeft(s, 0, t, 0, 128);
		check(t, 0x0F0F, 0xF0F0);
	}
	
	
	@Test
	public void testCopy3() {
		long[] s = newBA(0x0F, 0xF000000000000000L);
		long[] t = new long[2];
		BitsLong.copyBitsLeft(s, 60, t, 56, 64);
		check(t, 0xFF, 0);
	}
	
	
	@Test
	public void testCopy4() {
		long[] s = newBA(0x0F, 0xF000000000000000L);
		long[] t = new long[2];
		BitsLong.copyBitsLeft(s, 60, t, 64, 64);
		check(t, 0, 0xFF00000000000000L);
	}

	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopy5() {
		long[] s = newBA(0x00, 0x00);
		long[] t = newBA(0xFF, 0xFF00000000000000L);
		BitsLong.copyBitsLeft(s, 60, t, 60, 8);
		check(t, 0xF0, 0x0F00000000000000L);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopy6() {
		long[] s = newBA(0xF0, 0x0F00000000000000L);
		long[] t = newBA(0x0F, 0xF000000000000000L);
		BitsLong.copyBitsLeft(s, 60, t, 60, 8);
		check(t, 0x00, 0x00);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle1() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		BitsLong.copyBitsLeft(s, 60, s, 59, 1);
		check(s, 0xAAAAAAAAAAAAAABAL, 0xAAAAAAAAAAAAAAAAL);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle2() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		BitsLong.copyBitsLeft(s, 59, s, 60, 1);
		check(s, 0xAAAAAAAAAAAAAAA2L, 0xAAAAAAAAAAAAAAAAL);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle3a() {
		long[] s = newBA(0x0008, 0x00);
		long[] t = newBA(0x00, 0x00);
		BitsLong.copyBitsLeft(s, 60, t, 59, 1);
		check(t, 0x0010, 0x00);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle3b() {
		long[] s = newBA(0xFFFFFFFFFFFFFFF7L, 0xFFFFFFFFFFFFFFFFL);
		long[] t = newBA(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		BitsLong.copyBitsLeft(s, 60, t, 59, 1);
		check(t, 0xFFFFFFFFFFFFFFEFL, 0xFFFFFFFFFFFFFFFFL);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle4a() {
		long[] s = newBA(0x0010, 0x00);
		long[] t = newBA(0x00, 0x00);
		BitsLong.copyBitsLeft(s, 59, t, 60, 1);
		check(t, 0x08, 0x00);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopySingle4b() {
		long[] s = newBA(0xFFFFFFFFFFFFFFEFL, 0xFFFFFFFFFFFFFFFFL);
		long[] t = newBA(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		BitsLong.copyBitsLeft(s, 59, t, 60, 1);
		check(t, 0xFFFFFFFFFFFFFFF7L, 0xFFFFFFFFFFFFFFFFL);
	}
	
	@Test
	public void testCopyShift1_OneByteToTwoByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0x0AAAAAAAAAAAAAAAL);
		long[] t = newBA(0xAAAAAAAAAAAAAAAAL, 0x0AAAAAAAAAAAAAAAL);
		BitsLong.copyBitsLeft(s, 60, t, 62, 4);
		check(t, 0xAAAAAAAAAAAAAAAAL, 0x8AAAAAAAAAAAAAAAL);
	}
	
	@Test
	public void testCopyShift1_TwoByteToOneByte() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xF5AAAAAAAAAAAAAAL);
		long[] t = newBA(0xAAAAAAAAAAAAAAAAL, 0xF5AAAAAAAAAAAAAAL);
		BitsLong.copyBitsLeft(s, 62, t, 64, 4);
		check(t, 0xAAAAAAAAAAAAAAAAL, 0xB5AAAAAAAAAAAAAAL);
	}
	
	@Test
	public void testCopyLong1() {
		long[] s = newBA(0x000A, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		long[] t = newBA(0x000A, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
		BitsLong.copyBitsLeft(s, 60, t, 65, 129);
		check(t, 0x000A, 0xD555555555555555L, 0x5555555555555555L, 0x6AAAAAAAAAAAAAAAL);
	}
	
	@Test
	public void testCopySplitForwardA() {
		long[] s = newBA(0x000F, 0xF000000000000000L);
		long[] t = newBA(0x0000, 0x00000000);
		BitsLong.copyBitsLeft(s, 60, t, 61, 8);
		check(t, 0x0007, 0xF800000000000000L);
	}
	
	@Test
	public void testCopySplitForwardB() {
		long[] s = newBA(0xFFFFFFFFFFFFFFF0L, 0x0FFFFFFFFFFFFFFFL);
		long[] t = newBA(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		BitsLong.copyBitsLeft(s, 60, t, 61, 8);
		check(t, 0xFFFFFFFFFFFFFFF8L, 0x07FFFFFFFFFFFFFFL);
	}
	
	@Test
	public void testCopySplitBackwardA() {
		long[] s = newBA(0x000F, 0xF000000000000000L);
		long[] t = newBA(0x0000, 0x00000000);
		BitsLong.copyBitsLeft(s, 60, t, 59, 8);
		check(t, 0x001F, 0xE000000000000000L);
	}
	
	@Test
	public void testCopySplitBackwardB() {
		long[] s = newBA(0xFFFFFFFFFFFFFFF0L, 0x0FFFFFFFFFFFFFFFL);
		long[] t = newBA(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		BitsLong.copyBitsLeft(s, 60, t, 59, 8);
		check(t, 0xFFFFFFFFFFFFFFE0L, 0x1FFFFFFFFFFFFFFFL);
	}
	
	
	@Test
	public void testCopyLeftA() {
		long[] s = newBA(0xFFFFFFFFFFFFFFFFL, 0x00, 0x00);
		BitsLong.copyBitsLeft(s, 64, s, 62, 126);
		check(s, 0xFFFFFFFFFFFFFFFCL, 0x00, 0x00);
	}
	
	
	@Test
	public void testCopyLeftB() {
		long[] s = newBA(0x00, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
		BitsLong.copyBitsLeft(s, 64, s, 62, 126);
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
		System.err.println("WARNING: Test disabled: TestBitsLong.testCopyRSingle2()");
//		long[] s = newBA(0xAAAAAAAA, 0xAAAAAAAA);
//		BitsLong.copyBitsRight(s, 27, s, 28, 1);
//		check(s, 0xAAAAAAA2, 0xAAAAAAAA);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopyRSingle4a() {
		System.err.println("WARNING: Test disabled: TestBitsLong.testCopyRSingle4a()");
//		long[] s = newBA(0x0010, 0x0000);
//		long[] t = newBA(0x0000, 0x0000);
//		BitsLong.copyBitsRight(s, 27, t, 28, 1);
//		check(t, 0x0008, 0x0000);
	}
	
	/**
	 * Check retain set bits.
	 */
	@Test
	public void testCopyRSingle4b() {
		System.err.println("WARNING: Test disabled: TestBitsLong.testCopyRSingle4b()");
//		long[] s = newBA(0xFFFFFFEF, 0xFFFFFFFF);
//		long[] t = newBA(0xFFFFFFFF, 0xFFFFFFFF);
//		BitsLong.copyBitsRight(s, 27, t, 28, 1);
//		check(t, 0xFFFFFFF7, 0xFFFFFFFF);
	}
	
	@Test
	public void testCopyRShift1_OneByteToTwoByte() {
		System.err.println("WARNING: Test disabled: TestBitsLong.testCopyRShift1_OneByteToTwoByte()");
//		long[] s = newBA(0xAAAAAAAA, 0x0AAAAAAA);
//		BitsLong.copyBitsRight(s, 28, s, 30, 4);
//		check(s, 0xAAAAAAAA, 0x8AAAAAAA);
	}
	
	@Test
	public void testCopyRShift1_TwoByteToOneByte() {
		System.err.println("WARNING: Test disabled: TestBitsLong.testCopyRShift1_TwoByteToOneByte()");
//		long[] s = newBA(0xAAAAAAAA, 0xF5AAAAAA);
//		BitsLong.copyBitsRight(s, 30, s, 32, 4);
//		check(s, 0xAAAAAAAA, 0xB5AAAAAA);
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
				0xAAA0000000000000L);
		long[] s2 = s.clone();
		BitsLong.insertBits(s, 60, 5+128);
		insertBitsSlow(s2, 60, 5+128);
		check(60, 5+128, s, s2);
	}
	
	@Test
	public void testInsert1_TwoByteToOneByteBIG() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0xCCCCCCCCCCCCCCCCL, 0xCCCCCCCCCCCCCCCCL, 
				0xAAA0000000000000L);
		BitsLong.insertBits(s, 60, 3+128);
		check(60, 3+128, s, 0xAAAAAAAAAAAAAAABL, 0x5554000000000000L);
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
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0x5555);
		BitsLong.insertBits(s, 0, 64);
		check(0, 64, s, 0xAAAAAAAAAAAAAAAAL, 0xAAAAAAAAAAAAAAAAL);
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
		int N = 1000*1000;
		long[][] data = new long[N][];
		long[][] data2 = new long[N][];
		int[] start = new int[N];
		int[] ins = new int[N];
//		long t1 = System.currentTimeMillis();
		for (int i = 0; i < N; i++) {
			int LEN = r.nextInt(14)+1;
			data[i] = newBaPattern(LEN, r);
			data2[i] = data[i].clone();
			start[i] = r.nextInt(LEN*BITS+1);
			int maxIns = LEN*BITS-start[i];
			ins[i] = r.nextInt(maxIns+1);
		}
//		long t2 = System.currentTimeMillis();
//		System.out.println("prepare: " + (t2-t1));
//		t1 = System.currentTimeMillis();
		for (int i = 0; i < N; i++) {
			BitsLong.insertBits(data[i], start[i], ins[i]);
			//This is the old version
			BitsLong.insertBits1(data2[i], start[i], ins[i]);
//			System.out.println("s=" + start + " i=" + ins);
//			System.out.println("x=" + BitsLong.toBinary(x));
//			System.out.println("s=" + BitsLong.toBinary(s));
			check(data2[i], data[i]);
		}
//		t2 = System.currentTimeMillis();
//		System.out.println("shift: " + (t2-t1));
//		System.out.println("n=" + BitsLong.getStats());
	}
	
	@Test
	public void testInsert_Bug3() {
		long[] s = newBA(0x804D3F2, 0xF130A329, 0xE8DAF3B7, 0x9CF00000, 0x0, 0x0, 0x0, 0x0);
		long[] t = newBA(0x804D3F2, 0xF130A329, 0xE8DAF3B7, 0x9CF00000, 0x0, 0x0, 0x0, 0x0);
		BitsLong.insertBits(s, 13, 192);
		BitsLong.insertBits1(t, 13, 192);
		check(13, 192, s, t);
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

	
	@Test
	public void testCopyLeftRandom() {
		Random rnd = new Random(0);
		int BA_LEN = 6;
		long[] ba = new long[BA_LEN];
		for (int i1 = 0; i1 < 1000000; i1++) {
			//populate
			for (int i2 = 0; i2 < ba.length; i2++) {
				ba[i2] = rnd.nextLong();
			}
			
			//clone
			long[] ba1 = Arrays.copyOf(ba, ba.length);
			long[] ba2 = Arrays.copyOf(ba, ba.length);
			
			int start = rnd.nextInt(64) + 64;  //start somewhere in fourth short
			int nBits = rnd.nextInt(64 * 3); //remove up to two shorts 
			//compute
			//System.out.println("i=" + i1 + " start=" + start + " nBits=" + nBits);
			BitsLong.copyBitsLeft(ba1, start+nBits, ba1, start, ba1.length*BITS-start-nBits);
			//compute backup
			copyBitsLeftSlow(ba2, start+nBits, ba2, start, ba2.length*BITS-start-nBits);
			
			//check
			if (!Arrays.equals(ba2, ba1)) {
				System.out.println("i=" + i1 + " start=" + start + " nBits=" + nBits);
				System.out.println("i=" + i1 + " posSrc=" + (start+nBits) + " posTrg=" + start + 
						" n=" + (ba1.length*BITS-start-nBits));
				System.out.println("ori. = " + BitsLong.toBinary(ba));
				System.out.println("act. = " + BitsLong.toBinary(ba1));
				System.out.println("exp. = " + BitsLong.toBinary(ba2));
				System.out.println("ori. = " + Arrays.toString(ba));
				System.out.println("act. = " + Arrays.toString(ba1));
				System.out.println("exp. = " + Arrays.toString(ba2));
				fail();
			}
		}
	}
	
	@Test
	public void testCopyLeftRandomRndLen() {
		Random rnd = new Random(0);
		int BA_LEN = 10;//TODO
		long[] ba = new long[BA_LEN];
		for (int i1 = 0; i1 < 1000000; i1++) {
			//populate
			for (int i2 = 0; i2 < ba.length; i2++) {
				ba[i2] = rnd.nextLong();
			}
			
			//clone
			long[] ba1 = Arrays.copyOf(ba, ba.length);
			long[] ba2 = Arrays.copyOf(ba, ba.length);
			
			int start = rnd.nextInt(ba.length*BITS);  //start somewhere in fourth short
			int dest = rnd.nextInt(start+1);   //can be 0! 
			int nBits = rnd.nextInt(ba.length*BITS-start);
			//compute
			//System.out.println("i=" + i1 + " start=" + start + " nBits=" + nBits);
			BitsLong.copyBitsLeft(ba1, start, ba1, dest, nBits);
			//compute backup
			copyBitsLeftSlow(ba2, start, ba2, dest, nBits);
			
			//check
			if (!Arrays.equals(ba2, ba1)) {
				System.out.println("i=" + i1 + " start=" + start + " nBits=" + nBits);
				System.out.println("i=" + i1 + " posSrc=" + (start) + " posTrg=" + dest + 
						" nBits=" + nBits);
				System.out.println("ori. = " + BitsLong.toBinary(ba));
				System.out.println("act. = " + BitsLong.toBinary(ba1));
				System.out.println("exp. = " + BitsLong.toBinary(ba2));
				System.out.println("ori. = " + Arrays.toString(ba));
				System.out.println("act. = " + Arrays.toString(ba1));
				System.out.println("exp. = " + Arrays.toString(ba2));
				check(ba1, ba2);
				fail();
			}
		}
	}
	
	/**
	 * Copy to other allows start<dest.
	 */
	@Test
	public void testCopyLeftRandomToOther() {
		Random rnd = new Random(0);
		int BA_LEN = 10;
		long[] ba = new long[BA_LEN];
		for (int i1 = 0; i1 < 1000000; i1++) {
			//populate
			for (int i2 = 0; i2 < ba.length; i2++) {
				ba[i2] = rnd.nextLong();
			}
			
			//clone
			long[] src = newBaPattern(BA_LEN, rnd);
			long[] ba1 = Arrays.copyOf(src, src.length);
			long[] ba2 = Arrays.copyOf(src, src.length);
			
			int start = rnd.nextInt(ba.length*BITS);  //start somewhere in fourth short
			int dest = rnd.nextInt(ba.length*BITS);   //can be 0! 
			int nBits = rnd.nextInt(ba.length*BITS-(start<dest?dest:start));
			//compute
			//System.out.println("i=" + i1 + " start=" + start + " nBits=" + nBits);
			BitsLong.copyBitsLeft(src, start, ba1, dest, nBits);
			//compute backup
			copyBitsLeftSlow(src, start, ba2, dest, nBits);
			
			//check
			if (!Arrays.equals(ba2, ba1)) {
				System.out.println("i=" + i1 + " start=" + start + " nBits=" + nBits);
				System.out.println("i=" + i1 + " posSrc=" + (start) + " posTrg=" + dest + 
						" nBits=" + nBits);
				System.out.println("ori. = " + BitsLong.toBinary(ba));
				System.out.println("act. = " + BitsLong.toBinary(ba1));
				System.out.println("exp. = " + BitsLong.toBinary(ba2));
				System.out.println("ori. = " + Arrays.toString(ba));
				System.out.println("act. = " + Arrays.toString(ba1));
				System.out.println("exp. = " + Arrays.toString(ba2));
				fail();
			}
		}
	}
	
	@Test
	public void testCopyRightRandom() {
		System.err.println("WARNING: Test disabled: TestBitsLong.testCopyRightRandom()");
//		Random rnd = new Random();
//		int BA_LEN = 6;
//		long[] ba = new int[BA_LEN];
//		for (int i1 = 0; i1 < 1000000; i1++) {
//			//populate
//			for (int i2 = 0; i2 < ba.length; i2++) {
//				ba[i2] = rnd.nextInt();
//			}
//			
//			//clone
//			long[] ba1 = Arrays.copyOf(ba, ba.length);
//			long[] ba2 = Arrays.copyOf(ba, ba.length);
//			
//			int start = rnd.nextInt(8) + 8;  //start somewhere in fourth short
//			int nBits = rnd.nextInt(8 * 3); //remove up to three shorts 
//			//compute
//			//System.out.println("i=" + i1 + " start=" + start + " nBits=" + nBits);
//			BitsLong.copyBitsRight(ba1, start, ba1, start+nBits, ba1.length*BITS-start-nBits);
//			//compute backup
//			insertBitsSlow(ba2, start, nBits);
//			
//			//check
//			if (!Arrays.equals(ba2, ba1)) {
//				System.out.println("i=" + i1 + " start=" + start + " nBits=" + nBits);
//				System.out.println("ori. = " + BitsLong.toBinary(ba));
//				System.out.println("act. = " + BitsLong.toBinary(ba1));
//				System.out.println("exp. = " + BitsLong.toBinary(ba2));
//				System.out.println("ori. = " + Arrays.toString(ba));
//				System.out.println("act. = " + Arrays.toString(ba1));
//				System.out.println("exp. = " + Arrays.toString(ba2));
//				fail();
//			}
//		}
	}
	
	@Test
	public void testGetBit() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0x5555555555555555L, 
				0xFFFFFFFFFFFFFFFFL, 0x0L, 0xAAAA0000FFFF5555L);
		long[] t = newBA(0xF0F0F0F0F0F0F0F0L, 0xF0F0F0F0F0F0F0F0L, 0xF0F0F0F0F0F0F0F0L, 
				0xF0F0F0F0F0F0F0F0L, 0xF0F0F0F0F0F0F0F0L);
		for (int i = 0; i < BITS*s.length; i++) {
    		BitsLong.setBit(t, i, BitsLong.getBit(s, i));
		}
		check(t, s);
	}
	
	@Test
	public void testCopySlow() {
		long[] s = newBA(0xAAAAAAAAAAAAAAAAL, 0x5555555555555555L, 
				0xFFFFFFFFFFFFFFFFL, 0x0L, 0xAAAA0000FFFF5555L);
		long[] t = newBA(0xF0F0F0F0F0F0F0F0L, 0xF0F0F0F0F0F0F0F0L, 0xF0F0F0F0F0F0F0F0L, 
				0xF0F0F0F0F0F0F0F0L, 0xF0F0F0F0F0F0F0F0L);
		copyBitsLeftSlow(s, 0, t, 0, BITS*s.length);
		check(t, s);

		long[] t2 = newBA(0xF0F0F0F0F0F0F0F0L, 0xF0F0F0F0F0F0F0F0L, 0xF0F0F0F0F0F0F0F0L, 
				0xF0F0F0F0F0F0F0F0L, 0xF0F0F0F0F0F0F0F0L);
		BitsLong.copyBitsLeft(s, 0, t2, 0, BITS*s.length);
		check(t2, s);
	}
	
	@Test
	public void testCopyLeftBug1() {
		long[] s = {-1155484576, -723955400, 1033096058, -1690734402, -1557280266, 1327362106};
		//long[] t = new long[6]; //act. = [-591608102401, -370665164800, 528945182207, -865656013313, -797327496192, 1327362106]
		long[] e = {-591608102401L, -370665164800L, 528945182207L, -865656013313L, -797327496192L, 679609398330L};
								
		//ori. = 11111111.11111111.11111111.11111111.10111011.00100000.10110100.01100000, 11111111.11111111.11111111.11111111.11010100.11011001.01010001.00111000, 00000000.00000000.00000000.00000000.00111101.10010011.11001011.01111010, 11111111.11111111.11111111.11111111.10011011.00111001.01110000.10111110, 11111111.11111111.11111111.11111111.10100011.00101101.11001001.11110110, 00000000.00000000.00000000.00000000.01001111.00011101.11110000.00111010, 
		//act. = 11111111.11111111.11111111.01110110.01000001.01101000.11000001.11111111, 11111111.11111111.11111111.10101001.10110010.10100010.01110000.00000000, 00000000.00000000.00000000.01111011.00100111.10010110.11110101.11111111, 11111111.11111111.11111111.00110110.01110010.11100001.01111101.11111111, 11111111.11111111.11111111.01000110.01011011.10010011.11101100.00000000, 00000000.00000000.00000000.00000000.01001111.00011101.11110000.00111010, 
		//exp. = 11111111.11111111.11111111.01110110.01000001.01101000.11000001.11111111, 11111111.11111111.11111111.10101001.10110010.10100010.01110000.00000000, 00000000.00000000.00000000.01111011.00100111.10010110.11110101.11111111, 11111111.11111111.11111111.00110110.01110010.11100001.01111101.11111111, 11111111.11111111.11111111.01000110.01011011.10010011.11101100.00000000, 00000000.00000000.00000000.10011110.00111011.11100000.01110100.00111010, 
		BitsLong.copyBitsLeft(s, 21, s, 12, 363);
		check(s, e);
	}
	
	@Test
	public void testCopyLeftBug2() {
		long[] s = {1842130704, 1599145891, -1341955486, 1631478226, 1754478786, -1370798799};
		//long[] t = new long[6]; //act. = [-591608102401, -370665164800, 528945182207, -865656013313, -797327496192, 1327362106]
		long[] e = {1842130704, 1599145891, -1341955486, 1631478226, 1754478786, -1370798799};
								
		//ori. = 00000000.00000000.00000000.00000000.01101101.11001100.10101111.00010000, 00000000.00000000.00000000.00000000.01011111.01010001.00000111.10100011, 11111111.11111111.11111111.11111111.10110000.00000011.01100010.01100010, 00000000.00000000.00000000.00000000.01100001.00111110.01100001.11010010, 00000000.00000000.00000000.00000000.01101000.10010011.00111000.11000010, 11111111.11111111.11111111.11111111.10101110.01001011.01000101.00110001, 
		//act. = 00000000.00000000.00000000.00000000.01101101.11001100.10101111.00010000, 00000000.00000000.00000000.00000000.01011111.01010001.00000111.10100011, 00000000.00111111.11111111.11111111.10110000.00000011.01100010.01100010, 00000000.00000000.00000000.00000000.01100001.00111110.01100001.11010010, 00000000.00000000.00000000.00000000.01101000.10010011.00111000.11000010, 00000000.00111111.11111111.11111111.10101110.01001011.01000101.00110001, 
		//exp. = 00000000.00000000.00000000.00000000.01101101.11001100.10101111.00010000, 00000000.00000000.00000000.00000000.01011111.01010001.00000111.10100011, 11111111.11111111.11111111.11111111.10110000.00000011.01100010.01100010, 00000000.00000000.00000000.00000000.01100001.00111110.01100001.11010010, 00000000.00000000.00000000.00000000.01101000.10010011.00111000.11000010, 11111111.11111111.11111111.11111111.10101110.01001011.01000101.00110001, 
		BitsLong.copyBitsLeft(s, 10, s, 10, 374);
		check(s, e);
	}
	
	@Test
	public void testCopyLeftBug3() {
		//src=01100000.11111111.11000000.00000000.00000000.00000000.00000000.00000000, 00000011.11111111.00000000.00000000.00000000.00000000.00000000.00000000, 00000111.11111111.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000010.00000000.00000000.00000000.00000000.00000000.00000000, 00110000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 
		long[] src1 = {6989516252934832128L, 287948901175001088L, 576179277326712832L, 
				562949953421312L, 3458764513820540928L, 0};
		//p1=8
		//dst=01100010.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 
		long[] dst1a={7061644215716937728L, 0, 0, 0, 0, 0, 0, 0};
		long[] dst1b={7061644215716937728L, 0, 0, 0, 0, 0, 0, 0};
		//p2=10
		//n=124
		BitsLong.copyBitsLeft(src1, 8, dst1a, 10, 124);
		copyBitsLeftSlow(src1, 8, dst1b, 10, 124);
		//System.out.println(BitsLong.toBinary(src1));
		check(dst1a, dst1b);
		
		//src=01100000.11111111.11000000.00000000.00000000.00000000.00000000.00000000, 00000011.11111111.00000000.00000000.00000000.00000000.00000000.00000000, 00000111.11111111.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000010.00000000.00000000.00000000.00000000.00000000.00000000, 00110000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 
		long[] src2 = {6989516252934832128L, 287948901175001088L, 576179277326712832L, 
				562949953421312L, 3458764513820540928L, 0};

		//p1=134
		//dst=01100011.00111111.11110000.00000000.00000000.00000000.00000000.00000000, 00000000.11111111.11000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 
		long[] dst2a = {7151698616078303232L, 71987225293750272L, 0, 0, 0, 0, 0, 0};
		long[] dst2b = {7151698616078303232L, 71987225293750272L, 0, 0, 0, 0, 0, 0};
		//p2=134
		//n=124
		BitsLong.copyBitsLeft(src2, 134, dst2a, 134, 124);
		copyBitsLeftSlow(src2, 134, dst2b, 134, 124);
		check(dst2a, dst2b);

		//src=01100000.11111111.11000000.00000000.00000000.00000000.00000000.00000000, 00000011.11111111.00000000.00000000.00000000.00000000.00000000.00000000, 00000111.11111111.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000010.00000000.00000000.00000000.00000000.00000000.00000000, 00110000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 
		long[] src3 = {6989516252934832128L, 287948901175001088L, 576179277326712832L, 
				562949953421312L, 3458764513820540928L, 0};
		//p1=260
		//dst=01100011.01111111.11110000.00000000.00000000.00000000.00000000.00000000, 00000000.11111111.11000000.00000000.00000000.00000000.00000000.00000000, 00000011.11111111.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000010.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 00000000.00000000.00000000.00000000.00000000.00000000.00000000.00000000, 
		long[] dst3a = {7169713014587785216L, 71987225293750272L, 287948901175001088L, 
				562949953421312L, 0, 0, 0, 0};
		long[] dst3b = {7169713014587785216L, 71987225293750272L, 287948901175001088L, 
				562949953421312L, 0, 0, 0, 0};
		//p2=382
		//n=124
		BitsLong.copyBitsLeft(src3, 260, dst3a, 382, 124);
		copyBitsLeftSlow(src3, 260, dst3b, 382, 124);
		check(dst3a, dst3b);
	}
	
	@Test
	public void copyBitsLeftBug4() {
		long[] s = {//-4922475540349336432L, -3370274031255729188L, -2971948390163211717L, 
				//-8730854458729406051L, -1856941488587171136L, 
				//2470753881526567411L, 
				//1532230816302433446L, 
				//-395233716872471639L, 
				-5121607137339136235L, 
				-5256749969541153749L};
//		long[] r1 = {-4922475540349336432L, -3370274031255729188L, -2971948390163211717L, 
//				-8730854458729406051L, -1856941488587171136L, 2480197071496421608L, 
//				8671562125591512230L, -395233716872471639L, -5121607137339136235L, 
//				-5256749969541153749L};
//		long[] r2 = {-4922475540349336432L, -3370274031255729188L, -2971948390163211717L, 
//				-8730854458729406051L, -1856941488587171136L, 2480197071496421608L, 
//				8737990220095227046L, -395233716872471639L, -5121607137339136235L, 
//				-5256749969541153749L};

		long[] b1 = s.clone();
		long[] b2 = s.clone();
		int posSrc=59;//123;//251;//571;
		int posTrg=10;//330;
		int nBits=61;

		BitsLong.copyBitsLeft(b1, posSrc, b1, posTrg, nBits);
		copyBitsLeftSlow(b2, posSrc, b2, posTrg, nBits);
		check(b1, b2);
	}
	
	@Test
	public void copyBitsLeftBug5() {
		long[] s = {//2256040733390859228L, -7415412748790187952L, 5988196814927695714L, 
				//6456740631013180783L, 
				//492044650721807007L, -5612393831914860878L, 
				4754515495077470782L, -1948216037247540742L, -5215875920690959100L, 
		};//-8889461600749447869L};

		long[] b1 = s.clone();
		long[] b2 = s.clone();
		int posSrc=95;//223;//479;
		int posTrg=44;//300;
		int nBits=84;

		BitsLong.copyBitsLeft(b1, posSrc, b1, posTrg, nBits);
		copyBitsLeftSlow(b2, posSrc, b2, posTrg, nBits);
		check(b1, b2);
	}
	
	@Test
	public void copyBitsLeftBug6() {
		long[] s = {//26658067013538148L, -7165901376423424552L, 3830723913108681159L, 
				-7477064955817770196L, 2470570724734906539L, 4372500218542924969L, 
				//1592114322442893905L, //-3029650008794553899L, 4082479205780751270L, 
		};//-4513395991245529464L};

		long[] b1 = s.clone();
		long[] b2 = s.clone();
		int posSrc=45;//237;
		int posTrg=70;//262;
		int nBits=86;//278;

		//System.out.println("ori: " + BitsLong.toBinary(s));
		BitsLong.copyBitsLeft(s, posSrc, b1, posTrg, nBits);
		copyBitsLeftSlow(s, posSrc, b2, posTrg, nBits);
		check(b1, b2);
	}
	
	private void insertBitsSlow(long[] ba, int start, int nBits) {
		int bitsToShift = ba.length*BITS - start - nBits;
		for (int i = 0; i < bitsToShift; i++) {
			int srcBit = ba.length*BITS - nBits - i - 1;
			int trgBit = ba.length*BITS - i - 1;
			BitsLong.setBit(ba, trgBit, BitsLong.getBit(ba, srcBit));
		}

	}
	
//	private void removetBitsSlow(long[] ba, int start, int nBits) {
//		int bitsToShift = ba.length*BITS - start - (nBits);
//		for (int i = 0; i < bitsToShift; i++) {
//			int srcBit = start + (nBits) + i;
//			int trgBit = start + i;
//			BitsLong.setBit(ba, trgBit, BitsLong.getBit(ba, srcBit));
//		}
//	}
	
    public static void copyBitsLeftSlow(long[] src, int posSrc, long[] trg, int posTrg, int nBits) {
    	for (int i = 0; i < nBits; i++) {
    		BitsLong.setBit(trg, posTrg + i, BitsLong.getBit(src, posSrc + i));
    	}
    }
    
	@Test
	public void testBinarySearch() {
		long[] ba = {1, 34, 43, 123, 255, 1000};
		checkBinarySearch(ba, 0);
		checkBinarySearch(ba, 1);
		checkBinarySearch(ba, 2);
		checkBinarySearch(ba, 34);
		checkBinarySearch(ba, 40);
		checkBinarySearch(ba, 43);
		checkBinarySearch(ba, 45);
		checkBinarySearch(ba, 123);
		checkBinarySearch(ba, 255);
		checkBinarySearch(ba, 999);
		checkBinarySearch(ba, 1000);
		checkBinarySearch(ba, 1001);
	}
	
	private void checkBinarySearch(long[] ba, int key) {
		int i1 = Arrays.binarySearch(ba, key);
		int i2 = BitsLong.binarySearch(ba, 0, ba.length, key, 64, 0);
		assertEquals(i1, i2);
	}
	
	
	private void check(long[] t, long ... expected) {
		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != t[i]) {
				System.out.println("i=" + i);
				System.out.println("act:" + BitsLong.toBinary(t) + "/ " + t[i]);
				System.out.println("exp:" + BitsLong.toBinary(expected) + " / " + expected[i]);
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
//				System.out.println("c-mask: "+ Bits.toBinary(mask));
				t[i] &= mask;
				expected[i] &= mask;
			}
//			System.out.println("i=" + i + " \nex= " + BitsLong.toBinary(expected, 32) + " \nac= " + 
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

	private long[] newBaPattern(int n, Random r) {
		long[] ba = new long[n];
		for (int i = 0; i < n; i++) {
			ba[i] = r.nextLong();
		}
		return ba;
	}
}
