package de.lmu.ifi.dbs.elki.index.tree.spatial.ph;

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

import ch.ethz.globis.pht.pre.PreProcessorPointF;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

final class PhNumberVectorAdapter implements NumberVector {
  
  private final PreProcessorPointF pre;
  private final double[] min;
  //private double[] max;
  
  public PhNumberVectorAdapter(int dimension, PreProcessorPointF pre) {
    this.pre = pre;
    min = new double[dimension];
  }
  
  @Override
  public double getMin(int dimension) {
    return min[dimension];
  }

  @Override
  public double getMax(int dimension) {
    //return max[dimension];
    throw new UnsupportedOperationException();
  }

  @Override
  public int getDimensionality() {
    return min.length;
  }

  @Override
  public short shortValue(int dimension) {
    return (short) getMin(dimension);
  }

  @Override
  public long longValue(int dimension) {
    return (long) getMin(dimension);
  }

  @Override
  public int intValue(int dimension) {
    return (int) getMin(dimension);
  }

  @Override
  public Number getValue(int dimension) {
    return getMin(dimension);
  }

  @Override
  public Vector getColumnVector() {
    return new Vector(min);
  }

  @Override
  public float floatValue(int dimension) {
    return (float) getMin(dimension);
  }

  @Override
  public double doubleValue(int dimension) {
    return getMin(dimension);
  }

  @Override
  public byte byteValue(int dimension) {
    return (byte) getMin(dimension);
  }

  public NumberVector wrap(long[] v) {
    pre.post(v, min);
    return this;
  }
}