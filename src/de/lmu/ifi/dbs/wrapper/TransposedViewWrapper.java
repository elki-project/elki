package de.lmu.ifi.dbs.wrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * This wrapper class reads s data file and writes the transposed view of the
 * data file to the specified output. Additionmally a script file for gnuplot is
 * written.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class TransposedViewWrapper extends StandAloneInputWrapper {
//  /**
//   * Holds the class specific debug status.
//   */
//  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
////  private static final boolean DEBUG = true;
//
//  /**
//   * The logger of this class.
//   */
//  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Parameter for gnuplot output directory.
   */
  public static final String GNUPLOT_P = "gnu";

  /**
   * Description for parameter gnu.
   */
  public static final String GNUPLOT_D = "<filename>file to write the gnuplot script in.";

  /**
   * The filename to write the gnuplot script in.
   */
  private String gnuplot;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    TransposedViewWrapper wrapper = new TransposedViewWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
//      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
    	wrapper.verbose(e.getMessage());
//      wrapper.logger.info(e.getMessage());
    }
    catch (Exception e) {
    	wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
//      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Initializes the option handler.
   */
  public TransposedViewWrapper() {
    super();
    parameterToDescription.put(GNUPLOT_P + OptionHandler.EXPECTS_VALUE, GNUPLOT_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see Wrapper#run()
   */
  public void run() throws UnableToComplyException {
    try {
      File outFile = new File(getOutput());
      PrintStream out = new PrintStream(new FileOutputStream(outFile));

      // parse the data
      FileBasedDatabaseConnection<RealVector> dbConnection = new FileBasedDatabaseConnection<RealVector>();

      List<String> dbParameters = getRemainingParameters();
      dbParameters.add(FileBasedDatabaseConnection.PARSER_P);
      dbParameters.add(RealVectorLabelParser.class.getName());
      dbParameters.add(FileBasedDatabaseConnection.INPUT_P);
      dbParameters.add(getInput());
      dbConnection.setParameters(dbParameters.toArray(new String[dbParameters.size()]));

      Database<RealVector> db = dbConnection.getDatabase(null);

      // transpose the data
      double[][] transposed = new double[db.dimensionality()][db.size()];

      for (int i = 0; i < db.dimensionality(); i++) {
        Iterator<Integer> it = db.iterator();
        int j = 0;
        while (it.hasNext()) {
          Integer id = it.next();
          RealVector o = db.get(id);
          transposed[i][j++] = o.getValue(i + 1).doubleValue();
        }
      }

      // write to output
      for (double[] v : transposed) {
        for (double value : v) {
          out.print(value + " ");
        }
        out.println();
      }
      out.flush();
      out.close();

      // write gnuplot script
      String gnuplotScript = Util.transposedGnuplotScript(outFile.getName(), db.dimensionality(), db.size());
      PrintStream gnuplotOut = new PrintStream(new FileOutputStream(new File(gnuplot)));
      gnuplotOut.print(gnuplotScript);
      gnuplotOut.flush();
      gnuplotOut.close();

    }
    catch (FileNotFoundException e) {
      throw new UnableToComplyException(e);
    }
    catch (ParameterException e) {
      e.printStackTrace();
      throw new UnableToComplyException(e);
    }

  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // gnuplot
    gnuplot = optionHandler.getOptionValue(GNUPLOT_P);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(GNUPLOT_P, gnuplot);
    return settings;
  }
}
