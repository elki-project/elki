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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.instancewise;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorStreamConversionFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.Normalization;
import de.lmu.ifi.dbs.elki.distance.distancefunction.Norm;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Class to perform a normalization on vectors to norm 1.
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <V> vector type
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.normalization.LengthNormalization" })
public class LengthNormalization<V extends NumberVector> extends AbstractVectorStreamConversionFilter<V, V> implements Normalization<V> {
  /**
   * Norm to use.
   */
  Norm<? super V> norm;

  /**
   * Constructor.
   * 
   * @param norm Norm to use
   */
  public LengthNormalization(Norm<? super V> norm) {
    super();
    this.norm = norm;
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    final double d = norm.norm(featureVector);
    return factory.newNumberVector(VMath.timesEquals(featureVector.toArray(), d > 0 ? 1 / d : 1.));
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return in;
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Option ID for normalization norm.
     */
    public static final OptionID NORM_ID = new OptionID("normalization.norm", "Norm (length function) to use for computing the vector length.");

    /**
     * Norm to use.
     */
    Norm<? super V> norm;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Norm<? super V>> normP = new ObjectParameter<>(NORM_ID, Norm.class, EuclideanDistanceFunction.class);
      if(config.grab(normP)) {
        norm = normP.instantiateClass(config);
      }
    }

    @Override
    protected LengthNormalization<V> makeInstance() {
      return new LengthNormalization<>(norm);
    }
  }
}
