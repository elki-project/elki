package de.lmu.ifi.dbs.elki.data.model;

/**
 * Model for an OPTICS cluster
 * 
 * @author Erich Schubert
 */
public class OPTICSModel extends BaseModel {
  /**
   * Start index
   */
  private int startIndex;

  /**
   * End index
   */
  private int endIndex;

  /**
   * @param startIndex
   * @param endIndex
   */
  public OPTICSModel(int startIndex, int endIndex) {
    super();
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }

  /**
   * Starting index of OPTICS cluster
   * 
   * @return index of cluster start
   */
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * End index of OPTICS cluster
   * 
   * @return index of cluster end
   */
  public int getEndIndex() {
    return endIndex;
  }

  @Override
  public String toString() {
    return "OPTICSModel";
  }
}