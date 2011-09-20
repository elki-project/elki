package experimentalcode.students.roedler.parallelCoordinates.visualizer;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import experimentalcode.students.roedler.parallelCoordinates.projector.ParallelPlotProjector;
import experimentalcode.students.roedler.parallelCoordinates.visualizer.ParallelVisualization;


/**
 * Generates a SVG-Element containing axes, including labeling.
 * 
 * @author Robert Rödler
 * 
 * @apiviz.uses SVGSimpleLinearAxis
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class SelectionAxisVisibility<NV extends NumberVector<NV, ?>> extends ParallelVisualization<NV> implements ContextChangeListener {

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String SELECTAXISVISIBILITY = "SelectAxisVisibility";
  

  /**
   * CSS class for a tool button
   */
  public static final String SAV_BUTTON = "SAVbutton";
  
  /**
   * CSS class for a button border
   */
  public static final String SAV_BORDER = "SAVborder";
  
  /**
   * CSS class for a button cross
   */
  public static final String SAV_CROSS = "SAVbuttoncross";
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public SelectionAxisVisibility(VisualizationTask task) {
    super(task);
    incrementalRedraw();
  }
  
  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    int dim = DatabaseUtil.dimensionality(rep);

    
    Element back = svgp.svgRect(proj.getXpos(0) - 5.0, 115.0, (proj.getXpos(dim - 1) - proj.getMarginX()) + 10.0, 4.0);
    SVGUtil.addCSSClass(back, SELECTAXISVISIBILITY);
    layer.appendChild(back);
    
    Element border;
    Element[] rect = new Element[dim];
    
    int notvis = 0;
    int lastvis = 0;
    int c = 0;
    
    for (int i = 0; i < dim; i++){
      if (proj.isVisible(i)){
        if (notvis != 0){
          double dist = (proj.getXpos(i) - proj.getXpos(lastvis)) / ((double)notvis + 1.0);
          for (int j = 0; j < notvis; j++){
            border = svgp.svgRect(proj.getXpos(lastvis) + (1 + j) * dist - 1.5, 115.5, 3.0, 3.0);
            SVGUtil.addCSSClass(border, SAV_BORDER);
            layer.appendChild(border);
            
            rect[c] = svgp.svgRect(proj.getXpos(lastvis) + (1 + j) * dist - 1.5, 115.5, 3.0, 3.0);
            SVGUtil.addCSSClass(rect[c], SAV_BUTTON);
            addEventListener(rect[c], c);
            layer.appendChild(rect[c]);
            c++;
          }
        }
        double xpos = proj.getXpos(i) - 1.5;
        
        border = svgp.svgRect(xpos, 115.5, 3.0, 3.0);
        SVGUtil.addCSSClass(border, SAV_BORDER);
        layer.appendChild(border);
        
        Element cross = svgp.svgLine(xpos + 0.5, 116.0, xpos + 2.5, 118.0);
        SVGUtil.addCSSClass(cross, SAV_CROSS);
        layer.appendChild(cross);
        
        Element cross2 = svgp.svgLine(xpos + 2.5, 116.0, xpos + 0.5, 118.0);
        SVGUtil.addCSSClass(cross2, SAV_CROSS);
        layer.appendChild(cross2);
        
        rect[c] = svgp.svgRect(xpos, 115.5, 3.0, 3.0);
        SVGUtil.addCSSClass(rect[c], SAV_BUTTON);
        addEventListener(rect[c], c);
        layer.appendChild(rect[c]);
        
        lastvis = i;
        notvis = 0;
        c++;
      }
      else {
        notvis++;
      }
    }
  
  }
  
  /**
   * Add an event listener to the Element
   * 
   * @param tag Element to add the listener
   * @param tool Tool represented by the Element
   */
  private void addEventListener(final Element tag, final int i) {
    EventTarget targ = (EventTarget) tag;
    targ.addEventListener(SVGConstants.SVG_EVENT_CLICK, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (proj.getVisibleDimensions() > 2){
          proj.setVisible(!proj.isVisible(i), i);
          incrementalRedraw();
          context.fireContextChange(null);
        }
      }
    }, false);
  }
  
  public void contextChanged(ContextChangedEvent e){
    synchronizedRedraw();
  }
  
  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    final StyleLibrary style = context.getStyleLibrary();
    if(!svgp.getCSSClassManager().contains(SELECTAXISVISIBILITY)) {
      CSSClass cls = new CSSClass(this, SELECTAXISVISIBILITY);
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.1);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLUE_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
    if(!svgp.getCSSClassManager().contains(SAV_BORDER)) {
      CSSClass cls = new CSSClass(this, SAV_BORDER);
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_GREY_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) / 2.0);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
    if(!svgp.getCSSClassManager().contains(SAV_BUTTON)) {
      CSSClass cls = new CSSClass(this, SAV_BUTTON);
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.01);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_GREY_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
    if(!svgp.getCSSClassManager().contains(SAV_CROSS)) {
      CSSClass cls = new CSSClass(this, SAV_CROSS);
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
  }
  
  /**
   * Factory for axis visualizations
   * 
   * @author Erich Schubert
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
    private static final String NAME = "Selection Axis Visibility";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionAxisVisibility<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Iterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(result, ParallelPlotProjector.class);
      for(ParallelPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
        final VisualizationTask task = new VisualizationTask(NAME, p, p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
        task.put(VisualizationTask.META_NOEXPORT, true);
        baseResult.getHierarchy().add(p, task);
      }
    }
    
/*    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Iterator<Relation<? extends NumberVector<?, ?>>> reps = VisualizerUtil.iterateVectorFieldRepresentations(result);
      for(Relation<? extends NumberVector<?, ?>> rel : IterableUtil.fromIterator(reps)) {
     //   final VisualizationTask task = new VisualizationTask(NAME, rel, rel, this, ParallelVisualization.class);
        final VisualizationTask task = new VisualizationTask(NAME, rel, rel, this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
        task.put(VisualizationTask.META_NOEXPORT, true);
        baseResult.getHierarchy().add(rel, task);
      }
    }*/

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}
