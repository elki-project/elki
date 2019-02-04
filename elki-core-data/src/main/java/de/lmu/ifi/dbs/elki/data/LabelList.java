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
import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * A list of string labels.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - String
 */
public class LabelList {
  /**
   * Serializer.
   */
  public static final ByteBufferSerializer<LabelList> SERIALIZER = new Serializer();

  /**
   * Labels.
   */
  private String[] labels;

  /**
   * Empty label list.
   */
  public static final LabelList EMPTY_LABELS = new LabelList(0);

  /**
   * Constructor.
   * 
   * @param initialCapacity initial size
   */
  private LabelList(int initialCapacity) {
    super();
    labels = new String[initialCapacity];
  }

  /**
   * Private constructor. Use {@link #make}.
   * 
   * @param array Label list
   */
  protected LabelList(String[] array) {
    super();
    this.labels = array;
  }

  /**
   * Constructor replacement.
   * 
   * When the label list is empty, it will produce the same instance!
   * 
   * @param labels Existing labels
   * @return Label list instance.
   */
  public static LabelList make(Collection<String> labels) {
    int size = labels.size();
    if(size == 0) {
      return EMPTY_LABELS;
    }
    return new LabelList(labels.toArray(new String[size]));
  }

  /**
   * Size of label list.
   * 
   * @return Size
   */
  public int size() {
    return labels.length;
  }

  /**
   * Get the label at position i.
   * 
   * @param i Position
   * @return Label
   */
  public String get(int i) {
    return labels[i];
  }

  @Override
  public String toString() {
    return FormatUtil.format(labels, " ");
  }

  /**
   * Serialization class.
   *
   * @author Erich Schubert
   *
   * @assoc - serializes - SimpleClassLabel
   */
  public static class Serializer implements ByteBufferSerializer<LabelList> {
    @Override
    public LabelList fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int cnt = ByteArrayUtil.readUnsignedVarint(buffer);
      LabelList ret = new LabelList(cnt);
      for(int i = 0; i < cnt; i++) {
        ret.labels[i] = ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer);
      }
      return ret;
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, LabelList object) throws IOException {
      final int cnt = object.labels.length;
      ByteArrayUtil.writeUnsignedVarint(buffer, cnt);
      for(int i = 0; i < cnt; i++) {
        ByteArrayUtil.STRING_SERIALIZER.toByteBuffer(buffer, object.labels[i]);
      }
    }

    @Override
    public int getByteSize(LabelList object) throws IOException {
      final int cnt = object.labels.length;
      int size = ByteArrayUtil.getUnsignedVarintSize(cnt);
      for(int i = 0; i < cnt; i++) {
        size += ByteArrayUtil.STRING_SERIALIZER.getByteSize(object.labels[i]);
      }
      return size;
    }
  }
}
