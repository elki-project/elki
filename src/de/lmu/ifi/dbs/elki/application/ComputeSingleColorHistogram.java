package de.lmu.ifi.dbs.elki.application;

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.data.images.ComputeColorHistogram;
import de.lmu.ifi.dbs.elki.data.images.ComputeNaiveRGBColorHistogram;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;

/**
 * Application that computes the color histogram vector for a single image.
 * 
 * @author Erich Schubert
 */
public class ComputeSingleColorHistogram extends AbstractApplication {
  /**
   * Option id to use for computing the histogram. See {@link #COLORHIST_PARAM}
   */
  public static OptionID COLORHIST_ID = OptionID.getOrCreateOptionID("colorhist.generator", "Class that is used to generate a color histogram.");

  /**
   * Class parameter for computing the color histogram.
   * <p>
   * Key: {@code -colorhist.generator}
   * </p>
   */
  private ObjectParameter<ComputeColorHistogram> COLORHIST_PARAM = new ObjectParameter<ComputeColorHistogram>(COLORHIST_ID, ComputeColorHistogram.class, ComputeNaiveRGBColorHistogram.class);

  /**
   * OptionID for {@link #INPUT_PARAM}
   */
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("colorhist.in", "Input image for color histograms.");

  /**
   * Parameter that specifies the name of the input file.
   * <p>
   * Key: {@code -app.in}
   * </p>
   */
  private final FileParameter INPUT_PARAM = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);

  /**
   * Class that will compute the actual histogram
   */
  private ComputeColorHistogram histogrammaker;

  /**
   * Input file.
   */
  private File inputFile;

  public ComputeSingleColorHistogram(Parameterization config) {
    super(config);
    if(config.grab(COLORHIST_PARAM)) {
      histogrammaker = COLORHIST_PARAM.instantiateClass(config);
    }
    if(config.grab(INPUT_PARAM)) {
      inputFile = INPUT_PARAM.getValue();
    }
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
   * Main method to run this wrapper.
   * 
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    runCLIApplication(ComputeSingleColorHistogram.class, args);
  }
}