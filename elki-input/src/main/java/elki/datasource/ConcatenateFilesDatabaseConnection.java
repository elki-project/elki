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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import elki.data.type.TypeUtil;
import elki.datasource.bundle.BundleMeta;
import elki.datasource.bundle.BundleStreamSource;
import elki.datasource.bundle.BundleStreamSource.Event;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.filter.ObjectFilter;
import elki.datasource.parser.NumberVectorLabelParser;
import elki.datasource.parser.Parser;
import elki.datasource.parser.StreamingParser;
import elki.logging.Logging;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.FileUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileListParameter;
import elki.utilities.optionhandling.parameters.FileListParameter.FilesType;

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
  private List<URI> files;

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
  public ConcatenateFilesDatabaseConnection(List<URI> files, Parser parser, List<? extends ObjectFilter> filters) {
    super(filters);
    this.files = files;
    this.parser = parser;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    MultipleObjectsBundle objects = new MultipleObjectsBundle();
    objects.appendColumn(TypeUtil.STRING, new ArrayList<>());
    for(URI file : files) {
      String filestr = file.toString();
      try (InputStream inputStream = FileUtil.open(file)) {
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
            if(meta == null) {
              throw new IllegalStateException("Data without metadata in stream " + source.toString());
            }
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
  public static class Par extends AbstractDatabaseConnection.Par {
    /**
     * Parameter that specifies the name of the input files to be parsed.
     */
    public static final OptionID INPUT_ID = FileBasedDatabaseConnection.Par.INPUT_ID;

    /**
     * The input files.
     */
    private List<URI> files;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new FileListParameter(INPUT_ID, FilesType.INPUT_FILES) //
          .grab(config, x -> files = x);
      configFilters(config);
      configParser(config, Parser.class, NumberVectorLabelParser.class);
    }

    @Override
    public ConcatenateFilesDatabaseConnection make() {
      return new ConcatenateFilesDatabaseConnection(files, parser, filters);
    }
  }
}
