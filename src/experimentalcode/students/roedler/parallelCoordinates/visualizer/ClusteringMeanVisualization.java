package experimentalcode.students.roedler.parallelCoordinates.visualizer;

import java.util.Collection;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import experimentalcode.students.roedler.parallelCoordinates.gui.MenuOwner;
import experimentalcode.students.roedler.parallelCoordinates.gui.SubMenu;
import experimentalcode.students.roedler.parallelCoordinates.projector.ParallelPlotProjector;
import experimentalcode.students.roedler.parallelCoordinates.visualizer.ParallelVisualization;


/**
 * Generates a SVG-Element that visualizes cluster means.
 * 
 * @author Robert Rödler
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class ClusteringMeanVisualization<NV extends NumberVector<NV, ?>> extends ParallelVisualization<NV> implements ContextChangeListener, MenuOwner {

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERMEAN = "Clustermean";
  
  /**
   * The result we visualize
   */
  private Clustering<Model> clustering;
  
  /**
   * selected cluster
   */
  private boolean[] clustervis;
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public ClusteringMeanVisualization(VisualizationTask task) {
    super(task);
    this.clustering = task.getResult();
    context.addContextChangeListener(this);
    incrementalRedraw();
  }
  
  private void init(){
    clustervis = new boolean[clustering.getAllClusters().size()];
    for (int i = 0; i < clustervis.length; i++){
      clustervis[i] = false;
    }
  }
  
  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    int dim = DatabaseUtil.dimensionality(rep);
    
    SVGPath path;
    Vector yPos;
    
    double[][] mean = new double[dim][2];
    double count;
    
    if (clustervis == null) {init(); }
    
    Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();

    for (int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++){
    //  ci.next();
      count = 0.0;
      Cluster<?> clus = ci.next();
      
      if(!clustervis[cnum]){continue; }
        
        for(DBID objId : clus.getIDs()) {
          count += 1.0;
          yPos = proj.projectDataToRenderSpace(rep.get(objId));
           
          for (int i = 0; i < dim; i++){
            if (proj.isVisible(i)){
              mean[i][0] = proj.getXpos(i);
              mean[i][1] += yPos.get(i);
            } 
          }
        }
        
        for (int i = 0; i < dim; i++){
          mean[i][1] = mean[i][1] / count;
        }
        
        path = new SVGPath();
        for (int i = 0; i < mean.length; i++){
          if (proj.isVisible(i)){
            path.drawTo(mean[i][0], mean[i][1]);
          }
        }
       
  //      path.close();
        
        Element meanline = path.makeElement(svgp);
  
        SVGUtil.addCSSClass(meanline, CLUSTERMEAN + cnum);
        layer.appendChild(meanline);
        
        
    }
  }
  
  
  
  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    if(!svgp.getCSSClassManager().contains(CLUSTERMEAN)) {
      ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
      String color;
      int clusterID = 0;

      for(@SuppressWarnings("unused")
      Cluster<?> cluster : clustering.getAllClusters()) {
        CSSClass cls = new CSSClass(this, CLUSTERMEAN + clusterID);
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) * 2.0);

        if(clustering.getAllClusters().size() == 1) {
          color = "black";
        }
        else {
          color = colors.getColor(clusterID);
        }

        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);

        svgp.addCSSClassOrLogError(cls);
        clusterID++;
      }
    }
  }
  
  public void contextChanged(ContextChangedEvent e){
    incrementalRedraw();
  }
  
  @Override
  public SubMenu getMenu() {
    SubMenu myMenu = new SubMenu(this, CLUSTERMEAN);
    
    for(int num = 0; num < clustering.getAllClusters().size(); num++) {
      myMenu.addCheckBoxItem("Cluster " + num, Integer.toString(num), false);
    }
   /* 
    myMenu.addSeparator();
    
    myMenu.addItem("Select all", "-1");
    myMenu.addItem("Unselect all", "-2");
    myMenu.addItem("Invert all", "-3");
    */
    return myMenu;
  }

  @Override
  public void menuPressed(String id, boolean checked) {
    int iid = Integer.parseInt(id);
   /* if (iid < 0){
      if (iid == -1){
        for(int i = 0; i < clustervis.length; i++){
          clustervis[i] = true;
        }
      }
      if (iid == -2){
        for(int i = 0; i < clustervis.length; i++){
          clustervis[i] = false;
        }
      }
      if (iid == -3){
        for(int i = 0; i < clustervis.length; i++){
          clustervis[i] = !clustervis[i];
        }
      }
      incrementalRedraw();
      return;
    }*/
    
    clustervis[iid] = checked;
    incrementalRedraw(); 
  }
  
  
  /**
   * Factory for axis visualizations
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses AxisVisualization oneway - - «create»
   * 
   * @param <NV>
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Cluster Means";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusteringMeanVisualization<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find clusterings we can visualize:
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
      for(Clustering<?> c : clusterings) {
        if(c.getAllClusters().size() > 0) {
          Iterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ParallelPlotProjector.class);
          for(ParallelPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
            final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
            task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 4);
            baseResult.getHierarchy().add(c, task);
            baseResult.getHierarchy().add(p, task);
          }
        }
      }
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}
