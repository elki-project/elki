package de.lmu.ifi.dbs.elki.distance.distancevalue;

import de.lmu.ifi.dbs.elki.data.Bit;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.regex.Pattern;

/**
 * Provides a Distance for a bit-valued distance.
 * 
 * @author Arthur Zimek
 */
public class BitDistance extends NumberDistance<BitDistance, Bit> {
  /**
   * The static factory instance
   */
  public final static BitDistance FACTORY = new BitDistance();
  
  /**
   * The distance value
   */
  private boolean value;

  /**
   * Generated serial version UID
   */
  private static final long serialVersionUID = 6514853467081717551L;

  /**
   * Empty constructor for serialization purposes.
   */
  public BitDistance() {
    super();
  }

  /**
   * Constructs a new BitDistance object that represents the bit argument.
   * 
   * @param bit the value to be represented by the BitDistance.
   */
  public BitDistance(boolean bit) {
    super();
    this.value = bit;
  }

  /**
   * Constructs a new BitDistance object that represents the bit argument.
   * 
   * @param bit the value to be represented by the BitDistance.
   */
  public BitDistance(Bit bit) {
    super();
    this.value = bit.bitValue();
  }

  @Override
  public BitDistance plus(BitDistance distance) {
    return new BitDistance(this.bitValue() || distance.bitValue());
  }

  @Override
  public BitDistance minus(BitDistance distance) {
    return new BitDistance(this.bitValue() ^ distance.bitValue());
  }

  /**
   * Returns the value of this BitDistance as a boolean.
   * 
   * @return the value as a boolean
   */
  public boolean bitValue() {
    return this.value;
  }

  /**
   * Writes the bit value of this BitDistance to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeBoolean(this.bitValue());
  }

  /**
   * Reads the bit value of this BitDistance from the specified stream.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException {
    setValue(new Bit(in.readBoolean()));
  }

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return 1 (1 Byte for a boolean value)
   */
  @Override
  public int externalizableSize() {
    return 1;
  }

  @Override
  public Bit getValue() {
    return new Bit(this.value);
  }

  @Override
  void setValue(Bit value) {
    this.value = value.bitValue();
  }

  @Override
  public double doubleValue() {
    return value ? 1.0 : 0.0;
  }
  
  @Override
  public long longValue() {
    return value ? 1 : 0;
  }

  @Override
  public int intValue() {
    return value ? 1 : 0;
  }

  @Override
  public int compareTo(BitDistance other) {
    return this.intValue() - other.intValue();
  }

  @Override
  public BitDistance parseString(String val) throws IllegalArgumentException {
    if(testInputPattern(val)) {
      return new BitDistance(Bit.valueOf(val).bitValue());
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + val + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
    }
  }

  @Override
  public BitDistance infiniteDistance() {
    return new BitDistance(true);
  }

  @Override
  public BitDistance nullDistance() {
    return new BitDistance(false);
  }

  @Override
  public BitDistance undefinedDistance() {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_UNDEFINED_DISTANCE);
  }

  @Override
  public Pattern getPattern() {
    return Bit.BIT_PATTERN;
  }

  @Override
  public boolean isInfiniteDistance() {
    return false;
  }

  @Override
  public boolean isNullDistance() {
    return (value == false);
  }

  @Override
  public boolean isUndefinedDistance() {
    return false;
  }
}