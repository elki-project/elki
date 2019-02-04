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
package de.lmu.ifi.dbs.elki.datasource.filter.transform;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractStreamConversionFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.math.geodesy.EarthModel;
import de.lmu.ifi.dbs.elki.math.geodesy.SphericalVincentyEarthModel;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Project a 2D data set (longitude, latitude) to a 3D coordinate system (X, Y,
 * Z), such that Euclidean distance is line-of-sight.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - NumberVector
 * @composed - - - EarthModel
 * 
 * @param <V> Vector type.
 */
public class LngLatToECEFFilter<V extends NumberVector> extends AbstractStreamConversionFilter<V, V> {
  /**
   * Vector factory to use.
   */
  private NumberVector.Factory<V> factory;

  /**
   * Earth model to use.
   */
  private EarthModel model;

  /**
   * Constructor.
   * 
   * @param model Earth model
   */
  public LngLatToECEFFilter(EarthModel model) {
    super();
    this.model = model;
  }

  @Override
  protected V filterSingleObject(V obj) {
    return factory.newNumberVector(model.latLngDegToECEF(obj.doubleValue(1), obj.doubleValue(0)));
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD_2D;
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    factory = FilterUtil.guessFactory(in);
    return new VectorFieldTypeInformation<>(factory, 3, 3, in.getSerializer());
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
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Earth model to use.
     */
    private EarthModel model;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<EarthModel> modelP = new ObjectParameter<>(EarthModel.MODEL_ID, EarthModel.class, SphericalVincentyEarthModel.class);
      if(config.grab(modelP)) {
        model = modelP.instantiateClass(config);
      }
    }

    @Override
    protected LngLatToECEFFilter<V> makeInstance() {
      return new LngLatToECEFFilter<>(model);
    }
  }
}
