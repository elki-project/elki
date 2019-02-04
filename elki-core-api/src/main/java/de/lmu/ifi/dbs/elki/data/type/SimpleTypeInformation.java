/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.data.type;

import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Class wrapping a particular data type.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - ByteBufferSerializer
 * 
 * @param <T> Java type we represent.
 */
public class SimpleTypeInformation<T> implements TypeInformation {
  /**
   * The restriction class we represent.
   */
  private Class<? super T> cls;

  /**
   * Type label.
   */
  private String label;

  /**
   * Type serializer.
   */
  private ByteBufferSerializer<? super T> serializer;

  /**
   * Constructor.
   * 
   * @param cls restriction class
   */
  public SimpleTypeInformation(Class<? super T> cls) {
    super();
    this.cls = cls;
    this.label = null;
    this.serializer = null;
  }

  /**
   * Constructor.
   * 
   * @param cls restriction class
   * @param label type label
   */
  public SimpleTypeInformation(Class<? super T> cls, String label) {
    super();
    this.cls = cls;
    this.label = label;
    this.serializer = null;
  }

  /**
   * Constructor.
   * 
   * @param cls restriction class
   * @param serializer Serializer
   */
  public SimpleTypeInformation(Class<? super T> cls, ByteBufferSerializer<? super T> serializer) {
    super();
    this.cls = cls;
    this.label = null;
    this.serializer = serializer;
  }

  /**
   * Constructor.
   * 
   * @param cls restriction class
   * @param label type label
   * @param serializer Serializer
   */
  public SimpleTypeInformation(Class<? super T> cls, String label, ByteBufferSerializer<? super T> serializer) {
    super();
    this.cls = cls;
    this.label = label;
    this.serializer = serializer;
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
   * Cast the object to type T (actually to the given restriction class!).
   * 
   * @param other Object to cast.
   * @return Instance
   */
  @SuppressWarnings("unchecked")
  public T cast(Object other) {
    return (T) cls.cast(other);
  }

  @Override
  public String toString() {
    return getRestrictionClass().getSimpleName();
  }

  /**
   * Get the type label.
   * 
   * @return Label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Get the serializer for this type.
   * 
   * @return Type serializer
   */
  public ByteBufferSerializer<? super T> getSerializer() {
    return serializer;
  }

  /**
   * Set the serializer for this type.
   * 
   * @param serializer Serializer to use
   */
  public void setSerializer(ByteBufferSerializer<? super T> serializer) {
    this.serializer = serializer;
  }
}
