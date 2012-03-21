package experimentalcode.students.roedler.parallelCoordinates.visualizer;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import experimentalcode.students.roedler.parallelCoordinates.gui.MenuOwner;
import experimentalcode.students.roedler.parallelCoordinates.gui.SubMenu;
import experimentalcode.students.roedler.parallelCoordinates.projector.ParallelPlotProjector;
import experimentalcode.students.roedler.parallelCoordinates.svg.menu.CheckboxMenuItem;


/**
 * Generates a SVG-Element that visualizes cluster intervals.
 * 
 * @author Robert Rödler
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class ClusteringOutlineVisualization extends ParallelVisualization<NumberVector<?, ?>> implements DataStoreListener, MenuOwner {

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERAREA = "Clusteroutline";
  
  /**
   * The result we visualize
   */
  private Clustering<Model> clustering;
  
  /**
   * selected cluster
   */
  private boolean[] clustervis;
  
  /**
   * menu items
   */
  CheckboxMenuItem items[];
  
  List<Integer> list;
  
  boolean rounded;
  
  private static final double KAPPA = SVGHyperSphere.EUCLIDEAN_KAPPA;
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public ClusteringOutlineVisualization(VisualizationTask task, List<Integer> list, boolean rounded) {
    super(task);
    this.clustering = task.getResult();
    context.addDataStoreListener(this);
    this.list = list;
    this.rounded = rounded;
    init();
    incrementalRedraw();
  }
  
  private void init(){
    clustervis = new boolean[clustering.getAllClusters().size()];
    for (int i = 0; i < clustervis.length; i++){
      clustervis[i] = false;
    }
    if (list != null){
      for (Integer i : list){
        if (i < clustervis.length){
          clustervis[i] = true;
        }
      }
    }
  }
  
  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    int dim = DatabaseUtil.dimensionality(rep);
    
    double[][] max = new double[dim][2];
    double[][] min = new double[dim][2];
    double[][]midmax = new double[dim-1][2];
    double[][]midmin = new double[dim-1][2];
    int next;
    double temp;
    
