package experimentalcode.students.koosa;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
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

public class ContinuousUncertainObject<F extends ProbabilityFunction> extends AbstractContinuousUncertainObject {

  private F probabilityFunction;
  
  //Constructor
  public ContinuousUncertainObject() {
    // Constructs the plain object to be filled
    // from custom code.
  }
  
  //Constructor - meaningful
  public ContinuousUncertainObject(final HyperBoundingBox box, final F probabilityFunction) {
    this.mbr = box;
    this.dimensions = box.getDimensionality();
    this.probabilityFunction = probabilityFunction;
  }
  
  //Constructor - worth considering?
  public ContinuousUncertainObject(final double[] min, final double[] max, final F probabilityFunction) {
    this.mbr = new HyperBoundingBox(min, max);
    this.dimensions = min.length;
    this.probabilityFunction = probabilityFunction;
  }
  
  @Override
  public DoubleVector drawSample() {
    return probabilityFunction.drawValue(mbr, rand);
  }

  @Override
  public HyperBoundingBox getMBR() {
    return this.mbr;
  }

  @Override
  public void setMBR(HyperBoundingBox box) {
    this.mbr = box;
  }

  @Override
  public int getDimensionality() {
    return this.dimensions;
  }

  @Override
  public double getMin(int dimension) {
    return this.mbr.getMin(dimension);
  }

  @Override
  public double getMax(int dimension) {
    return this.mbr.getMax(dimension);
  }

}
