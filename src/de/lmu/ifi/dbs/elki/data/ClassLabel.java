package de.lmu.ifi.dbs.elki.data;

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

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.utilities.InspectionUtilFrequentlyScanned;

/**
 * A ClassLabel to identify a certain class of objects that is to discern from
 * other classes by a classifier.
 * 
 * @author Arthur Zimek
 */
public abstract class ClassLabel implements Comparable<ClassLabel> {
  /**
   * ClassLabels need an empty constructor for dynamic access. Subsequently, the
   * init method must be called.
   */
  protected ClassLabel() {
    // Initialized from factory
  }

  /**
   * Any ClassLabel should ensure a natural ordering that is consistent with
   * equals. Thus, if <code>this.compareTo(o)==0</code>, then
   * <code>this.equals(o)</code> should be <code>true</code>.
   * 
   * @param obj an object to test for equality w.r.t. this ClassLabel
   * @return true, if <code>this==obj || this.compareTo(o)==0</code>, false
   *         otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof ClassLabel)) {
      return false;
    }
    return this == obj || this.compareTo((ClassLabel) obj) == 0;
  }

  /**
   * Any ClassLabel requires a method to represent the label as a String. If
   * <code>ClassLabel a.equals((ClassLabel) b)</code>, then also
   * <code>a.toString().equals(b.toString())</code> should hold.
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public abstract String toString();

  /**
   * Returns the hashCode of the String-representation of this ClassLabel.
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * Class label factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has ClassLabel - - «creates»
   * @apiviz.stereotype factory
   */
  public abstract static class Factory<L extends ClassLabel> implements InspectionUtilFrequentlyScanned {
    /**
     * Set for reusing the same objects.
     */
    protected HashMap<String, L> existing = new HashMap<String, L>();

    /**
     * Convert a string into a class label
     * 
     * @param lbl String to convert
     * @return Class label instance.
     */
    public abstract L makeFromString(String lbl);
  }
}