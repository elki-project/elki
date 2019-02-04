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

/**
 * Abstract base class for histograms.
 * 
 * Note that this is abstracted from the actual data storage, so it can be
 * adapted for multiple use cases.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public abstract class AbstractStaticHistogram implements Histogram {
  /**
   * Array shift to account for negative indices.
   */
  protected int offset = 0;

  /**
   * Size of array storage.
   */
  protected int size;

  /**
   * Array 'base', i.e. the point of 0.0. Usually the minimum.
   */
  protected double base;

  /**
   * To avoid introducing an extra bucket for the maximum value.
   */
  protected double max;

  /**
   * Width of a bin.
   */
  protected double binsize;

  /**
   * Histogram constructor
   * 
   * @param bins Number of bins to use.
   * @param min Minimum Value
   * @param max Maximum Value
   */
  public AbstractStaticHistogram(int bins, double min, double max) {
    this.base = min;
    this.max = max;
    this.binsize = (max - min) / bins;
    this.size = bins;
  }

  /**
   * Compute the bin number. Has a special case for rounding max down to the
   * last bin.
   * 
   * @param coord Coordinate
   * @return bin number
   */
  protected int getBinNr(double coord) {
    if (Double.isInfinite(coord) || Double.isNaN(coord)) {
      throw new UnsupportedOperationException("Encountered non-finite value in Histogram: " + coord);
    }
    if (coord == max) {
      // System.err.println("Triggered special case: "+ (Math.floor((coord -
      // base) / binsize) + offset) + " vs. " + (size - 1));
      return size - 1;
    }
    return (int) Math.floor((coord - base) / binsize) + offset;
  }

  /**
   * Compute the size to grow to.
   * 
   * @param current Current size
   * @param requiredSize Required size
   * @return Size to allocate
   */
  protected static int growSize(int current, int requiredSize) {
    // Double until 64, then increase by 50% each time.
    int newCapacity = ((current < 64) ? ((current + 1) << 1) : ((current >> 1) * 3));
    // overflow?
    if (newCapacity < 0) {
      throw new OutOfMemoryError();
    }
    if (requiredSize > newCapacity) {
      newCapacity = requiredSize;
    }
    return requiredSize;
  }
  
  /**
   * Get the number of bins actually in use.
   * 
   * @return number of bins
   */
  @Override
  public int getNumBins() {
    return size;
  }

  /**
   * Get the size (width) of a bin.
   * 
   * @return bin size
   */
  @Override
  public double getBinsize() {
    return binsize;
  }

  /**
   * Get minimum (covered by bins, not data!)
   * 
   * @return minimum
   */
  @Override
  public double getCoverMinimum() {
    return base - offset * binsize;
  }

  /**
   * Get maximum (covered by bins, not data!)
   * 
   * @return maximum
   */
  @Override
  public double getCoverMaximum() {
    return base + (size - offset) * binsize;
  }

  /**
   * Get an iterator over all histogram bins.
   * 
   * @return Iterator
   */
  @Override
  public abstract Iter iter();

  /**
   * Iterator class to iterate over all bins.
   * 
   * @author Erich Schubert
   */
  public abstract class Iter implements Histogram.Iter {
    /**
     * Current bin number
     */
    int bin = 0;

    @Override
    public double getCenter() {
      return base + (bin + 0.5 - offset) * binsize;
    }

    @Override
    public double getLeft() {
      return base + (bin - offset) * binsize;
    }

    @Override
    public double getRight() {
      return base + (bin + 1 - offset) * binsize;
    }

    @Override
    public boolean valid() {
      return bin >= 0 && bin < size;
    }

    @Override
    public Iter advance() {
      bin++;
      return this;
    }

    @Override
    public int getOffset() {
      return bin;
    }

    @Override
    public Iter advance(int count) {
      bin += count;
      return this;
    }

    @Override
    public Iter retract() {
      bin--;
      return this;
    }

    @Override
    public Iter seek(int off) {
      bin = off;
      return this;
    }
  }
}
