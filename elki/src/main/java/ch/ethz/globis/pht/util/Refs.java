/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.util;

import java.util.Arrays;

import ch.ethz.globis.pht.PhTreeHelper;


public class Refs {
	
	private static final int BYTES_PER_UNIT = 4;//bytes
//	private static int statAExpand;
//	private static int statACreate;
//	private static int statATrim;
	
	public static final Object[] EMPTY_REF_ARRAY = {};
    private static final ArrayPool POOL = 
    		new ArrayPool(PhTreeHelper.ARRAY_POOLING_MAX_ARRAY_SIZE, 
    				PhTreeHelper.ARRAY_POOLING_POOL_SIZE);

    
    private static class ArrayPool {
    	private final int maxArraySize;
    	private final int maxArrayCount;
    	Object[][][] pool;
    	int[] poolSize;
    	ArrayPool(int maxArraySize, int maxArrayCount) {
			this.maxArraySize = maxArraySize;
			this.maxArrayCount = maxArrayCount;
			this.pool = new Object[maxArraySize+1][maxArrayCount][];
			this.poolSize = new int[maxArraySize+1];
		}
    	
    	Object[] getArray(int size) {
    		if (size == 0) {
    			return EMPTY_REF_ARRAY;
    		}
    		if (size > maxArraySize) {
    			return new Object[size];
    		}
    		synchronized (this) {
	    		int ps = poolSize[size]; 
	    		if (ps > 0) {
	    			poolSize[size]--;
	    			Object[] ret = pool[size][ps-1];
	    			Arrays.fill(ret, null);
	    			return ret;
	    		}
    		}
    		return new Object[size];
    	}
    	
    	synchronized void offer(Object[] a) {
    		int size = a.length;
    		if (size == 0 || size > maxArraySize) {
    			return;
    		}
    		synchronized (this) {
    			int ps = poolSize[size]; 
    			if (ps < maxArrayCount) {
    				//System.err.println("s=" + size + " ps=" + ps);
    				pool[size][ps] = a;
    				poolSize[size]++;
    			}
    		}
    	}
    }

    /**
     * Calculate array size for given number of bits.
     * This takes into account JVM memory management, which allocates multiples of 8 bytes.
     * @param nObjects
     * @return array size.
     */
	public static int calcArraySize(int nObjects) {
		//round up to 8byte=2refs
		//return (nObjects+1) & (~1);
		int arraySize = (nObjects+PhTreeHelper.ALLOC_BATCH_REF);// & (~1);
		int size = PhTreeHelper.ALLOC_BATCH_SIZE * 2;
		arraySize = (arraySize/size) * size;
		return arraySize;
	}

    /**
     * Resize an array.
     * @param oldA
     * @param newSize
     * @return New array larger array.
     */
    public static <T> T[] arrayExpand(T[] oldA, int newSize) {
    	T[] newA = arrayCreate(newSize);
    	System.arraycopy(oldA, 0, newA, 0, oldA.length);
    	POOL.offer(oldA);
    	//statAExpand++;
    	return newA;
    }
    
    @SuppressWarnings("unchecked")
	public static <T> T[] arrayCreate(int size) {
    	//T[] newA = (T[]) new Object[calcArraySize(size)];
    	T[] newA = (T[]) POOL.getArray(calcArraySize(size));
    	//statACreate++;
    	return newA;
    }
    
    /**
     * Ensure capacity of an array. Expands the array if required.
     * @param oldA
     * @param requiredSize
     * @return Same array or expanded array.
     */
    public static <T> T[] arrayEnsureSize(T[] oldA, int requiredSize) {
    	if (isCapacitySufficient(oldA, requiredSize)) {
    		return oldA;
    	}
    	return arrayExpand(oldA, requiredSize);
    }
    
    public static <T> boolean isCapacitySufficient(T[] a, int requiredSize) {
    	return (a.length >= requiredSize);
    }
    
    @SuppressWarnings("unchecked")
	public static <T> T[] arrayTrim(T[] oldA, int requiredSize) {
    	int reqSize = calcArraySize(requiredSize);
    	if (oldA.length == reqSize) {
    		return oldA;
    	}
    	T[] newA = (T[]) POOL.getArray(reqSize);//new Object[reqSize];
     	System.arraycopy(oldA, 0, newA, 0, reqSize);
     	POOL.offer(oldA);
    	//statATrim++;
    	return newA;
    }

	public static int arraySizeInByte(Object[] ba) {
		return ba.length*BYTES_PER_UNIT;
	}
	
	public static int arraySizeInByte(int arrayLength) {
		return arrayLength*BYTES_PER_UNIT;
	}

	
	public static <T> void insertAtPos(T[] values, int pos, T value) {
		//System.arraycopy(values, pos, values, pos+1, values.length-pos-1);
		copyRight(values, pos, values, pos+1, values.length-pos-1);
		values[pos] = value;
	}
	
	public static <T> void removeAtPos(T[] values, int pos) {
		if (pos < values.length-1) {
			//System.arraycopy(values, pos+1, values, pos, values.length-pos-1);
			copyLeft(values, pos+1, values, pos, values.length-pos-1);
		}
	}
	
	public static <T> void copyLeft(T[] src, int srcPos, T[] dst, int dstPos, int len) {
		if (len >= 7) {
			System.arraycopy(src, srcPos, dst, dstPos, len);
		} else {
			for (int i = 0; i < len; i++) {
				dst[dstPos+i] = src[srcPos+i]; 
			}
		}
	}
	
	public static <T> void copyRight(T[] src, int srcPos, T[] dst, int dstPos, int len) {
		if (len >= 7) {
			System.arraycopy(src, srcPos, dst, dstPos, len);
		} else {
			for (int i = len-1; i >= 0; i--) {
				dst[dstPos+i] = src[srcPos+i]; 
			}
		}
	}
}
