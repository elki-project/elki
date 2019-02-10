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

import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

/**
 * A flexible histogram storing double, that can dynamically adapt the number of
 * bins to the data fed into the histogram.
 *
 * @author Erich Schubert
 * @since 0.5.5
 */
public class DoubleDynamicHistogram extends DoubleHistogram {
  /**
   * Cache for data to be inserted.
   */
  private double[] cachec;

  /**
   * Cache for data to be inserted.
   */
  private double[] cachev;

 /**
   * Cache fill size
   */
  private int cachefill;

  /**
   * Destination (minimum) size of the structure.
   * At most destsize * 2 bins are allowed.
   */
  private int destsize;

  /**
   * Constructor.
   * 
   * @param bins Design number of bins - may become twice as large!
   */
  public DoubleDynamicHistogram(int bins) {
    super(-1, 0.0, 1.0);
    this.destsize = bins;
    cachec = new double[this.destsize << CACHE_SHIFT];
    cachev = new double[this.destsize << CACHE_SHIFT];
    cachefill = 0;
  }

  /**
   * Materialize the histogram from the cache.
   */
  void materialize() {
    // already materialized?
    if (cachefill < 0) {
      return;
    }
    // Compute minimum and maximum
    double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
    for (int i = 0; i < cachefill; i++) {
      min = Math.min(min, cachec[i]);
      max = Math.max(max, cachec[i]);
    }
    // use the LinearScale magic to round to "likely suiteable" step sizes.
    // TODO: extract into a reusable function?
    LinearScale scale = new LinearScale(min, max);
    min = scale.getMin();
    max = scale.getMax();
    this.base = min;
    this.max = max;
    this.binsize = (max - min) / this.destsize;
    // initialize array
    this.data = new double[this.destsize << 1];
    size = destsize;
    // re-insert data we have
    final int end = cachefill;
    cachefill = -1; // So reinsert works!
    for (int i = 0; i < end; i++) {
      increment(cachec[i], cachev[i]);
    }
    // delete cache, signal that we're initialized
    cachec = null;
    cachev = null;
  }

  @Override
  public double get(double coord) {
    materialize();
    testResample(coord);
    return super.get(coord);
  }

  /**
   * Put fresh data into the histogram (or into the cache)
   * 
   * @param coord Coordinate
   * @param value Value
   */
  @Override
  public void increment(double coord, double value) {
    // Store in cache
    if (cachefill >= 0) {
      if (cachefill < cachec.length) {
        cachec[cachefill] = coord;
        cachev[cachefill] = value;
        cachefill ++;
        return;
      } else {
        materialize();
        // But continue below!
      }
    }
    // Check if we need to resample to accomodate this bin.
    testResample(coord);
    // super class will handle histogram resizing / shifting
    super.increment(coord, value);
  }

  /**
   * Test (and perform) downsampling when neede.
   * 
   * @param coord coordinate to accomodate.
   */
  private void testResample(double coord) {
    final int bin = getBinNr(coord);
    final int sizereq, off;
    if (bin < 0) {
      sizereq = size - bin;
      off = -bin;
    } else if (bin >= data.length) {
      sizereq = bin + 1;
      off = 0;
    } else {
      // Within the designated size - nothing to do.
      return;
    }
    if (sizereq < data.length) {
      // Accomodate by shifting. Let super do the job in {@link #get}
      return;
    }
    // Resampling, eventually by multiple levels.
    final int levels = BitsUtil.magnitude(sizereq / this.destsize) - 1;
    assert (levels > 0) : "No resampling required?!? sizereq=" + sizereq + " destsize=" + destsize + " array=" + data.length;
    final int step = 1 << levels;

    // We want to map [i ... i+step[ -> (i+off)/step
    // Fix point: i = (i+off)/step; i*(step-1)=off; i=off/(step-1)
    final int fixpoint = off / (step - 1);
    {
      // Start positions for in-place bottom-up downsampling.
      int oup = (fixpoint >= 0) ? fixpoint : 0;
      int inp = (oup << levels) - off;
      assert (-step < inp && inp <= oup && oup < inp + step) : (inp + " -> " + oup + " s=" + step + " o=" + off + " l=" + levels);
      for (; inp < size; inp += step, oup++) {
        assert (oup < inp + step && oup < data.length);
        data[oup] = downsample(data, Math.max(0, inp), Math.min(size, inp + step), step);
      }
      // Clean upwards
      for (; oup < data.length; oup++) {
        data[oup] = 0;
      }
    }
    if(off >= step) {
      // Start positions for in-place downsampling top-down:
      int oup = (fixpoint - 1 < size) ? fixpoint - 1 : size - 1;
      int inp = (oup << levels) - off;
      assert (oup > inp) : (inp + " -> " + oup + " s=" + step + " o=" + off + " l=" + levels);
      for (; inp > -step; inp -= step, oup--) {
        assert (oup >= inp && oup >= 0);
        data[oup] = downsample(data, Math.max(0, inp), Math.min(size, inp + step), step);
      }
      for (; oup >= 0; oup--) {
        data[oup] = 0;
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
  protected double downsample(double[] data, int start, int end, int size) {
    double sum = 0;
    for (int i = start; i < end; i++) {
      sum += data[i];
    }
    return sum;
  }
}
