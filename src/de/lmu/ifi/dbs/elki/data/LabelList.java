package de.lmu.ifi.dbs.elki.data;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * A list of string labels
 * 
 * @author Erich Schubert
 */
public class LabelList extends ArrayList<String> {
  /**
   * Serial number
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public LabelList() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param initialCapacity
   */
  public LabelList(int initialCapacity) {
    super(initialCapacity);
  }

  @Override
  public String toString() {
    return FormatUtil.format(this, " ");
  }
}
