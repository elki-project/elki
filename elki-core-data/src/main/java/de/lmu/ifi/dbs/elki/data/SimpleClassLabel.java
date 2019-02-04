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
package de.lmu.ifi.dbs.elki.data;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * A simple class label casting a String as it is as label.
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @composed - - - String
 */
public class SimpleClassLabel extends ClassLabel {
  /**
   * Serializer.
   */
  public static final ByteBufferSerializer<SimpleClassLabel> SERIALIZER = new Serializer();

  /**
   * Type information.
   */
  public static final SimpleTypeInformation<SimpleClassLabel> TYPE = new SimpleTypeInformation<>(SimpleClassLabel.class);

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

  @Override
  public int compareTo(ClassLabel o) {
    SimpleClassLabel other = (SimpleClassLabel) o;
    return this.label.compareTo(other.label);
  }

  @Override
  public int hashCode() {
    return label.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass() || !super.equals(o)) {
      return false;
    }
    final SimpleClassLabel that = (SimpleClassLabel) o;
    return label.equals(that.label);
  }

  @Override
  public String toString() {
    return label;
  }

  /**
   * Serialization class.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - SimpleClassLabel
   */
  public static class Serializer implements ByteBufferSerializer<SimpleClassLabel> {
    @Override
    public SimpleClassLabel fromByteBuffer(ByteBuffer buffer) throws IOException {
      return new SimpleClassLabel(ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer));
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, SimpleClassLabel object) throws IOException {
      ByteArrayUtil.STRING_SERIALIZER.toByteBuffer(buffer, object.label);
    }

    @Override
    public int getByteSize(SimpleClassLabel object) throws IOException {
      return ByteArrayUtil.STRING_SERIALIZER.getByteSize(object.label);
    }
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @has - creates - SimpleClassLabel
   * @stereotype factory
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

    @Override
    public SimpleTypeInformation<? super SimpleClassLabel> getTypeInformation() {
      return TYPE;
    }
  }
}
