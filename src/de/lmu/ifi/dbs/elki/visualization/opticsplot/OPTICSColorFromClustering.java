package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Adapter that uses an existing clustering to colorize the OPTICS plot.
 * 
 * @author Erich Schubert
 */
public class OPTICSColorFromClustering implements OPTICSColorAdapter {
  /**
   * Logger
   */
  private Logging logger = Logging.getLogger(OPTICSPlot.class);
  
  /**
   * The final mapping of object IDs to colors.
   */
  private final HashMap<Integer, Integer> idToColor;

  /**
   * Constructor.
   * 
   * @param colors Color library to use
   * @param refc Clustering to use
   */
  public OPTICSColorFromClustering(ColorLibrary colors, Clustering<?> refc) {
    final List<?> allClusters = refc.getAllClusters();
    // Build a list of colors 
    int[] cols = new int[allClusters.size()];
    for(int i = 0; i < allClusters.size(); i++) {
      Color color = SVGUtil.stringToColor(colors.getColor(i));
      if(color != null) {
        cols[i] = color.getRGB();
      }
      else {
        logger.warning("Could not parse color: " + colors.getColor(i));
        cols[i] = 0x7F7F7F7F;
      }
    }

    idToColor = new HashMap<Integer, Integer>();
    int cnum = 0;
    for(Cluster<?> clus : refc.getAllClusters()) {
      Color color = SVGUtil.stringToColor(colors.getColor(cnum));
      if (color == null) {
        logger.warning("Could not parse color: "+colors.getColor(cnum));
        color = Color.BLACK;
      }
      int rgb = color.getRGB();
      for(Integer id : clus) {
        idToColor.put(id, rgb);
      }
      cnum++;
    }
  }

  @Override
  public int getColorForEntry(ClusterOrderEntry<?> coe) {
    return idToColor.get(coe.getID());
  }
}