package experimentalcode.students.doerre;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.visualization.ExportVisualizations;
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

/**
 * @author Niels Dörre
 * Visualization function for Cross-track distance function
 */
public class CrossTrackDistanceVisualization<V extends NumberVector<?>> implements ResultHandler {
  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(ExportVisualizations.class);

  /**
   * Holds the file to print results to.
   */
  private File out;

  /**
   * Constructor.
   * 
   * @param out
   * 
   */
  public CrossTrackDistanceVisualization(File out) {
    super();
    this.out = out;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    Database db = ResultUtil.findDatabase(baseResult);
    VectorTypeInformation<V> type = new VectorFieldTypeInformation<V>(NumberVector.class, 2, 2);
    Relation<V> rel = db.getRelation(type);
    processRelation(db, rel);
  }

  private void processRelation(Database db, Relation<V> rel) {

    final NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(rel);

    SpatialPrimitiveDoubleDistanceFunction<? super V> df2 = LatLngDistanceFunction.STATIC;
    
    //latlong-schreibweise
    V newyork2 = factory.newNumberVector(new double[]{40.788800, -74.011533});
    V berlin2 = factory.newNumberVector(new double[]{52.31, 13.24});

    V p1 = berlin2;
    V p2 = newyork2;

    double dist_bn = df2.doubleDistance(berlin2, newyork2);
    
    int distance_of_points = 5;
    
    int number_of_points = (int) (dist_bn / distance_of_points);
    
    double [] [] dest_points = new double [number_of_points][2];
    
    int i = 0;
    
    for (int iter=distance_of_points; iter<(int) dist_bn; iter=iter+distance_of_points){
      double []dest_point = experimentalcode.students.doerre.DestinationPointCalculation.DestinationPointCalculation(p1.doubleValue(1),p1.doubleValue(2),p2.doubleValue(1),p2.doubleValue(2), iter);
      dest_points[i] = dest_point;
      i++;
    }

    final int width = 2000, height = 1000;
    
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    
    // Calculate Cross-Track and Along-Track distances of all points 
    
    double [][] cross_track_distances = new double [width][height];
    DoubleMinMax minmax = new DoubleMinMax();

    
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        // x/y in lat/long übersetzen..
        double lon = x * 360. / width - 180.;
        double lat = y * -180. / height + 90.;
        // ..und übergeben
        double ctd = experimentalcode.students.doerre.CrossTrackDistance.CrossTrackDistance (p1.doubleValue(1),p1.doubleValue(2),p2.doubleValue(1),p2.doubleValue(2),lat,lon,0);    
        cross_track_distances [x][y] = ctd;
        minmax.put(ctd);
      }
    }
    
    // Red: left off-course, green: right off-course
    int red=0xffff0000;
    int green=0xff00ff00;
    
    for(int x = 0; x < width; x++) {
      for(int y = 0; y < height; y++) {
        if (cross_track_distances[x][y] < 0) { 
          double reldist = cross_track_distances[x][y] / minmax.getMax();
          int val = 255 - (0xFF & 16* (int) (15 * reldist - .95));
          int col = val << 24;
          int col_red = red - col;  // red
          img.setRGB(x, y, col_red);
        } else if (cross_track_distances[x][y] > 0){
          double reldist = cross_track_distances[x][y] / minmax.getMax();
          int val = 255 - (0xFF & 16* (int) (.5 + 15 * reldist));
          int col = val << 24;
          int col_green = green + col;  // green
          img.setRGB(x, y, col_green); 
        }
      }
    }
    
    // Create a graphics contents on the buffered image
    Graphics2D g2d = img.createGraphics();
    
    // Draw course points
    g2d.setColor(Color.black);
    
    // Read the coordinate values from the list - and draw points (mapped to 2000*1000)
    for (int iter2=0; iter2<number_of_points; iter2++) {
        
        int point_y2 = (int) ((dest_points[iter2][0] + 90) / 180. * height); 
        int point_y3 = height - point_y2; // changing of sides necessary
        int point_x2 = (int) ((dest_points[iter2][1] + 180) / 360. * width);
        
        g2d.drawRect(point_x2, point_y3, 1, 1);
    }
    
    // Graphics context no longer needed so dispose it
    g2d.dispose();

    try {
      ImageIO.write(img, "png", out);
    }
    catch(IOException e) {
      // Auto-generated catch block
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
  }

  public static class Parameterizer extends AbstractParameterizer {

    /**
     * Holds the file to print results to.
     */
    protected File out = null;

    /**
     * Parameter distanceOfLines.
     */
    protected int distanceOfLines;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // FileParameter for output file
      FileParameter outputP = new FileParameter(OptionID.OUTPUT, FileParameter.FileType.OUTPUT_FILE, true);
      if(config.grab(outputP)) {
        out = outputP.getValue();
      }
      
    }

    @Override
    protected CrossTrackDistanceVisualization makeInstance() {
      return new CrossTrackDistanceVisualization(out);
    }
  }

}