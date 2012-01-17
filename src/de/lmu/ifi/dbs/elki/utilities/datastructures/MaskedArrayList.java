package de.lmu.ifi.dbs.elki.utilities.datastructures;

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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class is a virtual collection based on masking an array list using a bit
 * mask.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype decorator
 * @apiviz.composedOf java.util.ArrayList
 * @apiviz.composedOf java.util.BitSet
 * 
 * @param <T> Object type
 */
public class MaskedArrayList<T> extends AbstractCollection<T> implements Collection<T> {
  /**
   * Data storage
   */
  protected ArrayList<T> data;

  /**
   * The bitmask used for masking
   */
  protected BitSet bits;

  /**
   * Flag whether to iterator over set or unset values.
   */
  protected boolean inverse = false;

  /**
   * Constructor.
   * 
   * @param data Data
   * @param bits Bitset to use as mask
   * @param inverse Flag to inverse the masking rule
   */
  public MaskedArrayList(ArrayList<T> data, BitSet bits, boolean inverse) {
    super();
    this.data = data;
    this.bits = bits;
    this.inverse = inverse;
  }

  @Override
  public boolean add(T e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<T> iterator() {
    if(inverse) {
      return new InvItr();
    }
    else {
      return new Itr();
    }
  }

  @Override
  public int size() {
    if(inverse) {
      return data.size() - bits.cardinality();
    }
    else {
      return bits.cardinality();
    }
  }

  /**
   * Iterator over set bits
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class Itr implements Iterator<T> {
    /**
     * Next position.
     */
    private int pos;

    /**
     * Constructor
     */
    protected Itr() {
      this.pos = bits.nextSetBit(0);
    }

    @Override
    public boolean hasNext() {
      return (pos >= 0) && (pos < data.size());
    }

    @Override
    public T next() {
      T cur = data.get(pos);
      pos = bits.nextSetBit(pos + 1);
      return cur;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Iterator over unset elements.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class InvItr implements Iterator<T> {
    /**
     * Next unset position.
     */
    private int pos;

    /**
     * Constructor
     */
    protected InvItr() {
      this.pos = bits.nextClearBit(0);
    }

    @Override
    public boolean hasNext() {
      return (pos >= 0) && (pos < data.size());
    }

    @Override
    public T next() {
      T cur = data.get(pos);
      pos = bits.nextClearBit(pos + 1);
      return cur;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}