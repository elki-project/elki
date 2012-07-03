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

/**
 * A simple class label casting a String as it is as label.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.composedOf String
 */
public class SimpleClassLabel extends ClassLabel {
  /**
   * Holds the String designating the label.
   */
  private String label;

  /**
   * Constructor.
   * 
   * @param label Label
   */
  public SimpleClassLabel(String label) {
    super();
    this.label = label;
  }

  /**
   * The ordering of two SimpleClassLabels is given by the ordering on the
   * Strings they represent.
   * <p/>
   * That is, the result equals <code>this.label.compareTo(o.label)</code>.
   */
  @Override
  public int compareTo(ClassLabel o) {
    SimpleClassLabel other = (SimpleClassLabel) o;
    return this.label.compareTo(other.label);
  }

  /**
   * The hash code of a simple class label is the hash code of the String
   * represented by the ClassLabel.
   */
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  /**
   * Any ClassLabel should ensure a natural ordering that is consistent with
   * equals. Thus, if <code>this.compareTo(o)==0</code>, then
   * <code>this.equals(o)</code> should be <code>true</code>.
   * 
   * @param o an object to test for equality w.r.t. this ClassLabel
   * @return true, if <code>this==obj || this.compareTo(o)==0</code>, false
   *         otherwise
   */
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }
    if(!super.equals(o)) {
      return false;
    }
    final SimpleClassLabel that = (SimpleClassLabel) o;

    return label.equals(that.label);
  }

  /**
   * Returns a new instance of the String covered by this SimpleClassLabel.
   * 
   * @return a new instance of the String covered by this SimpleClassLabel
   */
  @Override
  public String toString() {
    return label;
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has SimpleClassLabel - - «creates»
   * @apiviz.stereotype factory
   */
  public static class Factory extends ClassLabel.Factory<SimpleClassLabel> {
    @Override
    public SimpleClassLabel makeFromString(String lbl) {
      lbl = lbl.intern();
      SimpleClassLabel l = existing.get(lbl);
      if(l == null) {
        l = new SimpleClassLabel(lbl);
        existing.put(lbl, l);
      }
      return l;
    }
  }
}