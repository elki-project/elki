package de.lmu.ifi.dbs.elki.wrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * This wrapper class reads s data file and writes the transposed view of the
 * data file to the specified output. Additionally a script file for gnuplot is
 * written.
 * 
 * @author Elke Achtert
 * @param <V> vector type
 */
public class TransposedViewWrapper<V extends RealVector<V, ?>> extends StandAloneInputWrapper {
  /**
   * OptionID for {@link #GNUPLOT_PARAM}
   */
  public static final OptionID GNUPLOT_ID = OptionID.getOrCreateOptionID(
      "out.gnu", "file to write the gnuplot script in.");
  
  private final FileParameter GNUPLOT_PARAM = new FileParameter(GNUPLOT_ID, FileParameter.FileType.OUTPUT_FILE);

  /**
   * The filename to write the gnuplot script in.
   */
  private String gnuplot;

  /**
   * Main method to run this wrapper.
   * 
   * @param args the arguments to run this wrapper
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    new TransposedViewWrapper().runCLIWrapper(args);
  }

  /**
   * Adds parameter {@link #GNUPLOT_PARAM} to the option handler additionally to
   * parameters of super class.
   */
  public TransposedViewWrapper() {
    super();
    addOption(GNUPLOT_PARAM);
  }

  public void run() throws UnableToComplyException {
    try {
      File outFile = getOutput();
      PrintStream out = new PrintStream(new FileOutputStream(outFile));

      // parse the data
      FileBasedDatabaseConnection<V> dbConnection = new FileBasedDatabaseConnection<V>();

      List<String> dbParameters = getRemainingParameters();
      OptionUtil.addParameter(dbParameters, FileBasedDatabaseConnection.PARSER_ID, RealVectorLabelParser.class.getName());
      OptionUtil.addParameter(dbParameters, FileBasedDatabaseConnection.INPUT_ID, getInput().getPath());
      dbConnection.setParameters(dbParameters.toArray(new String[dbParameters.size()]));

      Database<V> db = dbConnection.getDatabase(null);

      // transpose the data
      double[][] transposed = new double[db.dimensionality()][db.size()];

      for(int i = 0; i < db.dimensionality(); i++) {
        Iterator<Integer> it = db.iterator();
        int j = 0;
        while(it.hasNext()) {
          Integer id = it.next();
          V o = db.get(id);
          transposed[i][j++] = o.getValue(i + 1).doubleValue();
        }
      }

      // write to output
      for(double[] v : transposed) {
        for(double value : v) {
          out.print(value + " ");
        }
        out.println();
      }
      out.flush();
      out.close();

      // write gnuplot script
      String gnuplotScript = transposedGnuplotScript(outFile.getName(), db.dimensionality(), db.size());
      PrintStream gnuplotOut = new PrintStream(new FileOutputStream(new File(gnuplot)));
      gnuplotOut.print(gnuplotScript);
      gnuplotOut.flush();
      gnuplotOut.close();

    }
    catch(FileNotFoundException e) {
      throw new UnableToComplyException(e);
    }
    catch(ParameterException e) {
      throw new UnableToComplyException(e);
    }
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    gnuplot = GNUPLOT_PARAM.getValue().getPath();

    return remainingParameters;
  }

  /**
   * Returns the description for the input parameter.
   * 
   * @return the description for the input parameter
   */
  @Override
  public String getInputDescription() {
    return "The name of the input file.";
  }

  /**
   * Returns the description for the output parameter.
   * 
   * @return the description for the output parameter
   */
  @Override
  public String getOutputDescription() {
    return "The name of the output file.";
  }

  /**
   * Provides a script-text for a gnuplot script to use for transposed view of a
   * specific file of given size of data set.
   * 
   * @param filename the filename of the transposed file to be plotted
   * @param datasetSize the size of the transposed data set
   * @param dimensionality the dimensionality of the transposed data set
   * @return a script-text for a gnuplot script to use for transposed view of a
   *         specific file of given size of data set
   */
  public static String transposedGnuplotScript(String filename, int datasetSize, int dimensionality) {
    StringBuffer script = new StringBuffer();
    // script.append("set terminal pbm color;\n");
    script.append("set nokey\n");
    script.append("set data style linespoints\n");
    script.append("set xlabel \"attribute\"\n");
    script.append("show xlabel\n");
    script.append("plot [0:");
    script.append(datasetSize - 1).append("] []");
    for(int p = 1; p <= dimensionality; p++) {
      script.append("\"").append(filename).append("\" using ").append(p);
      if(p < dimensionality) {
        script.append(", ");
      }
    }
    script.append("\npause -1");
    return script.toString();
  }
}
