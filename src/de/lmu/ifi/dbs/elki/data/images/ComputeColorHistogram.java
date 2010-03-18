package de.lmu.ifi.dbs.elki.data.images;

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for color histogram implementations.
 * 
 * @author Erich Schubert
 */
public interface ComputeColorHistogram extends Parameterizable {
  /**
   * Compute a color histogram given a file name.
   * 
   * @param file File name
   * @return Color histogram
   * @throws IOException on file read errors.
   */
  public double[] computeColorHistogram(File file) throws IOException;
}