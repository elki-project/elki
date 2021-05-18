/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package elki.application.experiments;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import elki.application.AbstractApplication;
import elki.data.DoubleVector;
import elki.data.ModifiableHyperBoundingBox;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.geodesy.EarthModel;
import elki.math.geodesy.SphereUtil;
import elki.math.geodesy.SphericalVincentyEarthModel;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Visualization function for Cross-track, Along-track, and minimum distance
 * function.
 * <p>
 * TODO: make origin point / rectangle configurable.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br>
 * Geodetic Distance Queries on R-Trees for Indexing Geographic Data<br>
 * Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)
 *
 * @author Niels Dörre
 * @author Erich Schubert
 * @since 0.5.5
 */
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
    title = "Geodetic Distance Queries on R-Trees for Indexing Geographic Data", //
    booktitle = "Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)", //
    url = "https://doi.org/10.1007/978-3-642-40235-7_9", //
    bibkey = "DBLP:conf/ssd/SchubertZK13")
public class VisualizeGeodesicDistances extends AbstractApplication {
  /**
   * Get a logger for this class.
   */
  private final static Logging LOG = Logging.getLogger(VisualizeGeodesicDistances.class);

  /**
   * Visualization mode.
   *
   * @author Erich Schubert
   */
  public enum Mode {
    /** Cross track distance */
    XTD,
    /** Along track distance */
    ATD,
    /** Mindist */
    MINDIST
  }

  /**
   * Holds the file to print results to.
   */
  private Path out;

  /**
   * Image size.
   */
  protected int width = 2000, height = 1000;

  /**
   * Number of steps for shades.
   */
  protected int steps = 10;

  /**
   * Visualization mode.
   */
  protected Mode mode = Mode.XTD;

  /**
   * Earth model.
   */
  protected EarthModel model;

  /**
   * Constructor.
   *
   * @param out Output filename
   * @param steps Number of steps in the color map
   * @param mode Visualization mode
   * @param model Earth model
   */
  public VisualizeGeodesicDistances(Path out, int resolution, int steps, Mode mode, EarthModel model) {
    super();
    this.width = resolution;
    this.height = resolution >> 1;
    this.out = out;
    this.steps = steps;
    this.mode = mode;
    this.model = model;
  }

