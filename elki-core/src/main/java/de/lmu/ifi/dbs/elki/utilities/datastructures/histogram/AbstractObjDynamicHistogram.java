/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.datastructures.histogram;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

/**
 * A dynamic histogram can dynamically adapt the number of bins to the data fed
 * into the histogram.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @param <T> Data type
 */
public abstract class AbstractObjDynamicHistogram<T> extends ObjHistogram<T> {
  /**
   * Cache for positions to be inserted.
   */
  private double[] cacheposs;

  /**
   * Cache for data to be inserted.
   */
  private Object[] cachevals;

  /**
   * Cache fill size
   */
  private int cachefill;

  /**
   * Destination (minimum) size of the structure. At most 2*destsize bins are
   * allowed.
   */
  private int destsize;

  /**
   * Constructor.
   * 
   * @param bins Design number of bins - may become twice as large!
   */
  public AbstractObjDynamicHistogram(int bins) {
    super(-1, 0.0, 1.0, null);
    this.destsize = bins;
    cacheposs = new double[this.destsize << 1];
    cachevals = new Object[this.destsize << 1];
    cachefill = 0;
    this.supplier = this::makeObject;
  }

  /**
   * Materialize the histogram from the cache.
   */
  @SuppressWarnings("unchecked")
  void materialize() {
    // already materialized?
    if(cachefill < 0) {
      return;
    }
    // Compute minimum and maximum
    double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
    for(int i = 0; i < cachefill; i++) {
      min = MathUtil.min(min, cacheposs[i]);
      max = MathUtil.max(max, cacheposs[i]);
    }
    // use the LinearScale magic to round to "likely suitable" step sizes.
    // TODO: extract into a reusable function?
    LinearScale scale = new LinearScale(min, max);
    min = scale.getMin();
    max = scale.getMax();
    this.base = min;
    this.max = max;
    this.binsize = (max - min) / this.destsize;
    // initialize array
    this.data = new Object[this.destsize << 1];
    for(int i = 0; i < this.destsize; i++) {
      this.data[i] = makeObject();
    }
    size = destsize;
    // re-insert data we have
    final int end = cachefill;
    cachefill = -1; // So reinsert works!
    for(int i = 0; i < end; i++) {
      putData(cacheposs[i], (T) cachevals[i]);
    }
    // delete cache, signal that we're initialized
    cacheposs = null;
    cachevals = null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T get(double coord) {
    // Store in cache
    if(cachefill >= 0 && cachefill < cacheposs.length) {
      cacheposs[cachefill] = coord;
      return (T) (cachevals[cachefill++] = makeObject());
    }
    materialize();
    testResample(coord);
    T ret = super.get(coord);
    return ret;
  }

  /**
   * Put fresh data into the histogram (or into the cache).
   * 
   * @param coord Coordinate
   * @param value Value
   */
  public void putData(double coord, T value) {
    // Store in cache
    if(cachefill >= 0 && cachefill < cacheposs.length) {
      cacheposs[cachefill] = coord;
      cachevals[cachefill++] = cloneForCache(value);
      return;
    }
    if(coord == Double.NEGATIVE_INFINITY) {
      aggregateSpecial(value, 0);
    }
    else if(coord == Double.POSITIVE_INFINITY) {
      aggregateSpecial(value, 1);
    }
    else if(Double.isNaN(coord)) {
      aggregateSpecial(value, 2);
    }
    else {
      // super class will handle histogram resizing / shifting
      T exist = get(coord);
      data[getBinNr(coord)] = aggregate(exist, value);
    }
  }

  /**
   * Aggregate for a special value.
   * 
   * @param value Parameter value
   * @param bin Special bin index.
   */
  protected void aggregateSpecial(T value, int bin) {
    final T exist = getSpecial(bin);
    // Note: do not inline above accessor, as getSpecial will initialize the
    // special variable used below!
    special[bin] = aggregate(exist, value);
  }

  /**
   * Test (and perform) downsampling when needed.
   * 
   * @param coord coordinate to accommodate.
   */
  private void testResample(double coord) {
    final int bin = getBinNr(coord);
    final int sizereq, off;
    if(bin < 0) {
      sizereq = size - bin;
      off = -bin;
    }
    else if(bin >= data.length) {
      sizereq = bin + 1;
      off = 0;
    }
    else {
      // Within the designated size - nothing to do.
      return;
    }
    if(sizereq < data.length) {
      // Accommodate by shifting. Let super do the job in {@link #get}
      return;
    }
    // Resampling, eventually by multiple levels.
    final int levels = BitsUtil.magnitude(sizereq / this.destsize) - 1;
    assert (levels > 0) : "No resampling required?!?";
    final int step = 1 << levels;

    // We want to map [i ... i+step[ -> (i+off)/step
    // Fix point: i = (i+off)/step; i*(step-1)=off; i=off/(step-1)
    final int fixpoint = off / (step - 1);
    {
      // Start positions for in-place bottom-up downsampling.
      int oup = (fixpoint >= 0) ? fixpoint : 0;
      int inp = (oup << levels) - off;
      assert (-step < inp && inp <= oup && oup < inp + step) : (inp + " -> " + oup + " s=" + step + " o=" + off + " l=" + levels);
      for(; inp < size; inp += step, oup++) {
        assert (oup < inp + step && oup < data.length);
        data[oup] = downsample(data, Math.max(0, inp), Math.min(size, inp + step), step);
      }
      // Clean upwards
      for(; oup < data.length; oup++) {
        data[oup] = null;
      }
    }
    if(off >= step) {
      // Start positions for in-place downsampling top-down:
      int oup = (fixpoint - 1 < size) ? fixpoint - 1 : size - 1;
      int inp = (oup << levels) - off;
      assert (oup > inp) : (inp + " -> " + oup + " s=" + step + " o=" + off + " l=" + levels);
      for(; inp > -step; inp -= step, oup--) {
        assert (oup >= inp && oup >= 0);
        data[oup] = downsample(data, Math.max(0, inp), Math.min(size, inp + step), step);
      }
      for(; oup >= 0; oup--) {
        data[oup] = supplier.make();
      }
    }
    // recalculate histogram base.
    base = base - (offset + off) * binsize;
    offset = 0;
    size = (size + 1) >> levels;
    binsize = binsize * (1 << levels);
    max = base + binsize * size;
  }

  @Override
  public Iter iter() {
    materialize();
    return super.iter();
  }

  @Override
  public int getNumBins() {
    materialize();
    return super.getNumBins();
  }

  @Override
  public double getBinsize() {
    materialize();
    return super.getBinsize();
  }

  @Override
  public double getCoverMinimum() {
    materialize();
    return super.getCoverMinimum();
  }

  @Override
  public double getCoverMaximum() {
    materialize();
    return super.getCoverMaximum();
  }

  /**
   * Perform downsampling on a number of bins.
   *
   * @param data Data array (needs cast!)
   * @param start Interval start
   * @param end Interval end (exclusive)
   * @param size Intended size - extra bins are assumed to be empty, should be a
   *        power of two
   * @return New bin value
   */
  protected abstract T downsample(Object[] data, int start, int end, int size);

  /**
   * Rule to combine two bins or entries into one.
   * <p>
   * Note: first and second MAY be modified and returned, they will not be used
   * afterwards.
   *
   * @param first First bin value
   * @param second Second bin value
   * @return combined bin value
   */
  protected abstract T aggregate(T first, T second);

  /**
   * Clone a data passed to the algorithm for computing the initial size.
   *
   * @param data Data to be cloned
   * @return cloned data
   */
  protected abstract T cloneForCache(T data);

  /**
   * Make a new empty bucket for the data store.
   *
   * @return New instance.
   */
  protected abstract T makeObject();
}
