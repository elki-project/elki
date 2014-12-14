package experimentalcode.students.koosa;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

public class ContinuousUncertainObject<F extends ProbabilityDensityFunction> extends AbstractContinuousUncertainObject {

  private F probabilityDensityFunction;
  
  //Constructor
  public ContinuousUncertainObject() {
    // Constructs the plain object to be filled
    // from custom code.
  }
  
  // Constructor
  public ContinuousUncertainObject(final HyperBoundingBox box, final F probabilityDensityFunction) {
    this(box, probabilityDensityFunction, new RandomFactory(null));
  }
  
  //Constructor - meaningful
  public ContinuousUncertainObject(final SpatialComparable bounds, final F probabilityDensityFunction, final RandomFactory randomFactory) {
    this.bounds = bounds;
    this.dimensions = bounds.getDimensionality();
    this.probabilityDensityFunction = probabilityDensityFunction;
    this.rand = randomFactory.getRandom();
  }
  
  // Constructor
  public ContinuousUncertainObject(final double[] min, final double[] max, final F probabilityDensityFunction) {
    this(min, max, probabilityDensityFunction, new RandomFactory(null));
  }
  
  //Constructor - worth considering?
  public ContinuousUncertainObject(final double[] min, final double[] max, final F probabilityDensityFunction, final RandomFactory randomFactory) {
    this.bounds = new HyperBoundingBox(min, max);
    this.dimensions = min.length;
    this.probabilityDensityFunction = probabilityDensityFunction;
    this.rand = randomFactory.getRandom();
  }
  
  @Override
  public DoubleVector drawSample() {
    return probabilityDensityFunction.drawValue(bounds, rand);
  }

  @Override
  public SpatialComparable getBounds() {
    return this.bounds;
  }

  @Override
  public void setBounds(final SpatialComparable bounds) {
    this.bounds = bounds;
  }

  @Override
  public int getDimensionality() {
    return this.dimensions;
  }

  @Override
  public double getMin(int dimension) {
    return this.bounds.getMin(dimension);
  }

  @Override
  public double getMax(int dimension) {
    return this.bounds.getMax(dimension);
  }

}