  @Override
  public void run() {
    // Format: Latitude, Longitude
    // München:
    DoubleVector stap = DoubleVector.wrap(new double[] { 48.133333, 11.566667 });
    // New York:
    DoubleVector endp = DoubleVector.wrap(new double[] { 40.712778, -74.005833 });
    // Bavaria:
    ModifiableHyperBoundingBox bb = new ModifiableHyperBoundingBox(new double[] { 47.27011150, 8.97634970 }, new double[] { 50.56471420, 13.83963710 });
    // Bavaria slice on lat
    // bb = new ModifiableHyperBoundingBox(new double[] { 47.27011150, -80 }, //
    // new double[] { 50.56471420, 80 });
    // Bavaria slice on lon
    // bb = new ModifiableHyperBoundingBox(new double[] { -10, 8.97634970 }, //
    // new double[] { 50, 13.83963710 });

    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    final double max = model.getEquatorialRadius() * Math.PI;

    // Red: left off-course, green: right off-course
    int red = 0xffff0000;
    int green = 0xff00ff00;

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("columns", width, LOG) : null;
    for(int x = 0; x < width; x++) {
      final double lon = x * 360. / width - 180.;
      for(int y = 0; y < height; y++) {
        final double lat = y * -180. / height + 90.;
        switch(mode){
        case ATD: {
          final double atd = model.getEquatorialRadius() * SphereUtil.alongTrackDistanceDeg(stap.doubleValue(0), stap.doubleValue(1), endp.doubleValue(0), endp.doubleValue(1), lat, lon);
          if(atd < 0) {
            img.setRGB(x, y, colorMultiply(red, -atd / max, false));
          }
          else {
            img.setRGB(x, y, colorMultiply(green, atd / max, false));
          }
          break;
        }
        case XTD: {
          final double ctd = model.getEquatorialRadius() * SphereUtil.crossTrackDistanceDeg(stap.doubleValue(0), stap.doubleValue(1), endp.doubleValue(0), endp.doubleValue(1), lat, lon);
          if(ctd < 0) {
            img.setRGB(x, y, colorMultiply(red, -ctd / max, false));
          }
          else {
            img.setRGB(x, y, colorMultiply(green, ctd / max, false));
          }
          break;
        }
        case MINDIST: {
          final double dist = model.minDistDeg(lat, lon, bb.getMin(0), bb.getMin(1), bb.getMax(0), bb.getMax(1));
          if(dist < 0) {
            img.setRGB(x, y, colorMultiply(red, -dist / max, true));
          }
          else {
            img.setRGB(x, y, colorMultiply(green, dist / max, true));
          }
          break;
        }
        }
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    try {
      ImageIO.write(img, "png", Files.newOutputStream(out));
    }
    catch(IOException e) {
      LOG.exception(e);
    }
  }

  private int colorMultiply(int col, double reldist, boolean ceil) {
    if(steps > 0) {
      if(!ceil) {
        reldist = FastMath.round(reldist * steps) / steps;
      }
      else {
        reldist = FastMath.ceil(reldist * steps) / steps;
      }
    }
    else if(steps < 0 && reldist > 0.) {
      double s = reldist * -steps;
      double off = Math.abs(s - FastMath.round(s));
      double factor = -steps * 1. / 1000; // height;
      if(off < factor) { // Blend with black:
        factor = (off / factor);
        int a = (col >> 24) & 0xFF;
        a = (int) (a * Math.sqrt(reldist)) & 0xFF;
        a = (int) ((1 - factor) * 0xFF + factor * a);
        int r = (int) (factor * ((col >> 16) & 0xFF));
        int g = (int) (factor * ((col >> 8) & 0xFF));
        int b = (int) (factor * (col & 0xFF));
        return a << 24 | r << 16 | g << 8 | b;
      }
    }
    int a = (col >> 24) & 0xFF, r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF,
        b = (col) & 0xFF;
    a = (int) (a * Math.sqrt(reldist)) & 0xFF;
    return a << 24 | r << 16 | g << 8 | b;
  }

  /**
   * Main method for application.
   *
   * @param args Parameters
   */
  public static void main(String[] args) {
    runCLIApplication(VisualizeGeodesicDistances.class, args);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par extends AbstractApplication.Par {
    /**
     * Number of steps in the distance map.
     */
    public static final OptionID STEPS_ID = new OptionID("geodistvis.steps", "Number of steps for the distance map. Use negative numbers to get contour lines.");

    /**
     * Image resolution.
     */
    public static final OptionID RESOLUTION_ID = new OptionID("geodistvis.resolution", "Horizontal resolution for the image map (vertical resolution is horizonal / 2).");

    /**
     * Visualization mode.
     */
    public static final OptionID MODE_ID = new OptionID("geodistvis.mode", "Visualization mode.");

    /**
     * Holds the file to print results to.
     */
    protected Path out = null;

    /**
     * Number of steps in the color map.
     */
    protected int steps = 0;

    /**
     * Horizontal resolution.
     */
    protected int resolution = 2000;

    /**
     * Visualization mode.
     */
    protected Mode mode = Mode.XTD;

    /**
     * Earth model to use.
     */
    protected EarthModel model;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      out = super.getParameterOutputFile(config, "Output image file name.");
      new IntParameter(STEPS_ID) //
          .setOptional(true) //
          .grab(config, x -> steps = x);
      new IntParameter(RESOLUTION_ID, 2000) //
          .grab(config, x -> resolution = x);
      new EnumParameter<Mode>(MODE_ID, Mode.class, Mode.XTD) //
          .grab(config, x -> mode = x);
      new ObjectParameter<EarthModel>(EarthModel.MODEL_ID, EarthModel.class, SphericalVincentyEarthModel.class) //
          .grab(config, x -> model = x);
    }

    @Override
    public VisualizeGeodesicDistances make() {
      return new VisualizeGeodesicDistances(out, resolution, steps, mode, model);
    }
  }
}
