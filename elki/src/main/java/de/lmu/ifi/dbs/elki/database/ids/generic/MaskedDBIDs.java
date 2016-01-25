package de.lmu.ifi.dbs.elki.database.ids.generic;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;

/**
 * View on an ArrayDBIDs masked using a BitMask for efficient mask changing.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.uses DBIDs
 */
public class MaskedDBIDs implements DBIDs {
  /**
   * Data storage.
   */
  protected ArrayDBIDs data;

  /**
   * The bitmask used for masking.
   */
  protected long[] bits;

  /**
   * Flag whether to iterator over set or unset values.
   */
  protected boolean inverse = false;

  /**
   * Constructor.
   * 
   * @param data Data
   * @param bits Bitset to use as mask
   * @param inverse Flag to inverse the masking rule
   */
  public MaskedDBIDs(ArrayDBIDs data, long[] bits, boolean inverse) {
    super();
    this.data = data;
    this.bits = bits;
    this.inverse = inverse;
  }

  @Override
  public DBIDIter iter() {
    return inverse ? new InvDBIDItr() : new DBIDItr();
  }

  @Override
  public int size() {
    if(inverse) {
      return data.size() - BitsUtil.cardinality(bits);
    }
    return BitsUtil.cardinality(bits);
  }

  @Override
  public boolean contains(DBIDRef o) {
    // TODO: optimize.
    for(DBIDIter iter = iter(); iter.valid(); iter.advance()) {
      if(DBIDFactory.FACTORY.equal(iter, o)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isEmpty() {
    return BitsUtil.isZero(bits);
  }

  /**
   * Iterator over set bits.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class DBIDItr implements DBIDIter {
    /**
     * Current position.
     */
    private int pos;

    /**
     * Array iterator, for referencing.
     */
    private DBIDArrayIter iter;

    /**
     * Constructor.
     */
    protected DBIDItr() {
      this.pos = BitsUtil.nextSetBit(bits, 0);
      this.iter = data.iter();
    }

    @Override
    public boolean valid() {
      return pos >= 0;
    }

    @Override
    public DBIDIter advance() {
      pos = BitsUtil.nextSetBit(bits, pos + 1);
      return this;
    }

    @Override
    public int internalGetIndex() {
      iter.seek(pos);
      return iter.internalGetIndex();
    }
  }

  /**
   * Iterator over set bits.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class InvDBIDItr implements DBIDIter {
    /**
     * Next position.
     */
    private int pos;

    /**
     * Array iterator, for referencing.
     */
    private DBIDArrayIter iter;

    /**
     * Constructor.
     */
    protected InvDBIDItr() {
      this.pos = BitsUtil.nextClearBit(bits, 0);
      this.iter = data.iter();
    }

    @Override
    public boolean valid() {
      return (pos >= 0) && (pos < data.size());
    }

    @Override
    public DBIDIter advance() {
      pos = BitsUtil.nextClearBit(bits, pos + 1);
      return this;
    }

    @Override
    public int internalGetIndex() {
      iter.seek(pos);
      return iter.internalGetIndex();
    }
  }
}