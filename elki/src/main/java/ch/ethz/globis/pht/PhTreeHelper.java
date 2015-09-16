package ch.ethz.globis.pht;

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

/**
 *
 * @author ztilmann (Tilmann Zaeschke)
 *
 */
public abstract class PhTreeHelper {

	public static final boolean DEBUG_FULL = false; //even more debug info, gets expensive
	public static final boolean DEBUG = false | DEBUG_FULL;
    
    private PhTreeHelper() {
    	//
    }
    
	/** 
	 * Determines how much memory should be allocated on array resizing. The batch
	 * size designates multiples of 16byte on a JVM with < 32GB. Higher values
	 * result in higher fixed memory requirements but reduce the number of arrays
	 * to be created, copied and garbage collected when modifying the tree.
	 * Recommended values are 1, 2, 3, 4.  Default is 1.
	 * @param size 
	 */
	public static void setAllocBatchSize(int size) {
		//This works as follows: For a long[] we always allocate even numbers, i.e. we allocate
		//2*size slots.
		//The exception is size=0, where we allocate just one slot. This is mainly for debugging.
		if (size == 0) {
			ALLOC_BATCH_SIZE_LONG = 63;
			ALLOC_BATCH_SIZE = 1;
			ALLOC_BATCH_REF = 1;
		} else {
			ALLOC_BATCH_SIZE_LONG = 64*size-1;
			ALLOC_BATCH_SIZE = size;
			ALLOC_BATCH_REF = 2*size-1;
		}
	}
	public static int ALLOC_BATCH_SIZE;// = 1;
	public static int ALLOC_BATCH_SIZE_LONG;// = 127;
	public static int ALLOC_BATCH_REF;// = 1;
	static {
		setAllocBatchSize(1);
	}
	
	/**
	 * Enable pooling of arrays. This should reduce garbage collection during inert()/put(),
	 * update() and delete() operations.
	 * We call POOL_SIZE=PS and ARRAY_SIZE=AS.
	 * The maximum memory allocation of the pool is 
	 * approx. (AS*AS)/2*PS*8byte = 1000*1000*100/2*8 = 400MB for the long[] pool and half the 
	 * size (200MB) for the Object[] pool, however the total size is typically much smaller, 
	 * around 1.2M*8=10MB.
	 * For DEPTH=64, suggested values  
	 */
	public static boolean ARRAY_POOLING = true;
	
	/** The maximum size of arrays that will be stored in the pool. The default is 1000, which
	 * mean 8KB for long[] and 4KB for Object[]. Also, there a separate pools for long[] and 
	 * Object[]. */
	public static int ARRAY_POOLING_MAX_ARRAY_SIZE = 100;
	
	/** The maximum size of the pool (per array). The pool consists of several sub-pool, one for
	 * each size of arrays. A max size of 100 means that there will be at most 100 arrays of each
	 * size in the pool. */
	public static int ARRAY_POOLING_POOL_SIZE = 100;
	
	/**
	 * Enable pooling of arrays. This should reduce garbage collection during inert()/put(),
	 * update() and delete() operations.
	 * @param flag
	 */
	public static void enablePooling(boolean flag) {
		ARRAY_POOLING = flag;
	}
	
    public static final void debugCheck() {
    	if (DEBUG) {
    		System.err.println("*************************************");
    		System.err.println("** WARNING ** DEBUG IS ENABLED ******");
    		System.err.println("*************************************");
    	}
//    	if (BLHC_THRESHOLD_DIM > 6) {
//    		System.err.println("*************************************");
//    		System.err.println("** WARNING ** BLHC IS DISABLED ******");
//    		System.err.println("*************************************");
//    	}
    }
    
    // ===== Adrien =====
    public void accept(PhTreeVisitor v) {
    	v.visit(this);
    }
    
