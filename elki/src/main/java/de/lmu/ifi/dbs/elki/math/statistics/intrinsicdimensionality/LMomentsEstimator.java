package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import de.lmu.ifi.dbs.elki.math.statistics.ProbabilityWeightedMoments;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Probability weighted moments based estimator using L-Moments.
 *
 * Derived from the L-Moments estimation for the exponential distribution.
 *
 * @author Jonathan von Brünken
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LMomentsEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final LMomentsEstimator STATIC = new LMomentsEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    final int begin = countLeadingZeros(data, adapter, end);
    final int len = end - begin;
    if(len < 2) {
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    if(len == 2) { // Fallback to MoM
      double v1 = adapter.getDouble(data, begin) / adapter.getDouble(data, begin + 1);
      return v1 / (1 - v1);
    }
    final double w = adapter.getDouble(data, end - 1);
    double[] lmom = ProbabilityWeightedMoments.samLMR(data, new ReverseAdapter<>(adapter, begin, end), 2);
    if(lmom[1] == 0) { // Fallback to first moment only.
      // TODO: is this the right thing to do? When does this happen?
      return -.5 * (lmom[0] * 2) / w * (len + .5) * len;
    }
    return -.5 * ((lmom[0] * lmom[0] / lmom[1]) - lmom[0]) / w;
  }

  /**
   * Adapter to process an array in reverse order.
   *
   * @author Erich Schubert
   *
   * @param <A> Array type
   */
  private static class ReverseAdapter<A> implements NumberArrayAdapter<Double, A> {
    /**
     * Size and last element.
     */
    private int size, last;

    /**
     * Adapter class.
     */
    private NumberArrayAdapter<?, ? super A> inner;

    /**
     * Constructor.
     *
     * @param inner Inner adapter
     * @param begin Begin
     * @param end End of the array
     */
    public ReverseAdapter(NumberArrayAdapter<?, ? super A> inner, int begin, int end) {
      this.inner = inner;
      this.size = end - begin;
      this.last = end - 1; // Inclusive
    }

    @Override
    public int size(A array) {
      return size;
    }

    @Override
    public Double get(A array, int off) throws IndexOutOfBoundsException {
      return inner.getDouble(array, last - off);
    }

    @Override
    public double getDouble(A array, int off) throws IndexOutOfBoundsException {
      return inner.getDouble(array, last - off);
    }

    @Override
    public float getFloat(A array, int off) throws IndexOutOfBoundsException {
      return inner.getFloat(array, last - off);
    }

    @Override
    public int getInteger(A array, int off) throws IndexOutOfBoundsException {
      return inner.getInteger(array, last - off);
    }

    @Override
    public short getShort(A array, int off) throws IndexOutOfBoundsException {
      return inner.getShort(array, last - off);
    }

    @Override
    public long getLong(A array, int off) throws IndexOutOfBoundsException {
      return inner.getLong(array, last - off);
    }

    @Override
    public byte getByte(A array, int off) throws IndexOutOfBoundsException {
      return inner.getByte(array, last - off);
    }
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
    protected LMomentsEstimator makeInstance() {
      return STATIC;
    }
  }
}
