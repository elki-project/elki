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

import java.io.IOException;
import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ClassInstantiationException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Class to handle the serialization and deserialization of type information.
 *
 * The serialization format is custom, and not very extensible. However, the
 * standard Java "serializable" API did not seem well suited for this task, and
 * assumes an object stream context, while we intend to focus on Java NIO.
 *
 * TODO: on the long run, this code needs to be refactored, and a more
 * extensible format needs to be created. Maybe, a protobuf based API would be
 * possible.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @composed - - - SimpleTypeSerializer
 * @composed - - - VectorTypeSerializer
 * @composed - - - VectorFieldTypeSerializer
 */
public class TypeInformationSerializer implements ByteBufferSerializer<TypeInformation> {
  /**
   * Static instance.
   */
  public static final TypeInformationSerializer STATIC = new TypeInformationSerializer();

  /**
   * Tag for simple type.
   */
  private static final byte TAG_SIMPLE = 0;

  /**
   * Tag for non-field vector type.
   */
  private static final byte TAG_VECTOR = 1;

  /**
   * Tag for vector field type.
   */
  private static final byte TAG_VECTOR_FIELD = 2;

  @Override
  public TypeInformation fromByteBuffer(ByteBuffer buffer) throws IOException, UnsupportedOperationException {
    byte type = buffer.get();
    switch(type) {
    case TAG_SIMPLE:
      return SIMPLE_TYPE_SERIALIZER.fromByteBuffer(buffer);
    case TAG_VECTOR:
      return VECTOR_TYPE_SERIALIZER.fromByteBuffer(buffer);
    case TAG_VECTOR_FIELD:
      return VECTOR_FIELD_TYPE_SERIALIZER.fromByteBuffer(buffer);
    default:
      throw new UnsupportedOperationException("No deserialization known for type " + type);
    }
  }

  @Override
  public void toByteBuffer(ByteBuffer buffer, TypeInformation object) throws IOException, UnsupportedOperationException {
    final Class<?> clz = object.getClass();
    // We have three supported type informations.
    // We need to check with equals, because we canNOT allow subclasses!
    if (VectorFieldTypeInformation.class.equals(clz)) {
      buffer.put(TAG_VECTOR_FIELD);
      VECTOR_FIELD_TYPE_SERIALIZER.toByteBuffer(buffer, (VectorFieldTypeInformation<?>) object);
      return;
    }
    if (VectorTypeInformation.class.equals(clz)) {
      buffer.put(TAG_VECTOR);
      VECTOR_TYPE_SERIALIZER.toByteBuffer(buffer, (VectorTypeInformation<?>) object);
      return;
    }
    if (SimpleTypeInformation.class.equals(clz)) {
      buffer.put(TAG_SIMPLE);
      SIMPLE_TYPE_SERIALIZER.toByteBuffer(buffer, (SimpleTypeInformation<?>) object);
      return;
    }
    throw new UnsupportedOperationException("Unsupported type information.");
  }

  @Override
  public int getByteSize(TypeInformation object) throws IOException, UnsupportedOperationException {
    final Class<?> clz = object.getClass();
    // We have three supported type informations.
    // We need to check with equals, because we canNOT allow subclasses!
    if (VectorFieldTypeInformation.class.equals(clz)) {
      return 1 + VECTOR_FIELD_TYPE_SERIALIZER.getByteSize((VectorFieldTypeInformation<?>) object);
    }
    if (VectorTypeInformation.class.equals(clz)) {
      return 1 + VECTOR_TYPE_SERIALIZER.getByteSize((VectorTypeInformation<?>) object);
    }
    if (SimpleTypeInformation.class.equals(clz)) {
      return 1 + SIMPLE_TYPE_SERIALIZER.getByteSize((SimpleTypeInformation<?>) object);
    }
    throw new UnsupportedOperationException("Unsupported type information.");
  }

  /**
   * Serializer for simple types only.
   */
  static final ByteBufferSerializer<SimpleTypeInformation<?>> SIMPLE_TYPE_SERIALIZER = new SimpleTypeSerializer();

  /**
   * Serializer for non-field vectors.
   */
  static final ByteBufferSerializer<VectorTypeInformation<?>> VECTOR_TYPE_SERIALIZER = new VectorTypeSerializer();

