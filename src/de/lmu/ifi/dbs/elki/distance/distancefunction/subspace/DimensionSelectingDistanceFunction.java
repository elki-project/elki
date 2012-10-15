package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

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
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides a distance function that computes the distance between feature
 * vectors as the absolute difference of their values in a specified dimension.
 * 
 * @author Elke Achtert
 */
public class DimensionSelectingDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?>, DoubleDistance> implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?>> {
  /**
   * Parameter for dimensionality.
   */
  public static final OptionID DIM_ID = OptionID.getOrCreateOptionID("dim", "an integer between 1 and the dimensionality of the " + "feature space 1 specifying the dimension to be considered " + "for distance computation.");

  /**
   * The dimension to be considered for distance computation.
   */
  private int dim;

  /**
   * Constructor.
   * 
   * @param dim Dimension
   */
  public DimensionSelectingDistanceFunction(int dim) {
    super();
    this.dim = dim;
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function.
   * 
   * @param v1 first DatabaseObject
   * @param v2 second DatabaseObject
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    if(dim >= v1.getDimensionality() || dim >= v2.getDimensionality() || dim < 0) {
      throw new IllegalArgumentException("Specified dimension to be considered " + "is larger that dimensionality of FeatureVectors:" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n  dimension: " + dim);
    }

    double manhattan = v1.doubleValue(dim) - v2.doubleValue(dim);
    return Math.abs(manhattan);
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(dim >= mbr1.getDimensionality() || dim >= mbr2.getDimensionality() || dim < 0) {
      throw new IllegalArgumentException("Specified dimension to be considered " + "is larger that dimensionality of FeatureVectors:" + "\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString() + "\n  dimension: " + dim);
    }

    double m1, m2;
    if(mbr1.getMax(dim) < mbr2.getMin(dim)) {
      m1 = mbr1.getMax(dim);
      m2 = mbr2.getMin(dim);
    }
    else if(mbr1.getMin(dim) > mbr2.getMax(dim)) {
      m1 = mbr1.getMin(dim);
      m2 = mbr2.getMax(dim);
    }
    else { // The mbrs intersect!
      m1 = 0;
      m2 = 0;
    }
    double manhattan = m1 - m2;

    return Math.abs(manhattan);
  }

  @Override
  public DoubleDistance distance(NumberVector<?> o1, NumberVector<?> o2) {
    return new DoubleDistance(doubleDistance(o1, o2));
  }

  @Override
  public DoubleDistance minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleMinDist(mbr1, mbr2));
  }

  /**
   * Returns the selected dimension.
   * 
   * @return the selected dimension
   */
  public int getSelectedDimension() {
    return dim;
  }

  @Override
  public VectorTypeInformation<? super NumberVector<?>> getInputTypeRestriction() {
    return new VectorFieldTypeInformation<NumberVector<?>>(NumberVector.class, dim, Integer.MAX_VALUE);
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public <T extends NumberVector<?>> SpatialPrimitiveDistanceQuery<T, DoubleDistance> instantiate(Relation<T> database) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(database, this);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(!this.getClass().equals(obj.getClass())) {
      return false;
    }
    return this.dim == ((DimensionSelectingDistanceFunction) obj).dim;
  }
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected int dim = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter dimP = new IntParameter(DIM_ID);
      dimP.addConstraint(new GreaterEqualConstraint(0));
      if(config.grab(dimP)) {
        dim = dimP.getValue();
      }
    }

    @Override
    protected DimensionSelectingDistanceFunction makeInstance() {
      return new DimensionSelectingDistanceFunction(dim);
    }
  }
}