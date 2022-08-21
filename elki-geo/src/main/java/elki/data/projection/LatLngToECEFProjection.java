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
package elki.data.projection;

import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.math.geodesy.EarthModel;
import elki.math.geodesy.SphericalVincentyEarthModel;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Project (Latitude, Longitude) vectors to (X, Y, Z), from spherical
 * coordinates to ECEF (earth-centered earth-fixed).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @composed - - - EarthModel
 * @assoc - - - NumberVector
 * 
 * @param <V> Vector type
 */
public class LatLngToECEFProjection<V extends NumberVector> implements Projection<V, V> {
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
   * @param model Earth model to use.
   */
  public LatLngToECEFProjection(EarthModel model) {
    super();
    this.model = model;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(SimpleTypeInformation<? extends V> in) {
    final VectorFieldTypeInformation<V> vin = (VectorFieldTypeInformation<V>) in;
    factory = (NumberVector.Factory<V>) vin.getFactory();
  }

  @Override
  public V project(V data) {
    return factory.newNumberVector(model.latLngDegToECEF(data.doubleValue(0), data.doubleValue(1)));
  }

  @Override
  public SimpleTypeInformation<? super V> getInputDataTypeInformation() {
    return TypeUtil.NUMBER_VECTOR_FIELD_2D;
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    return new VectorFieldTypeInformation<>(factory, 3, 3, factory.getDefaultSerializer());
  }

  /**
   * Parameterization class.
   * 
   * @hidden
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Earth model to use.
     */
    EarthModel model;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<EarthModel>(EarthModel.MODEL_ID, EarthModel.class, SphericalVincentyEarthModel.class) //
          .grab(config, x -> model = x);
    }

    @Override
    public LatLngToECEFProjection<NumberVector> make() {
      return new LatLngToECEFProjection<>(model);
    }
  }
}
