package experimentalcode.students.doerre;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.geo.LatLngDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
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
public class CrossTrackDistanceVisualization<V extends NumberVector<V, ?>> implements ResultHandler {
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
    // TODO: distanz-funktion parameterisierbar
    DistanceQuery<V, DoubleDistance> dq = db.getDistanceQuery(rel, EuclideanDistanceFunction.STATIC);
    //KNNQuery<V, DoubleDistance> knnq = db.getKNNQuery(dq);
    V factory = DatabaseUtil.assumeVectorField(rel).getFactory();

    SpatialPrimitiveDoubleDistanceFunction<? super V> df = EuclideanDistanceFunction.STATIC;
    //ModifiableHyperBoundingBox bb = new ModifiableHyperBoundingBox(new double[]{37.788800, -77.011533}, new double[]{43.788800, -71.011533});
    ModifiableHyperBoundingBox bb = new ModifiableHyperBoundingBox(new double[]{-77.011533, +37.788800}, new double[]{-71.011533, +43.788800});
    SpatialPrimitiveDoubleDistanceFunction<? super V> df2 = LatLngDistanceFunction.STATIC;
    
    V newyork = factory.newNumberVector(new double[]{-74.011533, +40.788800});
    V berlin = factory.newNumberVector(new double[]{+13.24, +52.31});
    
    //latlong-schreibweise
    V newyork2 = factory.newNumberVector(new double[]{40.788800, -74.011533});
    V berlin2 = factory.newNumberVector(new double[]{52.31, 13.24});
    V reykjavik2 = factory.newNumberVector(new double[]{64.9, -21.56});
    V dublin2 = factory.newNumberVector(new double[]{53.21, -6.16});  
    
    V p1 = berlin2;
    V p2 = newyork2;
    V p3 = reykjavik2;
    V p4 = dublin2;
    //V p1 = factory.newNumberVector(new double[]{50.0359, -67.5253});
    //V p2 = factory.newNumberVector(new double[]{51.3838, -3.0412});
    //V p3 = factory.newNumberVector(new double[]{63.3838, -19.0412});
    
    double dist2 = df2.doubleDistance(p1, p2);
    double dist3 = df2.doubleMinDist(p1, p2); //geht
    double dist_bn = df2.doubleDistance(berlin2, newyork2);
    HyperBoundingBox hbb = new HyperBoundingBox (new double[]{50.0359, -67.5253}, new double[]{51.3838, -3.0412});
    //double dist4 = df2.doubleMinDist(p3, hbb);
    
    // Calculate Cross-Track and Along-Track distances of Berlin-New York-Track and two Test Points
    double dist_ctd = experimentalcode.students.doerre.CrossTrackDistance.CrossTrackDistance (p1.doubleValue(1),p1.doubleValue(2),p2.doubleValue(1),p2.doubleValue(2),p3.doubleValue(1),p3.doubleValue(2));    
    double dist_alt = experimentalcode.students.doerre.AlongTrackDistance.AlongTrackDistance (p1.doubleValue(1),p1.doubleValue(2),p2.doubleValue(1),p2.doubleValue(2),p3.doubleValue(1),p3.doubleValue(2));  
    double dist_ctd2 = experimentalcode.students.doerre.CrossTrackDistance.CrossTrackDistance (p1.doubleValue(1),p1.doubleValue(2),p2.doubleValue(1),p2.doubleValue(2),p4.doubleValue(1),p4.doubleValue(2));
    double dist_alt2 = experimentalcode.students.doerre.AlongTrackDistance.AlongTrackDistance (p1.doubleValue(1),p1.doubleValue(2),p2.doubleValue(1),p2.doubleValue(2),p4.doubleValue(1),p4.doubleValue(2));
    
    System.out.println("Strecke: Berlin - New York. Großkreis-Distanz: " + (int) dist_bn + " km");
    
    System.out.println("Cross-Track Distance (Reykjavik): " + (int) dist_ctd + " km");
    System.out.println("Along-Track Distance (Reykjavik): " + (int) dist_alt + " km");
    System.out.println("Cross-Track Distance (Dublin): " + (int) dist_ctd2 + " km");
    System.out.println("Along-Track Distance (Dublin): " + (int) dist_alt2 + " km");
    
    // Calculate coordinates of nearest points on the track
    
    double [] coordinates = experimentalcode.students.doerre.AlongTrackDistance.AlongTrackCoordinates (p1.doubleValue(1),p1.doubleValue(2),p2.doubleValue(1),p2.doubleValue(2),p3.doubleValue(1),p3.doubleValue(2));
    double [] coordinates2 = experimentalcode.students.doerre.AlongTrackDistance.AlongTrackCoordinates (p1.doubleValue(1),p1.doubleValue(2),p2.doubleValue(1),p2.doubleValue(2),p4.doubleValue(1),p4.doubleValue(2));

    //System.out.println ("Koordinaten des nächsten Punktes (zu Reykjavik) auf dem Kurs - Latitude: " + coordinates[0] + " Longitude: " + coordinates[1]);
    //System.out.println ("Koordinaten des nächsten Punktes (zu Dublin) auf dem Kurs - Latitude: " + coordinates2[0] + " Longitude: " + coordinates2[1]);
    
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
    
    // Create a graphics contents on the buffered image
    Graphics2D g2d = img.createGraphics();

    // Draw graphics - 1. background
    g2d.setColor(Color.black);
    g2d.fillRect(0, 0, width, height);
    
    // Draw graphics - 2. course points
    g2d.setColor(Color.white);
    
    // Read the coordinate values from the list - and draw points (mapped to 2000*1000)
    for (int iter2=0; iter2<number_of_points; iter2++) {
        
        int point_y2 = (int) ((dest_points[iter2][0] + 90) / 180. * height); 
        int point_y3 = height - point_y2; // oben/unten vertauschen notwendig!
        int point_x2 = (int) ((dest_points[iter2][1] + 180) / 360. * width);
        
        g2d.drawRect(point_x2, point_y3, 1, 1);
    }
    
    // Draw graphics - 3. Cross-track points (green right from track, red left from track)
    int y21 = (int) (((int)p3.doubleValue(1) + 90) / 180. * height);
    int y211 = height - y21;
    int x22 = (int) (((int)p3.doubleValue(2) + 180) / 360. * width);
    
    if (dist_ctd2 < 0) {
      g2d.setColor(Color.green);
    } else {
      g2d.setColor(Color.red);
    }
    g2d.drawRect(x22, y211, 5, 5); 
    
    int y11 = (int) (((int)p4.doubleValue(1) + 90) / 180. * height);
    int y111 = height - y11;
    int x12 = (int) (((int)p4.doubleValue(2) + 180) / 360. * width);
    
    if (dist_ctd < 0) {
      g2d.setColor(Color.green);
    } else {
      g2d.setColor(Color.red);
    }
    g2d.drawRect(x12, y111, 5, 5);
    
    //System.out.println (x22+ " "+y211+" "+y21+" "+p);
    //System.out.println (x12+ " "+y111+" "+y11);
    
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