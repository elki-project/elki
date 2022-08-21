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
package elki.application;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import elki.datasource.DatabaseConnection;
import elki.datasource.FileBasedDatabaseConnection;
import elki.datasource.bundle.BundleWriter;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.logging.Logging;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Convert an input file to the more efficient ELKI bundle format.
 *
 * @author Erich Schubert
 * @since 0.5.5
 */
public class ConvertToBundleApplication extends AbstractApplication {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(ConvertToBundleApplication.class);

  /**
   * The data input step.
   */
  private DatabaseConnection input;

  /**
   * Output filename.
   */
  private Path outfile;

  /**
   * Constructor.
   *
   * @param input Data source configuration
   * @param outfile Output filename
   */
  public ConvertToBundleApplication(DatabaseConnection input, Path outfile) {
    super();
    this.input = input;
    this.outfile = outfile;
  }

  @Override
  public void run() {
    if(LOG.isVerbose()) {
      LOG.verbose("Loading data.");
    }
    MultipleObjectsBundle bundle = input.loadData();
    if(LOG.isVerbose()) {
      LOG.verbose("Serializing to output file: " + outfile.toString());
    }
    // TODO: make configurable?
    try (FileChannel channel = FileChannel.open(outfile, //
        StandardOpenOption.WRITE)) {
      new BundleWriter().writeBundleStream(bundle.asStream(), channel);
    }
    catch(IOException e) {
      LOG.exception("IO Error", e);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par extends AbstractApplication.Par {
    /**
     * Option to specify the data source for the database.
     */
    public static final OptionID DATABASE_CONNECTION_ID = new OptionID("dbc", "Database connection class.");

    /**
     * The data input step.
     */
    private DatabaseConnection input;

    /**
     * Output filename.
     */
    private Path outfile;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<DatabaseConnection>(DATABASE_CONNECTION_ID, DatabaseConnection.class, FileBasedDatabaseConnection.class) //
          .grab(config, x -> input = x);
      outfile = super.getParameterOutputFile(config, "File name to serialize the bundle to.");
    }

    @Override
    public ConvertToBundleApplication make() {
      return new ConvertToBundleApplication(input, outfile);
    }
  }

  /**
   * Run command line application.
   *
   * @param args Command line parameters
   */
  public static void main(String[] args) {
    runCLIApplication(ConvertToBundleApplication.class, args);
  }
}
