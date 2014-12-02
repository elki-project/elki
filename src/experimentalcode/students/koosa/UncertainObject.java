package experimentalcode.students.koosa;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

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

public class UncertainObject<U extends UOModel<SpatialComparable>> implements SpatialComparable {
  
  protected U sampleModel;
  protected int id;
  
  // Pretty weird Constructor
  public UncertainObject() {
    // One could want to use this and therefore
    // extend this class by setters or by making
    // it's fields public - honestly I don't really
    // like that idea...
  }
  
  // Constructor
  public UncertainObject(int id, final U sampleModel) {
    this.sampleModel = sampleModel;
    this.id = id;
  }
 
  public DoubleVector drawSample() {
    return sampleModel.drawSample();
  }
  
  public U getModel() {
    return this.sampleModel;
  }

  @Override
  public int getDimensionality() {
    return sampleModel.getDimensionality();
  }

  @Override
  public double getMin(int dimension) {
    return sampleModel.getMin(dimension);
  }

  @Override
  public double getMax(int dimension) {
    return sampleModel.getMax(dimension);
  }
  
  public SpatialComparable getBounds() {
    return sampleModel.getBounds();
  }
  
  public void setBounds(final SpatialComparable bounds) {
    this.sampleModel.setBounds(bounds);
  }
  
  public int getWeight() {
    return this.sampleModel.getWeight();
  }
}