    public abstract static class PhTreeVisitor {
    	public abstract void visit(PhTreeHelper tree);
    }
    
    
    public static final class Stats {
	    public int nNodes;
	    public int nInnerNodes;
	    public int nLeafNodes;
	    public int nLonely;
	    public int nLeafSingle;
	    public int nLeafSingleNoPrefix;
	    public long size;
	    public int nSubOnly; 
	    public int nChildren; //subs or posts
	    public int nHCP;
	    public int nHCS;
	    public int nNI;
	    public int nTooLarge;
	    public int nTooLarge2;
	    public int nTooLarge4;
	    @Override
	    public String toString() {
	        return " nNodes=" + nNodes + "  nInner=" + nInnerNodes + " (" + nLonely +
	        ")  nLeaf=" + nLeafNodes + " (" + nLeafSingle + "/" + nLeafSingleNoPrefix + ")" +
	        "  size="+size + "/" + (size/1024) + "/" + (size/1024/1024) + "\n" +
	        "  avgSub=" + (double)nSubOnly/(double)nInnerNodes + 
	        "  avgChildren=" + (double)nChildren/(double)nNodes + "\n" +
	        "  postHC=" + nHCP + "  subHC=" + nHCS + "  NI=" + nNI + "\n" +
	        "  tooLarge=" + nTooLarge + "  tooLarge2=" + nTooLarge2 + "  tooLarge4=" + nTooLarge4 + "\n" +
	        //"  sizeRef:" + nInnerNodes*DIM*DIM*4/1024 + " sizeBool:" + nLeafNodes*DIM*DIM/8/1024
	        (DEBUG ? "DEBUG = TRUE\n" : "") +
	        "";
	    }
	}


	public static final int align8(int n) {
    	return (int) (8*Math.ceil(n/8.0));
    }

    public static final int getMaxConflictingBits(long[] v1, long[] v2, int bitsToCheck) {
    	if (bitsToCheck == 0) {
    		return 0;
    	}
    	long mask = bitsToCheck==64 ? ~0L : ~(-1L << bitsToCheck); //mask, because value2 may not have leading bits set
    	return getMaxConflictingBitsWithMask(v1, v2, mask);
    }
    
    /**
     * 
     * @param v1
     * @param v2
     * @param mask Mask that indicates which bits to check. Only bits where mask=1 are checked.
     * @return Position of the highest conflicting bit (counted from the right) or 0 if none.
     */
    public static final int getMaxConflictingBitsWithMask(long[] v1, long[] v2, long mask) {
        long x = 0;
        for (int i = 0; i < v1.length; i++) {
        	//write all differences to x, we just check x afterwards
            x |= v1[i] ^ v2[i];
        }
        x &= mask;
        return (x==0) ? 0 : Long.SIZE - Long.numberOfLeadingZeros(x);
    }
    

    
    /**
     * Encode the bits at the given position of all attributes into a hyper-cube address.
     * Currently, the first attribute determines the left-most (high-value) bit of the address 
     * (left to right ordered)
     * 
     * @param valSet
     * @param postLen
     * @return Encoded HC position
     */
    public static final long posInArray(long[] valSet, int postLen) {
        //n=DIM,  i={0..n-1}
        // i = 0 :  |0|1|0|1|0|1|0|1|
        // i = 1 :  | 0 | 1 | 0 | 1 |
        // i = 2 :  |   0   |   1   |
        //len = 2^n
        //Following formula was for inverse ordering of current ordering...
        //pos = sum (i=1..n, len/2^i) = sum (..., 2^(n-i))

    	long valMask = 1l << postLen;
    	
        long pos = 0;
        for (long v: valSet) {
        	pos <<= 1;
        	//set pos-bit if bit is set in value
            if ((valMask & v) != 0) {
                pos |= 1L;
            }
        }
        return pos;
    }

   /**
     * Apply a HC-position to a value. This means setting one bit for each dimension.
     * Leading and trailing bits in the value remain untouched.
     * @param pos
     * @param currentPostLen
     * @param val
     */
    public static void applyHcPos(long pos, int currentPostLen, long[] val) {
    	long mask = 1L << currentPostLen;
    	long posMask = 1L<<val.length;
		for (int d = 0; d < val.length; d++) {
			posMask >>>= 1;
			long x = pos & posMask;
			//Hack to avoid branching. However, this is faster than rotating 'pos' i.o. posMask
			val[d] = (val[d] & ~mask) | (Long.bitCount(x) * mask);
//			if (x != 0) {
//				val[d] |= mask;
//			} else {
//				val[d] &= ~mask;
//			}
		}
    }

}

