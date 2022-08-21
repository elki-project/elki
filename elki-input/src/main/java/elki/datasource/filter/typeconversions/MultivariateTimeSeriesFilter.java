/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.datasource.filter.typeconversions;

import elki.data.FeatureVector;
import elki.data.type.MultivariateSeriesTypeInformation;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorTypeInformation;
import elki.datasource.filter.AbstractStreamConversionFilter;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Class to "fold" a flat number vector into a multivariate time series.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <V> Vector type
 */
public class MultivariateTimeSeriesFilter<V extends FeatureVector<?>> extends AbstractStreamConversionFilter<V, V> {
  /**
   * Number of variates to use.
   */
  int variates;

  /**
   * Constructor.
   * 
   * @param variates Number of variates.
   */
  public MultivariateTimeSeriesFilter(int variates) {
    super();
    this.variates = variates;
  }

  @Override
  protected V filterSingleObject(V obj) {
    if(obj.getDimensionality() % variates != 0) {
      throw new AbortException("Vector length " + obj.getDimensionality() + " not divisible by the number of variates " + variates);
    }
    return obj;
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.FEATURE_VECTORS;
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    VectorTypeInformation<V> vin = (VectorTypeInformation<V>) in;
    return new MultivariateSeriesTypeInformation<>(vin.getFactory(), in.getSerializer(), vin.mindim(), vin.maxdim(), variates);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   *
   * @param <V> Vector type
   */
  public static class Par<V extends FeatureVector<?>> implements Parameterizer {
    /**
     * Parameter for specifying the number of variates of this series.
     */
    public static final OptionID VARIATES_ID = new OptionID("series.variates", "Number of variates this time series has.");

    /**
     * Number of variates to use.
     */
    int variates;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(VARIATES_ID)//
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> variates = x);
    }

    @Override
    public MultivariateTimeSeriesFilter<V> make() {
      return new MultivariateTimeSeriesFilter<>(variates);
    }
  }
}
