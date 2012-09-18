package de.lmu.ifi.dbs.elki.data;

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

import gnu.trove.impl.unmodifiable.TUnmodifiableIntDoubleMap;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.Arrays;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * <p>
 * A SparseDoubleVector is to store real values as double values.
 * </p>
 * 
 * A SparseDoubleVector only requires storage for those attribute values that
 * are non-zero.
 * 
 * @author Arthur Zimek
 */
// TODO: implement ByteArraySerializer<SparseDoubleVector>
public class SparseDoubleVector extends AbstractNumberVector<Double> implements SparseNumberVector<Double> {
  /**
   * Static instance.
   */
  public static final SparseDoubleVector.Factory FACTORY = new SparseDoubleVector.Factory();

  /**
   * Indexes of values.
   */
  private final int[] indexes;

  /**
   * Stored values.
   */
  private final double[] values;

  /**
   * The dimensionality of this feature vector.
   */
  private int dimensionality;

  /**
   * Direct constructor.
   * 
   * @param indexes Indexes Must be sorted!
   * @param values Associated value.
   * @param dimensionality "true" dimensionality
   */
  public SparseDoubleVector(int[] indexes, double[] values, int dimensionality) {
    super();
    this.indexes = indexes;
    this.values = values;
    this.dimensionality = dimensionality;
  }

  /**
   * Provides a SparseDoubleVector consisting of double values according to the
   * specified mapping of indices and values.
   * 
   * @param values the values to be set as values of the real vector
   * @param dimensionality the dimensionality of this feature vector
   * @throws IllegalArgumentException if the given dimensionality is too small
   *         to cover the given values (i.e., the maximum index of any value not
   *         zero is bigger than the given dimensionality)
   */
  public SparseDoubleVector(TIntDoubleMap values, int dimensionality) throws IllegalArgumentException {
    if (values.size() > dimensionality) {
      throw new IllegalArgumentException("values.size() > dimensionality!");
    }

    this.indexes = new int[values.size()];
    this.values = new double[values.size()];
    // Import and sort the indexes
    {
      TIntDoubleIterator iter = values.iterator();
      for (int i = 0; iter.hasNext(); i++) {
        iter.advance();
        this.indexes[i] = iter.key();
      }
      Arrays.sort(this.indexes);
    }
    // Import the values accordingly
    {
      for (int i = 0; i < values.size(); i++) {
        this.values[i] = values.get(this.indexes[i]);
      }
    }
    this.dimensionality = dimensionality;
    final int maxdim = getMaxDim();
    if (maxdim > dimensionality) {
      throw new IllegalArgumentException("Given dimensionality " + dimensionality + " is too small w.r.t. the given values (occurring maximum: " + maxdim + ").");
    }
  }

  /**
   * Get the maximum dimensionality.
   * 
   * @return the maximum dimensionality seen
   */
  private int getMaxDim() {
    if (this.indexes.length == 0) {
      return 0;
    } else {
      return this.indexes[this.indexes.length - 1];
    }
  }

  /**
   * Provides a SparseDoubleVector consisting of double values according to the
   * specified mapping of indices and values.
   * 
   * @param values the values to be set as values of the real vector
   * @throws IllegalArgumentException if the given dimensionality is too small
   *         to cover the given values (i.e., the maximum index of any value not
   *         zero is bigger than the given dimensionality)
   */
  public SparseDoubleVector(double[] values) throws IllegalArgumentException {
    this.dimensionality = values.length;

    // Count the number of non-zero entries
    int size = 0;
    {
      for (int i = 0; i < values.length; i++) {
        if (values[i] != 0.0f) {
          size++;
        }
      }
    }
    this.indexes = new int[size];
    this.values = new double[size];

    // Copy the values
    {
      int pos = 0;
      for (int i = 0; i < values.length; i++) {
        double value = values[i];
        if (value != 0.0f) {
          this.indexes[pos] = i + 1;
          this.values[pos] = value;
          pos++;
        }
      }
    }
  }

  @Override
  public int getDimensionality() {
    return dimensionality;
  }

