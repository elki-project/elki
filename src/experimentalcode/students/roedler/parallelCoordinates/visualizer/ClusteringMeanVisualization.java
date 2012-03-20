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
 * Generates a SVG-Element that visualizes cluster means.
 * 
 * @author Robert Rödler
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class ClusteringMeanVisualization<NV extends NumberVector<NV, ?>> extends ParallelVisualization<NV> implements DataStoreListener, MenuOwner {

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
   * menu items
   */
  CheckboxMenuItem items[];
  
  List<Integer> list;
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public ClusteringMeanVisualization(VisualizationTask task, List<Integer> list) {
    super(task);
    this.clustering = task.getResult();
    context.addDataStoreListener(this);
    this.list = list;
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
    
    SVGPath path;
    Vector yPos;
    
    double[][] mean = new double[dim][2];
    double count;
    
//    if (clustervis == null) {init(); }
    
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
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) / (StyleLibrary.SCALE / 2.));

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
  
  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
    
  }
  
  @Override
  public SubMenu getMenu() {
    SubMenu myMenu = new SubMenu(CLUSTERMEAN, this);
    
    int clus = clustering.getAllClusters().size();
    
    items = new CheckboxMenuItem[clus]; 
    
    for(int num = 0; num < clus; num++) {
      items[num] = myMenu.addCheckBoxItem("Cluster " + num, Integer.toString(num), clustervis[num]);
    }
    
  /*  myMenu.addSeparator();
    
    myMenu.addItem("Select all", "-1");
    myMenu.addItem("Unselect all", "-2");
    myMenu.addItem("Invert all", "-3");*/
    
    return myMenu;
  }

  @Override
  public void menuPressed(String id, boolean checked) {
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
    private static final String NAME = "Cluster Means";
    
    public static final OptionID VISIBLE_ID = OptionID.getOrCreateOptionID("parallel.clustermean.visible", "Select visible Clustermeans");

    /**
     * selected cluster
     */
    private List<Integer> list;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory(List<Integer> list) {
      super();
      this.list = list;
      
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusteringMeanVisualization<NV>(task, list);
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
            task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 5);
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
    public static class Parameterizer<NV extends NumberVector<NV, ?>> extends AbstractParameterizer {
      protected List<Integer> p;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final IntListParameter visL = new IntListParameter(VISIBLE_ID, true);
        if(config.grab(visL)) {
          p = visL.getValue();
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<NV>(p);
      }
    }
  }
}
