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
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.GeoUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Distance function for 2D vectors in Latitude, Longitude form.
 * 
 * @author Erich Schubert
 */
public class LatLngDistanceFunction extends AbstractVectorDoubleDistanceFunction implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?>> {
  /**
   * Static instance.
   */
  public static final LatLngDistanceFunction STATIC = new LatLngDistanceFunction();

  /**
   * Constructor. Use static instance instead!
   */
  @Deprecated
  public LatLngDistanceFunction() {
    super();
  }

  @Override
  public double doubleDistance(NumberVector<?> o1, NumberVector<?> o2) {
    return GeoUtil.haversineFormulaDeg(o1.doubleValue(0), o1.doubleValue(1), o2.doubleValue(0), o2.doubleValue(1));
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if (mbr1 instanceof NumberVector) {
      if (mbr2 instanceof NumberVector) {
        return doubleDistance((NumberVector<?>) mbr1, (NumberVector<?>) mbr2);
      } else {
        NumberVector<?> o1 = (NumberVector<?>) mbr1;
        return GeoUtil.latlngMinDist(o1.doubleValue(0), o1.doubleValue(1), mbr2.getMin(0), mbr2.getMin(1), mbr2.getMax(0), mbr2.getMax(1));
      }
    } else {
      if (mbr2 instanceof NumberVector) {
        NumberVector<?> o2 = (NumberVector<?>) mbr2;
        return GeoUtil.latlngMinDist(o2.doubleValue(0), o2.doubleValue(1), mbr1.getMin(0), mbr1.getMin(1), mbr1.getMax(0), mbr1.getMax(1));
      } else {
        throw new UnsupportedOperationException("MBR to MBR mindist is not yet implemented.");
      }
    }
  }

  @Override
  public DoubleDistance minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleMinDist(mbr1, mbr2));
  }

  @Override
  public SimpleTypeInformation<? super NumberVector<?>> getInputTypeRestriction() {
    return new VectorFieldTypeInformation<NumberVector<?>>(NumberVector.class, 2);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
  }

  @Override
  public <T extends NumberVector<?>> SpatialPrimitiveDistanceQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(relation, this);
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
    protected LatLngDistanceFunction makeInstance() {
      return LatLngDistanceFunction.STATIC;
    }
  }
}
