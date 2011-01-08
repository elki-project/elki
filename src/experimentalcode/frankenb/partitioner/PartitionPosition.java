/**
 * 
 */
package experimentalcode.frankenb.partitioner;

class PartitionPosition {
  private final int[] position;
  private int hashCode;
  private boolean tainted = true;
  
  public PartitionPosition(int dimensonality) {
    this.position = new int[dimensonality];
    for (int i = 0; i < this.position.length; ++i) {
      this.position[i] = 0;
    }
  }
  
  public PartitionPosition(int[] position) {
    this.position = position;
  }
  
  public void setPosition(int dimension, int position) {
    this.position[dimension] = position;
    tainted = true;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    if (tainted) {
      hashCode = 0;
      for (int i = 0; i < position.length; ++i) {
        hashCode ^= this.position[i];
      }
      tainted = false;
    }
    return hashCode;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PartitionPosition)) {
      return false;
    }
    PartitionPosition other = (PartitionPosition) o;
    if (this.hashCode() != other.hashCode) return false;
    if (this.position.length != other.position.length) return false;
    for (int i = 0; i < this.position.length; ++i) {
      if (this.position[i] != other.position[i]) return false;
    }
    return true;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("position [");
    boolean first = true;
    for (int aPosition : position) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(aPosition);
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * @return
   */
  public int[] getPosition() {
    return position;
  }
}