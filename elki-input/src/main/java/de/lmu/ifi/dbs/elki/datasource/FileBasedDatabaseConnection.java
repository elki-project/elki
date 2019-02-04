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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.ArffParser;
import de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser;
import de.lmu.ifi.dbs.elki.datasource.parser.Parser;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * File based database connection based on the parser to be set.
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @opt nodefillcolor LemonChiffon
 */
@Alias("de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection")
@Priority(Priority.IMPORTANT)
public class FileBasedDatabaseConnection extends InputStreamDatabaseConnection {
  /**
   * Constructor.
   * 
   * @param filters Filters, can be null
   * @param parser the parser to provide a database
   * @param infile File to load the data from
   */
  public FileBasedDatabaseConnection(List<ObjectFilter> filters, Parser parser, File infile) {
    super(() -> {
      try {
        return new BufferedInputStream(FileUtil.tryGzipInput(new FileInputStream(infile)));
      }
      catch(IOException e) {
        throw new AbortException("Could not load input file: " + infile, e);
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
  public FileBasedDatabaseConnection(List<ObjectFilter> filters, Parser parser, String infile) {
    super(() -> {
      try {
        return new BufferedInputStream(FileUtil.tryGzipInput(new FileInputStream(infile)));
      }
      catch(IOException e) {
        throw new AbortException("Could not load input file: " + infile, e);
      }
    }, filters, parser);
  }

  /**
   * Constructor.
   * 
   * @param filters Filters, can be null
   * @param parser the parser to provide a database
   * @param in Input stream
   */
  public FileBasedDatabaseConnection(List<ObjectFilter> filters, Parser parser, InputStream in) {
    super(in, filters, parser);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends InputStreamDatabaseConnection.Parameterizer {
    /**
     * Parameter that specifies the name of the input file to be parsed.
     */
    public static final OptionID INPUT_ID = new OptionID("dbc.in", "The name of the input file to be parsed.");

    /**
     * Input stream to process.
     */
    protected File infile;

    @Override
    protected void makeOptions(Parameterization config) {
      Class<? extends Parser> defaultParser = NumberVectorLabelParser.class;
      // Add the input file first, for usability reasons.
      final FileParameter inputParam = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(inputParam)) {
        infile = inputParam.getValue();
        String nam = infile.getName();
        if(nam != null && (nam.endsWith(".arff") || nam.endsWith(".arff.gz"))) {
          defaultParser = ArffParser.class;
        }
      }
      configParser(config, Parser.class, defaultParser);
      configFilters(config);
    }

    @Override
    protected FileBasedDatabaseConnection makeInstance() {
      return new FileBasedDatabaseConnection(filters, parser, infile);
    }
  }
}
