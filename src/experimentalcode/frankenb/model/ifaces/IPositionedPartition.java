/**
 * 
 */
package experimentalcode.frankenb.model.ifaces;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface IPositionedPartition extends IPartition {

  /**
   * Returns the position associated with this partition. Dimension starts at 1
   * 
   * @param dimension
   * @return
   */
  public int getPosition(int dimension);
  
}
