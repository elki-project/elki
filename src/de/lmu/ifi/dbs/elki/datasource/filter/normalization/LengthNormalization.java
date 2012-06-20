package de.lmu.ifi.dbs.elki.datasource.filter.normalization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DoubleNorm;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Class to perform a normalization on vectors to norm 1.
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 * 
 * @param <V> vector type
 */
public class LengthNormalization<V extends NumberVector<V, ?>> extends AbstractStreamNormalization<V> {
  /**
   * Norm to use
   */
  DoubleNorm<? super V> norm;

  /**
   * Constructor
   * 
   * @param norm Norm to use
   */
  public LengthNormalization(DoubleNorm<? super V> norm) {
    super();
    this.norm = norm;
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    final double d = norm.doubleNorm(featureVector);
    return featureVector.newNumberVector(featureVector.getColumnVector().timesEquals(1 / d).getArrayRef());
  }

  @Override
  public V restore(V featureVector) throws NonNumericFeaturesException {
    throw new UnsupportedOperationException();
  }

  @Override
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
    // TODO.
    throw new UnsupportedOperationException();
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    /**
     * Option ID for normalization norm
     */
    public static final OptionID NORM_ID = OptionID.getOrCreateOptionID("normalization.norm", "Norm (length function) to use for computing the vector length.");

    /**
     * Norm to use
     */
    DoubleNorm<? super V> norm;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DoubleNorm<? super V>> normP = new ObjectParameter<DoubleNorm<? super V>>(NORM_ID, DoubleNorm.class, EuclideanDistanceFunction.class);
      if(config.grab(normP)) {
        norm = normP.instantiateClass(config);
      }
    }

    @Override
    protected LengthNormalization<V> makeInstance() {
      return new LengthNormalization<V>(norm);
    }
  }
}