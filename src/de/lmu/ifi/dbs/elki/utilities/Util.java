package de.lmu.ifi.dbs.elki.utilities;

import java.io.PrintStream;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;

/**
 * This class collects various static helper methods.
 * 
 * For helper methods related to special application fields see other utilities classes.
 * 
 * 
 * @see de.lmu.ifi.dbs.elki.utilities
 */
public final class Util {
  /**
   * Returns the prefix of the specified fileName (i.e. the name of the file
   * without extension).
   * 
   * @param fileName the name of the file
   * @return the prefix of the specified fileName
   */
  public static String getFilePrefix(final String fileName) {
    final int index = fileName.lastIndexOf(Character.getNumericValue('.'));
    if(index < 0) {
      return fileName;
    }
    return fileName.substring(0, index);
  }

  /**
   * Returns a new String array containing the same objects as are contained in
   * the given array.
   * 
   * @param array an array to copy
   * @return the copied array
   */
  public static String[] copy(String[] array) {
    String[] copy = new String[array.length];
    System.arraycopy(array, 0, copy, 0, array.length);
    return copy;
  }

  /**
   * Returns a new double array containing the same objects as are contained in
   * the given array.
   * 
   * @param array an array to copy
   * @return the copied array
   */
  public static double[] copy(double[] array) {
    double[] copy = new double[array.length];
    System.arraycopy(array, 0, copy, 0, array.length);
    return copy;
  }

  /**
   * Returns the unboxed double array of the given Object Double array.
   * 
   * @param array the array to be unboxed
   * @return the unboxed double array
   */
  public static double[] unbox(Double[] array) {
    double[] unboxed = new double[array.length];
    // noinspection ManualArrayCopy
    for(int i = 0; i < unboxed.length; i++) {
      unboxed[i] = array[i];
    }
    return unboxed;
  }

  /**
   * Returns the unboxed double array of the given Object Number array.
   * 
   * @param array the array to be unboxed
   * @return the unboxed double array
   */
  public static double[] unbox(Number[] array) {
    double[] unboxed = new double[array.length];
    for(int i = 0; i < unboxed.length; i++) {
      unboxed[i] = array[i].doubleValue();
    }
    return unboxed;
  }

  /**
   * Returns the unboxed float array of the given Object Number array.
   * 
   * @param array the array to be unboxed
   * @return the unboxed float array
   */
  public static float[] unboxToFloat(Number[] array) {
    float[] unboxed = new float[array.length];
    for(int i = 0; i < unboxed.length; i++) {
      unboxed[i] = array[i].floatValue();
    }
    return unboxed;
  }

  /**
   * Returns a new <code>Double</code> array initialized to the values
   * represented by the specified <code>String</code> and separated by comma, as
   * performed by the <code>valueOf</code> method of class <code>Double</code>.
   * 
   * @param s the string to be parsed.
   * @return a new <code>Double</code> array represented by s
   */
  public static double[] parseDoubles(String s) {
    List<Double> result = new ArrayList<Double>();
    StringTokenizer tokenizer = new StringTokenizer(s, ",");
    while(tokenizer.hasMoreTokens()) {
      String d = tokenizer.nextToken();
      result.add(Double.parseDouble(d));
    }
    return unbox(result.toArray(new Double[result.size()]));
  }

  /**
   * Returns a new <code>Float</code> array initialized to the values
   * represented by the specified <code>String</code> and separated by comma, as
   * performed by the <code>valueOf</code> method of class <code>Float</code>.
   * 
   * @param s the string to be parsed.
   * @return a new <code>Float</code> array represented by s
   */
  public static float[] parseFloats(String s) {
    List<Float> result = new ArrayList<Float>();
    StringTokenizer tokenizer = new StringTokenizer(s, ",");
    while(tokenizer.hasMoreTokens()) {
      String d = tokenizer.nextToken();
      result.add(Float.parseFloat(d));
    }
    return unboxToFloat(result.toArray(new Float[result.size()]));
  }

  /**
   * Converts the specified list of double objects to a list of float objects.
   * 
   * @param values the list of double objects to be converted
   * @return the converted list of float objects
   */
  public static List<Float> convertToFloat(List<Double> values) {
    List<Float> result = new ArrayList<Float>(values.size());
    for(Double value : values) {
      result.add(new Float(value));
    }
    return result;
  }

