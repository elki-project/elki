/*
 * Copyright 2011-2014 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.util;

import java.util.Iterator;

public interface PhIteratorBase<K, V, E> extends Iterator<V> {

	public K nextKey();

	public V nextValue();

	public E nextEntry();

}