  /**
   * Serializer for vector fields.
   */
  static final ByteBufferSerializer<VectorFieldTypeInformation<?>> VECTOR_FIELD_TYPE_SERIALIZER = new VectorFieldTypeSerializer();

  /**
   * Serialization class for pure simple types.
   *
   * @author Erich Schubert
   *
   * @assoc - - - SimpleTypeInformation
   */
  static class SimpleTypeSerializer implements ByteBufferSerializer<SimpleTypeInformation<?>> {
    @SuppressWarnings("unchecked")
    @Override
    public SimpleTypeInformation<?> fromByteBuffer(ByteBuffer buffer) throws IOException, UnsupportedOperationException {
      try {
        String typename = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
        Class<Object> clz = (Class<Object>) Class.forName(typename);
        String label = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
        label = ("".equals(label)) ? null : label;
        String sername = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
        ByteBufferSerializer<Object> serializer = (ByteBufferSerializer<Object>) Class.forName(sername).newInstance();
        return new SimpleTypeInformation<>(clz, label, serializer);
      } catch (ClassNotFoundException e) {
        throw new UnsupportedOperationException("Cannot deserialize - class not found: " + e, e);
      } catch (InstantiationException|IllegalAccessException e) {
        throw new UnsupportedOperationException("Cannot deserialize - cannot instantiate serializer: " + e.getMessage(), e);
      }
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, SimpleTypeInformation<?> object) throws IOException, UnsupportedOperationException {
      // First of all, the type needs to have an actual serializer in the type
      final ByteBufferSerializer<?> serializer = object.getSerializer();
      if (serializer == null) {
        throw new UnsupportedOperationException("No serializer for type " + toString() + " available.");
      }
      // Make sure there is a constructor for the serialization class:
      try {
        serializer.getClass().getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        throw new UnsupportedOperationException("No automatic serialization possible - no default constructor for serializer.");
      } catch (SecurityException e) {
        throw new UnsupportedOperationException("Serialization not possible.", e);
      }
      // Type class
      ByteArrayUtil.writeString(buffer, object.getRestrictionClass().getName());
      // Name, or an empty string.
      ByteArrayUtil.writeString(buffer, object.getLabel());
      // Serializer class
      ByteArrayUtil.writeString(buffer, serializer.getClass().getName());
    }

    @Override
    public int getByteSize(SimpleTypeInformation<?> object) throws IOException, UnsupportedOperationException {
      // First of all, the type needs to have an actual serializer in the type
      final ByteBufferSerializer<?> serializer = object.getSerializer();
      if (serializer == null) {
        throw new UnsupportedOperationException("No serializer for type " + toString() + " available.");
      }
      // Make sure there is a constructor for the serialization class:
      try {
        serializer.getClass().getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        throw new UnsupportedOperationException("No automatic serialization possible - no default constructor for serializer.");
      } catch (SecurityException e) {
        throw new UnsupportedOperationException("Serialization not possible.", e);
      }
      int total = 0;
      // Type class
      total += ByteArrayUtil.STRING_SERIALIZER.getByteSize(object.getRestrictionClass().getName());
      // Name, or an empty string.
      total += ByteArrayUtil.STRING_SERIALIZER.getByteSize(object.getLabel());
      // Serializer class
      total += ByteArrayUtil.STRING_SERIALIZER.getByteSize(serializer.getClass().getName());
      return total;
    }
  }

