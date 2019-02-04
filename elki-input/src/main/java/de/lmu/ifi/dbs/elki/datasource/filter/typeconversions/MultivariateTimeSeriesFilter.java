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
package de.lmu.ifi.dbs.elki.datasource.filter.typeconversions;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.type.MultivariateSeriesTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractStreamConversionFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

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
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(MultivariateTimeSeriesFilter.class);

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
  public static class Parameterizer<V extends FeatureVector<?>> extends AbstractParameterizer {
    /**
     * Parameter for specifying the number of variates of this series.
     */
    public static final OptionID VARIATES_ID = new OptionID("series.variates", "Number of variates this time series has.");

    /**
     * Number of variates to use.
     */
    int variates;

    @Override
    protected void makeOptions(Parameterization config) {
      IntParameter variatesP = new IntParameter(VARIATES_ID)//
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(variatesP)) {
        variates = variatesP.intValue();
        if(variates == 1) {
          LOG.warning("For univariate series, you should not need to use this filter.");
        }
      }
    }

    @Override
    protected MultivariateTimeSeriesFilter<V> makeInstance() {
      return new MultivariateTimeSeriesFilter<>(variates);
    }
  }
}
