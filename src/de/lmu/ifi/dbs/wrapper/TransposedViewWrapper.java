package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This wrapper class reads s data file and writes the transposed view
 * of the data file to the specified output. Additionmally a script file for gnuplot
 * is written.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class TransposedViewWrapper extends AbstractWrapper {
  /**
   * Parameter for gnuplot output directory.
   */
  public static final String GNUPLOT_P = "gnu";

  /**
   * Description for parameter gnu.
   */
  public static final String GNUPLOT_D = "<filename>file to write the gnuplot script in.";

  /**
   * The output string for the gnuplot script.
   */
  String gnuplot;

  /**
   * Initializes the option handler.
   */
  public TransposedViewWrapper() {
    super();
    parameterToDescription.put(GNUPLOT_P + OptionHandler.EXPECTS_VALUE, GNUPLOT_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see Wrapper#run(String[])
   */
  public void run(String[] args) {
    this.setParameters(args);

    try {
      File outFile = new File(output);
      PrintStream out = new PrintStream(new FileOutputStream(outFile));

      // parse the data
      FileBasedDatabaseConnection<DoubleVector> dbConnection = new FileBasedDatabaseConnection<DoubleVector>();

      ArrayList<String> params = getRemainingParameters();
      params.add(FileBasedDatabaseConnection.PARSER_P);
      params.add(DoubleVectorLabelParser.class.getName());
      params.add(FileBasedDatabaseConnection.INPUT_P);
      params.add(input);
      dbConnection.setParameters(params.toArray(new String[params.size()]));

      Database<DoubleVector> db = dbConnection.getDatabase(null);

      // transpose the data
      double[][] transposed = new double[db.dimensionality()][db.size()];

      for (int i = 0; i < db.dimensionality(); i++) {
        Iterator<Integer> it = db.iterator();
        int j = 0;
        while (it.hasNext()) {
          Integer id = it.next();
          DoubleVector o = db.get(id);
          transposed[i][j++] = o.getValue(i + 1);
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
      e.printStackTrace();
    }

  }

  /**
   * Sets the parameter gnu additionally to the parameters set
   * by the super-class' method. Parameter gnu is a required
   * parameter.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    super.setParameters(args);
    try {
      gnuplot = optionHandler.getOptionValue(GNUPLOT_P);
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return new String[0];
  }

  public static void main(String[] args) {
    TransposedViewWrapper wrapper = new TransposedViewWrapper();
    try {
      wrapper.run(args);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
