package de.lmu.ifi.dbs.elki.distance.distancefunction.geo;

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
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractSpatialDistanceFunction;
import de.lmu.ifi.dbs.elki.math.geodesy.EarthModel;
import de.lmu.ifi.dbs.elki.math.geodesy.SphericalVincentyEarthModel;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Distance function for 2D vectors in Longitude, Latitude form.
 * 
 * The input data must be in degrees (not radians), and the output distance will
 * be in meters (see {@link EarthModel#distanceDeg}).
 * 
 * This implementation allows index accelerated queries using R*-trees (by
 * providing a point-to-rectangle minimum distance), as published in:
 * <p>
 * Erich Schubert, Arthur Zimek and Hans-Peter Kriegel<br />
 * Geodetic Distance Queries on R-Trees for Indexing Geographic Data<br />
 * Advances in Spatial and Temporal Databases - 13th International Symposium,
 * SSTD 2013, Munich, Germany
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.composedOf EarthModel
 */
public class LngLatDistanceFunction extends AbstractSpatialDistanceFunction {
  /**
   * Earth model to use.
   */
  private EarthModel model;

  /**
   * Constructor.
   */
  public LngLatDistanceFunction(EarthModel model) {
    super();
    this.model = model;
  }

  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    return model.distanceDeg(o1.doubleValue(1), o1.doubleValue(0), o2.doubleValue(1), o2.doubleValue(0));
  }

  @Override
  @Reference(authors = "Erich Schubert, Arthur Zimek and Hans-Peter Kriegel", title = "Geodetic Distance Queries on R-Trees for Indexing Geographic Data", booktitle = "Advances in Spatial and Temporal Databases - 13th International Symposium, SSTD 2013, Munich, Germany")
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1 instanceof NumberVector) {
      if(mbr2 instanceof NumberVector) {
        return distance((NumberVector) mbr1, (NumberVector) mbr2);
      }
      else {
        NumberVector o1 = (NumberVector) mbr1;
        return model.minDistDeg(o1.doubleValue(1), o1.doubleValue(0), mbr2.getMin(1), mbr2.getMin(0), mbr2.getMax(1), mbr2.getMax(0));
      }
    }
    else {
      if(mbr2 instanceof NumberVector) {
        NumberVector o2 = (NumberVector) mbr2;
        return model.minDistDeg(o2.doubleValue(1), o2.doubleValue(0), mbr1.getMin(1), mbr1.getMin(0), mbr1.getMax(1), mbr1.getMax(0));
      }
      else {
        throw new NotImplementedException("This distance function cannot - yet - be used with this algorithm, as the lower bound rectangle to rectangle distances have not yet been formalized for geodetic data.");
      }
    }
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD_2D;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((model == null) ? 0 : model.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    LngLatDistanceFunction other = (LngLatDistanceFunction) obj;
    if(model == null) {
      if(other.model != null) {
        return false;
      }
    }
    else if(!model.equals(other.model)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "LngLatDistanceFunction [model=" + model + "]";
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
     * Earth model used.
     */
    EarthModel model;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<EarthModel> modelP = new ObjectParameter<>(EarthModel.MODEL_ID, EarthModel.class, SphericalVincentyEarthModel.class);
      if(config.grab(modelP)) {
        model = modelP.instantiateClass(config);
      }
    }

    @Override
    protected LngLatDistanceFunction makeInstance() {
      return new LngLatDistanceFunction(model);
    }
  }
}
