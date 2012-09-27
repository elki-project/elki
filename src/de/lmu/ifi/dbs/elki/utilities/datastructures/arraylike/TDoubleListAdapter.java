package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;

import gnu.trove.list.TDoubleList;

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
/**
 * Adapter for using Trove TDoubleLists as array-like.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses TDoubleList
 */
public class TDoubleListAdapter implements NumberArrayAdapter<Double, TDoubleList> {
  /**
   * Constructor.
   */
  protected TDoubleListAdapter() {
    super();
  }

  @Override
  public int size(TDoubleList array) {
    return array.size();
  }

  @Override
  @Deprecated
  public Double get(TDoubleList array, int off) throws IndexOutOfBoundsException {
    return Double.valueOf(array.get(off));
  }

  @Override
  public double getDouble(TDoubleList array, int off) throws IndexOutOfBoundsException {
    return array.get(off);
  }

  @Override
  public float getFloat(TDoubleList array, int off) throws IndexOutOfBoundsException {
    return (float) array.get(off);
  }

  @Override
  public int getInteger(TDoubleList array, int off) throws IndexOutOfBoundsException {
    return (int) array.get(off);
  }

  @Override
  public short getShort(TDoubleList array, int off) throws IndexOutOfBoundsException {
    return (short) array.get(off);
  }

  @Override
  public long getLong(TDoubleList array, int off) throws IndexOutOfBoundsException {
    return (long) array.get(off);
  }

  @Override
  public byte getByte(TDoubleList array, int off) throws IndexOutOfBoundsException {
    return (byte) array.get(off);
  }
}