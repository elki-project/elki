/*
 * Copyright 2009-2014 Tilmann Zaeschke. All rights reserved.
 * 
 * This file is part of ZooDB.
 * 
 * ZooDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ZooDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ZooDB.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the README and COPYING files for further information. 
 */
package ch.ethz.globis.pht.util;

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
     * @param value
     * @param ret The array used to store the return value
     * @return long representation.
     */
	public static long[] toSortableLong(double[] value, long[] ret) {
		//To create a sortable long, we convert the double to a long using the IEEE-754 standard,
		//which stores floats in the form <sign><exponent-127><mantissa> .
		//This result is properly ordered longs for all positive doubles. Negative values have
		//inverse ordering. For negative doubles, we therefore simply invert them to make them 
		//sortable, however the sign must be inverted again to stay negative.
		for (int i = 0; i < value.length; i++) {
			long r = Double.doubleToRawLongBits(value[i]);
			ret[i] = (r >= 0) ? r : r ^ 0x7FFFFFFFFFFFFFFFL;
		} 
		return ret;
	}

	public static long[] toSortableLong(float[] value, long[] ret) {
		//see toSortableLong(double)
		for (int i = 0; i < value.length; i++) {
			int r =  Float.floatToRawIntBits(value[i]);
			ret[i] = (r >= 0) ? r : r ^ 0x7FFFFFFF;
		}
		return ret;
	}

	public static double[] toDouble(long value[], double[] ret) {
		for (int i = 0; i < value.length; i++) {
			ret[i] = Double.longBitsToDouble(
					value[i] >= 0.0 ? value[i] : value[i] ^ 0x7FFFFFFFFFFFFFFFL);
		}
		return ret;
	}

	public static float[] toFloat(long[] value, float[] ret) {
		for (int i = 0; i < value.length; i++) {
			int iVal = (int) value[i];
			ret[i] = Float.intBitsToFloat(iVal >= 0.0 ? iVal : iVal ^ 0x7FFFFFFF);
		}
		return ret;
	}

	public static long toSortableLong(String s) {
    	// store magic number: 6 chars + (hash >> 16)
		long n = 0;
    	int i = 0;
    	for ( ; i < 6 && i < s.length(); i++ ) {
    		n |= (byte) s.charAt(i);
    		n = n << 8;
    	}
    	//Fill with empty spaces if string is too short
    	for ( ; i < 6; i++) {
    		n = n << 8;
    	}
    	n = n << 8;

    	//add hashcode
    	n |= (0xFFFF & s.hashCode());
		return n;
	}
	
	
	/**
	 * Reverses the value, considering that not all 64bits of the long value are used.
	 * @param l
	 * @param usedBits
	 * @return Reversed value
	 */
	public static long reverse(long l, int usedBits) {
		long r = Long.reverse(l);
		r >>>= (64-usedBits);
		return r;
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
	 * Merges to long values into a single value by interleaving there respective bits.
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
	 * Merges two long values into a single value by interleaving there respective bits.
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
	 * Splits a value.
	 * This is the inverse operation to merge(...).
	 * @param DIM 
	 * @param toSplit
	 * @param nBitsPerValue Number of bits of source value
	 * @return The split value
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
	 * @param ba 
	 * @param posBit Counts from left to right!!!
	 * @return the bit as boolean 
	 */
    public static boolean getBit(long[] ba, int posBit) {
        int pA = posBit >>> 6; // 1/64
        //last 6 bit [0..63]
        posBit &= 0x3F;
        return (ba[pA] & (0x8000000000000000L >>> posBit)) != 0;
	}

	/**
	 * @param ba 
	 * @param posBit Counts from left to right (highest to lowest)!!!
	 * @param b 
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

    /**
     * Compares to z-values.
     * This takes at most O(w) time. This may actually worse then dim-by-dim comparison for w > k. 
     * Optimisation: with a startBit we get worst case (w-startBit) < k.
     * The nice thing: average case should be much better than worst case.
     * --> If we look at standard PH-trees, our average case should be related (equal?) to 
     * the average depth of the tree, ie. (64-avgPostlen). That means we have on average 
     * 20 comparisons or so...<p> 
     * 
     * How does that relate to standard comparison? There we can also abort early...
     * --> 0.5 if one dimension is non-dominated
     * -->  
     * More precisely, for the task at hand (comparing two z-values), testing can only aborted if
     * v2 is better than v1 in a given dimension. When having a large number of points, 
     * many of which are potentially dominated, this is somewhat unlikely.
     * To confirm that v2 is dominated, we can not abort early but have to compare ALL dimensions.
     * Since most points will have O(k), the average will also approach O(k).<p>  
     * 
     * Also, can we use quadrant properties to avoid comparison?
     * Quadrant IDs are leading bit-clusters. 
     * - We can exclude that a 00-quadrant is dominated by any other quadrant, but the same 
     *   information is implied by the z-ordering.
     * - We can also conclude that 11-quadrants are never non-dominated if there is anything in the
     *   00-quadrant. This is included in the algorithm below, but maybe we can skip 11-quadrants.
     *   This may also be possible directly in the PH-tree, if we use one. Unfortunately, the 
     *   value of this check decreases with k, because there is only 1 of 2^k such quadrants.
     *   On the other hand, the quadrant may be quite big...
     * - Finally, we could identify incomparable quadrants (see Lee&Hwang, 2014).
     *   Two quadrants are incomparable if both dominate the other in at least on dimension.
     *   This is equal to (diff&v1&domMask != 0 && diff&v2&domMask != 0). Hmm, do we really
     *   need the domMask here?
     *   This is also implicit in the algorithm below, but maybe we could check it in the
     *   surrounding data structure? How does the PH-tree do this? 
     *   
     * 
     * 
     * @param v1 smaller Z-value with w values
     * @param v2 bigger Z-value with w values
     * @param startBit the bit at which we start comparison [0..(w-1)].
     * @return True is v2 is dominated by v1.
     */
    public static boolean isBiggerZDominated(long[] v1, long[] v2, int startBit) {
    	long domMask = 0; //THis mask has a bit for every dimension where v1 dominates.
    	for (int i = 0; i < v1.length; i++) {
    		long diff = v1[i] ^ v2[i];
    		if (diff != 0) {
	    		if ((v2[i] & diff & ~domMask) != 0) {
	    			//Do we have a '1' in v2 in a non-dominated dimension?
	    			//--> v2 is not dominated
	    			return false;
	    		}
	    		domMask |= diff;
    		}
    	}
    	return true;
    }
}
