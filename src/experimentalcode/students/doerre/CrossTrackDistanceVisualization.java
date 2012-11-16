package experimentalcode.students.doerre;

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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.GeoUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Visualization function for Cross-track distance function
 * 
 * TODO: make resolution configurable.
 * 
 * TODO: make start and end configurable.
 * 
 * @author Niels Dörre
 * @author Erich Schubert
 */
public class CrossTrackDistanceVisualization extends AbstractApplication {
  /**
   * Get a logger for this class.
   */
  private final static Logging LOG = Logging.getLogger(CrossTrackDistanceVisualization.class);

  /**
   * Visualization mode.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static enum Mode {
    /** Cross track distance */
    CTD,
    /** Along track distance */
    ATD,
  }

  /**
   * Holds the file to print results to.
   */
  private File out;

  /**
   * Image size.
   */
  final int width = 2000, height = 1000;

  /**
   * Number of steps for shades.
   */
  protected int steps = 10;

  /**
   * Visualiaztion mode
   */
  private Mode mode = Mode.CTD;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param out Output filename
   * @param steps Number of steps in the color map
   * @param mode Visualization mode
   */
  public CrossTrackDistanceVisualization(boolean verbose, File out, int steps, Mode mode) {
    super(verbose);
    this.out = out;
    this.steps = steps;
    this.mode = mode;
  }

  @Override
  public void run() throws UnableToComplyException {
    // Format: Latitude, Longitude
    // Berlin:
    DoubleVector stap = new DoubleVector(new double[] { 52.31, 13.24 });
    // New York:
    DoubleVector endp = new DoubleVector(new double[] { 40.712778, -74.005833 });

    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    final double max;
    switch(mode) {
    case ATD:
      max = 2 * GeoUtil.EARTH_RADIUS * Math.PI;
      break;
    case CTD:
      // Quarter (!) the circumference is the maximum CTD!
      max = .5 * GeoUtil.EARTH_RADIUS * Math.PI;
      break;
    default:
      throw new AbortException("Invalid mode: " + mode);
    }
    // Red: left off-course, green: right off-course
    int red = 0xffff0000;
    int green = 0xff00ff00;

    for (int x = 0; x < width; x++) {
      final double lon = x * 360. / width - 180.;
      for (int y = 0; y < height; y++) {
        final double lat = y * -180. / height + 90.;
        switch(mode) {
        case ATD: {
          final double atd = GeoUtil.alongTrackDistance(stap.doubleValue(0), stap.doubleValue(1), lat, lon, endp.doubleValue(0), endp.doubleValue(1));
          double reldist = atd / max;
          img.setRGB(x, y, colorMultiply(green, reldist));
          break;
        }
        case CTD: {
          final double ctd = GeoUtil.crossTrackDistance(stap.doubleValue(0), stap.doubleValue(1), lat, lon, endp.doubleValue(0), endp.doubleValue(1));
          if (ctd < 0) {
            double reldist = -ctd / max;
            img.setRGB(x, y, colorMultiply(red, reldist));
          } else {
            double reldist = ctd / max;
            img.setRGB(x, y, colorMultiply(green, reldist));
          }
          break;
        }
        default:
          throw new AbortException("Invalid mode: " + mode);
        }
      }
    }

    try {
      ImageIO.write(img, "png", out);
    } catch (IOException e) {
      LOG.exception(e);
    }
  }

  private int colorMultiply(int col, double reldist) {
    if (steps > 0) {
      reldist = Math.round(reldist * steps) * 1. / steps;
    }
    int a = (col >> 24) & 0xFF, r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = (col) & 0xFF;
    a = (int) (a * Math.sqrt(reldist)) & 0xFF;
    // r = (int) (r * (1 - reldist)) & 0xFF;
    // g = (int) (g * (1 - reldist)) & 0xFF;
    // b = (int) (b * (1 - reldist)) & 0xFF;
    return a << 24 | r << 16 | g << 8 | b;
  }

  /**
   * Main method for application.
   * 
   * @param args Parameters
   */
  public static void main(String[] args) {
    CrossTrackDistanceVisualization.runCLIApplication(CrossTrackDistanceVisualization.class, args);
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
     * Number of steps in the distance map.
     */
    public static final OptionID STEPS_ID = OptionID.getOrCreateOptionID("ctdvis.steps", "Number of steps for the distance map.");

    /**
     * Visualization mode.
     */
    public static final OptionID MODE_ID = OptionID.getOrCreateOptionID("ctdvis.mode", "Visualization mode.");

    /**
     * Holds the file to print results to.
     */
    protected File out = null;

    /**
     * Number of steps in the color map
     */
    protected int steps = 0;

    /**
     * Visualization mode
     */
    protected Mode mode = Mode.CTD;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      out = super.getParameterOutputFile(config, "Output image file");
      IntParameter stepsP = new IntParameter(STEPS_ID);
      stepsP.setOptional(true);
      stepsP.addConstraint(new GreaterEqualConstraint(0));
      if (config.grab(stepsP)) {
        steps = stepsP.intValue();
      }
      EnumParameter<Mode> modeP = new EnumParameter<Mode>(MODE_ID, Mode.class, Mode.CTD);
      if (config.grab(modeP)) {
        mode = modeP.getValue();
      }
    }

    @Override
    protected CrossTrackDistanceVisualization makeInstance() {
      return new CrossTrackDistanceVisualization(verbose, out, steps, mode);
    }
  }
}
