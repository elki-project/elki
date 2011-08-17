package de.lmu.ifi.dbs.elki.data.type;
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

/**
 * Class wrapping a particular data type.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Java type we represent.
 */
public class SimpleTypeInformation<T> implements TypeInformation {
  /**
   * The restriction class we represent.
   */
  private Class<? super T> cls;

  /**
   * Constructor.
   * 
   * @param cls restriction class
   */
  public SimpleTypeInformation(Class<? super T> cls) {
    super();
    this.cls = cls;
  }

  /**
   * Get the raw restriction class.
   * 
   * @return Restriction class
   */
  public final Class<? super T> getRestrictionClass() {
    return cls;
  }

  @Override
  public boolean isAssignableFromType(TypeInformation type) {
    if(!(type instanceof SimpleTypeInformation)) {
      return false;
    }
    final SimpleTypeInformation<?> simpleType = (SimpleTypeInformation<?>) type;
    return cls.isAssignableFrom(simpleType.getRestrictionClass());
  }

  @Override
  public boolean isAssignableFrom(Object other) {
    return cls.isInstance(other);
  }

  /**
   * Cast the object to type T (actually to the given restriction class!)
   * 
   * @param other Object to cast.
   * @return Instance
   */
  @SuppressWarnings("unchecked")
  public T cast(Object other) {
    return (T) cls.cast(other);
  }

  /**
   * Pseudo constructor that is often convenient to use when T is not completely
   * known.
   * 
   * @param <T> Type
   * @param cls Class restriction
   * @return Type
   */
  public static <T> SimpleTypeInformation<T> get(Class<T> cls) {
    return new SimpleTypeInformation<T>(cls);
  }

  @Override
  public String toString() {
    return getRestrictionClass().getSimpleName();
  }
}