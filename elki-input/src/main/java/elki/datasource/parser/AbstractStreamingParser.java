/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.datasource.parser;

import java.io.IOException;
import java.io.InputStream;

import elki.database.ids.DBIDVar;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.logging.Logging;
import elki.utilities.io.TokenizedReader;
import elki.utilities.io.Tokenizer;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Base class for streaming parsers.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @assoc - - - CSVReaderFormat
 * @composed - - - TokenizedReader
 * @composed - - - Tokenizer
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
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Reader format.
     */
    protected CSVReaderFormat format;

    @Override
    public void configure(Parameterization config) {
      format = config.tryInstantiate(CSVReaderFormat.class);
    }

    @Override
    public abstract AbstractStreamingParser make();
  }
}
