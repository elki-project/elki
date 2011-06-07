package de.lmu.ifi.dbs.elki.datasource.filter;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;

/**
 * Object filters as part of the input step.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.uses MultipleObjectsBundle oneway - - «filters»
 */
public interface ObjectFilter {
  /**
   * Filter a set of object packages.
   * 
   * @param objects Object to filter
   * @return Filtered objects
   */
  MultipleObjectsBundle filter(MultipleObjectsBundle objects);
}
