package de.lmu.ifi.dbs.elki.utilities;

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
import java.util.Map;

/**
 * ConstantObject provides a parent class for constant objects, that are
 * immutable and unique by class and name.
 * 
 * @author Arthur Zimek
 * @param <D> Class self reference for {@link Comparable} restriction
 */
public abstract class ConstantObject<D extends ConstantObject<D>> implements Comparable<D> {
  /**
   * Index of constant objects.
   */
  private static final Map<Class<?>, Map<String, ConstantObject<?>>> CONSTANT_OBJECTS_INDEX = new HashMap<Class<?>, Map<String, ConstantObject<?>>>();

  /**
   * Holds the value of the property's name.
   */
  private final String name;

  /**
   * The cached hash code of this object.
   */
  private final int hashCode;

  /**
   * Provides a ConstantObject of the given name.
   * 
   * @param name name of the ConstantObject
   */
  protected ConstantObject(final String name) {
    if(name == null) {
      throw new IllegalArgumentException("The name of a constant object must not be null.");
    }
    Map<String, ConstantObject<?>> index = CONSTANT_OBJECTS_INDEX.get(this.getClass());
    if(index == null) {
      index = new HashMap<String, ConstantObject<?>>();
      CONSTANT_OBJECTS_INDEX.put(this.getClass(), index);
    }
    if(index.containsKey(name)) {
      throw new IllegalArgumentException("A constant object of type \"" + this.getClass().getName() + "\" with value \"" + name + "\" is existing already.");
    }
    this.name = new String(name);
    index.put(name, this);
    this.hashCode = name.hashCode();
  }

  /**
   * Returns the name of the ConstantObject.
   * 
   * @return the name of the ConstantObject
   */
  public String getName() {
    return name;
  }

  /**
   * Provides a ConstantObject of specified class and name if it exists.
   * 
   * @param <D> Type for compile time type checking
   * @param type the type of the desired ConstantObject
   * @param name the name of the desired ConstantObject
   * @return the ConstantObject of designated type and name if it exists, null
   *         otherwise
   */
  @SuppressWarnings("unchecked")
  public static final <D extends ConstantObject<D>> D lookup(final Class<D> type, final String name) {
    Map<String, ConstantObject<?>> typeindex = CONSTANT_OBJECTS_INDEX.get(type);
    if (typeindex == null) {
      return null;
    }
    return (D) typeindex.get(name);
  }

  /**
   * Method for use by the serialization mechanism to ensure identity of
   * ConstantObjects.
   * 
   * @return the ConstantObject that already exists in the virtual machine
   *         rather than a new instance as created by the serialization
   *         mechanism
   */
  @SuppressWarnings("unchecked")
  protected Object readResolve(){
    Object result = lookup(getClass(), getName());
    if(result == null) {
      throw new NullPointerException("No constant object of type \"" + getClass().getName() + "\" found for name \"" + getName() + "\".");
    }
    return result;
  }

  /**
   * @see Object#equals(Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }

    final D that = (D) o;

    if(hashCode != that.hashCode()) {
      return false;
    }
    if (name == null) {
      return (that.getName() == null);
    }
    return name.equals(that.getName());
  }

  /**
   * @see Object#hashCode()
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * Two constant objects are generally compared by their name. The result
   * reflects the lexicographical order of the names by
   * {@link String#compareTo(String) this.getName().compareTo(o.getName()}.
   * @param o Object to compare to.
   * @return comparison result
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(D o) {
    return this.getName().compareTo(o.getName());
  }

}
