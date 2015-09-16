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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.ethz.globis.pht.PhTree.PhIterator;
import ch.ethz.globis.pht.PhTree.PhQuery;
import ch.ethz.globis.pht.pre.EmptyPP;
import ch.ethz.globis.pht.pre.PreProcessorPoint;
import ch.ethz.globis.pht.util.PhIteratorBase;

/**
 * k-dimensional index (quad-/oct-/n-tree).
 * Supports key/value pairs.
 *
 *
 * @author ztilmann (Tilmann Zaeschke)
 *
 */
public class PhTreeF<T> {

  private final PhTree<T> pht;
  private final PreProcessorPoint pre;

  /**
   * Create a new tree with the specified number of dimensions.
   * 
   * @param dim number of dimensions
   */
  public static <T> PhTreeF<T> create(int dim) {
    return new PhTreeF<T>(dim, new EmptyPP());
  }

  /**
   * Create a new tree with the specified number of dimensions.
   * 
   * @param dim number of dimensions
   * @param pre The preprocessor top be used.
   */
  public static <T> PhTreeF<T> create(int dim, PreProcessorPoint pre) {
    return new PhTreeF<T>(dim, pre);
  }

  private PhTreeF(int dim, PreProcessorPoint pre) {
    pht = PhTree.create(dim);
    this.pre = pre;
  }

  public PhTreeF(PhTree<T> tree) {
    pht = tree;
    pre = new EmptyPP();
  }

  public int size() {
    return pht.size();
  }

  /**
   * Insert an entry associated with a k dimensional key.
   * @param key
   * @param value
   * @return the previously associated value or {@code null} if the key was found
   */
  public T put(double[] key, T value) {
    long[] lKey = new long[key.length];
    pre.pre(key, lKey);
    return pht.put(lKey, value);
  };

  public boolean contains(double ... key) {
    long[] lKey = new long[key.length];
    pre.pre(key, lKey);
    return pht.contains(lKey);
  }

  public T get(double ... key) {
    long[] lKey = new long[key.length];
    pre.pre(key, lKey);
    return pht.get(lKey);
  }


  /**
   * Remove the entry associated with a k dimensional key.
   * @param key
   * @return the associated value or {@code null} if the key was found
   */
  public T remove(double... key) {
    long[] lKey = new long[key.length];
    pre.pre(key, lKey);
    return pht.remove(lKey);
  }

  public PhIteratorF<T> queryExtent() {
    return new PhIteratorF<T>(pht.queryExtent(), pht.getDIM(), pre);
  }


  /**
   * Performs a range query. The parameters are the min and max keys.
   * @param min
   * @param max
   * @return Result iterator.
   */
  public PhQueryF<T> query(double[] min, double[] max) {
    long[] lMin = new long[min.length];
    long[] lMax = new long[max.length];
    pre.pre(min, lMin);
    pre.pre(max, lMax);
    return new PhQueryF<>(pht.query(lMin, lMax), pht.getDIM(), pre);
  }

  public int getDim() {
    return pht.getDIM();
  }

  /**
   * Locate nearest neighbours for a given point in space.
   * @param nMin number of entries to be returned. More entries may be returned with several have
   * 				the same distance.
   * @param key
   * @return List of neighbours.
   */
  public List<double[]> nearestNeighbour(int nMin, double... key) {
    long[] lKey = new long[key.length];
    pre.pre(key, lKey);
    ArrayList<double[]> ret = new ArrayList<>();
    for (long[] l: pht.nearestNeighbour(nMin, lKey)) {
      double[] dKey = new double[l.length];
      pre.post(l, dKey);
      ret.add(dKey);
    }
    return ret;
  }

  public static class PhIteratorF<T> implements PhIteratorBase<double[], T, PhEntryF<T>> {
    private final PhIterator<T> iter;
    protected final PreProcessorPoint pre;
    private final int DIM;

    private PhIteratorF(PhIterator<T> iter, int DIM, PreProcessorPoint pre) {
      this.iter = iter;
      this.pre = pre;
      this.DIM = DIM;
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public T next() {
      return nextValue();
    }

    @Override
    public PhEntryF<T> nextEntry() {
      double[] d = new double[DIM];
      PhEntry<T> e = iter.nextEntry();
      pre.post(e.getKey(), d);
      return new PhEntryF<T>(d, e.getValue());
    }

    @Override
    public double[] nextKey() {
      double[] d = new double[DIM];
      pre.post(iter.nextKey(), d);
      return d;
    }

    @Override
    public T nextValue() {
      return iter.nextValue();
    }

    @Override
    public void remove() {
      iter.remove();
    }
  }

  public static class PhQueryF<T> extends PhIteratorF<T> {
    private final long[] lMin, lMax;
    private final PhQuery<T> q;
    private final double[] MIN;
    private final double[] MAX;

    private PhQueryF(PhQuery<T> iter, int DIM, PreProcessorPoint pre) {
      super(iter, DIM, pre);
      q = iter;
      MIN = new double[DIM];
      Arrays.fill(MIN, Double.NEGATIVE_INFINITY);
      MAX = new double[DIM];
      Arrays.fill(MAX, Double.POSITIVE_INFINITY);
      lMin = new long[DIM];
      lMax = new long[DIM];
    }

    public void reset(double[] lower, double[] upper) {
      pre.pre(lower, lMin);
      pre.pre(upper, lMax);
      q.reset(lMin, lMax);
    }
  }

  /**
   * Entry class for Double entries.
   * @author ztilmann
   *
   * @param <T>
   */
  public static class PhEntryF<T> {
    private final double[] key;
    private final T value;
    public PhEntryF(double[] key, T value) {
      this.key = key;
      this.value = value;
    }

    public double[] getKey() {
      return key;
    }

    public T getValue() {
      return value;
    }
  }

  /**
   * Update the key of an entry. Update may fail if the old key does not exist, or if the new
   * key already exists.
   * @param oldKey
   * @param newKey
   * @return the value (can be {@code null}) associated with the updated key if the key could be 
   * updated, otherwise {@code null}.
   */
  public T update(double[] oldKey, double[] newKey) {
    long[] oldL = new long[oldKey.length];
    long[] newL = new long[newKey.length];
    pre.pre(oldKey, oldL);
    pre.pre(newKey, newL);
    return pht.update(oldL, newL);
  }

  /**
   * Clear the tree.
   */
  void clear() {
    pht.clear();
  }

  /**
   * 
   * @return the internal PhTree that backs this PhTreeF.
   */
  public PhTree<T> getInternalTree() {
    return pht;
  }

}

