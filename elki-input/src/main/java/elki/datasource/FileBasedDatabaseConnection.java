/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.datasource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;

import elki.datasource.filter.ObjectFilter;
import elki.datasource.parser.ArffParser;
import elki.datasource.parser.NumberVectorLabelParser;
import elki.datasource.parser.Parser;
import elki.utilities.Priority;
import elki.utilities.io.FileUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * File based database connection based on the parser to be set.
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @opt nodefillcolor LemonChiffon
 */
@Priority(Priority.IMPORTANT)
public class FileBasedDatabaseConnection extends InputStreamDatabaseConnection {
  /**
   * Constructor.
   * 
   * @param filters Filters, can be null
   * @param parser the parser to provide a database
   * @param infile File to load the data from
   */
  public FileBasedDatabaseConnection(List<? extends ObjectFilter> filters, Parser parser, URI infile) {
    super(() -> {
      try {
        return new BufferedInputStream(FileUtil.open(infile));
      }
      catch(IOException e) {
        throw new UncheckedIOException("Could not load input file: " + infile, e);
      }
    }, filters, parser);
  }

  /**
   * Constructor.
   * 
   * @param filters Filters, can be null
   * @param parser the parser to provide a database
   * @param infile File to load the data from
   */
  public FileBasedDatabaseConnection(List<? extends ObjectFilter> filters, Parser parser, String infile) {
    this(filters, parser, URI.create(infile));
  }

  /**
   * Constructor.
   * 
   * @param filters Filters, can be null
   * @param parser the parser to provide a database
   * @param in Input stream
   */
  public FileBasedDatabaseConnection(List<? extends ObjectFilter> filters, Parser parser, InputStream in) {
    super(in, filters, parser);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par extends InputStreamDatabaseConnection.Par {
    /**
     * Parameter that specifies the name of the input file to be parsed.
     */
    public static final OptionID INPUT_ID = new OptionID("dbc.in", "The name of the input file to be parsed.");

    /**
     * Input stream to process.
     */
    protected URI infile;

    @Override
    public void configure(Parameterization config) {
      // Add the input file first, for usability reasons.
      new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE) //
          .grab(config, x -> infile = x);
      Class<? extends Parser> defaultParser = NumberVectorLabelParser.class;
      if(infile != null && (infile.toString().endsWith(".arff") || infile.toString().endsWith(".arff.gz"))) {
        defaultParser = ArffParser.class;
      }
      configParser(config, Parser.class, defaultParser);
      configFilters(config);
    }

    @Override
    public FileBasedDatabaseConnection make() {
      return new FileBasedDatabaseConnection(filters, parser, infile);
    }
  }
}
