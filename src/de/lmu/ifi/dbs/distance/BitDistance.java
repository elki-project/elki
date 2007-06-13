package de.lmu.ifi.dbs.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * TODO comment
 * @author Arthur Zimek
 */
public class BitDistance extends NumberDistance<BitDistance> {
  /**
   * Generated serial version UID
   */
  private static final long serialVersionUID = 6514853467081717551L;

  /**
   * The bit value of this distance.
   */
  private boolean bit;

  /**
   *
   */
  public BitDistance() {
    super();
  }

  /**
   *
   */
  public BitDistance(boolean bit) {
    super();
    this.bit = bit;
  }

  /**
   * @see NumberDistance#getDoubleValue()
   */
  @Override
  public double getDoubleValue() {
    return bit ? 1 : 0;
  }

  /**
   * @see AbstractDistance#hashCode()
   */
  @Override
  public int hashCode() {
    return this.isBit() ? 1231 : 1237;
  }

  /**
   * @see Distance#plus(Distance)
   */
  public BitDistance plus(BitDistance distance) {
    return new BitDistance(this.isBit() || distance.isBit());
  }

  /**
   * @see Distance#minus(Distance)
   */
  public BitDistance minus(BitDistance distance) {
    return new BitDistance(this.isBit() ^ distance.isBit());
  }

  /**
   * @see Distance#externalizableSize()
   */
  public int externalizableSize() {
    return 1;
  }

  /**
   * @see Comparable#compareTo(Object)
   */
  public int compareTo(BitDistance o) {
    return ((Boolean) this.isBit()).compareTo(o.isBit());
  }

  /**
   * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeBoolean(this.isBit());
  }

  /**
   * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.bit = in.readBoolean();
  }

  public boolean isBit() {
    return this.bit;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    if (this.bit) return "1";
    else return "0";
  }


}