//    if (clustervis == null) {init(); }
    
    Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();

    for (int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++){
    //  ci.next();
      Cluster<?> clus = ci.next();
      
      if(!clustervis[cnum]){continue; }
        boolean firstRun = true;
        
        for(DBID objId : clus.getIDs()) {
          Vector yPos = getYPositions(objId);
           
          for (int i = 0; i < dim; i++){
            if (proj.isVisible(i)){
              next = proj.getNextVisibleDimension(i); 
              if (firstRun == true){
                max[i][0] = proj.getXpos(i);
                min[i][0] = proj.getXpos(i);
                max[i][1] = yPos.get(i);
                min[i][1] = yPos.get(i);
                if (i < dim - 1){
                  midmax[i][0] = (proj.getXpos(i) + proj.getXpos(next)) / 2.;
                  midmin[i][0] = (proj.getXpos(i) + proj.getXpos(next)) / 2.;
                  midmax[i][1] = (yPos.get(i) + yPos.get(next)) / 2.;
                  midmin[i][1] = (yPos.get(i) + yPos.get(next)) / 2.;
                }
                continue;
              }
              if (yPos.get(i) > max[i][1]) {
                max[i][1] = yPos.get(i);
                max[i][0] = proj.getXpos(i);
                continue;
              }
              if (yPos.get(i) < min[i][1]){
                min[i][1] = yPos.get(i);
                min[i][0] = proj.getXpos(i);
              }
              if (i < dim - 1 && next > 0){
                temp = (yPos.get(i) + yPos.get(next)) / 2.;
                if (temp > midmax[i][1]){
                  midmax[i][1] = temp;
                }
                if (temp < midmin[i][1]){
                  midmin[i][1] = temp; 
                }
              }
            }
            
          }
          firstRun = false;
        }
        
        SVGPath path = new SVGPath();
        boolean first = true;
        if (rounded){
          for (int i = 0; i < min.length; i++){
            if (proj.isVisible(i)){
              if (first){
                path.drawTo(min[i][0], min[i][1]);
                first = false;
              }
              else{
                path.cubicTo(midmin[i - 1][0], midmin[i - 1][1] + (midmin[i - 1][1] - min[i - 1][1]) * KAPPA, midmin[i - 1][0] - (midmin[i - 1][0] - min[i - 1][0]) * KAPPA, midmin[i - 1][1], midmin[i - 1][0], midmin[i - 1][1]);
                path.cubicTo(midmin[i - 1][0] + (min[i][0] - midmin[i - 1][0]) * KAPPA, midmin[i - 1][1], min[i][0], min[i][1] - (midmin[i - 1][1] - min[i][1]) * KAPPA, min[i][0], min[i][1]);
              }
            }
          }
          first = true;
          for (int i = max.length - 1; i > 0; i--){
            if (proj.isVisible(i)){
              if (first){
                path.drawTo(max[i][0], max[i][1]);
                first = false;
              }
              else{
                path.cubicTo(max[i][0] + (max[i][0] - midmax[i - 1][0]) * KAPPA, midmax[i - 1][1], midmax[i - 1][0], midmax[i - 1][1] - (midmax[i - 1][1] - max[i][1]) * KAPPA, midmax[i - 1][0], midmax[i - 1][1]);
                path.cubicTo(midmax[i - 1][0] - (midmax[i - 1][0] - max[i - 1][0]) * KAPPA, midmax[i - 1][1], midmax[i - 1][0], midmax[i - 1][1] - (midmax[i - 1][1] - max[i - 1][1]) * KAPPA, max[i - 1][0], max[i - 1][1]);
              }
            }
          }
        }
        else {
          for (int i = 0; i < min.length; i++){
            if (proj.isVisible(i)){
              path.drawTo(min[i][0], min[i][1]);
              if (i < midmin.length){
                path.drawTo(midmin[i][0], midmin[i][1]);
              } 
            }
          }
          for (int i = max.length - 1; i >= 0; i--){
            if (proj.isVisible(i)){
              if (i < midmax.length){
                path.drawTo(midmax[i][0], midmax[i][1]);
              }
              path.drawTo(max[i][0], max[i][1]);  
            }
          }
        }
        path.close();
        
        Element intervals = path.makeElement(svgp);
  
        SVGUtil.addCSSClass(intervals, CLUSTERAREA + cnum);
        layer.appendChild(intervals);
    }
  }
  
  
  
  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    if(!svgp.getCSSClassManager().contains(CLUSTERAREA)) {
      ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
      String color;
      int clusterID = 0;

      for(@SuppressWarnings("unused")
      Cluster<?> cluster : clustering.getAllClusters()) {
        CSSClass cls = new CSSClass(this, CLUSTERAREA + clusterID);
     //   cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) / 2.0);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.5);
        if(clustering.getAllClusters().size() == 1) {
          color = "black";
        }
        else {
          color = colors.getColor(clusterID);
        }

     //   cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);

        svgp.addCSSClassOrLogError(cls);
        clusterID++;
      }
    }
  }
  
  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
    
  }
  
  @Override
  public SubMenu getMenu() {
    SubMenu myMenu = new SubMenu(CLUSTERAREA, this);
    int clus = clustering.getAllClusters().size();
    
    items = new CheckboxMenuItem[clus]; 
    
    for(int num = 0; num < clus; num++) {
      items[num] = myMenu.addCheckBoxItem("Cluster " + num, Integer.toString(num), clustervis[num]);
    }
 //   myMenu.addCheckBoxItem("rounded", "rounded", rounded);
    
 /*   myMenu.addSeparator();
    
    myMenu.addItem("Select all", "-1");
    myMenu.addItem("Unselect all", "-2");
    myMenu.addItem("Invert all", "-3");
 */
    return myMenu;
  }

  @Override
  public void menuPressed(String id, boolean checked) {
    if (id == "rounded"){
      rounded = checked;
      incrementalRedraw();
      return;
    }
    
    int iid = Integer.parseInt(id);
 /*   if (iid < 0){
      if (iid == -1){
        for(int i = 0; i < clustervis.length; i++){
          items[i].setSelected(true);
          clustervis[i] = true;
        }
      }
      if (iid == -2){
        for(int i = 0; i < clustervis.length; i++){
          items[i].setSelected(false);
          clustervis[i] = false;
        }
      }
      if (iid == -3){
        for(int i = 0; i < clustervis.length; i++){
          items[i].setSelected(!clustervis[i]);
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
    private static final String NAME = "Cluster Outline";
    
    public static final OptionID VISIBLE_ID = OptionID.getOrCreateOptionID("parallel.clusteroutline.visible", "Select visible Clusteroutlines");

    public static final OptionID FILL_ID = OptionID.getOrCreateOptionID("parallel.clusteroutline.rounded", "Draw lines rounded");
    /**
     * selected cluster
     */
    private List<Integer> list;
    
    private boolean rounded;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory(List<Integer> list, boolean rounded) {
      super();
      this.list = list;
      this.rounded = rounded;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusteringOutlineVisualization(task, list, rounded);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find clusterings we can visualize:
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
      for(Clustering<?> c : clusterings) {
        if(c.getAllClusters().size() > 0) {
          IterableIterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ParallelPlotProjector.class);
          for(ParallelPlotProjector<?> p : ps) {
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
    
    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      protected List<Integer> p;
      protected boolean rounded = false;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
 //       Flag fillF = new Flag(FILL_ID);
 //       fillF.setDefaultValue(true);
 //       if(config.grab(fillF)) {
 //         rounded = fillF.getValue();
 //       }
        final IntListParameter visL = new IntListParameter(VISIBLE_ID, true);
        if(config.grab(visL)) {
          p = visL.getValue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(p, rounded);
      }
    }
  }
}
