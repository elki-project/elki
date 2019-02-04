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
package de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector.Factory;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.UnweightedDiscreteUncertainObject;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Class to generate unweighted discrete uncertain objects.
 *
 * This is a second-order generator: it requires the use of another generator to
 * sample from (e.g. {@link UniformUncertainifier} or
 * {@link SimpleGaussianUncertainifier}).
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - UnweightedDiscreteUncertainObject
 */
public class UnweightedDiscreteUncertainifier extends AbstractDiscreteUncertainifier<UnweightedDiscreteUncertainObject> {
  /**
   * Constructor.
   *
   * @param inner Inner uncertainifier
   * @param minQuant Minimum number of samples
   * @param maxQuant Maximum number of samples
   */
  public UnweightedDiscreteUncertainifier(Uncertainifier<?> inner, int minQuant, int maxQuant) {
    super(inner, minQuant, maxQuant);
  }

  @Override
  public <A> UnweightedDiscreteUncertainObject newFeatureVector(Random rand, A array, NumberArrayAdapter<?, A> adapter) {
    UncertainObject uo = inner.newFeatureVector(rand, array, adapter);
    final int distributionSize = rand.nextInt((maxQuant - minQuant) + 1) + minQuant;
    DoubleVector[] samples = new DoubleVector[distributionSize];
    for(int i = 0; i < distributionSize; i++) {
      samples[i] = uo.drawSample(rand);
    }
    return new UnweightedDiscreteUncertainObject(samples);
  }

  @Override
  public Factory<UnweightedDiscreteUncertainObject, ?> getFactory() {
    return UnweightedDiscreteUncertainObject.FACTORY;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDiscreteUncertainifier.Parameterizer {
    @Override
    protected UnweightedDiscreteUncertainifier makeInstance() {
      return new UnweightedDiscreteUncertainifier(inner, minQuant, maxQuant);
    }
  }
}