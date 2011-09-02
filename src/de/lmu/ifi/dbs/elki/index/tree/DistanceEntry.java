package de.lmu.ifi.dbs.elki.index.tree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Helper class: encapsulates an entry in an Index and a distance value
 * belonging to this entry.
 * 
 * @author Elke Achtert
 * @param <E> the type of Entry used in the index
 * @param <D> the type of Distance used
 */
public class DistanceEntry<D extends Distance<D>, E extends Entry> implements Comparable<DistanceEntry<D, E>> {
  /**
   * The entry of the Index.
   */
  private E entry;

  /**
   * The distance value belonging to the entry.
   */
  private D distance;

  /**
   * The index of the entry in its parent's child array.
   */
  private int index;

  /**
   * Constructs a new DistanceEntry object with the specified parameters.
   * 
   * @param entry the entry of the Index
   * @param distance the distance value belonging to the entry
   * @param index the index of the entry in its parent' child array
   */
  public DistanceEntry(E entry, D distance, int index) {
    this.entry = entry;
    this.distance = distance;
    this.index = index;
  }

  /**
   * Returns the entry of the Index.
   * 
   * @return the entry of the Index
   */
  public E getEntry() {
    return entry;
  }

  /**
   * Returns the distance value belonging to the entry.
   * 
   * @return the distance value belonging to the entry
   */
  public D getDistance() {
    return distance;
  }

  /**
   * Returns the index of this entry in its parents child array.
   * 
   * @return the index of this entry in its parents child array
   */
  public int getIndex() {
    return index;
  }

  /**
   * Compares this object with the specified object for order.
   * 
   * @param o the Object to be compared.
   * @return a negative integer, zero, or a positive integer as this object is
   *         less than, equal to, or greater than the specified object.
   * @throws ClassCastException if the specified object's type prevents it from
   *         being compared to this Object.
   */
  @Override
  public int compareTo(DistanceEntry<D, E> o) {
    int comp = distance.compareTo(o.distance);
    if(comp != 0) {
      return comp;
    }

    //return entry.getEntryID().compareTo(o.entry.getEntryID());
    return 0;
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return "" + entry.toString() + "(" + distance + ")";
  }
}