  /**
   * Converts the specified array of doubles to an array of floats.
   * 
   * @param values the array of doubles to be converted
   * @return the converted array of floats
   */
  public static float[] convertToFloat(double[] values) {
    float[] result = new float[values.length];
    for(int i = 0; i < values.length; i++) {
      // noinspection NumericCastThatLosesPrecision
      result[i] = (float) values[i];
    }
    return result;
  }

  /**
   * Converts the specified array of doubles to an array of floats.
   * 
   * @param values the array of doubles to be converted
   * @return the converted array of floats
   */
  public static double[] convertToDoubles(float[] values) {
    double[] result = new double[values.length];
    for(int i = 0; i < values.length; i++) {
      result[i] = values[i];
    }
    return result;
  }

  /**
   * Converts the specified list of Double objects to an array of doubles.
   * 
   * @param values the list of Double objects to be converted
   * @return the converted array of doubles
   */
  public static double[] convertToDoubles(List<Double> values) {
    double[] result = new double[values.size()];
    for(int i = 0; i < result.length; i++) {
      result[i] = values.get(i);
    }
    return result;
  }

  /**
   * Prints the given list to the specified PrintStream. The list entries are
   * separated by the specified separator. The last entry is not followed by a
   * separator. Thus, if a newline is used as separator, it might make sense to
   * print a newline to the PrintStream after calling this method.
   * 
   * @param <O> object class 
   * @param list the list to be printed
   * @param separator the separator to separate entries of the list
   * @param out the target PrintStream
   */
  public static <O> void print(List<O> list, String separator, PrintStream out) {
    for(Iterator<O> iter = list.iterator(); iter.hasNext();) {
      out.print(iter.next());
      if(iter.hasNext()) {
        out.print(separator);
      }
    }
  }

  /**
   * Returns the index of the maximum of the given values. If no value is bigger
   * than the first, the index of the first entry is returned.
   * 
   * @param values the values to find the index of the maximum
   * @return the index of the maximum in the given values
   * @throws ArrayIndexOutOfBoundsException if <code>values.length==0</code>
   */
  public static int getIndexOfMaximum(double[] values) throws ArrayIndexOutOfBoundsException {
    int index = 0;
    double max = values[index];
    for(int i = 0; i < values.length; i++) {
      if(values[i] > max) {
        max = values[i];
        index = i;
      }
    }
    return index;
  }

  /**
   * Returns a new <code>BitSet</code> initialized to the values represented by
   * the specified <code>String</code> only containing 0 and 1 values.
   * 
   * @param s the string to be parsed.
   * @return a new <code>BitSet</code> represented by s
   */
  public static BitSet parseBitSet(String s) {
    try {
      return parseBitSet(s.toCharArray());
    }
    catch(IllegalArgumentException e) {
      throw new IllegalArgumentException("The specified String does not represent a bit set " + "containing only 0 and 1 values: " + s);
    }
  }

  /**
   * Returns a new <code>BitSet</code> initialized to the values represented by
   * the specified <code>char</code> array only containing '0' and '1' values.
   * 
   * @param s the char array to be parsed.
   * @return a new <code>BitSet</code> represented by s
   */
  public static BitSet parseBitSet(char[] s) {
    BitSet result = new BitSet();
    for(int i = 0; i < s.length; i++) {
      if(s[i] == '1') {
        result.set(i);
      }
      else if(s[i] != '0') {
        throw new IllegalArgumentException("The specified String does not represent a bit set " + "containing only 0 and 1 values: " + String.valueOf(s));
      }
    }
    return result;
  }

  /**
   * Returns a string that represents the selected bits of the specified
   * <code>BitSet</code>, while the first bit starts with 1. The selected bits
   * are separated by the specified separator <code>sep</code>.
   * 
   * If <code>sep</code> is &quot;,&quot;, the result is suitable as a
   * parameter for an IntListParameter.
   * 
   * @param b the bit set to be parsed
   * @param sep the separator
   * @return a string representing the selected bits of the specified
   *         <code>BitSet</code>
   */
  public static String parseSelectedBits(BitSet b, String sep) {
    StringBuffer result = new StringBuffer();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      if(result.length() != 0) {
        result.append(sep).append(i + 1);
      }
      else {
        result.append((i + 1));
      }
    }
    
