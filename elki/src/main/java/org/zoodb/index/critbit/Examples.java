/*
 * Copyright 2009-2015 Tilmann Zaeschke. All rights reserved.
 * 
 * The author can be contacted via email: zoodb@gmx.de
 * https://github.com/tzaeschke/critbit
 * 
 * This file is part of ZooDB / Critbit.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.zoodb.index.critbit;

import java.util.Arrays;

import org.zoodb.index.critbit.CritBit.QueryIterator;
import org.zoodb.index.critbit.CritBit.QueryIteratorKD;

/**
 * 
 * @author Tilmann Zaeschke
 */
public class Examples {

	public static void main(String[] args) {
		ex1D_32();
		ex1D_float();
		ex1D_256();
		
		ex4D();
	}
	
	/**
	 * Example of a 1D crit-bit tree with 32 bit integer keys.
	 */
	private static void ex1D_32() {
		log("Testing 1D 32 bit tree");
		log("======================");
		CritBit1D<String> cb = CritBit.create1D(32);
		long[] key = new long[]{1234};
		cb.put(key, "hello 32");
		log("contains() --> " + cb.contains(key));
		log("get() --> "+ cb.get(key));
		long[] min = new long[]{123}; 
		long[] max = new long[]{12345}; 
		QueryIterator<String> it = cb.query(min, max); 
		log("iterator val: " + it.next());
		QueryIterator<String> it2 = cb.query(min, max); 
		log("iterator key: " + it2.nextKey()[0]);
		log("remove: " + cb.remove(key));
	}

	/**
	 * Example of a 1D crit-bit tree with 64 bit float keys.
	 */
	private static void ex1D_float() {
		log("Testing 1D float tree");
		log("=====================");
		//double-float requires 64 bit
		CritBit1D<String> cb = CritBit.create1D(64);
		long[] key = new long[]{BitTools.toSortableLong(12.34)};
		cb.put(key, "hello float");
		log("contains() --> " + cb.contains(key));
		log("get() --> "+ cb.get(key));
		long[] min = new long[]{BitTools.toSortableLong(1.0)}; 
		long[] max = new long[]{BitTools.toSortableLong(15.0)}; 
		QueryIterator<String> it = cb.query(min, max); 
		log("iterator val: " + it.next());
		QueryIterator<String> it2 = cb.query(min, max); 
		log("iterator key: " + BitTools.toDouble(it2.nextKey()[0]));
		log("remove: " + cb.remove(key));
	}

	/**
	 * Example of a 1D crit-bit tree with 256 bit integer keys.
	 */
	private static void ex1D_256() {
		log("Testing 1D 256 bit tree");
		log("=======================");
		CritBit1D<String> cb = CritBit.create1D(256);  
		long[] key = new long[]{ 123456789012L, 3456, 4567, 12345678901234L};
		cb.put(key, "hello 256");
		log("contains() --> " + cb.contains(key));
		log("get() --> "+ cb.get(key));
		long[] min = new long[]{0, 0, 0, 0}; 
		long[] max = new long[]{Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE}; 
		QueryIterator<String> it = cb.query(min, max); 
		log("iterator val: " + it.next());
		QueryIterator<String> it2 = cb.query(min, max); 
		log("iterator key: " + it2.nextKey()[0]);
		log("remove: " + cb.remove(key));
	}

	/**
	 * Example of a 4D crit-bit tree with 1 float dimension.
	 */
	private static void ex4D() {
		log("Testing 4D tree");
		log("===============");
		CritBitKD<String> cb = CritBit.createKD(64, 4);  
		long[] key = new long[]{ 
				123456789012L, 
				3456, 
				BitTools.toSortableLong(1234.5678), // double dimension
				12345678901234L};
		cb.putKD(key, "hello 4D");
		log("contains() --> " + cb.containsKD(key));
		log("get() --> "+ cb.getKD(key));
		long[] min = new long[]{0, 0, BitTools.toSortableLong(1.0), 0}; 
		long[] max = new long[]{Long.MAX_VALUE, Long.MAX_VALUE,
				BitTools.toSortableLong(Double.MAX_VALUE), Long.MAX_VALUE}; 
		QueryIteratorKD<String> it = cb.queryKD(min, max); 
		log("iterator val: " + it.next());
		QueryIteratorKD<String> it2 = cb.queryKD(min, max); 
		log("iterator key: " + Arrays.toString(it2.nextKey()));
		log("remove: " + cb.removeKD(key));
	}

	private static void log(String msg) {
		System.out.println(msg);
	}
}
