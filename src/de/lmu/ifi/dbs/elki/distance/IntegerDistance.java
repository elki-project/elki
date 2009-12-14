package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Provides an integer distance value.
 * 
 * @author Arthur Zimek
 */
public class IntegerDistance extends NumberDistance<IntegerDistance, Integer> {
  /**
   * The distance value
   */
  int value;

  /**
   * Created serial version UID.
   */
  private static final long serialVersionUID = 5583821082931825810L;

  /**
   * Empty constructor for serialization purposes.
   */
  public IntegerDistance() {
    super();
  }

  /**
   * Constructor
   * 
   * @param value distance value
   */
  public IntegerDistance(int value) {
    super();
    this.value = value;
  }

  public String description() {
    return "IntegerDistance.intValue";
  }

  public IntegerDistance minus(IntegerDistance distance) {
    return new IntegerDistance(this.getValue() - distance.getValue());
  }

  public IntegerDistance plus(IntegerDistance distance) {
    return new IntegerDistance(this.getValue() + distance.getValue());
  }

  /**
   * Writes the integer value of this IntegerDistance to the specified stream.
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(getValue());
  }

  /**
   * Reads the integer value of this IntegerDistance from the specified stream.
   */
  public void readExternal(ObjectInput in) throws IOException {
    setValue(in.readInt());
  }

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return 4 (4 Byte for an integer value)
   */
  public int externalizableSize() {
    return 4;
  }

  @Override
  public Integer getValue() {
    return this.value;
  }

  @Override
  void setValue(Integer value) {
    this.value = value;
  }

  @Override
  public double doubleValue() {
    return value;
  }
  
  @Override
  public long longValue() {
    return value;
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public int compareTo(IntegerDistance other) {
    return (this.value<other.value ? -1 : (this.value==other.value ? 0 : 1));
  }
}
