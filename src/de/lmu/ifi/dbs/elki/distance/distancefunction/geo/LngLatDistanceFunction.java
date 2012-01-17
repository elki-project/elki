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

/**
 * Distance function for 2D vectors in Longitude, Latitude form.
 * 
 * @author Erich Schubert
 */
public class LngLatDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Static instance
   */
  public static final LngLatDistanceFunction STATIC = new LngLatDistanceFunction();

  /**
   * Constructor. Use static instance instead!
   */
  @Deprecated
  public LngLatDistanceFunction() {
    super();
  }

  @Override
  public double doubleDistance(NumberVector<?, ?> o1, NumberVector<?, ?> o2) {
    return MathUtil.latlngDistance(o1.doubleValue(2), o1.doubleValue(1), o2.doubleValue(2), o2.doubleValue(1));
  }

  @Override
  public SimpleTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return VectorFieldTypeInformation.get(NumberVector.class, 2);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if(this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LngLatDistanceFunction makeInstance() {
      return LngLatDistanceFunction.STATIC;
    }
  }
}