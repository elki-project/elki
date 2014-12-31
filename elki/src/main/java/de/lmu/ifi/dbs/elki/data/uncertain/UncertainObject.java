package de.lmu.ifi.dbs.elki.data.uncertain;

import de.lmu.ifi.dbs.elki.data.AbstractNumberVector;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

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


/**
 * This class is a generic wrapper, that encapsulates
 * sampleModels derived from {@link UOModel}.
 *
 * @author Alexander Koos
 *
 * @param <U>
 */
public class UncertainObject<U extends UOModel<SpatialComparable>> extends AbstractNumberVector implements SpatialComparable, TextWriteable {

  protected U sampleModel;
  protected int id;
  private final double[] values;

  // Pretty weird Constructor
  public UncertainObject() {
    // One could want to use this and therefore
    // extend this class by setters or by making
    // it's fields public - honestly I don't really
    // like that idea...
    this.values = null;
  }

  // Constructor
  public UncertainObject(final int id, final U sampleModel) {
    this.sampleModel = sampleModel;
    this.id = id;
    this.values = sampleModel.getAnker().getValues();
  }

  public UncertainObject(final int id, final U sampleModel, final DoubleVector values) {
    this.sampleModel = sampleModel;
    this.id = id;
    this.values = values.getValues();
  }

  public UncertainObject(final U sampleModel, final DoubleVector values) {
    this.sampleModel = sampleModel;
    this.values = values.getValues();
  }

  public DoubleVector drawSample() {
    return this.sampleModel.drawSample();
  }

  public U getModel() {
    return this.sampleModel;
  }

  public double[] getValues() {
    return this.values;
  }

  public double[] getObservation() {
    return this.sampleModel.getAnker().getValues();
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

  public SpatialComparable getBounds() {
    return this.sampleModel.getBounds();
  }

  public void setBounds(final SpatialComparable bounds) {
    this.sampleModel.setBounds(bounds);
  }

  public int getWeight() {
    return this.sampleModel.getWeight();
  }

  @Override
  public void writeToText(final TextWriterStream out, final String label) {
    this.sampleModel.writeToText(out, label);

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