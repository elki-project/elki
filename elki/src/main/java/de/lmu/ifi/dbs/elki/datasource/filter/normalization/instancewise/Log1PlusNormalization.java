package de.lmu.ifi.dbs.elki.datasource.filter.normalization.instancewise;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.AbstractStreamNormalization;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Normalize the data set by applying log(1+|x|*b)/log(b+1) to any value. If the
 * input data was in [0;1], then the resulting values will be in the same range.
 * 
 * By default b=1, and thus the transformation is log2(1+|x|).
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <V> vector type
 */
public class Log1PlusNormalization<V extends NumberVector> extends AbstractStreamNormalization<V> {
  /**
   * Static instance.
   */
  public static final Log1PlusNormalization<NumberVector> STATIC = new Log1PlusNormalization<>(1.);

  /**
   * Boosting factor, and scaling coefficient.
   */
  protected double boost, scale;

  /**
   * Constructor.
   * 
   * @param boost Boosting parameter
   */
  public Log1PlusNormalization(double boost) {
    super();
    this.boost = boost;
    this.scale = 1. / Math.log1p(boost);
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    double[] data = new double[featureVector.getDimensionality()];
    for(int d = 0; d < data.length; ++d) {
      data[d] = featureVector.doubleValue(d);
      data[d] = Math.log1p((data[d] > 0 ? data[d] : -data[d]) * boost) * scale;
    }
    return factory.newNumberVector(data);
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Boosting factor parameter.
     */
    public static final OptionID BOOST_ID = new OptionID("log1pscale.boost", "Boosting factor. Larger values will yield a steeper curve.");

    /**
     * Boosting factor.
     */
    protected double boost;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter boostP = new DoubleParameter(BOOST_ID, 1.) //
      .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(boostP)) {
        boost = boostP.doubleValue();
      }
    }

    @Override
    protected Log1PlusNormalization<V> makeInstance() {
      return new Log1PlusNormalization<>(boost);
    }
  }
}
