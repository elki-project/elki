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
package de.lmu.ifi.dbs.elki.data.uncertain;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Weighted version of discrete uncertain objects.
 * <ul>
 * <li>Every object is represented by a finite number of discrete samples.</li>
 * <li>Every sample has a weight associated with it.</li>
 * <li>Samples with higher weight are more likely to be returned by
 * {@link #drawSample}.
 * </ul>
 * References:
 * <p>
 * This is called the block independent-disjoint (BID model) in:
 * <p>
 * N. Dalvi, C. Ré, D. Suciu<br>
 * Probabilistic databases: diamonds in the dirt<br>
 * Communications of the ACM 52, 7
 * <p>
 * This is also known as the X-Tuple model in:
 * <p>
 * O. Benjelloun, A. D. Sarma, A. Halevy, J. Widom<br>
 * ULDBs: Databases with uncertainty and lineage<br>
 * In Proc. of the 32nd Int. Conf. on Very Large Data Bases (VLDB)
 * <p>
 * If only a single sample is provided, this can be used to model existential
 * uncertainty as in:
 * <p>
 * N. Dalvi, D. Suciu<br>
 * Efficient query evaluation on probabilistic databases.<br>
 * The VLDB Journal, 16(4)
 * <p>
 * and:
 * <p>
 * Thomas Bernecker, Hans-Peter Kriegel, Matthias Renz, Florian Verhein,
 * Andreas Züfle<br>
 * Probabilistic frequent itemset mining in uncertain databases.<br>
 * In Proc. 15th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "N. Dalvi, C. Ré, D. Suciu", //
    title = "Probabilistic databases: diamonds in the dirt", //
    booktitle = "Communications of the ACM 52, 7", //
    url = "https://doi.org/10.1145/1538788.1538810", //
    bibkey = "DBLP:journals/cacm/DalviRS09")
@Reference(authors = "O. Benjelloun, A. D. Sarma, A. Halevy, J. Widom", //
    title = "ULDBs: Databases with uncertainty and lineage", //
    booktitle = "Proc. of the 32nd Int. Conf. on Very Large Data Bases (VLDB)", //
    url = "http://www.vldb.org/conf/2006/p953-benjelloun.pdf", //
    bibkey = "DBLP:conf/vldb/BenjellounSHW06")
@Reference(authors = "Thomas Bernecker, Hans-Peter Kriegel, Matthias Renz, Florian Verhein, Andreas Züfle", //
    title = "Probabilistic frequent itemset mining in uncertain databases", //
    booktitle = "Proc. 15th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining", //
    url = "https://doi.org/10.1145/1557019.1557039", //
    bibkey = "DBLP:conf/kdd/BerneckerKRVZ09")
public class WeightedDiscreteUncertainObject extends AbstractUncertainObject implements DiscreteUncertainObject {
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
   * Constructor.
   *
   * @param samples Samples
   * @param weights Weights (must be in ]0:1] and sum up to at most 1).
   */
  public WeightedDiscreteUncertainObject(DoubleVector[] samples, double[] weights) {
    super();
    if(samples.length == 0) {
      throw new AbortException("Discrete Uncertain Objects must have at least one point.");
    }
    double check = 0;
    for(double weight : weights) {
      if(!(weight > 0 && weight < 1.)) {
        throw new IllegalArgumentException("Probabilities must be in ]0:1], but is " + weight);
      }
      check += weight;
    }
    if(!(check > 0 && check <= 1.0000001)) {
      throw new IllegalArgumentException("Probability totals must be in ]0:1], but total is " + check);
    }
    this.samples = samples;
    this.bounds = computeBounds(samples);
    this.weights = weights;
  }

  @Override
  public DoubleVector drawSample(Random rand) {
    // Weighted sampling:
    double r = rand.nextDouble();
    int index = weights.length;
    while(--index >= 0 && r < weights[index]) {
      r -= weights[index];
    }
    if(index < 0) {
      if(r < Double.MIN_NORMAL * samples.length) {
        // Within rounding errors, assume the total weight is exactly 1.
        index = rand.nextInt(samples.length);
      }
      else {
        return null;
      }
    }
    return samples[index];
  }

  @Override
  public DoubleVector getCenterOfMass() {
    final int dim = getDimensionality();
    // Weighted average.
    double[] meanVals = new double[dim];
    double weightSum = 0.;
    for(int i = 0; i < samples.length; i++) {
      DoubleVector v = samples[i];
      final double w = weights[i];
      for(int d = 0; d < dim; d++) {
        meanVals[d] += v.doubleValue(d) * w;
      }
      weightSum += w;
    }

    for(int d = 0; d < dim; d++) {
      meanVals[d] /= weightSum;
    }
    return DoubleVector.wrap(meanVals);
  }

  @Override
  public int getNumberSamples() {
    return samples.length;
  }

  @Override
  public DoubleVector getSample(int i) {
    return samples[i];
  }

  @Override
  public double getWeight(int i) {
    return weights[i];
  }

  /**
   * Factory class for this data type. Not for public use, use
   * {@link de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier.Uncertainifier} to
   * derive uncertain objects from certain vectors.
   * <p>
   * TODO: provide serialization functionality.
   *
   * @author Erich Schubert
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