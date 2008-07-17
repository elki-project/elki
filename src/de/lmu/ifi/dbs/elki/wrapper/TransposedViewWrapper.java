package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * This wrapper class reads s data file and writes the transposed view of the
 * data file to the specified output. Additionmally a script file for gnuplot is
 * written.
 *
 * @author Elke Achtert
 * todo parameter
 */
public class TransposedViewWrapper<V extends RealVector<V, ?>> extends StandAloneInputWrapper {

    /**
     * Parameter for gnuplot output directory.
     */
    public static final String GNUPLOT_P = "gnu";

    /**
     * Description for parameter gnu.
     */
    public static final String GNUPLOT_D = "file to write the gnuplot script in.";

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
        TransposedViewWrapper<?> wrapper = new TransposedViewWrapper();
        try {
            wrapper.setParameters(args);
            wrapper.run();
        }
        catch (ParameterException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
        }
        catch (AbortException e) {
            wrapper.verbose(e.getMessage());
        }
        catch (Exception e) {
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
        }
    }

    /**
     * Adds parameter
     * {@link #} todo 
     * to the option handler additionally to parameters of super class.
     */
    public TransposedViewWrapper() {
        super();
        optionHandler.put(new FileParameter(GNUPLOT_P, GNUPLOT_D, FileParameter.FileType.OUTPUT_FILE));
    }

    /**
     * @see Wrapper#run()
     */
    public void run() throws UnableToComplyException {
        try {
            File outFile = getOutput();
            PrintStream out = new PrintStream(new FileOutputStream(outFile));

            // parse the data
            FileBasedDatabaseConnection<V> dbConnection = new FileBasedDatabaseConnection<V>();

            List<String> dbParameters = getRemainingParameters();
            dbParameters.add(FileBasedDatabaseConnection.PARSER_P);
            dbParameters.add(RealVectorLabelParser.class.getName());
            dbParameters.add(FileBasedDatabaseConnection.INPUT_P);
            dbParameters.add(getInput().getPath());
            dbConnection.setParameters(dbParameters.toArray(new String[dbParameters.size()]));

            Database<V> db = dbConnection.getDatabase(null);

            // transpose the data
            double[][] transposed = new double[db.dimensionality()][db.size()];

            for (int i = 0; i < db.dimensionality(); i++) {
                Iterator<Integer> it = db.iterator();
                int j = 0;
                while (it.hasNext()) {
                    Integer id = it.next();
                    V o = db.get(id);
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
            throw new UnableToComplyException(e);
        }
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        // gnuplot
        gnuplot = ((File) optionHandler.getOptionValue(GNUPLOT_P)).getPath();

        return remainingParameters;
    }


    /**
     * Returns the description for the input parameter.
     *
     * @return the description for the input parameter
     */
    public String getInputDescription() {
        return "The name of the input file.";
    }

    /**
     * Returns the description for the output parameter.
     *
     * @return the description for the output parameter
     */
    public String getOutputDescription() {
        return "The name of the output file.";
    }
}
