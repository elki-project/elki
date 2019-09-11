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
package elki.distance.geo;

import elki.data.NumberVector;
import elki.data.spatial.SpatialComparable;
import elki.data.type.SimpleTypeInformation;
import elki.distance.NumberVectorDistance;
import elki.distance.SpatialPrimitiveDistance;
import elki.math.geodesy.EarthModel;
import elki.math.geodesy.SphericalVincentyEarthModel;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.NotImplementedException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Distance function for 2D vectors in Latitude, Longitude form.
 * <p>
 * The input data must be in degrees (not radians), and the output distance will
 * be in meters (see {@link EarthModel#distanceDeg}).
 * <p>
 * This implementation allows index accelerated queries using R*-trees (by
 * providing a point-to-rectangle minimum distance).
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br>
 * Geodetic Distance Queries on R-Trees for Indexing Geographic Data<br>
 * Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @composed - - - EarthModel
 */
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
    title = "Geodetic Distance Queries on R-Trees for Indexing Geographic Data", //
    booktitle = "Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)", //
    url = "https://doi.org/10.1007/978-3-642-40235-7_9", //
    bibkey = "DBLP:conf/ssd/SchubertZK13")
public class LatLngDistance implements SpatialPrimitiveDistance<NumberVector>, NumberVectorDistance<NumberVector> {
  /**
   * Earth model to use.
   */
  private EarthModel model;

  /**
   * Constructor.
   */
  public LatLngDistance(EarthModel model) {
    super();
    this.model = model;
  }

  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    return model.distanceDeg(o1.doubleValue(0), o1.doubleValue(1), o2.doubleValue(0), o2.doubleValue(1));
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1 instanceof NumberVector) {
      if(mbr2 instanceof NumberVector) {
        return distance((NumberVector) mbr1, (NumberVector) mbr2);
      }
      else {
        NumberVector o1 = (NumberVector) mbr1;
        return model.minDistDeg(o1.doubleValue(0), o1.doubleValue(1), mbr2.getMin(0), mbr2.getMin(1), mbr2.getMax(0), mbr2.getMax(1));
      }
    }
    else {
      if(mbr2 instanceof NumberVector) {
        NumberVector o2 = (NumberVector) mbr2;
        return model.minDistDeg(o2.doubleValue(0), o2.doubleValue(1), mbr1.getMin(0), mbr1.getMin(1), mbr1.getMax(0), mbr1.getMax(1));
      }
      else {
        throw new NotImplementedException("This distance function cannot - yet - be used with this algorithm, as the lower bound rectangle to rectangle distances have not yet been formalized for geodetic data.");
      }
    }
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return NumberVector.FIELD_2D;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public int hashCode() {
    return model.hashCode() + getClass().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (this == obj) || (obj != null && obj instanceof LatLngDistance && //
        this.model.equals(((LatLngDistance) obj).model));
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Earth model used.
     */
    EarthModel model;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<EarthModel>(EarthModel.MODEL_ID, EarthModel.class, SphericalVincentyEarthModel.class) //
          .grab(config, x -> model = x);
    }

    @Override
    public LatLngDistance make() {
      return new LatLngDistance(model);
    }
  }
}
