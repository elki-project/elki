package de.lmu.ifi.dbs.elki.database.ids.generic;

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

import java.util.BitSet;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * View on an ArrayDBIDs masked using a BitMask for efficient mask changing.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses DBIDs
 */
public class MaskedDBIDs implements DBIDs {
  /**
   * Data storage
   */
  protected ArrayDBIDs data;

  /**
   * The bitmask used for masking
   */
  protected BitSet bits;

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
  public MaskedDBIDs(ArrayDBIDs data, BitSet bits, boolean inverse) {
    super();
    this.data = data;
    this.bits = bits;
    this.inverse = inverse;
  }

  @Override
  public Iterator<DBID> iterator() {
    if(inverse) {
      return new InvItr();
    }
    else {
      return new Itr();
    }
  }

  @Override
  public DBIDIter iter() {
    if(inverse) {
      return new InvDBIDItr();
    }
    else {
      return new DBIDItr();
    }
  }

  @Override
  public int size() {
    if(inverse) {
      return data.size() - bits.cardinality();
    }
    else {
      return bits.cardinality();
    }
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
    return size() == 0;
  }

  /**
   * Iterator over set bits
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class Itr implements Iterator<DBID> {
    /**
     * Next position.
     */
    private int pos;

    /**
     * Constructor
     */
    protected Itr() {
      this.pos = bits.nextSetBit(0);
    }

    @Override
    public boolean hasNext() {
      return (pos >= 0) && (pos < data.size());
    }

    @Override
    public DBID next() {
      DBID cur = data.get(pos);
      pos = bits.nextSetBit(pos + 1);
      return cur;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Iterator over set bits
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class DBIDItr implements DBIDIter {
    /**
     * Next position.
     */
    private int pos;

    /**
     * Constructor
     */
    protected DBIDItr() {
      this.pos = bits.nextSetBit(0);
    }

    @Override
    public boolean valid() {
      return pos >= 0;
    }

    @Override
    public void advance() {
      pos = bits.nextSetBit(pos + 1);
    }

    @Override
    public int getIntegerID() {
      return DBIDFactory.FACTORY.asInteger(data.get(pos));
    }

    @Override
    public DBID getDBID() {
      return data.get(pos);
    }
  }

  /**
   * Iterator over unset elements.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class InvItr implements Iterator<DBID> {
    /**
     * Next unset position.
     */
    private int pos;

    /**
     * Constructor
     */
    protected InvItr() {
      this.pos = bits.nextClearBit(0);
    }

    @Override
    public boolean hasNext() {
      return (pos >= 0) && (pos < data.size());
    }

    @Override
    public DBID next() {
      DBID cur = data.get(pos);
      pos = bits.nextClearBit(pos + 1);
      return cur;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Iterator over set bits
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
     * Constructor
     */
    protected InvDBIDItr() {
      this.pos = bits.nextClearBit(0);
    }

    @Override
    public boolean valid() {
      return (pos >= 0) && (pos < data.size());
    }

    @Override
    public void advance() {
      pos = bits.nextClearBit(pos + 1);
    }

    @Override
    public int getIntegerID() {
      return DBIDFactory.FACTORY.asInteger(data.get(pos));
    }

    @Override
    public DBID getDBID() {
      return data.get(pos);
    }
  }
}