    return result.toString();
  }
  
  /**
   * Convert a bit set to a list of integers, representing bits that are set
   * 
   * @param b Bitset
   * @param off Offset, set to 0 to start counting at 0, 1 to start counting at 1.
   * @return List
   */
  public static List<Integer> convertBitSetToListInt(BitSet b, int off) {
    List<Integer> list = new ArrayList<Integer>();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      list.add(i + off);
    }    
    return list;
  }
  
  /**
   * Creates a new BitSet of fixed cardinality with randomly set bits.
   * 
   * @param cardinality the cardinality of the BitSet to create
   * @param capacity the capacity of the BitSet to create - the randomly generated indices of the bits set to true will be uniformly distributed between 0 (inclusive) and capacity (exclusive)
   * @param random a Random Object to create the sequence of indices set to true - the same number occurring twice or more is ignored but the already selected bit remains true
   * @return a new BitSet with randomly set bits
   */
  public static BitSet randomBitSet(int cardinality, int capacity, Random random){
    BitSet bitset = new BitSet(capacity);
    while(bitset.cardinality() < cardinality){
      bitset.set(random.nextInt(capacity));
    }
    return bitset;
  }
  
  /**
   * Provides a new DoubleVector as a projection on the specified attributes.
   * 
   * If the given DoubleVector has already an ID not <code>null</code>, the same ID is set in the returned new DoubleVector.
   * Nevertheless, the returned DoubleVector is not backed by the given DoubleVector, i.e., any changes affecting <code>v</code> after calling this method will not affect the newly returned DoubleVector.
   * 
   * @param v a DoubleVector to project
   * @param selectedAttributes the attributes selected for projection
   * @return a new DoubleVector as a projection on the specified attributes
   * @throws IllegalArgumentException if the given selected attributes specify an attribute as selected which is out of range for the given DoubleVector.
   * @see DoubleVector#doubleValue(int)
   */
  public static DoubleVector project(DoubleVector v, BitSet selectedAttributes){
    double[] newAttributes = new double[selectedAttributes.cardinality()];
    int i = 0;
    for (int d = selectedAttributes.nextSetBit(0); d >= 0; d = selectedAttributes.nextSetBit(d + 1)) {
      newAttributes[i] = v.doubleValue(d+1);
      i++;
    }
    DoubleVector projectedVector = new DoubleVector(newAttributes);
    Integer id = v.getID();
    if(id != null){
      projectedVector.setID(id);
    }
    return projectedVector;
  }
  
  /**
   * Provides a new SparseFloatVector as a projection on the specified attributes.
   * 
   * If the given SparseFloatVector has already an ID not <code>null</code>, the same ID is set in the returned new SparseFloatVector.
   * Nevertheless, the returned SparseFloatVector is not backed by the given SparseFloatVector, i.e., any changes affecting <code>v</code> after calling this method will not affect the newly returned SparseFloatVector.
   * 
   * @param v a SparseFloatVector to project
   * @param selectedAttributes the attributes selected for projection
   * @return a new SparseFloatVector as a projection on the specified attributes
   * @throws IllegalArgumentException if the given selected attributes specify an attribute as selected which is out of range for the given SparseFloatVector.
   */
  public static SparseFloatVector project(SparseFloatVector v, BitSet selectedAttributes){
    Map<Integer, Float> values = new HashMap<Integer, Float>(selectedAttributes.cardinality(),1);
    for (int d = selectedAttributes.nextSetBit(0); d >= 0; d = selectedAttributes.nextSetBit(d + 1)) {
      if(v.getValue(d+1) != 0.0f){
        values.put(d, v.getValue(d+1));
      }
    }
    SparseFloatVector projectedVector = new SparseFloatVector(values,selectedAttributes.cardinality());
    Integer id = v.getID();
    if(id != null){
      projectedVector.setID(id);
    }
    return projectedVector;
  }

  /**
   * Returns the index of the n<sup>th</sup> set bit in the given BitSet. For
   * the parameter <code>nthSetBit</code>, following condition is assumed:
   * <code>1 &le; nthSetBit &le; bitset.cardinality()</code>. Otherwise, i.e.,
   * if the Bitset contains less than <code>nthSetBit</code> set bits or
   * <code>nthSetBit</code> is not a positive number, the method throws an
   * IllegalArgumentException.
   * 
   * The worstcase runtime complexity of this method is in <i>O(
   * <code>bitset.cardinality()</code>)</i>.
   * 
   * @param bitset the BitSet to derive the index of the n<sup>th</sup> set bit
   *        in
   * @param nthSetBit which set bit to derive the index of
   * @return the index of the n<sup>th</sup> set bit in the given BitSet
   * @throws IllegalArgumentException if the Bitset contains less than
   *         <code>nthSetBit</code> set bits or <code>nthSetBit</code> is not a
   *         positive number
   */
  public static int indexOfNthSetBit(BitSet bitset, int nthSetBit) throws IllegalArgumentException {
    if(nthSetBit < 1 || nthSetBit > bitset.cardinality()) {
      throw new IllegalArgumentException("Parameter nthSetBit out of range: nthSetBit=" + nthSetBit + ", bitset.cardinality=" + bitset.cardinality());
    }
    int i = 0;
    int index = -1;
    for(int d = bitset.nextSetBit(0); d >= 0 && i < nthSetBit; d = bitset.nextSetBit(d + 1)) {
      i++;
      index = d;
    }
    return index;
  }

  /**
   * Provides the intersection of the two specified sets in the given result
   * set.
   * 
   * @param <O> object class 
   * @param s1 the first set
   * @param s2 the second set
   * @param result the result set
   */
  public static <O> void intersection(Set<O> s1, Set<O> s2, Set<O> result) {
    for(O object : s1) {
      if(s2.contains(object)) {
        result.add(object);
      }
    }
  }

  /**
   * Converts the specified positive integer value into a bit representation,
   * where bit 0 denotes 2<sup>0</sup>, bit 1 denotes 2<sup>1</sup> etc.
   * 
   * @param n the positive integer value to be converted
   * @return the specified integer value into a bit representation
   */
  public static BitSet int2Bit(int n) {
    if(n < 0) {
      throw new IllegalArgumentException("Parameter n hast to be greater than or equal to zero!");
    }

    BitSet result = new BitSet();
    int i = 0;
    while(n > 0) {
      boolean rest = (n % 2 == 1);
      if(rest) {
        result.set(i);
      }
      n = n / 2;
      i++;
    }
    return result;
  }

  /**
   * Joins the specified arrays.
   * 
   * @param array1 the first array
   * @param array2 the second array
   * @return a new array containing the entries of <code>array1</code> and the
   *         <code>array2</code>.
   */
  public static String[] joinArray(String[] array1, String[] array2) {
    String[] newArray = new String[array1.length + array2.length];
    System.arraycopy(array1, 0, newArray, 0, array1.length);
    System.arraycopy(array2, 0, newArray, array1.length, array2.length);
    return newArray;
  }

  /**
   * Adds the entries of the specified array to the end of the given list.
   * 
   * @param <O> object class 
   * @param list the list
   * @param array the array containing the objects to be added to the list
   */
  public static <O> void addToList(List<O> list, O[] array) {
    for(O object : array) {
      list.add(object);
    }
  }

  /**
   * Search an (unsorted) array linearly for an object.
   * 
   * @param arr Array to search
   * @param ref Object to search for
   * @return Index of object or -1 if not found.
   */
  public static int arrayFind(String[] arr, Object ref) {
    for (int index = 0; index < arr.length; index++) {
      if (ref.equals(arr[index])) {
        return index;
      }
    }
    return -1;
  }

  /**
   * This class is a virtual collection based on masking an array list using a bit mask.
   * 
   * @author Erich Schubert
   *
   * @param <T> Object type
   */
  public static class MaskedArrayList<T> extends AbstractCollection<T> implements Collection<T> {
    /**
     * Data storage
     */
    protected ArrayList<T> data;
  
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
    public MaskedArrayList(ArrayList<T> data, BitSet bits, boolean inverse) {
      super();
      this.data = data;
      this.bits = bits;
      this.inverse = inverse;
    }
  
    @Override
    public boolean add(@SuppressWarnings("unused") T e) {
      throw new UnsupportedOperationException();
    }
  
    @Override
    public Iterator<T> iterator() {
      if(inverse) {
        return new InvItr();
      }
      else {
        return new Itr();
      }
    }
  
    @Override
    public int size() {
      if (inverse) {
        return data.size() - bits.cardinality();
      } else {
        return bits.cardinality();
      }
    }
  
    /**
     * Iterator over set bits
     * 
     * @author Erich Schubert
     */
    protected class Itr implements Iterator<T> {
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
      public T next() {
        T cur = data.get(pos);
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
    protected class InvItr implements Iterator<T> {
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
      public T next() {
        T cur = data.get(pos);
        pos = bits.nextClearBit(pos + 1);
        return cur;
      }
  
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
  }
}