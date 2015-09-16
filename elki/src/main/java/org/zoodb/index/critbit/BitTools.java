package org.zoodb.index.critbit;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2009-2015
Tilmann Zaeschke
The author can be contacted via email: zoodb@gmx.de
https://github.com/tzaeschke/critbit

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

/**
 * 
 * @author Tilmann Zaeschke
 */
public class BitTools {

    /**
     * @param value
     * @return long representation.
     */
	public static long toSortableLong(double value) {
		//To create a sortable long, we convert the double to a long using the IEEE-754 standard,
		//which stores floats in the form <sign><exponent-127><mantissa> .
		//This result is properly ordered longs for all positive doubles. Negative values have
		//inverse ordering. For negative doubles, we therefore simply invert them to make them 
		//sortable, however the sign must be inverted again to stay negative.
		long r = Double.doubleToRawLongBits(value);
		return (r >= 0) ? r : r ^ 0x7FFFFFFFFFFFFFFFL;
	}

	public static long toSortableLong(float value) {
		//see toSortableLong(double)
		int r =  Float.floatToRawIntBits(value);
		return (r >= 0) ? r : r ^ 0x7FFFFFFF;
	}

	public static double toDouble(long value) {
		return Double.longBitsToDouble(value >= 0.0 ? value : value ^ 0x7FFFFFFFFFFFFFFFL);
	}

	public static float toFloat(long value) {
		int iVal = (int) value;
		return Float.intBitsToFloat(iVal >= 0.0 ? iVal : iVal ^ 0x7FFFFFFF);
	}

	/**
	 * Splits a value and write it to trgV at position trg1 and trg2.
	 * This is the inverse operation to merge(...).
	 * @param toSplit
	 * @param trgV
	 * @param trg1
	 * @param trg2
	 * @param nBits Number of bits of source value
	 */
	public static void split(final long toSplit, long[] trgV, final int trg1, final int trg2, 
			int nBits) {
		long maskSrc = 1L << (nBits-1);
		long t1 = 0;
		long t2 = 0;
		for (int i = 0; i < nBits; i++) {
			if ((i&1) == 0) {
				t1 <<= 1;
				if ((toSplit & maskSrc) != 0L) {
					t1 |= 1L;
				}
			} else {
				t2 <<= 1;
				if ((toSplit & maskSrc) != 0L) {
					t2 |= 1L;
				}
			}
			maskSrc >>>= 1;
		}
		trgV[trg1] = t1;
		trgV[trg2] = t2;
	}

	/**
	 * Merges two long values into a single value by interleaving there respective bits.
	 * This is the inverse operation to split(...).
	 * @param srcV Source array
	 * @param src1 Position of 1st source value
	 * @param src2 Position of 2nd source value
	 * @param nBits Number of bits of RESULT
	 * @return Merged result
	 */
	public static long merge(long[] srcV, final int src1, final int src2, int nBits) {
		long maskTrg = 1L;
		long v = 0;
		long s1 = srcV[src1];
		long s2 = srcV[src2];
		for (int i = nBits-1; i >=0; i--) {
			if ( (i & 1) == 0) {
				if ((s1 & 1L) == 1L) {
					v |= maskTrg;
				}
				s1 >>>= 1;
			} else {
				if ((s2 & 1L) == 1L) {
					v |= maskTrg;
				}
				s2 >>>= 1;
			}
			maskTrg <<= 1;
		}
		return v;
	}

	/**
	 * Merges long values into a single value by interleaving there respective bits.
	 * This is the inverse operation to split(...).
	 * @param src Source array
	 * @param nBitsPerValue Number of bits of each source value
	 * @return Merged result
	 */
	public static long[] mergeLong(final int nBitsPerValue, long[] src) {
		final int DIM = src.length;
		int intArrayLen = (src.length*nBitsPerValue+63) >>> 6;
		long[] trg = new long[intArrayLen];
		
		long maskSrc = 1L << (nBitsPerValue-1);
		long maskTrg = 0x8000000000000000L;
		int srcPos = 0;
		int trgPos = 0;
		for (int j = 0; j < nBitsPerValue*DIM; j++) {
	        if ((src[srcPos] & maskSrc) != 0) {
	        	trg[trgPos] |= maskTrg;
	        } else {
	        	trg[trgPos] &= ~maskTrg;
	        }
			maskTrg >>>= 1;
			if (maskTrg == 0) {
				maskTrg = 0x8000000000000000L;
				trgPos++;
			}
			if (++srcPos == DIM) {
				srcPos = 0;
				maskSrc >>>= 1;
			}
		}
		return trg;
	}
	