  /**
   * Serialization class for non-field vector types.
   *
   * FIXME: "label" is actually not supported.
   *
   * @author Erich Schubert
   *
   * @assoc - - - VectorTypeInformation
   */
  static class VectorTypeSerializer implements ByteBufferSerializer<VectorTypeInformation<?>> {
    @SuppressWarnings("unchecked")
    @Override
    public VectorTypeInformation<?> fromByteBuffer(ByteBuffer buffer) throws IOException, UnsupportedOperationException {
      try {
        // Factory type!
        String typename = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
        NumberVector.Factory<NumberVector> factory = (NumberVector.Factory<NumberVector>) ClassGenericsUtil.instantiate(NumberVector.Factory.class, typename);
        String label = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
        label = ("".equals(label)) ? null : label;
        String sername = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
        ByteBufferSerializer<NumberVector> serializer = (ByteBufferSerializer<NumberVector>) Class.forName(sername).newInstance();
        int mindim = ByteArrayUtil.readSignedVarint(buffer);
        int maxdim = ByteArrayUtil.readSignedVarint(buffer);
        return new VectorTypeInformation<NumberVector>(factory, serializer, mindim, maxdim);
      } catch (ClassInstantiationException e) {
        throw new UnsupportedOperationException("Cannot deserialize - cannot instantiate factory: "+e, e);
      } catch (ClassNotFoundException e) {
        throw new UnsupportedOperationException("Cannot deserialize - class not found: "+e, e);
      } catch (InstantiationException | IllegalAccessException e) {
        throw new UnsupportedOperationException("Cannot deserialize - cannot instantiate serializer: "+e.getMessage(), e);
      }
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, VectorTypeInformation<?> object) throws IOException, UnsupportedOperationException {
      // First of all, the type needs to have an actual serializer in the type
      final ByteBufferSerializer<?> serializer = object.getSerializer();
      if (serializer == null) {
        throw new UnsupportedOperationException("No serializer for type " + toString() + " available.");
      }
      // Make sure there is a constructor for the serialization class:
      try {
        serializer.getClass().getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        throw new UnsupportedOperationException("No automatic serialization possible - no default constructor for serializer.");
      } catch (SecurityException e) {
        throw new UnsupportedOperationException("Serialization not possible.", e);
      }
      // Use *factory* class!
      ByteArrayUtil.writeString(buffer, object.getFactory().getClass().getName());
      // Name, or an empty string.
      ByteArrayUtil.writeString(buffer, object.getLabel());
      // Serializer class
      ByteArrayUtil.writeString(buffer, serializer.getClass().getName());
      ByteArrayUtil.writeSignedVarint(buffer, object.mindim());
      ByteArrayUtil.writeSignedVarint(buffer, object.maxdim());
    }

    @Override
    public int getByteSize(VectorTypeInformation<?> object) throws IOException, UnsupportedOperationException {
      // First of all, the type needs to have an actual serializer in the type
      final ByteBufferSerializer<?> serializer = object.getSerializer();
      if (serializer == null) {
        throw new UnsupportedOperationException("No serializer for type " + toString() + " available.");
      }
      // Make sure there is a constructor for the serialization class:
      try {
        serializer.getClass().getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        throw new UnsupportedOperationException("No automatic serialization possible - no default constructor for serializer.");
      } catch (SecurityException e) {
        throw new UnsupportedOperationException("Serialization not possible.", e);
      }
      int total = 0;
      // Type class
      total += ByteArrayUtil.STRING_SERIALIZER.getByteSize(object.getRestrictionClass().getName());
      // Name, or an empty string.
      total += ByteArrayUtil.STRING_SERIALIZER.getByteSize(object.getLabel());
      // Serializer class
      total += ByteArrayUtil.STRING_SERIALIZER.getByteSize(serializer.getClass().getName());
      // Dimensionality
      total += ByteArrayUtil.getSignedVarintSize(object.mindim());
      total += ByteArrayUtil.getSignedVarintSize(object.maxdim());
      return total;
    }
  }

