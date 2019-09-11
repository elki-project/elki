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
package elki.datasource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import elki.datasource.bundle.BundleReader;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.filter.ObjectFilter;
import elki.logging.Logging;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Class to load a database from a bundle file.
 *
 * Bundle files are stored in a compact binary format along with metadata, so
 * that parsing should be simpler, albeit the focus was on using it in on-disk
 * indexes.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @composed - - - BundleReader
 */
public class BundleDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BundleDatabaseConnection.class);

  /**
   * File to load.
   */
  private File infile;

  /**
   * Constructor.
   *
   * @param filters Filters
   * @param infile Input file
   */
  public BundleDatabaseConnection(List<? extends ObjectFilter> filters, File infile) {
    super(filters);
    this.infile = infile;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    try {
      FileInputStream fis = new FileInputStream(infile);
      FileChannel channel = fis.getChannel();
      MultipleObjectsBundle bundle = invokeStreamFilters(new BundleReader(channel)).asMultipleObjectsBundle();
      channel.close();
      fis.close();
      return bundle;
    }
    catch(IOException e) {
      throw new AbortException("IO error loading bundle", e);
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
  public static class Par extends AbstractDatabaseConnection.Par {
    /**
     * Option ID for the bundle parameter.
     */
    private static final OptionID BUNDLE_ID = new OptionID("bundle.input", "Bundle file to load the data from.");

    /**
     * File to load.
     */
    private File infile;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      configFilters(config);
      new FileParameter(BUNDLE_ID, FileParameter.FileType.INPUT_FILE) //
          .grab(config, x -> infile = x);
    }

    @Override
    public BundleDatabaseConnection make() {
      return new BundleDatabaseConnection(filters, infile);
    }
  }
}
