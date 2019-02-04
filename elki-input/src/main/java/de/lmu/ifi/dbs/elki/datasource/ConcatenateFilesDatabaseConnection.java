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
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource.Event;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser;
import de.lmu.ifi.dbs.elki.datasource.parser.Parser;
import de.lmu.ifi.dbs.elki.datasource.parser.StreamingParser;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileListParameter.FilesType;

/**
 * Database that will loading multiple files, concatenating the results.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ConcatenateFilesDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ConcatenateFilesDatabaseConnection.class);

  /**
   * Input file list.
   */
  private List<File> files;

  /**
   * The parser.
   */
  private Parser parser;

  /**
   * Constructor.
   * 
   * @param files Input files
   * @param parser Parser
   * @param filters Filters
   */
  public ConcatenateFilesDatabaseConnection(List<File> files, Parser parser, List<ObjectFilter> filters) {
    super(filters);
    this.files = files;
    this.parser = parser;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    MultipleObjectsBundle objects = new MultipleObjectsBundle();
    objects.appendColumn(TypeUtil.STRING, new ArrayList<>());
    for(File file : files) {
      String filestr = file.getPath();
      try (FileInputStream fis = new FileInputStream(file); //
          InputStream inputStream = FileUtil.tryGzipInput(new BufferedInputStream(fis))) {
        final BundleStreamSource source;
        if(parser instanceof StreamingParser) {
          final StreamingParser streamParser = (StreamingParser) parser;
          streamParser.initStream(inputStream);
          source = streamParser;
        }
        else {
          MultipleObjectsBundle parsingResult = parser.parse(inputStream);
          // normalize objects and transform labels
          source = parsingResult.asStream();
        }
        BundleMeta meta = null; // NullPointerException on invalid streams
        loop: for(Event e = source.nextEvent();; e = source.nextEvent()) {
          switch(e){
          case END_OF_STREAM:
            break loop;
          case META_CHANGED:
            meta = source.getMeta();
            for(int i = 0; i < meta.size(); i++) {
              if(i + 1 >= objects.metaLength()) {
                objects.appendColumn(meta.get(i), new ArrayList<>());
              }
              else {
                // Ensure compatibility:
                if(!objects.meta(i + 1).isAssignableFromType(meta.get(i))) {
                  throw new AbortException("Incompatible files loaded. Cannot concatenate with unaligned columns, please preprocess manually.");
                }
              }
            }
            break; // switch
          case NEXT_OBJECT:
            Object[] o = new Object[objects.metaLength()];
            o[0] = filestr;
            for(int i = 0; i < meta.size(); i++) {
              o[i + 1] = source.data(i);
            }
            objects.appendSimple(o);
            break; // switch
          }
        }
      }
      catch(IOException e) {
        throw new AbortException("Loading file " + filestr + " failed: " + e.toString(), e);
      }
    }
    parser.cleanup();
    // Invoke filters
    if(LOG.isDebugging()) {
      LOG.debugFine("Invoking filters.");
    }
    return invokeBundleFilters(objects);
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
     * Parameter that specifies the name of the input files to be parsed.
     */
    public static final OptionID INPUT_ID = FileBasedDatabaseConnection.Parameterizer.INPUT_ID;

    /**
     * The input files.
     */
    private List<File> files;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileListParameter filesP = new FileListParameter(INPUT_ID, FilesType.INPUT_FILES);
      if(config.grab(filesP)) {
        files = filesP.getValue();
      }
      configFilters(config);
      configParser(config, Parser.class, NumberVectorLabelParser.class);
    }

    @Override
    protected ConcatenateFilesDatabaseConnection makeInstance() {
      return new ConcatenateFilesDatabaseConnection(files, parser, filters);
    }
  }
}