  /**
   * Serialization class for field vector types.
   *
   * FIXME: "relation label" is actually not properly supported.
   *
   * @author Erich Schubert
   *
   * @assoc - - - VectorFieldTypeInformation
   */
  static class VectorFieldTypeSerializer implements ByteBufferSerializer<VectorFieldTypeInformation<?>> {
    @SuppressWarnings("unchecked")
    @Override
    public VectorFieldTypeInformation<?> fromByteBuffer(ByteBuffer buffer) throws IOException, UnsupportedOperationException {
      try {
        // Factory type!
        String typename = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
        NumberVector.Factory<NumberVector> factory = (NumberVector.Factory<NumberVector>) ClassGenericsUtil.instantiate(NumberVector.Factory.class, typename);
        // Relation label
        String label = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
        label = ("".equals(label)) ? null : label;
        // Serialization class
        String sername = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
        ByteBufferSerializer<NumberVector> serializer = (ByteBufferSerializer<NumberVector>) Class.forName(sername).newInstance();
        // Dimensionalities
        int mindim = ByteArrayUtil.readSignedVarint(buffer);
        int maxdim = ByteArrayUtil.readSignedVarint(buffer);
        // Column names
        int cols = ByteArrayUtil.readUnsignedVarint(buffer);
        if (cols > 0) {
          assert(mindim == maxdim && maxdim == cols) : "Inconsistent dimensionality and column names!";
          String[] labels = new String[cols];
          for (int i = 0; i < cols; i++) {
            labels[i] = ByteArrayUtil.readString(buffer);
          }
          return new VectorFieldTypeInformation<>(factory, mindim, labels, serializer);
        } else {
          return new VectorFieldTypeInformation<>(factory, mindim, maxdim, serializer);
        }
      } catch (ClassInstantiationException e) {
        throw new UnsupportedOperationException("Cannot deserialize - cannot instantiate factory: "+e, e);
      } catch (ClassNotFoundException e) {
        throw new UnsupportedOperationException("Cannot deserialize - class not found: "+e, e);
      } catch (InstantiationException | IllegalAccessException e) {
        throw new UnsupportedOperationException("Cannot deserialize - cannot instantiate serializer: "+e.getMessage(), e);
      }
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, VectorFieldTypeInformation<?> object) throws IOException, UnsupportedOperationException {
      // First of all, the type needs to have an actual serializer in the type
      final ByteBufferSerializer<?> serializer = object.getSerializer();
      if (serializer == null) {
        throw new UnsupportedOperationException("No serializer for type " + toString() + " available.");
      }
      // Make sure there is a constructor for the serialization class:
      try {
        serializer.getClass().getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        throw new UnsupportedOperationException("No automatic serialization possible - no default constructor for serializer.");
      } catch (SecurityException e) {
        throw new UnsupportedOperationException("Serialization not possible.", e);
      }
      // Use *factory* class!
      ByteArrayUtil.writeString(buffer, object.getFactory().getClass().getName());
      // Name, or an empty string.
      ByteArrayUtil.writeString(buffer, object.getLabel());
      // Serializer class
      ByteArrayUtil.writeString(buffer, serializer.getClass().getName());
      // Dimensionality
      ByteArrayUtil.writeSignedVarint(buffer, object.mindim());
      ByteArrayUtil.writeSignedVarint(buffer, object.maxdim());
      // Column names
      String[] labels = object.getLabels();
      if (labels == null) {
        ByteArrayUtil.writeUnsignedVarint(buffer, 0);
      } else {
        ByteArrayUtil.writeUnsignedVarint(buffer, labels.length);
        for (String s : labels) {
          ByteArrayUtil.writeString(buffer, s);
        }
      }
    }

    @Override
    public int getByteSize(VectorFieldTypeInformation<?> object) throws IOException, UnsupportedOperationException {
      // First of all, the type needs to have an actual serializer in the type
      final ByteBufferSerializer<?> serializer = object.getSerializer();
      if (serializer == null) {
        throw new UnsupportedOperationException("No serializer for type " + toString() + " available.");
      }
      // Make sure there is a constructor for the serialization class:
      try {
        serializer.getClass().getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        throw new UnsupportedOperationException("No automatic serialization possible - no default constructor for serializer.");
      } catch (SecurityException e) {
        throw new UnsupportedOperationException("Serialization not possible.", e);
      }
      int total = 0;
      // Type class
      total += ByteArrayUtil.STRING_SERIALIZER.getByteSize(object.getRestrictionClass().getName());
      // Name, or an empty string.
      total += ByteArrayUtil.STRING_SERIALIZER.getByteSize(object.getLabel());
      // Serializer class
      total += ByteArrayUtil.STRING_SERIALIZER.getByteSize(serializer.getClass().getName());
      // Dimensionality
      total += ByteArrayUtil.getSignedVarintSize(object.mindim());
      total += ByteArrayUtil.getSignedVarintSize(object.maxdim());
      // Column names
      String[] labels = object.getLabels();
      if (labels == null) {
        total += ByteArrayUtil.getUnsignedVarintSize(0);
      } else {
        total += ByteArrayUtil.getUnsignedVarintSize(labels.length);
        for (String s : labels) {
          total += ByteArrayUtil.getStringSize(s);
        }
      }
      return total;
    }
  }
}
