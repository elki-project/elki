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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * A list of string labels.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf String
 */
public class LabelList extends ArrayList<String> {
  /**
   * Serializer.
   */
  public static final ByteBufferSerializer<LabelList> SERIALIZER = new Serializer();

  /**
   * Serial number.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public LabelList() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param c existing collection
   */
  public LabelList(Collection<? extends String> c) {
    super(c);
  }

  /**
   * Constructor.
   * 
   * @param initialCapacity initial size
   */
  public LabelList(int initialCapacity) {
    super(initialCapacity);
  }

  @Override
  public String toString() {
    return FormatUtil.format(this, " ");
  }

  /**
   * Serialization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has SimpleClassLabel - - «serializes»
   */
  private static class Serializer implements ByteBufferSerializer<LabelList> {
    @Override
    public LabelList fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int cnt = ByteArrayUtil.readUnsignedVarint(buffer);
      LabelList ret = new LabelList(cnt);
      for (int i = 0; i < cnt; i++) {
        ret.add(ByteArrayUtil.STRING_SERIALIZER.fromByteBuffer(buffer));
      }
      return ret;
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, LabelList object) throws IOException {
      final int cnt = object.size();
      ByteArrayUtil.writeUnsignedVarint(buffer, cnt);
      for (int i = 0; i < cnt; i++) {
        ByteArrayUtil.STRING_SERIALIZER.toByteBuffer(buffer, object.get(i));
      }
    }

    @Override
    public int getByteSize(LabelList object) throws IOException {
      final int cnt = object.size();
      int size = ByteArrayUtil.getUnsignedVarintSize(cnt);
      for (int i = 0; i < cnt; i++) {
        size += ByteArrayUtil.STRING_SERIALIZER.getByteSize(object.get(i));
      }
      return size;
    }

    @Override
    public void writeMetadata(ByteBuffer buffer) throws IOException, UnsupportedOperationException {
      ByteArrayUtil.STRING_SERIALIZER.toByteBuffer(buffer, getClass().getName());
    }
  }
}
