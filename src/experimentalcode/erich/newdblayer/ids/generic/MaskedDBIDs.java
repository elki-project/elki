package experimentalcode.erich.newdblayer.ids.generic;

import java.util.AbstractCollection;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;

import experimentalcode.erich.newdblayer.ids.ArrayDBIDs;
import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.ids.DBIDs;

/**
 * View on an ArrayDBIDs masked using a BitMask for efficient mask changing.
 * 
 * @author Erich Schubert
 */
public class MaskedDBIDs extends AbstractCollection<DBID> implements DBIDs, Collection<DBID> {
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
  public Collection<DBID> asCollection() {
    return this;
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

  /**
   * Iterator over set bits
   * 
   * @author Erich Schubert
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
   * Iterator over unset elements.
   * 
   * @author Erich Schubert
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

  @Override
  public boolean add(@SuppressWarnings("unused") DBID e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(@SuppressWarnings("unused") Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
