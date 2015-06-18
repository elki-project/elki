package de.lmu.ifi.dbs.elki.datasource.parser;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.io.TokenizedReader;
import de.lmu.ifi.dbs.elki.utilities.io.Tokenizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract superclass for all parsers providing the option handler for handling
 * options.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Tokenizer
 */
public abstract class AbstractParser {
  /**
   * Tokenized reader.
   */
  protected TokenizedReader reader;

  /**
   * Tokenizer.
   */
  protected Tokenizer tokenizer;

  /**
   * Constructor.
   * 
   * @param format Input format.
   */
  public AbstractParser(CSVReaderFormat format) {
    super();
    this.reader = format.makeReader();
    this.tokenizer = reader.getTokenizer();
  }

  /**
   * Get the logger for this class.
   * 
   * @return Logger.
   */
  protected abstract Logging getLogger();

  /**
   * Cleanup internal data structures.
   */
  public void cleanup() {
    tokenizer.cleanup();
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return getClass().getName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Reader format.
     */
    protected CSVReaderFormat format;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      format = ClassGenericsUtil.parameterizeOrAbort(CSVReaderFormat.class, config);
    }

    @Override
    protected abstract AbstractParser makeInstance();
  }
}
