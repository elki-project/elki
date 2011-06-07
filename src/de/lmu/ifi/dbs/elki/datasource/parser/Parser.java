package de.lmu.ifi.dbs.elki.datasource.parser;

import java.io.InputStream;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtilFrequentlyScanned;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * A Parser shall provide a ParsingResult by parsing an InputStream.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 * @apiviz.uses InputStream
 * @apiviz.uses MultipleObjectsBundle oneway - - «create»
 */
public interface Parser extends Parameterizable, InspectionUtilFrequentlyScanned {
  /**
   * Returns a list of the objects parsed from the specified input stream.
   * 
   * @param in the stream to parse objects from
   * @return a list containing those objects parsed from the input stream
   */
  MultipleObjectsBundle parse(InputStream in);
}
