package experimentalcode.shared.index.xtree.util;
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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Bitset encoding for larger sets. Stores all bits in an array of
 * <code>long</code> variables, each of which can store 64 bits. <br>
 * <br>
 * The size of the set should be given in the constructor -- else, it is
 * automatically set to 64. The operations {@link #equals(Object)},
 * {@link #contains(LargeProperties)}, etc. are a lot faster than comparable
 * solutions using common Sets or BitSets, if the size of the query sets is not
 * exceptionally small. <br>
 * Wildcard inclusion is also enabled and implemented as efficiently as
 * possible.
 * 
 * @author Marisa Thoma
 */
public class LargeProperties implements Serializable, Cloneable, Iterable<Boolean>, Comparable<LargeProperties> {

  private static final long serialVersionUID = 8796667867811605733L;

  public static int LONG_STEP = 64;

  protected long[] propArray = null;

  /**
   * Number of attributes that can be stored in this == the capacity, not the
   * number of set elements.
   */
  protected int size = 0;

  /**
   * First index not allowed anymore for testing in the final entry of
   * <code>propArray</code>
   */
  protected int lastLongStop = 0;

  /**
   * Filter for excluding all entries allowed for the final entry of
   * <code>propArray</code>. Defaults to: everything is allowed.
   */
  protected long finalEntryFilter = 0;

  public LargeProperties() {
    propArray = new long[1];
    size = LONG_STEP;
    lastLongStop = size;
    finalEntryFilter = (((~(long) 0) << (lastLongStop - 2)) & (~(((long) 1) << ((long) -1))));
  }

  public LargeProperties(int size) {
    propArray = new long[(int) Math.ceil(((double) size) / LONG_STEP)];
    this.size = size;
    lastLongStop = size - LONG_STEP * (propArray.length - 1);

    finalEntryFilter = (((~(long) 0) << (lastLongStop - 1)) & (~(((long) 1) << ((long) -1))));
    assert lastLongStop <= LONG_STEP + 1 && lastLongStop > 0;
  }

  public LargeProperties(int size, Set<Integer> set) {
    this(size);
    setProperties(set);
  }

  /**
   * Not the most useful variant -- only contains one long-container. Can thus
   * only treat {@link #LONG_STEP} variables.
   * 
   * @param set
   */
  public LargeProperties(Set<Integer> set) {
    this();
    setProperties(set);
  }

  public void setProperties(Set<Integer> set) {
    Arrays.fill(this.propArray, 0);
    int intProperty;
    for(Iterator<Integer> iterator = set.iterator(); iterator.hasNext();) {
      intProperty = iterator.next();
      if(intProperty >= size) {
        throw new IllegalArgumentException("Property '" + intProperty + "' cannot be set for LongProperty of size " + size + " (" + toString() + ", for " + set.toString() + ")");
      }
      int index = intProperty / LONG_STEP;
      propArray[index] |= (((long) 1) << ((long) ((intProperty - index * LONG_STEP) - 1)));
    }
  }

  public void setProperty(int property) {
    if(property >= size) {
      throw new IllegalArgumentException("Property '" + property + "' cannot be set for LongProperty of size " + size);
    }
    int index = property / LONG_STEP;
    propArray[index] |= (((long) 1) << ((long) ((property - index * LONG_STEP) - 1)));
  }

  public void removeProperty(int property) {
    if(property >= size) {
      throw new IllegalArgumentException("Property '" + property + "' cannot be removed for LongProperty of size " + size);
    }
    int index = property / LONG_STEP;
    propArray[index] &= ~(((long) 1) << ((long) (property - index * LONG_STEP - 1)));
  }

  public boolean hasProperty(int property) {
    int index = property / LONG_STEP;
    return (propArray[index] & (((long) 1) << ((long) (property - index * LONG_STEP - 1)))) != 0;
  }

  public void join(LargeProperties lp) {
    if(lp.size != size) {
      throw new IllegalArgumentException("cannot join this (size " + size + ") with property set (" + lp.size + ")");
    }
    for(int i = 0; i < propArray.length; i++) {
      propArray[i] |= lp.propArray[i];
    }
  }

  public void intersect(LargeProperties lp) {
    if(lp.size != size) {
      throw new IllegalArgumentException("cannot intersect this (size" + size + ") with property set (" + lp.size + ")");
    }
    for(int i = 0; i < propArray.length; i++) {
      propArray[i] &= lp.propArray[i];
    }
  }

  public void reset() {
    Arrays.fill(propArray, 0);
  }

  public boolean contains(LargeProperties lp) {
    if(lp.size != size) {
      throw new IllegalArgumentException("query property set (size " + lp.size + ") is larger than this (" + size + ")");
    }
    for(int i = 0; i < propArray.length; i++) {
      if((lp.propArray[i] & propArray[i]) != lp.propArray[i]) {
        return false;
      }
    }
    return true;
  }

  public boolean contains(LargeProperties lp, LargeProperties wild_cards) {
    if(lp.size != size) {
      throw new IllegalArgumentException("query property set (size " + lp.size + ") is larger than this (" + size + ")");
    }
    if(wild_cards.size != size) {
      throw new IllegalArgumentException("wild_card property set (size " + wild_cards.size + ") is larger than this (" + size + ")");
    }
    for(int i = 0; i < propArray.length; i++) {
      if((lp.propArray[i] & (propArray[i] | wild_cards.propArray[i])) != lp.propArray[i]) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    // crude, but it's a hash code
    long sum = 0;
    for(int i = 0; i < propArray.length; i++) {
      sum += propArray[i];
    }
    return (int) sum;
  }

  @Override
  public boolean equals(Object obj) {
    LargeProperties lp = (LargeProperties) obj;
    if(lp.size != size) {
      return false;
    }
    for(int i = 0; i < propArray.length; i++) {
      if(propArray[i] != lp.propArray[i]) {
        return false;
      }
    }
    return true;
  }

  public boolean equals(LargeProperties lp, LargeProperties wild_cards) {
    if(wild_cards == null) {
      return equals(lp);
    }
    if(lp.size != size) {
      return false;
    }
    if(size != wild_cards.size) {
      throw new IllegalArgumentException("wild_card property set (size " + wild_cards.size + ") is larger than this (" + size + ")");
    }
    for(int i = 0; i < propArray.length; i++) {
      if((propArray[i] | wild_cards.propArray[i]) != (lp.propArray[i] | wild_cards.propArray[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return a new {@link LargeProperties} object with <code>~this</code> = all
   *         boolean variables in <code>this</code> are reversed.
   */
  public LargeProperties not() {
    LargeProperties not = new LargeProperties(size);
    for(int i = 0; i < propArray.length - 1; i++) {
      not.propArray[i] = ~propArray[i];
    }
    not.propArray[propArray.length - 1] = ~(propArray[propArray.length - 1] | ((~(long) 0) << (lastLongStop - 1)));
    return not;
  }

  public Set<Integer> getComplementSet() {
    Set<Integer> complement = new java.util.HashSet<Integer>();
    for(int i = 0; i < propArray.length; i++) {
      Set<Integer> partialComplement;
      if(i == propArray.length - 1) {
        partialComplement = toSet(~(propArray[i] | finalEntryFilter));
      }
      else {
        partialComplement = toSet(~propArray[i]);
      }
      for(Iterator<Integer> iterator = partialComplement.iterator(); iterator.hasNext();) {
        complement.add(iterator.next() + LONG_STEP * i);
      }
    }
    return complement;
  }

  public static Set<Integer> toSet(long set) {
    Set<Integer> outSet = new HashSet<Integer>();
    for(int i = 0; i < 64; i++) {
      long target = (((long) 1) << ((long) i - 1));
      if((set & target) == target) {
        outSet.add(i);
      }
    }
    return outSet;
  }

  @Override
  public String toString() {
    char[] chars = new char[size];
    for(int i = 0; i < chars.length; i++) {
      if(hasProperty(i)) {
        chars[i] = '1';
      }
      else {
        chars[i] = '0';
      }
    }
    return new String(chars);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    super.clone();
    LargeProperties lp = new LargeProperties(size);
    lp.propArray = propArray.clone();
    lp.lastLongStop = lastLongStop;
    return lp;
  }

  /**
   * @param lp1 must have the same size as <code>lp2</code>
   * @param lp2
   * @return a new {@link LargeProperties} object, the joined set of
   *         <code>lp1</code> and <code>lp2</code>
   */
  public static LargeProperties join(LargeProperties lp1, LargeProperties lp2) {
    LargeProperties lp;
    try {
      lp = (LargeProperties) lp1.clone();
    }
    catch(CloneNotSupportedException e) {
      e.printStackTrace();
      throw new InternalError("Error: this class DOES implement clone(), but: '" + e.getMessage() + "'");
    }
    lp.join(lp2);
    return lp;
  }

  /**
   * @param lp1 must have the same size as <code>lp2</code>
   * @param lp2
   * @return a new {@link LargeProperties} object, the intersect of
   *         <code>lp1</code> and <code>lp2</code>
   */
  public static LargeProperties intersect(LargeProperties lp1, LargeProperties lp2) {
    LargeProperties lp;
    try {
      lp = (LargeProperties) lp1.clone();
    }
    catch(CloneNotSupportedException e) {
      e.printStackTrace();
      throw new InternalError("Error: this class DOES implement clone(), but: '" + e.getMessage() + "'");
    }
    lp.intersect(lp2);
    return lp;
  }

  /**
   * @return Number of attributes that can be stored in this == the capacity,
   *         <i>not</i> the number of set elements.
   */
  public int getSize() {
    return size;
  }

  /**
   * @return Number of attributes which are set to <code>true</code>.
   */
  public int getNumberOfElements() {
    int numSet = 0;
    for(Iterator<Boolean> iterator = newPropertyIterator(); iterator.hasNext();) {
      if(iterator.next())
        numSet++;
    }
    return numSet;
  }

  /**
   * @return <code>true</code> if no attributes of <code>this</code> are set,
   *         else <code>false</code>.
   */
  public boolean isEmpty() {
    for(int i = 0; i < propArray.length; i++) {
      if(propArray[i] != 0)
        return false;
    }
    return true;
  }

  @Override
  public Iterator<Boolean> iterator() {
    return newPropertyIterator();
  }

  protected Iterator<Boolean> newPropertyIterator() {
    return new Iterator<Boolean>() {
      int index = 0, end = lastLongStop + (propArray.length - 1) * LONG_STEP;

      @Override
      public boolean hasNext() {
        return index < end;
      }

      @Override
      public Boolean next() {
        if(index >= end)
          throw new NoSuchElementException();
        return hasProperty(index++);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public int compareTo(LargeProperties lp) {
    if(lp.size != size) {
      throw new IllegalArgumentException("ZOrder not implemented here: property objects must be of the same size; " + lp.size + "!=" + size + ".");
    }
    for(int i = 0; i < propArray.length; i++) {
      if(propArray[i] < lp.propArray[i]) {
        return -1;
      }
      if(propArray[i] > lp.propArray[i]) {
        return 1;
      }
    }
    return 0;
  }

  /**
   * Writes the complete property vector to the specified output stream.
   * 
   * @param out the stream to write the history to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(size);
    for(int i = 0; i < propArray.length; i++) {
      out.writeLong(propArray[i]);
    }
  }

  /**
   * Reads the property vector from the specified input stream into a new
   * LargeProperties object.
   * 
   * @param in the stream to read data from in order to restore the object
   * @return the properties read from <code>in</code>
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  public static LargeProperties readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int psize = in.readInt();
    LargeProperties lp = new LargeProperties(psize);
    for(int i = 0; i < lp.propArray.length; i++) {
      lp.propArray[i] = in.readLong();
    }
    return lp;
  }

  /**
   * FIXME: this is more efficient than {@link #writeExternal(ObjectOutput)},
   * however, due to the usage of the sign offset, there are some difficulties.
   * Writes the properties to the specified output stream.
   * 
   * @param out the stream to write the history to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternalSmallButWrong(ObjectOutput out) throws IOException {
    int numBytes = (int) Math.ceil(getSize() / 8.0);
    out.writeInt(getSize());
    byte[] bytes = new byte[numBytes];
    for(int i = 0; i < propArray.length; i++) {
      for(int j = 0; j < 8 && i * 8 + j < bytes.length; j++) {
        // use 11111111 filter at all used byte positions
        bytes[i * 8 + j] = (byte) (propArray[i] >>> (8 * j));
      }
    }
    out.write(bytes);
  }

  /**
   * FIXME: this is more efficient than {@link #writeExternal(ObjectOutput)},
   * however, due to the usage of the sign offset, there are some difficulties.
   * Writes the split history to the specified output stream. Reads the
   * properties from the specified input stream into a new LargeProperties
   * object.
   * 
   * @param in the stream to read data from in order to restore the object
   * @return the properties read from <code>in</code>
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  public static LargeProperties readExternalSmallButWrong(ObjectInput in) throws IOException, ClassNotFoundException {
    int psize = in.readInt();
    LargeProperties lp = new LargeProperties(psize);
    int numBytes = (int) Math.ceil(psize / 8.0);
    byte[] bytes = new byte[numBytes];
    in.read(bytes);
    for(int i = 0; i < lp.propArray.length; i++) {
      lp.propArray[i] = 0x0L; // init
      int stopPos = 7;
      if(bytes.length - i * 8 < 8) {
        stopPos = bytes.length - i * 8;
      }
      else {
        // init with special case (sign bit)
        lp.propArray[i] = (((long) bytes[i * 8 + 7]) << 56);
      }
      for(int j = 0; j < stopPos; j++) {
        lp.propArray[i] += ((bytes[i * 8 + j] & 0xFFL) << (8 * j));
      }
    }
    return lp;
  }
}