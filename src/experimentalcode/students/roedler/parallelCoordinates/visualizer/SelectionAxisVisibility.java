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
 * interactive SVG-Element for selecting visible axis.
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
  
  private Element border;
  private Element[] rect;
  private int c;
  
  double hs = proj.getSizeY() / 35.;
  double qs = hs / 2.;
  double cs = hs / 8.;
  double bhs = (proj.getSizeY() / 35.) * 0.75;
  double hbs = bhs / 2.;
  double ypos = proj.getMarginY() * 1.5 + proj.getAxisHeight() + hs / 8;
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public SelectionAxisVisibility(VisualizationTask task) {
    super(task);
    incrementalRedraw();
    context.addContextChangeListener(this);
  }
  
  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    int dim = DatabaseUtil.dimensionality(rep);

    
   // Element back = svgp.svgRect(proj.getXpos(0) - 8.0, 115.0, (proj.getXpos(proj.getLastVisibleDimension()) - proj.getMarginX()) + 16.0, 4.0);
    Element back = svgp.svgRect(0.0, (proj.getMarginY() * 1.5 + proj.getAxisHeight()), proj.getSizeX(), proj.getSizeY() / 35.);
    SVGUtil.addCSSClass(back, SELECTAXISVISIBILITY);
    layer.appendChild(back);
    
    
    
    int notvis = 0;
    int lastvis = 0;
    boolean ls = true;
    c = 0;
    rect = new Element[dim];
    
    for (int i = 0; i <= dim; i++){

      if (i == dim || proj.isVisible(i)) {
        if (notvis != 0){
          addEmptyButton(lastvis, i, notvis, dim, ls);
        }

        if (i == dim) { break; }
        
        double xpos = proj.getXpos(i) - bhs / 2.;        
        
        border = svgp.svgRect(xpos, ypos, bhs, bhs);
    //    border = svgp.svgRect(xpos, 115.5, 3.0, 3.0);
        SVGUtil.addCSSClass(border, SAV_BORDER);
        layer.appendChild(border);
      
        Element cross = svgp.svgLine(xpos + cs, ypos + cs, xpos + cs + qs, ypos + cs + qs);
    //    Element cross = svgp.svgLine(xpos + 0.5, 116.0, xpos + 2.5, 118.0);
        SVGUtil.addCSSClass(cross, SAV_CROSS);
        layer.appendChild(cross);
        
        Element cross2 = svgp.svgLine(xpos + cs + qs, ypos + cs, xpos + cs, ypos + cs + qs);
    //    Element cross2 = svgp.svgLine(xpos + 2.5, 116.0, xpos + 0.5, 118.0);
        SVGUtil.addCSSClass(cross2, SAV_CROSS);
        layer.appendChild(cross2);
      
        rect[c] = svgp.svgRect(xpos, ypos, bhs, bhs);
    //    rect[c] = svgp.svgRect(xpos, 115.5, 3.0, 3.0);
        SVGUtil.addCSSClass(rect[c], SAV_BUTTON);
        addEventListener(rect[c], c);
        layer.appendChild(rect[c]);
        
        lastvis = i;
        notvis = 0;
        ls = false;
        c++;
      }
      else {
        notvis++;
      }
    }
  }
  
  private void addEmptyButton(int last, int vis, int notvis, int dim, boolean ls){
    double dist;
    
    if (notvis > 2 && ((last == 0 && ls) || vis == dim)){

      dist = (cs + qs) * 2.;
      // dist = 5.0;
      
      if (vis == dim){
      
        for (int j = 0; j < notvis; j++){
          border = svgp.svgRect(proj.getXpos(last) + dist - hbs, ypos - j * dist, bhs, bhs);
          //border = svgp.svgRect(proj.getXpos(last) +  dist - 1.5, 115.5 - j * 5.0, 3.0, 3.0);
          SVGUtil.addCSSClass(border, SAV_BORDER);
          layer.appendChild(border);
          
          rect[c] = svgp.svgRect(proj.getXpos(last) + dist - hbs, ypos - j * dist, bhs, bhs);
          //rect[c] = svgp.svgRect(proj.getXpos(last) + dist - 1.5, 115.5 - j * 5.0, 3.0, 3.0);
          SVGUtil.addCSSClass(rect[c], SAV_BUTTON);
          addEventListener(rect[c], c);
          layer.appendChild(rect[c]);
          c++;
        }
      }
      else {
        for (int j = 0; j < notvis; j++){
          border = svgp.svgRect(proj.getXpos(last) + dist - hbs, (ypos - (notvis - 1) * dist) + j * dist, bhs, bhs);
         // border = svgp.svgRect(proj.getXpos(last) +  dist - 1.5, (115.5 - (notvis - 1) * 5.0) + j * 5.0, 3.0, 3.0);
          SVGUtil.addCSSClass(border, SAV_BORDER);
          layer.appendChild(border);
          
          rect[c] = svgp.svgRect(proj.getXpos(last) + dist - hbs, (ypos - (notvis - 1) * dist) + j * dist, bhs, bhs);
          //rect[c] = svgp.svgRect(proj.getXpos(last) + dist - 1.5, (115.5 - (notvis - 1) * 5.0) + j * 5.0, 3.0, 3.0);
          SVGUtil.addCSSClass(rect[c], SAV_BUTTON);
          addEventListener(rect[c], c);
          layer.appendChild(rect[c]);
          c++;
        }
      }
    }
    else {
      if (vis == dim) { dist = proj.getMarginX() / (notvis + 1.); } //dist = 10.0 / (notvis + 1); }
      else { dist = (proj.getXpos(vis) - proj.getXpos(last)) / ((double)notvis + 1.0); }
  
      for (int j = 0; j < notvis; j++){
        border = svgp.svgRect(proj.getXpos(last) + (1 + j) * dist - hbs, ypos, bhs, bhs);
        //border = svgp.svgRect(proj.getXpos(last) + (1 + j) * dist - 1.5, 115.5, 3.0, 3.0);
        SVGUtil.addCSSClass(border, SAV_BORDER);
        layer.appendChild(border);
        
        rect[c] = svgp.svgRect(proj.getXpos(last) + (1 + j) * dist - hbs, ypos, bhs, bhs);
        //rect[c] = svgp.svgRect(proj.getXpos(last) + (1 + j) * dist - 1.5, 115.5, 3.0, 3.0);
        SVGUtil.addCSSClass(rect[c], SAV_BUTTON);
        addEventListener(rect[c], c);
        layer.appendChild(rect[c]);
        c++;
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
        if (proj.getVisibleDimensions() > 2 || !proj.isVisible(i)){
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

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}
