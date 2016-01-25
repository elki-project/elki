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

import java.io.IOException;
import java.io.InputStream;

import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.io.TokenizedReader;
import de.lmu.ifi.dbs.elki.utilities.io.Tokenizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Base class for streaming parsers.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @apiviz.uses CSVReaderFormat
 * @apiviz.composedOf TokenizedReader
 * @apiviz.composedOf Tokenizer
 */
public abstract class AbstractStreamingParser implements StreamingParser {
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
   * @param format Reader format
   */
  public AbstractStreamingParser(CSVReaderFormat format) {
    super();
    this.reader = format.makeReader();
    this.tokenizer = reader.getTokenizer();
  }

  @Override
  final public MultipleObjectsBundle parse(InputStream in) {
    this.initStream(in);
    return MultipleObjectsBundle.fromStream(this);
  }

  @Override
  public void initStream(InputStream in) {
    reader.reset(in);
  }

  @Override
  public boolean hasDBIDs() {
    return false;
  }

  @Override
  public boolean assignDBID(DBIDVar var) {
    var.unset();
    return false;
  }

  @Override
  public MultipleObjectsBundle asMultipleObjectsBundle() {
    return MultipleObjectsBundle.fromStream(this);
  }

  @Override
  public void cleanup() {
    try {
      reader.close();
    }
    catch(IOException e) {
      getLogger().exception(e);
    }
  }

  /**
   * Get the logger for this class.
   *
   * @return Logger.
   */
  protected abstract Logging getLogger();

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
    protected abstract AbstractStreamingParser makeInstance();
  }
}
