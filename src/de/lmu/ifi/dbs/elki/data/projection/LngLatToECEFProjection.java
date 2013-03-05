package de.lmu.ifi.dbs.elki.data.projection;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.math.GeoUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;

/**
 * Project (Latitude, Longitude) vectors to (X, Y, Z), from WGS84 to ECEF.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class LngLatToECEFProjection<V extends NumberVector<?>> implements Projection<V, V> {
  /**
   * Earth model to use.
   * 
   * @author Erich Schubert
   */
  public static enum EarthModel { //
    SPHERICAL, // Simple spherical model
    WGS84 // WGS 84 standard ellipsoid
  }

  /**
   * Earth model to use
   */
  EarthModel model = EarthModel.WGS84;

  /**
   * Vector factory to use.
   */
  private NumberVector.Factory<V, ?> factory;

  /**
   * Constructor.
   * 
   * @param model Earth model to use.
   */
  public LngLatToECEFProjection(EarthModel model) {
    super();
    this.model = model;
  }

  @Override
  public void initialize(SimpleTypeInformation<V> in) {
    final VectorFieldTypeInformation<V> vin = (VectorFieldTypeInformation<V>) in;
    factory = (NumberVector.Factory<V, ?>) vin.getFactory();
  }

  @Override
  public V project(V data) {
    switch(model){
    case SPHERICAL:
      return factory.newNumberVector(GeoUtil.latLngDegSphericalToECEF(data.doubleValue(1), data.doubleValue(0)));
    case WGS84:
      return factory.newNumberVector(GeoUtil.latLngDegWGS84ToECEF(data.doubleValue(1), data.doubleValue(0)));
    default:
      throw new AbortException("Unsupported Model");
    }
  }

  @Override
  public SimpleTypeInformation<? super V> getInputDataTypeInformation() {
    return new VectorFieldTypeInformation<>(NumberVector.class, 2, 2);
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    return new VectorFieldTypeInformation<>(factory, 3, 3, factory.getDefaultSerializer());
  }

  /**
   * Parameterization class.
   * 
   * @apiviz.exclude
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to choose the earth model to use.
     */
    public static final OptionID MODEL_ID = new OptionID("ecef.model", "Earth model to use for projection. Default: WGS84 model.");

    /**
     * Earth model to use.
     */
    EarthModel model;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      EnumParameter<EarthModel> modelP = new EnumParameter<>(MODEL_ID, EarthModel.class, EarthModel.WGS84);
      if(config.grab(modelP)) {
        model = modelP.getValue();
      }
    }

    @Override
    protected LngLatToECEFProjection<NumberVector<?>> makeInstance() {
      return new LngLatToECEFProjection<>(model);
    }
  }
}
