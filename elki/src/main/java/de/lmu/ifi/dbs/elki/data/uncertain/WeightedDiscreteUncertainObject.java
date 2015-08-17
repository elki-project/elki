package de.lmu.ifi.dbs.elki.data.uncertain;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Weighted version of discrete uncertain objects.
 *
 * <ul>
 * <li>Every object is represented by a finite number of discrete samples.</li>
 * <li>Every sample has a weight associated with it.</li>
 * <li>Samples with higher weight are more likely to be returned by
 * {@link #drawSample}.
 * </ul>
 *
 * @author Alexander Koos
 * @author Erich Schubert
 */
public class WeightedDiscreteUncertainObject extends AbstractDiscreteUncertainObject {
  /**
   * Vector factory.
   */
  public static final FeatureVector.Factory<WeightedDiscreteUncertainObject, ?> FACTORY = new Factory();

  /**
   * Samples
   */
  private DoubleVector[] samples;

  /**
   * Sample weights
   */
  private double[] weights;

  /**
   * Total sum of weights.
   */
  private double weightSum;

  // Constructor
  public WeightedDiscreteUncertainObject(DoubleVector[] samples, double[] weights) {
    super();
    if(samples.length == 0) {
      throw new AbortException("Discrete Uncertain Objects must have at least one point.");
    }
    double check = 0;
    for(double weight : weights) {
      if(!(weight > 0 && weight < Double.POSITIVE_INFINITY)) {
        throw new IllegalArgumentException("Probabilities must be in positive and finite.");
      }
      check += weight;
    }
    this.samples = samples;
    this.bounds = computeBounds(samples);
    this.weights = weights;
    this.weightSum = check;
  }

  @Override
  public DoubleVector drawSample(Random rand) {
    // Weighted sampling:
    double r = rand.nextDouble() * weightSum;
    int index = weights.length;
    while(--index >= 0 && r < weights[index]) {
      r -= weights[index];
    }
    if(index < 0) { // Within rounding errors
      index = rand.nextInt(samples.length);
    }
    return samples[index];
  }

  @Override
  public DoubleVector getCenterOfMass() {
    final int dim = getDimensionality();
    // Weighted average.
    double[] meanVals = new double[dim];
    for(int i = 0; i < samples.length; i++) {
      DoubleVector v = samples[i];
      for(int d = 0; d < dim; d++) {
        meanVals[d] += v.doubleValue(d) * weights[i];
      }
    }

    for(int d = 0; d < dim; d++) {
      meanVals[d] /= weightSum;
    }
    return new DoubleVector(meanVals);
  }

  /**
   * Factory class for this data type. Not for public use, use
   * {@link de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier.Uncertainifier} to
   * derive uncertain objects from certain vectors.
   *
   * TODO: provide serialization functionality.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  private static class Factory implements FeatureVector.Factory<WeightedDiscreteUncertainObject, Number> {
    @Override
    public <A> WeightedDiscreteUncertainObject newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ByteBufferSerializer<WeightedDiscreteUncertainObject> getDefaultSerializer() {
      return null; // No serializer available.
    }

    @Override
    public Class<? super WeightedDiscreteUncertainObject> getRestrictionClass() {
      return WeightedDiscreteUncertainObject.class;
    }
  }
}