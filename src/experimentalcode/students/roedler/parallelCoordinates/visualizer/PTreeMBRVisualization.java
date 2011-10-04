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
 import java.util.Iterator;

 import org.apache.batik.util.SVGConstants;
 import org.w3c.dom.Element;

 import de.lmu.ifi.dbs.elki.data.NumberVector;
 import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
 import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
 import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
 import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
 import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
 import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
 import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
 import de.lmu.ifi.dbs.elki.result.Result;
 import de.lmu.ifi.dbs.elki.result.ResultUtil;
 import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
 import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
 import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
 import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
 import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
 import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
 import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
 import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
 import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
 import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
 import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
 import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
 import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
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
  * @param <NV> Type of the DatabaseObject being visualized.
  * @param <N> Tree node type
  * @param <E> Tree entry type
  */
 // TODO: listen for tree changes instead of data changes?
 public class PTreeMBRVisualization<NV extends NumberVector<NV, ?>, N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends ParallelVisualization<NV> implements MenuOwner, ContextChangeListener, DataStoreListener {
   /**
    * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
    */
   public static final String INDEX = "parallelindex";

   /**
    * A short name characterizing this Visualizer.
    */
   public static final String NAME = "Parallel Index MBRs";

   /**
    * Fill parameter.
    */
   protected boolean fill = true;
   
   /**
    * visible parameter.
    */
   protected boolean visible = false;

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
   public PTreeMBRVisualization(VisualizationTask task) {
     super(task);
     this.tree = AbstractRStarTree.class.cast(task.getResult());
     incrementalRedraw();
     context.addDataStoreListener(this);
   }

   @Override
   protected void redraw() {
     addCSSClasses(svgp);
     if(tree != null && visible) {
       E root = tree.getRootEntry();
       visualizeRTreeEntry(svgp, layer, proj, tree, root, 0);
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
   private void visualizeRTreeEntry(SVGPlot svgp, Element layer, ProjectionParallel proj, AbstractRStarTree<? extends N, E> rtree, E entry, int depth) {
     final int dim = DatabaseUtil.dimensionality(rep);

     SVGPath path = new SVGPath();
     for (int i = 0; i < dim; i++){
       if (proj.isVisible(i)){
         path.drawTo(proj.getXpos(i), proj.projectDimension(i, entry.getMax(i + 1)));
       }
     }
     for (int i = dim - 1; i >= 0; i--){
       if (proj.isVisible(i)){
         path.drawTo(proj.getXpos(i), proj.projectDimension(i, entry.getMin(i + 1)));
       }
     }
     path.close();
     
     Element intervals = path.makeElement(svgp);

     SVGUtil.addCSSClass(intervals, INDEX + depth);
     layer.appendChild(intervals);
     
     if(!entry.isLeafEntry()) {
       N node = rtree.getNode(entry);
       for(int i = 0; i < node.getNumEntries(); i++) {
         E child = node.getEntry(i);
         if(!child.isLeafEntry()) {
           visualizeRTreeEntry(svgp, layer, proj, rtree, child, depth + 1);
         }
       }
     }
   }

   @Override
   public SubMenu getMenu() {
   SubMenu myMenu = new SubMenu(this, NAME);
   myMenu.addCheckBoxItem("visible", "vis", false);
   myMenu.addCheckBoxItem("fill", "fill", true);
   
   return myMenu;
   }

   @Override
   public void menuPressed(String id, boolean checked) {
     if (id == "vis") { visible = checked; }
     if (id == "fill") { 
       fill = checked;
       removeCSSClasses(svgp);
     }
     incrementalRedraw();
   }
   
   @Override
   public void destroy() {
     super.destroy();
     context.removeDataStoreListener(this);
   }

   public void contextChanged(ContextChangedEvent e){
     incrementalRedraw();
   }
   
   @Override
   public void contentChanged(DataStoreEvent e) {
     synchronizedRedraw();
   }

   /**
    * Factory
    * 
    * @author Erich Schubert
    * 
    * @apiviz.stereotype factory
    * @apiviz.uses PTreeMBRVisualization oneway - - «create»
    * 
    * @param <NV> vector type
    */
   public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {

     /**
      * Constructor.
      */
     public Factory() {
       super();
     }

     @Override
     public Visualization makeVisualization(VisualizationTask task) {
       return new PTreeMBRVisualization<NV, RStarTreeNode, SpatialEntry>(task);
     }

     @Override
     public void processNewResult(HierarchicalResult baseResult, Result result) {
       ArrayList<AbstractRStarTree<RStarTreeNode, SpatialEntry>> trees = ResultUtil.filterResults(result, AbstractRStarTree.class);
       for(AbstractRStarTree<RStarTreeNode, SpatialEntry> tree : trees) {
         if(tree instanceof Result) {
           Iterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ParallelPlotProjector.class);
           for(ParallelPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
             final VisualizationTask task = new VisualizationTask(NAME, (Result) tree, p.getRelation(), this);
             task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_BACKGROUND + 2);
             baseResult.getHierarchy().add((Result) tree, task);
             baseResult.getHierarchy().add(p, task);
           }
         }
       }
     }
   }
 }
