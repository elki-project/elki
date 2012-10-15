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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Provides a distance function that computes the distance (which is a double
 * distance) between feature vectors only in specified dimensions.
 * 
 * @author Elke Achtert
 * @param <V> the type of FeatureVector to compute the distances in between
 */
public abstract class AbstractDimensionsSelectingDoubleDistanceFunction<V extends FeatureVector<?>> extends AbstractPrimitiveDistanceFunction<V, DoubleDistance> implements PrimitiveDoubleDistanceFunction<V>, DimensionSelectingSubspaceDistanceFunction<V, DoubleDistance> {
  /**
   * Dimensions parameter.
   */
  public static final OptionID DIMS_ID = OptionID.getOrCreateOptionID("distance.dims", "a comma separated array of integer values, where 0 <= d_i < the dimensionality of the feature space specifying the dimensions to be considered for distance computation. If this parameter is not set, no dimensions will be considered, i.e. the distance between two objects is always 0.");

  /**
   * The dimensions to be considered for distance computation.
   */
  protected BitSet dimensions;

  /**
   * Constructor.
   * 
   * @param dimensions
   */
  public AbstractDimensionsSelectingDoubleDistanceFunction(BitSet dimensions) {
    super();
    this.dimensions = dimensions;
  }

  @Override
  public DoubleDistance distance(V o1, V o2) {
    return new DoubleDistance(doubleDistance(o1, o2));
  }

  @Override
  public BitSet getSelectedDimensions() {
    return dimensions;
  }

  @Override
  public void setSelectedDimensions(BitSet dimensions) {
    this.dimensions.clear();
    this.dimensions.or(dimensions);
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!this.getClass().equals(obj.getClass())) {
      return false;
    }
    return this.dimensions.equals(((AbstractDimensionsSelectingDoubleDistanceFunction<?>) obj).dimensions);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    protected BitSet dimensions = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      dimensions = new BitSet();
      final IntListParameter dimsP = new IntListParameter(DIMS_ID);
      dimsP.addConstraint(new ListGreaterEqualConstraint(0));
      dimsP.setOptional(true);
      if (config.grab(dimsP)) {
        for (int d : dimsP.getValue()) {
          dimensions.set(d);
        }
      }
    }
  }
}
