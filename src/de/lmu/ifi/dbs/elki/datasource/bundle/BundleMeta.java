package de.lmu.ifi.dbs.elki.datasource.bundle;

import java.util.ArrayList;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;

/**
 * Store the package metadata in an array list. While this is a trivial class,
 * it improves code readability and type safety.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf SimpleTypeInformation
 */
public class BundleMeta extends ArrayList<SimpleTypeInformation<?>> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public BundleMeta() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param initialCapacity
   */
  public BundleMeta(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructor.
   * 
   * @param types
   */
  public BundleMeta(SimpleTypeInformation<?>... types) {
    super(Arrays.asList(types));
  }
}