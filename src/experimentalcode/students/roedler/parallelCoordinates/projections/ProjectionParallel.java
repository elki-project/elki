package experimentalcode.students.roedler.parallelCoordinates.projections;

import de.lmu.ifi.dbs.elki.visualization.projections.Projection;

public interface ProjectionParallel extends Projection {

  public double getAxisHeight();
  
  public double getDist();
  
  public double getXpos(int dim);
  
  public boolean isVisible(int dim);
  
  public double getMarginX();
  
  public double getMarginY();
  
  public void setVisible(boolean vis, int dim);
  
  public int getVisibleDimensions();
  
  public int getFirstVisibleDimension();
  
}
