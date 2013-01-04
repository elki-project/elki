package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

import java.util.Comparator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

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
 * Heap for Objects of type K, using a comparator.
 * 
 * @author Erich Schubert
 * 
 * @param <K> Object type
 */
public class ComparatorObjectHeap<K> extends ObjectHeap<K> {
  /**
   * Comparator in use.
   */
  protected Comparator<? super K> comparator;

  /**
   * Constructor with default size.
   * 
   * @param comparator Comparator
   */
  public ComparatorObjectHeap(Comparator<? super K> comparator) {
    super(DEFAULT_INITIAL_CAPACITY);
    this.comparator = comparator;
  }

  /**
   * Constructor with initial size.
   * 
   * @param size Initial size
   * @param comparator Comparator
   */
  public ComparatorObjectHeap(int size, Comparator<? super K> comparator) {
    super(size);
    this.comparator = comparator;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean comp(Object o1, Object o2) {
    return comparator.compare((K) o1, (K) o2) >= 0;
  }
}
