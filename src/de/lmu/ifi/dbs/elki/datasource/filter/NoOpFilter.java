package de.lmu.ifi.dbs.elki.datasource.filter;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;

/**
 * Dummy filter that doesn't do any filtering.
 * 
 * Useful for command line parameterization when you have multiple identically
 * named parameters, and want to set the second parameter only. Then you can
 * just use this dummy filter as first parameter.
 * 
 * @author Erich Schubert
 */
public class NoOpFilter implements ObjectFilter {
  /**
   * Constructor.
   */
  public NoOpFilter() {
    super();
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    return objects;
  }
}
