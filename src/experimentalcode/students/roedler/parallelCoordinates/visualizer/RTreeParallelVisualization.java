package experimentalcode.students.roedler.parallelCoordinates.visualizer;

/*This file is part of ELKI:
  Environment for Developing KDD-Applications Supported by Index-Structures

  Copyright (C) 2011
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

 import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
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
import experimentalcode.students.roedler.parallelCoordinates.projections.ProjectionParallel;
import experimentalcode.students.roedler.parallelCoordinates.projector.ParallelPlotProjector;

 /**
  * Visualize the  of an R-Tree based index.
  * 
  * @author Robert Rödler
  * 
  * @apiviz.has AbstractRStarTree oneway - - visualizes
  * 
  * @param <N> Tree node type
  * @param <E> Tree entry type
  */
 // TODO: listen for tree changes instead of data changes?
 public class RTreeParallelVisualization<N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends ParallelVisualization<NumberVector<?, ?>> implements MenuOwner, DataStoreListener {
   /**
    * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
    */
   public static final String INDEX = "parallelrtree";

   /**
    * A short name characterizing this Visualizer.
    */
   public static final String NAME = "R-Tree Index MBRs";

   /**
    * Fill parameter.
    */
   protected boolean fill = true;
   
   /**
    * page visibility
    */
   protected boolean[] pagevis;

   /**
    * The tree we visualize
    */
   protected AbstractRStarTree<N, E> tree;
   
   /**
    * Constructor.
    * 
    * @param task Visualization task
    * @param fill Fill flag
    */
   @SuppressWarnings("unchecked")
   public RTreeParallelVisualization(VisualizationTask task, boolean fill) {
     super(task);
     this.tree = AbstractRStarTree.class.cast(task.getResult());
     this.fill = fill;
     context.addDataStoreListener(this);
     context.addResultListener(this);
     init();
     incrementalRedraw();
   }
   
   @Override
   public void destroy() {
     context.removeDataStoreListener(this);
     context.removeResultListener(this);
     super.destroy();
   }

   private void init(){
     pagevis = new boolean[tree.getRootEntry().getDimensionality()];
     for (int i = 0; i < pagevis.length; i++){
       pagevis[i] = true;
     }
   }

   @Override
   protected void redraw() {
     if (pagevis ==  null){ init(); }
     addCSSClasses(svgp);
     if(tree != null) {
       E root = tree.getRootEntry();
       visualizeRTreeEntry(svgp, layer, proj, tree, root, 0, 0);
     }
   }
   
   /**
    * Adds the required CSS-Classes
    * 
    * @param svgp SVG-Plot
    */
   private void addCSSClasses(SVGPlot svgp) {
     ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
     
     for(int i = 0; i < tree.getHeight(); i++) {
     
       if(!svgp.getCSSClassManager().contains(INDEX + i)) {
         CSSClass cls = new CSSClass(this, INDEX + i);
         
      // Relative depth of this level. 1.0 = toplevel
         final double relDepth = 1. - (((double) i) / tree.getHeight());
         if(fill) {
           cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
           cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
           cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, colors.getColor(i));
           cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.3);
         }
         else {
           cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
           cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
           cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
         }
         cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
         cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
         svgp.addCSSClassOrLogError(cls);
       }
     }
     svgp.updateStyleElement();
   }
   /**
    * removes the required CSS-Classes
    * in order to add others
    * 
    * @param svgp SVG-Plot
    */
   private void removeCSSClasses(SVGPlot svgp){
     for (int i = 0; i < tree.getHeight(); i++){
       if (svgp.getCSSClassManager().contains(INDEX + i)){
          svgp.getCSSClassManager().removeClass(svgp.getCSSClassManager().getClass(INDEX + i));
       }
     }
     svgp.updateStyleElement();
   }

   /**
    * Recursively draw the MBR rectangles.
    * 
    * @param svgp SVG Plot
    * @param layer Layer
    * @param proj Projection
    * @param rtree Rtree to visualize
    * @param entry Current entry
    * @param depth Current depth
    */
   private void visualizeRTreeEntry(SVGPlot svgp, Element layer, ProjectionParallel proj, AbstractRStarTree<? extends N, E> rtree, E entry, int depth, int step) {
     if (pagevis[step] == true){
       final int dim = proj.getVisibleDimensions();
       double[] min = proj.fastProjectDataToRenderSpace(SpatialUtil.getMin(entry));
       double[] max = proj.fastProjectDataToRenderSpace(SpatialUtil.getMax(entry));
       assert(min.length == dim && max.length == dim);
       SVGPath path = new SVGPath();
       for (int i = 0; i < dim; i++){
         path.drawTo(getAxisX(i), Math.max(min[i], max[i]));
       }
       for (int i = dim - 1; i >= 0; i--){
         path.drawTo(getAxisX(i), Math.min(min[i], max[i]));
       }
       path.close();
       
       Element intervals = path.makeElement(svgp);
  
       SVGUtil.addCSSClass(intervals, INDEX + depth);
       layer.appendChild(intervals);
     }
     
     if(!entry.isLeafEntry()) {
       N node = rtree.getNode(entry);
       for(int i = 0; i < node.getNumEntries(); i++) {
         E child = node.getEntry(i);
         if(!child.isLeafEntry()) {
           visualizeRTreeEntry(svgp, layer, proj, rtree, child, depth + 1, ++step);
         }
       }
     }
   }

   @Override
   public SubMenu getMenu() {
   SubMenu myMenu = new SubMenu(NAME, this);
   myMenu.addCheckBoxItem("fill", "fill", fill);
   
   for (int i = 0; i < pagevis.length; i++){
     myMenu.addCheckBoxItem("Pagevisible " + i, String.valueOf(i), pagevis[i]);
   }
   
   return myMenu;
   }

   @Override
   public void menuPressed(String id, boolean checked) {
     if (id == "fill") { 
       fill = checked;
       removeCSSClasses(svgp);
       incrementalRedraw();
       return;
     }
     int iid = Integer.parseInt(id);
     pagevis[iid] = checked;
     incrementalRedraw();
   }
   
   /**
    * Factory
    * 
    * @author Robert Rödler
    * 
    * @apiviz.stereotype factory
    * @apiviz.uses RTreeParallelVisualization oneway - - «create»
    */
   public static class Factory extends AbstractVisFactory {
     /**
      * Flag for half-transparent filling of pages.
      * 
      * <p>
      * Key: {@code -index.fill}
      * </p>
      */
     public static final OptionID FILL_ID = OptionID.getOrCreateOptionID("parallel.index.fill", "Partially transparent filling of index pages.");

     /**
      * Fill parameter.
      */
     protected boolean fill = true;

     /**
      * Constructor.
      * 
      * @param fill
      */
     public Factory(boolean fill) {
       super();
       this.fill = fill;
     }

     @Override
     public Visualization makeVisualization(VisualizationTask task) {
       return new RTreeParallelVisualization<RStarTreeNode, SpatialEntry>(task, fill);
     }

     @Override
     public void processNewResult(HierarchicalResult baseResult, Result result) {
       ArrayList<AbstractRStarTree<RStarTreeNode, SpatialEntry>> trees = ResultUtil.filterResults(result, AbstractRStarTree.class);
       for(AbstractRStarTree<RStarTreeNode, SpatialEntry> tree : trees) {
         if(tree instanceof Result) {
           IterableIterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ParallelPlotProjector.class);
           for(ParallelPlotProjector<?> p : ps) {
             final VisualizationTask task = new VisualizationTask(NAME, (Result) tree, p.getRelation(), this);
             task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_BACKGROUND + 2);
             baseResult.getHierarchy().add((Result) tree, task);
             baseResult.getHierarchy().add(p, task);
           }
         }
       }
     }
     
     /**
      * Parameterization class.
      * 
      * @author Erich Schubert
      * 
      * @apiviz.exclude
      */
     public static class Parameterizer extends AbstractParameterizer {
       protected boolean fill = true;

       @Override
       protected void makeOptions(Parameterization config) {
         super.makeOptions(config);
         Flag fillF = new Flag(FILL_ID);
         fillF.setDefaultValue(true);
         if(config.grab(fillF)) {
           fill = fillF.getValue();
         }
       }

       @Override
       protected Factory makeInstance() {
         return new Factory(fill);
       }
     }
   }
 }
