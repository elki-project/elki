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
import elki.data.type.VectorFieldTypeInformation;
import elki.distance.NumberVectorDistance;
import elki.distance.SpatialPrimitiveDistance;
import elki.math.geodesy.EarthModel;
import elki.math.geodesy.SphericalVincentyEarthModel;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.NotImplementedException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
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
public class DimensionSelectingLatLngDistance implements SpatialPrimitiveDistance<NumberVector>, NumberVectorDistance<NumberVector> {
  /**
   * Latitude dimension.
   */
  final int dimlat;

  /**
   * Longitude dimension.
   */
  final int dimlng;

  /**
   * Earth model used.
   */
  final EarthModel model;

  /**
   * Constructor.
   * 
   * @param dimlat Dimension storing the latitude
   * @param dimlng Dimension storing the longitude
   * @param model Earth model
   */
  public DimensionSelectingLatLngDistance(int dimlat, int dimlng, EarthModel model) {
    super();
    this.dimlat = dimlat;
    this.dimlng = dimlng;
    this.model = model;
  }

  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    return model.distanceDeg(o1.doubleValue(dimlat), o1.doubleValue(dimlng), o2.doubleValue(dimlat), o2.doubleValue(dimlng));
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1 instanceof NumberVector) {
      if(mbr2 instanceof NumberVector) {
        return distance((NumberVector) mbr1, (NumberVector) mbr2);
      }
      else {
        NumberVector o1 = (NumberVector) mbr1;
        return model.minDistDeg(o1.doubleValue(dimlat), o1.doubleValue(dimlng), mbr2.getMin(dimlat), mbr2.getMin(dimlng), mbr2.getMax(dimlat), mbr2.getMax(dimlng));
      }
    }
    else {
      if(mbr2 instanceof NumberVector) {
        NumberVector o2 = (NumberVector) mbr2;
        return model.minDistDeg(o2.doubleValue(dimlat), o2.doubleValue(dimlng), mbr1.getMin(dimlat), mbr1.getMin(dimlng), mbr1.getMax(dimlat), mbr1.getMax(dimlng));
      }
      else {
        throw new NotImplementedException("This distance function cannot - yet - be used with this algorithm, as the lower bound rectangle to rectangle distances have not yet been formalized for geodetic data.");
      }
    }
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return VectorFieldTypeInformation.typeRequest(NumberVector.class, Math.max(dimlat, dimlng), Integer.MAX_VALUE);
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + dimlat;
    result = prime * result + dimlng;
    result = prime * result + ((model == null) ? 0 : model.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    return (this == obj) || (obj != null && obj instanceof DimensionSelectingLatLngDistance && //
        this.model.equals(((DimensionSelectingLatLngDistance) obj).model));
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Latitude dimension parameter.
     */
    public static final OptionID LATDIM_ID = new OptionID("distance.latitudedim", "The dimension containing the latitude.");

    /**
     * Longitude dimension parameter.
     */
    public static final OptionID LNGDIM_ID = new OptionID("distance.longitudedim", "The dimension containing the longitude.");

    /**
     * Latitude dimension.
     */
    int dimlat;

    /**
     * Longitude dimension.
     */
    int dimlng;

    /**
     * Earth model used.
     */
    EarthModel model;

    @Override
    public void configure(Parameterization config) {
      IntParameter dimlatP = new IntParameter(LATDIM_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      dimlatP.grab(config, x -> dimlat = x);
      IntParameter dimlngP = new IntParameter(LNGDIM_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      dimlngP.grab(config, x -> dimlng = x);
      // Non-formalized parameter constraint:
      if(dimlat == dimlng) {
        config.reportError(new WrongParameterValueException(dimlatP, "and", dimlngP, "should be different columns."));
      }
      new ObjectParameter<EarthModel>(EarthModel.MODEL_ID, EarthModel.class, SphericalVincentyEarthModel.class) //
          .grab(config, x -> model = x);
    }

    @Override
    public DimensionSelectingLatLngDistance make() {
      return new DimensionSelectingLatLngDistance(dimlat, dimlng, model);
    }
  }
}
