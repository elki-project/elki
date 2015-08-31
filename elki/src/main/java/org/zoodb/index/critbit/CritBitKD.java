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

import org.zoodb.index.critbit.CritBit.QueryIteratorKD;

/**
 * 
 * @author Tilmann Zaeschke
 */
public interface CritBitKD<V> {

	/** @see CritBit#putKD(long[], Object) */
	V putKD(long[] key, V value);

	/** @see CritBit#containsKD(long[]) */
	boolean containsKD(long[] key);

	/** @see CritBit#size() */  
	int size();

	/** @see CritBit#queryKD(long[], long[]) */  
	QueryIteratorKD<V> queryKD(long[] lowerLeft, long[] upperRight);

	/** @see CritBit#removeKD(long[]) */  
	V removeKD(long[] key);

	/** @see CritBit#printTree() */  
	void printTree();

	/** @see CritBit#getKD(long[]) */  
	V getKD(long[] key);

}
