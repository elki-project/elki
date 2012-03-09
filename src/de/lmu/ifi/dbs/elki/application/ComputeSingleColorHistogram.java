package de.lmu.ifi.dbs.elki.application;

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
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("colorhist.in", "Input image file for color histogram.");

  /**
   * Parameter that specifies the name of the mask input file.
   * <p>
   * Key: {@code -colorhist.mask}
   * </p>
   */
  public static final OptionID MASK_ID = OptionID.getOrCreateOptionID("colorhist.mask", "Input mask image file.");

  /**
   * Class that will compute the actual histogram
   */
  private ComputeColorHistogram histogrammaker;

  /**
   * Input file.
   */
  private File inputFile;

  /**
   * Mask file.
   */
  private File maskFile;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param histogrammaker Class to compute histograms with
   * @param inputFile Input file
   * @param maskFile Mask file
   */
  public ComputeSingleColorHistogram(boolean verbose, ComputeColorHistogram histogrammaker, File inputFile, File maskFile) {
    super(verbose);
    this.histogrammaker = histogrammaker;
    this.inputFile = inputFile;
    this.maskFile = maskFile;
  }

  @Override
  public void run() throws UnableToComplyException {
    double[] hist;
    try {
      hist = histogrammaker.computeColorHistogram(inputFile, maskFile);
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

    /**
     * Mask file.
     */
    private File maskFile;

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
      final FileParameter maskP = new FileParameter(MASK_ID, FileParameter.FileType.INPUT_FILE, true);
      if(config.grab(maskP)) {
        maskFile = maskP.getValue();
      }
    }

    @Override
    protected ComputeSingleColorHistogram makeInstance() {
      return new ComputeSingleColorHistogram(verbose, histogrammaker, inputFile, maskFile);
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