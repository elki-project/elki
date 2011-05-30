package de.lmu.ifi.dbs.elki.application;

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.data.images.ComputeColorHistogram;
import de.lmu.ifi.dbs.elki.data.images.ComputeNaiveRGBColorHistogram;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Application that computes the color histogram vector for a single image.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf ComputeColorHistogram
 * @apiviz.has File
 */
public class ComputeSingleColorHistogram extends AbstractApplication {
  /**
   * Class parameter for computing the color histogram.
   * <p>
   * Key: {@code -colorhist.generator}
   * </p>
   */
  public static OptionID COLORHIST_ID = OptionID.getOrCreateOptionID("colorhist.generator", "Class that is used to generate a color histogram.");

  /**
   * Parameter that specifies the name of the input file.
   * <p>
   * Key: {@code -colorhist.in}
   * </p>
   */

  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("colorhist.in", "Input image for color histograms.");

  /**
   * Class that will compute the actual histogram
   */
  private ComputeColorHistogram histogrammaker;

  /**
   * Input file.
   */
  private File inputFile;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param histogrammaker Class to compute histograms with
   * @param inputFile Input file
   */
  public ComputeSingleColorHistogram(boolean verbose, ComputeColorHistogram histogrammaker, File inputFile) {
    super(verbose);
    this.histogrammaker = histogrammaker;
    this.inputFile = inputFile;
  }

  @Override
  public void run() throws UnableToComplyException {
    double[] hist;
    try {
      hist = histogrammaker.computeColorHistogram(inputFile);
    }
    catch(IOException e) {
      throw new UnableToComplyException(e);
    }
    System.out.println(FormatUtil.format(hist, " "));
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
     * Class that will compute the actual histogram
     */
    private ComputeColorHistogram histogrammaker;

    /**
     * Input file.
     */
    private File inputFile;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<ComputeColorHistogram> colorhistP = new ObjectParameter<ComputeColorHistogram>(COLORHIST_ID, ComputeColorHistogram.class, ComputeNaiveRGBColorHistogram.class);
      if(config.grab(colorhistP)) {
        histogrammaker = colorhistP.instantiateClass(config);
      }
      final FileParameter inputP = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(inputP)) {
        inputFile = inputP.getValue();
      }
    }

    @Override
    protected ComputeSingleColorHistogram makeInstance() {
      return new ComputeSingleColorHistogram(verbose, histogrammaker, inputFile);
    }
  }

  /**
   * Main method to run this application.
   * 
   * @param args the arguments to run this application
   */
  public static void main(String[] args) {
    runCLIApplication(ComputeSingleColorHistogram.class, args);
  }
}