  /**
   * Sets the dimensionality to the new value.
   * 
   * 
   * @param dimensionality the new dimensionality
   * @throws IllegalArgumentException if the given dimensionality is too small
   *         to cover the given values (i.e., the maximum index of any value not
   *         zero is bigger than the given dimensionality)
   */
  @Override
  public void setDimensionality(int dimensionality) throws IllegalArgumentException {
    final int maxdim = getMaxDim();
    if (maxdim > dimensionality) {
      throw new IllegalArgumentException("Given dimensionality " + dimensionality + " is too small w.r.t. the given values (occurring maximum: " + maxdim + ").");
    }
    this.dimensionality = dimensionality;
  }

  @Override
  @Deprecated
  public Double getValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    if (pos >= 0) {
      return values[pos];
    } else {
      return 0.0;
    }
  }

  @Override
  public double doubleValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    if (pos >= 0) {
      return values[pos];
    } else {
      return 0.0;
    }
  }

  @Override
  public long longValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    if (pos >= 0) {
      return (long) values[pos];
    } else {
      return 0;
    }
  }

  @Override
  public Vector getColumnVector() {
    return new Vector(getValues());
  }

  /**
   * <p>
   * Provides a String representation of this SparseDoubleVector as suitable for
   * {@link de.lmu.ifi.dbs.elki.datasource.parser.SparseNumberVectorLabelParser}
   * .
   * </p>
   * 
   * <p>
   * The returned String is a single line with entries separated by
   * {@link AbstractNumberVector#ATTRIBUTE_SEPARATOR}. The first entry gives the
   * number of values actually not zero. Following entries are pairs of Integer
   * and Double where the Integer gives the index of the dimensionality and the
   * Double gives the corresponding value.
   * </p>
   * 
   * <p>
   * Example: a vector (0,1.2,1.3,0)<sup>T</sup> would result in the String<br>
   * <code>2 2 1.2 3 1.3</code><br>
   * </p>
   * 
   * @return a String representation of this SparseDoubleVector
   */
  @Override
  public String toString() {
    StringBuilder featureLine = new StringBuilder();
    featureLine.append(this.indexes.length);
    for (int i = 0; i < this.indexes.length; i++) {
      featureLine.append(ATTRIBUTE_SEPARATOR);
      featureLine.append(this.indexes[i]);
      featureLine.append(ATTRIBUTE_SEPARATOR);
      featureLine.append(this.values[i]);
    }

    return featureLine.toString();
  }

  /**
   * Returns an array consisting of the values of this feature vector.
   * 
   * @return an array consisting of the values of this feature vector
   */
  private double[] getValues() {
    double[] vals = new double[dimensionality];
    for (int i = 0; i < indexes.length; i++) {
      vals[this.indexes[i]] = this.values[i];
    }
    return vals;
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   */
  public static class Factory extends AbstractNumberVector.Factory<SparseDoubleVector, Double> implements SparseNumberVector.Factory<SparseDoubleVector, Double> {
    @Override
    public <A> SparseDoubleVector newFeatureVector(A array, ArrayAdapter<Double, A> adapter) {
      int dim = adapter.size(array);
      double[] values = new double[dim];
      for (int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i);
      }
      // TODO: inefficient
      return new SparseDoubleVector(values);
    }

    @Override
    public <A> SparseDoubleVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      int dim = adapter.size(array);
      double[] values = new double[dim];
      for (int i = 0; i < dim; i++) {
        values[i] = adapter.getDouble(array, i);
      }
      // TODO: inefficient
      return new SparseDoubleVector(values);
    }

    @Override
    public SparseDoubleVector newNumberVector(TIntDoubleMap values, int maxdim) {
      return new SparseDoubleVector(values, maxdim);
    }

    @Override
    public ByteBufferSerializer<SparseDoubleVector> getDefaultSerializer() {
      // FIXME: add a serializer
      return null;
    }
    
    @Override
    public Class<? super SparseDoubleVector> getRestrictionClass() {
      return SparseDoubleVector.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected SparseDoubleVector.Factory makeInstance() {
        return FACTORY;
      }
    }
  }

  @Override
  public BitSet getNotNullMask() {
    BitSet b = new BitSet();
    for (int key : indexes) {
      b.set(key);
    }
    return b;
  }

  /**
   * Empty map.
   */
  public static final TIntDoubleMap EMPTYMAP = new TUnmodifiableIntDoubleMap(new TIntDoubleHashMap());
}
