package de.lmu.ifi.dbs.elki.data.uncertain;

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

import de.lmu.ifi.dbs.elki.data.AbstractNumberVector;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * This class is a generic wrapper, that encapsulates sampleModels derived from
 * {@link UOModel}.
 *
 * @author Alexander Koos
 *
 * @param <U> Model type
 */
public class UncertainObject<U extends UOModel> extends AbstractNumberVector {
  private final U sampleModel;

  private final double[] values;

  public UncertainObject(final U sampleModel, final NumberVector values) {
    this.sampleModel = sampleModel;
    this.values = values.getColumnVector().getArrayRef();
  }

  public DoubleVector drawSample() {
    return this.sampleModel.drawSample();
  }
  
  public DoubleVector getMean() {
    return this.sampleModel.getMean();
  }

  public DoubleVector drawCenter() {
    return new DoubleVector(this.values);
  }

  public U getModel() {
    return this.sampleModel;
  }

  @Override
  public int getDimensionality() {
    return this.sampleModel.getDimensionality();
  }

  @Override
  public double getMin(final int dimension) {
    return this.sampleModel.getMin(dimension);
  }

  @Override
  public double getMax(final int dimension) {
    return this.sampleModel.getMax(dimension);
  }

  @Deprecated
  @Override
  public Number getValue(final int dimension) {
    return this.values[dimension];
  }

  @Override
  public double doubleValue(final int dimension) {
    return this.values[dimension];
  }

  @Override
  public long longValue(final int dimension) {
    return (long) this.values[dimension];
  }

  @Override
  public Vector getColumnVector() {
    return new Vector(this.values.clone());
  }
}