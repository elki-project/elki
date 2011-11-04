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
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
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
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class SelectionDimensionOrder<NV extends NumberVector<NV, ?>> extends ParallelVisualization<NV> implements ContextChangeListener {

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String SELECTDIMENSIONORDER = "SelectDimensionOrder";
  

  /**
   * CSS class for a tool button
   */
  public static final String SDO_BUTTON = "SDObutton";
  
  /**
   * CSS class for a button border
   */
  public static final String SDO_BORDER = "SDOborder";
  
  /**
   * CSS class for a button cross
   */
  public static final String SDO_ARROW = "SDOarrow";
  
  private int selecteddim = -1;
  private boolean selected = false;
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public SelectionDimensionOrder(VisualizationTask task) {
    super(task);
    incrementalRedraw();
    context.addContextChangeListener(this);
  }
  
  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    int dim = DatabaseUtil.dimensionality(rep);

    Element arrow;
    Element button;
    double last = -1.;
    
    double as = proj.getSizeY() / 70.;
    double bs = as * 1.5;
    double hbs = bs / 2.;
    double qas = as / 4.;
    double ypos = proj.getMarginY() * 2. + proj.getAxisHeight();
    double dist = 2.5 * as; 

      Element back = svgp.svgRect(0.0, (proj.getMarginY() * 2. + proj.getAxisHeight()), proj.getSizeX(), proj.getSizeY() / 35.);
      SVGUtil.addCSSClass(back, SELECTDIMENSIONORDER);
      layer.appendChild(back);
      
    for (int i = 0; i < dim; i++){
      if (proj.isVisible(i)){
        if (!selected){
          int j = 0;
          int end = 3;
          if(i == 0 || i == proj.getFirstVisibleDimension()){j = 1; }
          if (i == proj.getLastVisibleDimension()){end = 2; }
          for (; j < end; j++){
            arrow = getArrow(j, (proj.getXpos(i) - dist) + j * dist, ypos + as, as);
            SVGUtil.addCSSClass(arrow, SDO_ARROW);
            layer.appendChild(arrow);
            button = svgp.svgRect((proj.getXpos(i) - (dist + hbs)) + j * dist, ypos + qas, bs, bs);
            SVGUtil.addCSSClass(button, SDO_BUTTON);
            addEventListener(button, i, j);
            layer.appendChild(button);
          }
        }
        else{
          arrow = getArrow(3, proj.getXpos(i), ypos + as, as);
          SVGUtil.addCSSClass(arrow, SDO_ARROW);
          layer.appendChild(arrow);
          button = svgp.svgRect((proj.getXpos(i) - hbs) , ypos + qas, bs, bs);
          SVGUtil.addCSSClass(button, SDO_BUTTON);
          addEventListener(button, i, 3);
          layer.appendChild(button);
          
          if (last > 0.){
            arrow = getArrow(3, last + ((proj.getXpos(i)) - last) / 2., ypos + as, as);
            SVGUtil.addCSSClass(arrow, SDO_ARROW);
            layer.appendChild(arrow);
            button = svgp.svgRect(last + (((proj.getXpos(i)) - last) / 2.) - hbs, ypos + qas, bs, bs);
            SVGUtil.addCSSClass(button, SDO_BUTTON);
            addEventListener(button, i, 4);
            layer.appendChild(button);
          }
          last = proj.getXpos(i);
        }
      }
    }
  }
  
  /**
   * Add an event listener to the Element
   * 
   * @param tag Element to add the listener
   * @param i represented axis
   */
  private void addEventListener(final Element tag, final int i, final int j) {
    EventTarget targ = (EventTarget) tag;
    targ.addEventListener(SVGConstants.SVG_EVENT_CLICK, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
   
 /*       if (i == -1){
          hide = !hide;
        }*/
        if (j == 1){
          selected = true;
          selecteddim = i;
        }
        if (j == 3 || j == 4){
          if (j == 3){
            proj.swapDimensions(selecteddim, i);
          }
          else {
            if (selecteddim != i) {
              proj.shiftDimension(selecteddim, i);
            }
          }
          selected = false;
          selecteddim = -1;
        }
        if (j == 0 || j == 2){
          if (j == 0){
            proj.swapDimensions(i, proj.getLastVisibleDimension(i));
          }
          else {
            proj.swapDimensions(i, proj.getNextVisibleDimension(i));
          }
        }
        
        incrementalRedraw();
        context.fireContextChange(null);
      }
    }, false);
  }
  
  private Element getArrow(int dir, double x, double y, double size){
    SVGPath path = new SVGPath();
    double hs = size / 2.;
    
    switch (dir) {
      case 0: {
        path.drawTo(x + hs, y + hs);
        path.drawTo(x - hs, y);
        path.drawTo(x + hs, y - hs);
        path.drawTo(x + hs, y + hs);
        break;
      }
      case 1: {
        path.drawTo(x - hs, y - hs);
        path.drawTo(x + hs, y - hs);
        path.drawTo(x, y + hs);
        path.drawTo(x - hs, y - hs);
        break;
      }
      case 2: {
        path.drawTo(x - hs, y - hs);
        path.drawTo(x + hs, y);
        path.drawTo(x - hs, y + hs);
        path.drawTo(x - hs, y - hs);
        break;
      }
      case 3: {
        
        path.drawTo(x - hs, y + hs);
        path.drawTo(x, y - hs);
        path.drawTo(x + hs, y + hs);
        path.drawTo(x - hs, y + hs);
      }
    }
    
    path.close();
    return path.makeElement(svgp);
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
    if(!svgp.getCSSClassManager().contains(SELECTDIMENSIONORDER)) {
      CSSClass cls = new CSSClass(this, SELECTDIMENSIONORDER);
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.1);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLUE_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
    if(!svgp.getCSSClassManager().contains(SDO_BORDER)) {
      CSSClass cls = new CSSClass(this, SDO_BORDER);
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_GREY_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) / (proj.getScale() *2.0));
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
    if(!svgp.getCSSClassManager().contains(SDO_BUTTON)) {
      CSSClass cls = new CSSClass(this, SDO_BUTTON);
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.01);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_GREY_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
    if(!svgp.getCSSClassManager().contains(SDO_ARROW)) {
      CSSClass cls = new CSSClass(this, SDO_ARROW);
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_DARKGREY_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) / (proj.getScale() * 1.5));
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
  }
  
  /**
   * Factory for dimension selection visualizer
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
    private static final String NAME = "Selection Dimension Order";

    /**
     * Constructor, adhering to
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionDimensionOrder<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Iterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(result, ParallelPlotProjector.class);
      for(ParallelPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
        final VisualizationTask task = new VisualizationTask(NAME, p, p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
        task.put(VisualizationTask.META_NOEXPORT, true);
        task.put(VisualizationTask.META_NOTHUMB, true);
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