	/**
	 * Splits a value and write it to trgV at position trg1 and trg2.
	 * This is the inverse operation to merge(...).
	 * @param toSplit
	 * @param nBitsPerValue Number of bits of source value
	 */
	public static long[] splitLong(final int DIM, final int nBitsPerValue, final long[] toSplit) {
		long[] trg = new long[DIM];

		long maskTrg = 1L << (nBitsPerValue-1);
		for (int k = 0; k < nBitsPerValue; k++) {
			for (int j = 0; j < trg.length; j++) {
				int posBit = k*trg.length + j; 
				boolean bit = getBit(toSplit, posBit);
				if (bit) {
					trg[j] |= maskTrg;
				}
			}
			maskTrg >>>= 1;
		}
		return trg;
	}

	/**
	 * @param posBit Counts from left to right!!!
	 */
    public static boolean getBit(long[] ba, int posBit) {
        int pA = posBit >>> 6; // 1/64
        //last 6 bit [0..63]
        posBit &= 0x3F;
        return (ba[pA] & (0x8000000000000000L >>> posBit)) != 0;
	}

	/**
	 * @param posBit Counts from left to right!!!
	 */
    public static boolean getBit(long l, int posBit) {
        //last 6 bit [0..63]
        return (l & (0x8000000000000000L >>> posBit)) != 0;
	}

	/**
	 * Reads a bit from {@code src}, writes it to {@code dst} and returns it.
	 * @param posBit Counts from left to right
	 */
    public static boolean getAndCopyBit(long[] src, int posBit, long[] dst) {
        int pA = posBit >>> 6; // 1/64
        //last 6 bit [0..63]
        posBit &= 0x3F;
        long mask = (0x8000000000000000L >>> posBit);
        if ( (src[pA] & mask) != 0 ) {
            dst[pA] |= mask;
            return true;
        } else {
            dst[pA] &= ~mask;
            return false;
        }
	}

	/**
	 * @param posBit Counts from left to right (highest to lowest)!!!
	 */
    public static void setBit(long[] ba, int posBit, boolean b) {
        int pA = posBit >>> 6;  // 1/64
        //last 6 bit [0..63]
        posBit &= 0x3F;
        if (b) {
            ba[pA] |= (0x8000000000000000L >>> posBit);
        } else {
            ba[pA] &= (~(0x8000000000000000L >>> posBit));
        }
	}


    public static long setBit(long ba, int posBit, boolean b) {
        if (b) {
            return ba | (0x8000000000000000L >>> posBit);
        } else {
            return ba & (~(0x8000000000000000L >>> posBit));
        }
	}

    public static long set1(long ba, int posBit) {
    	return ba | (0x8000000000000000L >>> posBit);
	}

    public static long set0(long ba, int posBit) {
    	return ba & (~(0x8000000000000000L >>> posBit));
	}


	public static String toBinary(long[] la, int DEPTH) {
	    StringBuilder sb = new StringBuilder();
	    for (long l: la) {
	    	sb.append(toBinary(l, DEPTH));
	        sb.append(", ");
	    }
	    return sb.toString();
	}

	public static String toBinary(long l, int DEPTH) {
        StringBuilder sb = new StringBuilder();
        //long mask = DEPTH < 64 ? (1<<(DEPTH-1)) : 0x8000000000000000L;
        for (int i = 0; i < DEPTH; i++) {
            long mask = (1l << (long)(DEPTH-i-1));
            if ((l & mask) != 0) { sb.append("1"); } else { sb.append("0"); }
            if ((i+1)%8==0 && (i+1)<DEPTH) sb.append('.');
        	mask >>>= 1;
        }
        return sb.toString();
    }


}
