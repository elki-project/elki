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
package elki.datasource.filter.transform;

import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.datasource.filter.AbstractStreamConversionFilter;
import elki.datasource.filter.FilterUtil;
import elki.math.geodesy.EarthModel;
import elki.math.geodesy.SphericalVincentyEarthModel;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Earth model to use.
     */
    private EarthModel model;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<EarthModel>(EarthModel.MODEL_ID, EarthModel.class, SphericalVincentyEarthModel.class) //
          .grab(config, x -> model = x);
    }

    @Override
    public LngLatToECEFFilter<V> make() {
      return new LngLatToECEFFilter<>(model);
    }
  }
}
