package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for any class that can handle results
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses Result oneway - - processes
 */
public interface ResultHandler extends Parameterizable, ResultProcessor {
  // Empty - moved to ResultProcessor, this interface merely serves UI purposes.
}