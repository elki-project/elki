package de.lmu.ifi.dbs.elki.distance.distancefunction.geo;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.NoDuplicateValueGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Distance function for 2D vectors in Latitude, Longitude form.
 * 
 * @author Erich Schubert
 */
public class DimensionSelectingLatLngDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Latitude dimension.
   */
  final int dimlat;

  /**
   * Longitude dimension.
   */
  final int dimlng;

  /**
   * Constructor.
   * 
   * @param dimlat Dimension storing the latitude
   * @param dimlng Dimension storing the longitude
   */
  public DimensionSelectingLatLngDistanceFunction(int dimlat, int dimlng) {
    super();
    this.dimlat = dimlat;
    this.dimlng = dimlng;
  }

  @Override
  public double doubleDistance(NumberVector<?> o1, NumberVector<?> o2) {
    return MathUtil.latlngDistance(o1.doubleValue(dimlat), o1.doubleValue(dimlng), o2.doubleValue(dimlat), o2.doubleValue(dimlng));
  }

  @Override
  public SimpleTypeInformation<? super NumberVector<?>> getInputTypeRestriction() {
    return new VectorFieldTypeInformation<NumberVector<?>>(NumberVector.class, Math.max(dimlat, dimlng), Integer.MAX_VALUE);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Latitude dimension parameter.
     */
    public static final OptionID LATDIM_ID = OptionID.getOrCreateOptionID("distance.latitudedim", "The dimension containing the latitude.");

    /**
     * Longitude dimension parameter.
     */
    public static final OptionID LNGDIM_ID = OptionID.getOrCreateOptionID("distance.longitudedim", "The dimension containing the longitude.");

    /**
     * Latitude dimension.
     */
    int dimlat;

    /**
     * Longitude dimension.
     */
    int dimlng;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter dimlatP = new IntParameter(LATDIM_ID);
      dimlatP.addConstraint(new GreaterEqualConstraint(0));
      if(config.grab(dimlatP)) {
        dimlat = dimlatP.getValue();
      }
      final IntParameter dimlngP = new IntParameter(LNGDIM_ID);
      dimlngP.addConstraint(new GreaterEqualConstraint(0));
      if(config.grab(dimlngP)) {
        dimlng = dimlngP.getValue();
      }
      config.checkConstraint(new NoDuplicateValueGlobalConstraint(dimlatP, dimlngP));
    }

    @Override
    protected DimensionSelectingLatLngDistanceFunction makeInstance() {
      return new DimensionSelectingLatLngDistanceFunction(dimlat, dimlng);
    }
  }
}