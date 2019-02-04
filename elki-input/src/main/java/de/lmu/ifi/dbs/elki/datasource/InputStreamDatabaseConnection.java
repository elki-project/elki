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
package de.lmu.ifi.dbs.elki.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser;
import de.lmu.ifi.dbs.elki.datasource.parser.Parser;
import de.lmu.ifi.dbs.elki.datasource.parser.StreamingParser;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Database connection expecting input from an input stream such as stdin.
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @navassoc - runs - Parser
 */
@Title("Input-Stream based database connection")
@Description("Parse an input stream such as STDIN into a database.")
public class InputStreamDatabaseConnection extends AbstractDatabaseConnection implements AutoCloseable {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(InputStreamDatabaseConnection.class);

  /**
   * Holds the instance of the parser.
   */
  Parser parser;

  /**
   * The input stream to parse from.
   */
  Supplier<InputStream> in = () -> System.in;

  /**
   * The stream that we are processing.
   */
  InputStream ins;

  /**
   * Constructor.
   * 
   * @param in Input stream opener
   * @param filters Filters to use
   * @param parser the parser to provide a database
   */
  public InputStreamDatabaseConnection(Supplier<InputStream> in, List<ObjectFilter> filters, Parser parser) {
    super(filters);
    this.in = in;
    this.parser = parser;
  }

  /**
   * Constructor.
   * 
   * @param ins Input stream to process
   * @param filters Filters to use
   * @param parser the parser to provide a database
   */
  public InputStreamDatabaseConnection(InputStream ins, List<ObjectFilter> filters, Parser parser) {
    super(filters);
    this.ins = ins;
    this.parser = parser;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    // Run parser
    if(LOG.isDebugging()) {
      LOG.debugFine("Invoking parsers.");
    }
    // Streaming parsers may yield to stream filters immediately.
    if(parser instanceof StreamingParser) {
      final StreamingParser streamParser = (StreamingParser) parser;
      ins = ins != null ? ins : in.get();
      streamParser.initStream(ins);
      // normalize objects and transform labels
      if(LOG.isDebugging()) {
        LOG.debugFine("Parsing as stream.");
      }
      Duration duration = LOG.isStatistics() ? LOG.newDuration(this.getClass().getName() + ".load").begin() : null;
      MultipleObjectsBundle objects = invokeStreamFilters(streamParser).asMultipleObjectsBundle();
      parser.cleanup();
      if(duration != null) {
        LOG.statistics(duration.end());
      }
      return objects;
    }
    else {
      // For non-streaming parsers, we first parse, then filter
      Duration duration = LOG.isStatistics() ? LOG.newDuration(this.getClass().getName() + ".parse").begin() : null;
      ins = ins != null ? ins : in.get();
      MultipleObjectsBundle parsingResult = parser.parse(ins);
      parser.cleanup();
      if(duration != null) {
        LOG.statistics(duration.end());
      }

      // normalize objects and transform labels
      if(LOG.isDebugging()) {
        LOG.debugFine("Invoking filters.");
      }
      Duration fduration = LOG.isStatistics() ? LOG.newDuration(this.getClass().getName() + ".filter").begin() : null;
      MultipleObjectsBundle objects = invokeBundleFilters(parsingResult);
      if(fduration != null) {
        LOG.statistics(fduration.end());
      }
      return objects;
    }
  }

  @Override
  public void close() throws IOException {
    if(ins != null) {
      ins.close();
      ins = null;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDatabaseConnection.Parameterizer {
    /**
     * Input stream to read.
     * <p>
     * Note that this parameter is not used by the subclass
     * {@link FileBasedDatabaseConnection}. It is mostly useful with the
     * Parameterization API from java.
     */
    public static final OptionID STREAM_ID = new OptionID("dbc.inputstream", "Input stream to read. Defaults to standard input.");

    /**
     * Input stream.
     */
    protected InputStream instream;

    @Override
    protected void makeOptions(Parameterization config) {
      ObjectParameter<InputStream> inP = new ObjectParameter<>(STREAM_ID, InputStream.class, System.in);
      if(config.grab(inP)) {
        instream = inP.instantiateClass(config);
      }
      configParser(config, Parser.class, NumberVectorLabelParser.class);
      configFilters(config);
    }

    @Override
    protected InputStreamDatabaseConnection makeInstance() {
      return new InputStreamDatabaseConnection(instream, filters, parser);
    }
  }
}
