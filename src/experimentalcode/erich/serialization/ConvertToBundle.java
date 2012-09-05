package experimentalcode.erich.serialization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.StreamFromBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Convert an input file to the more efficient ELKI bundle format.
 * 
 * @author Erich Schubert
 */
public class ConvertToBundle extends AbstractApplication {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(ConvertToBundle.class);

  /**
   * The data input step.
   */
  private DatabaseConnection input;

  /**
   * Output filename.
   */
  private File outfile;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param input Data source configuration
   * @param outfile Output filename
   */
  public ConvertToBundle(boolean verbose, DatabaseConnection input, File outfile) {
    super(verbose);
    this.input = input;
    this.outfile = outfile;
  }

  @Override
  public void run() throws UnableToComplyException {
    if (LOG.isVerbose()) {
      LOG.verbose("Loading data.");
    }
    MultipleObjectsBundle bundle = input.loadData();
    if (LOG.isVerbose()) {
      LOG.verbose("Serializing to output file: " + outfile.toString());
    }
    // TODO: make configurable?
    BundleWriter writer = new BundleWriter();
    try {
      FileOutputStream fos = new FileOutputStream(outfile);
      FileChannel channel = fos.getChannel();
      writer.writeBundleStream(new StreamFromBundle(bundle), channel);
      channel.close();
      fos.close();
    } catch (IOException e) {
      LOG.exception("IO Error", e);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * The data input step.
     */
    private DatabaseConnection input;

    /**
     * Output filename.
     */
    private File outfile;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DatabaseConnection> inputP = new ObjectParameter<DatabaseConnection>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class);
      if (config.grab(inputP)) {
        input = inputP.instantiateClass(config);
      }
      outfile = super.getParameterOutputFile(config, "File name to serialize the bundle to.");
    }

    @Override
    protected ConvertToBundle makeInstance() {
      return new ConvertToBundle(verbose, input, outfile);
    }
  }

  /**
   * Run command line application.
   * 
   * @param args Command line parameters
   */
  public static void main(String[] args) {
    runCLIApplication(ConvertToBundle.class, args);
  }
}
