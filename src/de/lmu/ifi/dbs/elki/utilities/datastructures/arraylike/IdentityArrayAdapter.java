package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;

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
 * Single-item subset adapter
 * 
 * Use the static instance from {@link ArrayLikeUtil}!
 * 
 * @author Erich Schubert
 * 
 * @param <T> Item type
 */
public class IdentityArrayAdapter<T> implements ArrayAdapter<T, T> {
  /**
   * Constructor.
   * 
   * Use the static instance from {@link ArrayLikeUtil}!
   */
  protected IdentityArrayAdapter() {
    super();
  }

  @Override
  public int size(T array) {
    return 1;
  }

  @Override
  public T get(T array, int off) throws IndexOutOfBoundsException {
    assert (off == 0) : "Invalid get()";
    return array;
  }
}