package experimentalcode.students.goldhofa.visualizers.visunproj;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MouseEvent;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import experimentalcode.students.goldhofa.CCConstants;
import experimentalcode.students.goldhofa.ClusteringComparison;
import experimentalcode.students.goldhofa.ClusteringComparisonResult;
import experimentalcode.students.goldhofa.Color;
import experimentalcode.students.goldhofa.SegmentID;
import experimentalcode.students.goldhofa.Segments;


/**
 * Visualizer to draw circle segments of clusterings
 * 
 * BUG: double precision not enough for complete circle (?)
 * 
 * TODO
 *  - remove completely unpaired segments
 * 
 * @author Sascha Goldhofer
 */
public class CircleSegmentsVisualizer extends AbstractVisFactory implements ContextChangeListener {
  
  /**
   * CircleSegments visualizer name
   */
  private static final String NAME = "CircleSegments";
  
  /**
   * Comparison Result of {@link ClusteringComparison}
   */
  private ClusteringComparisonResult ccr;
  
  /**
   * Segmentation of Clusterings
   */
  private Segments segments;
  
  /**
   * Pairsegments
   */
  private TreeMap<SegmentID, Integer> pairSegments;
  
  /**
   * Max number of clusters of a clustering
   */
  private int clusterSize;
  
  /**
   * Number of clusterings (rings)
   */
  private int clusterings;
  
  /**
   * 
   */
  private Element layer;
  
  /**
   * CSS class name for the clusterings.
   */
  private static final String CLUSTERID = "cluster";
  
  /**
   * Center of CircleSegments
   */
  private Point2D.Double center = new Point2D.Double(0.5, 0.5);
  
  /**
   * Properties of a Segment
   */
  private static enum Properties {
    
    // Constant values
    CLUSTERING_DISTANCE(0.01),    // Margin between two rings
    CLUSTER_MIN_WIDTH(0.01),      // Minimum width (radian) of Segment
    CLUSTER_DISTANCE(0.01),       // Margin (radian) between segments
    RADIUS_INNER(0.05),           // Offset from center to first ring
    RADIUS_OUTER(0.48),           // Radius of while CircleSegments
    
    // Calculated Values
    ANGLE_PAIR(0.0),              // width of a pair (radian)
    BORDER_WIDTH(0.0),            // Width of cluster borders
    CLUSTER_MIN_COUNT(0.0),       // Count of clusters needed to be resized 
    PAIR_MIN_COUNT(0.0),          // Less Paircount needs resizing
    RADIUS_DELTA(0.0);            // Height of a clustering (ring)
    
    // getter/setter
    double value;  
    Properties(double value) { this.value = value; }
    public double getValue() { return this.value; }
    public void setValue(double newval) { this.value = newval; }
  }
  
  /**
   * Color coding of CircleSegments
   */
  private static enum Colors { BORDER("#FF0073"),
     HOVER_ALPHA("1.0"), HOVER_INCLUSTER("#008e9e"), HOVER_SELECTION("#73ff00"), HOVER_PAIRED("#4ba600"), HOVER_UNPAIRED("#b20000"),
     HOVER_SUBSET("#009900"), HOVER_INTERSECTION("#990000");
  
    // getter/setter
    String color;  
    Colors(String color) { this.color = color; }
    public String getColor() { return this.color; }
  }
  
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public CircleSegmentsVisualizer() {
    super();
  }
  
  public void redraw() {
    
    this.contextChanged(null);
  }
  
  @Override
  public void contextChanged(ContextChangedEvent e) {
/*
    DBIDSelection selContext = context.getSelection();
    
    if (selContext != null) {
      
      Database<?> database = context.getDatabase();
      DBIDs selection = selContext.getSelectedIds();
      
      //final double linewidth = 3 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT);
      
      NodeList elements = layer.getChildNodes();
      
      
      // TODO for pairs (?)
      
      SortedSet<String> selectedSegments = new TreeSet<String>();
      for (DBID id : selection) {

        selectedSegments.add(segments.getSegmentID(id).toString());        
      }

      for (int i=0; i<elements.getLength(); i++) {
        
        Element current = (Element) elements.item(i);

        if (current.hasAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE)) {
          
          // Search Element and flag as Selected
          
          String currentSegment = current.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE);
          
          if (selectedSegments.contains(currentSegment)) {
            
            //System.out.println("select");
            SVGUtil.addCSSClass(current, CCConstants.CLR_SELECTED_CLASS);
            current.setAttribute(CCConstants.CLR_SELECTED_ATTRIBUTE, "DBSELECTION");

          } else {
            
            SVGUtil.removeCSSClass(current, CCConstants.CLR_SELECTED_CLASS);
            current.removeAttribute(CCConstants.CLR_SELECTED_ATTRIBUTE);                        
          }
        }        
      }
      
        
        //System.out.println(segments.getSegmentID(i).toString());
        /*
        double[] v = proj.fastProjectDataToRenderSpace(database.get(i));
        Element dot = svgp.svgCircle(v[0], v[1], linewidth);
        SVGUtil.addCSSClass(dot, MARKER);
        layer.appendChild(dot);
        *
    }
    
    //currentSelection = (short)(currentSelection % 32766);
  */
  }
  
  
  @Override
  public void addVisualizers(VisualizerContext context, Result result) {
    
    // If no comparison result found abort
    List<ClusteringComparisonResult> ccr = ResultUtil.filterResults(result, ClusteringComparisonResult.class);
    if (ccr.size() != 1) return;
    
    final VisualizationTask task = new VisualizationTask(NAME, context, ccr.get(0), null, this, null);
    task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
    context.addVisualizer(ccr.get(0), task);
  }
  
 /* 
  public void init(VisualizerContext context, ClusteringComparisonResult ccr) {
    super.init(context);
    
    
  }
  */

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    
    VisualizerContext context = task.getContext();
    SVGPlot svgp = task.getPlot();
    
    ccr = task.getResult();
    
    //
    // init
    //

    this.segments = ccr.getSegments();
    this.layer    = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    
    //
    context.addContextChangeListener(this);
    
    //
    pairSegments = segments.getSegments();
    clusterSize = segments.getHighestClusterCount();
    clusterings = this.segments.getClusterings();
    
    Properties.ANGLE_PAIR.setValue((2*Math.PI-(Properties.CLUSTER_DISTANCE.getValue()*pairSegments.size()))/segments.getPairCount());
    Properties.PAIR_MIN_COUNT.setValue(Math.ceil(Properties.CLUSTER_MIN_WIDTH.getValue()/Properties.ANGLE_PAIR.getValue()));
    
    // number of segments needed to be resized
    int segMinCount = 0;
    for (Integer size : pairSegments.values()) {
      
      if(size <= Properties.PAIR_MIN_COUNT.getValue()) segMinCount++;
    }
    Properties.CLUSTER_MIN_COUNT.setValue(segMinCount);
    
    // update width of a pair
    Properties.ANGLE_PAIR.setValue((2*Math.PI-(Properties.CLUSTER_DISTANCE.getValue()*pairSegments.size()+segMinCount*Properties.CLUSTER_MIN_WIDTH.getValue()))/(segments.getPairCount()-Properties.CLUSTER_MIN_COUNT.getValue()));
    
    Properties.RADIUS_DELTA.setValue((Properties.RADIUS_OUTER.getValue()-Properties.RADIUS_INNER.getValue()-clusterings*Properties.CLUSTERING_DISTANCE.getValue())/(clusterings));
    Properties.BORDER_WIDTH.setValue(Properties.CLUSTER_DISTANCE.getValue());
   

    //
    // CSS classes
    //
    
    addCSSClasses(svgp);
    
    // Get color gradient for clusters and their CSS Class
    String[] clusterColorShades = getGradient(clusterSize, Color.getColorSet(Color.ColorSet.GREY));
    CSSClass[] cssClr = new CSSClass[clusterSize];
    
    for (int i=0; i<clusterSize; i++) {
      
      cssClr[i] = new CSSClass(this, CLUSTERID+"_"+(i+1));
      cssClr[i].setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, clusterColorShades[i]);
      cssClr[i].setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      svgp.addCSSClassOrLogError(cssClr[i]);
    }
    
    
    //
    // Events
    //
    
    EventListener mouseOver = new MouseOverSegmentCluster();
    EventListener mouseOut = new MouseOutSegmentCluster();
    EventListener mouseClick = new MouseClickSegmentCluster(context, segments);
    
   
    //
    // Draw Circle Segments
    //
    
    int refClustering = 0;
    int refSegment = 0;
    double offsetAngle = 0;

    for (SegmentID id : pairSegments.descendingKeySet()) {
      
      int currentPairCount = pairSegments.get(id);
      
      // resize small segments if below minimum
      double alpha = Properties.CLUSTER_MIN_WIDTH.getValue();
      if (currentPairCount > Properties.PAIR_MIN_COUNT.getValue()) alpha = Properties.ANGLE_PAIR.getValue()*currentPairCount;

      // draw segment for every clustering if clusterID != 0 (unpaired)
      for (int i=0; i<id.size(); i++) {
        
        double currentRadius = i*(Properties.RADIUS_DELTA.getValue()+Properties.CLUSTERING_DISTANCE.getValue())+Properties.RADIUS_INNER.getValue();
        
        if ((refSegment != id.get(refClustering)) && refClustering==i) {
          
          Element border = getSegment(offsetAngle-Properties.CLUSTER_DISTANCE.getValue(), center, Properties.BORDER_WIDTH.getValue(), currentRadius, Properties.RADIUS_OUTER.getValue()-Properties.CLUSTERING_DISTANCE.getValue()).makeElement(svgp);
          border.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_BORDER_CLASS);
          layer.appendChild(border);  
          
          if (id.get(refClustering) == 0) refClustering = Math.min(refClustering+1, clusterings-1);
          
          refSegment = id.get(refClustering);                    
        }
        
        int cluster = id.get(i);
        if (cluster != 0) {

          //SVGPath segmentPath = getSegment(offsetAngle, center, alpha, currentRadius, currentRadius+radiusDelta);
          //Element segment = segmentPath.makeElement(svgp);
          Element segment = getSegment(offsetAngle, center, alpha, currentRadius, currentRadius+Properties.RADIUS_DELTA.getValue()).makeElement(svgp);
          /*
          Element segmentShadow = (Element) segment.cloneNode(false);
          segmentShadow.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#ccc");
          segmentShadow.setAttribute(SVGConstants.SVG_FILTER_ATTRIBUTE, "url(#shadow)");
          layer.appendChild(segmentShadow);
          */
          
          segment.setAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE, ""+cluster);
          segment.setAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE, ""+i);
          segment.setAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE, id.toString());
          //segment.setAttribute(CCConstants.SEG_PAIRCOUNT_ATTRIBUTE, pairSegments.get(id).toString());
          //segment.setAttribute(CCConstants.CLR_PAIRCOUNT_ATTRIBUTE, ""+segments.getPairCount(i, cluster));

          
          // Mouseevent on segment cluster
          EventTarget targ = (EventTarget) segment;
          targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, mouseOver, false);
          targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, mouseOut, false);
          targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, mouseClick, false);
          
          // Coloring based on clusterID
          // TODO color coding between rings
          segment.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, cssClr[id.get(i)-1].getName());
          
          layer.appendChild(segment);
        }
      }
      
      offsetAngle += alpha+Properties.CLUSTER_DISTANCE.getValue();
    }

    
    //SortableList sortableList = new SortableList(svgp);
    
    /*
    Element item = SVGUtil.svgRect(svgp.getDocument(), 0.0, 0.0, 0.15, 0.03);
//    Element itemContent = SVGUtil.svgRect(svgp.getDocument(), 0.03, 0.03, 0.24, 0.04);
//    itemContent.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#f00");
//    item.appendChild(itemContent);
    
    for (int i=0; i<clusterings; i++) {
      
      Element newItem = (Element)item.cloneNode(false);
      newItem.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, cssClr[i].getName());
      newItem.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#000");
      newItem.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.001");
      sortableList.addListItem(newItem);
    }

//    Element item2 = SVGUtil.svgRect(svgp.getDocument(), 0.0, 0.0, 0.3, 0.1);
//    item2.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#0f0");
//    sortableList.addListItem(item2);
//    
//    Element item3 = SVGUtil.svgRect(svgp.getDocument(), 0.0, 0.0, 0.3, 0.1);
//    item3.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#00f");
//    sortableList.addListItem(item3);

    /*
    ListItem item = new ListItem(svgp);

    Element itemContent = SVGUtil.svgRect(svgp.getDocument(), 0.0, 0.0, 0.3, 0.1);
    itemContent.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#ccc");
    itemContent.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#000");
    itemContent.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.001");
    item.addContent(itemContent, 0.0, 0.0);

    Element label = svgp.svgText(0, 0.7, "Clustering 1");
    label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.015");
    item.addContent(label, 0.01, 0.02);

    sortableList.addListItem(item);
    
    //*/
    
    //layer.appendChild(sortableList.getContainer());

    // TODO hack -> loadSelection (naming | different visualizer)
    redraw();
    
    return new StaticVisualization(task, layer);
  }
  
  
  /**
   * Define and add required CSS classes
   * 
   * @param svgp
   */
  private void addCSSClasses(SVGPlot svgp) {
    
    // CLUSTER BORDER
    CSSClass cssReferenceBorder = new CSSClass(this, CCConstants.CLR_BORDER_CLASS);
    cssReferenceBorder.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, Colors.BORDER.getColor());
    svgp.addCSSClassOrLogError(cssReferenceBorder);
    
    // CLUSTER HOVER
    CSSClass cluster_hover = new CSSClass(this, CCConstants.CLR_HOVER_CLASS);
    cluster_hover.setStatement(SVGConstants.SVG_FILL_OPACITY_ATTRIBUTE, Colors.HOVER_ALPHA.getColor() );
    cluster_hover.setStatement(SVGConstants.SVG_CURSOR_TAG, SVGConstants.SVG_POINTER_VALUE );
    svgp.addCSSClassOrLogError(cluster_hover);
    
    CSSClass cluster_selection = new CSSClass(this, CCConstants.CLR_HOVER_SELECTION_CLASS);
    cluster_selection.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, Colors.HOVER_SELECTION.getColor()+" !important");
    cluster_selection.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
    svgp.addCSSClassOrLogError(cluster_selection);
    
    CSSClass cluster_identical = new CSSClass(this, CCConstants.CLR_HOVER_INCLUSTER_CLASS);
    cluster_identical.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, Colors.HOVER_INCLUSTER.getColor()+" !important" );
    cluster_identical.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
    svgp.addCSSClassOrLogError(cluster_identical);
    
    CSSClass cluster_unpaired = new CSSClass(this, CCConstants.CLR_HOVER_UNPAIRED_CLASS);
    cluster_unpaired.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, Colors.HOVER_UNPAIRED.getColor()+" !important");
    cluster_unpaired.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
    svgp.addCSSClassOrLogError(cluster_unpaired);
    
    CSSClass cluster_paired = new CSSClass(this, CCConstants.CLR_HOVER_PAIRED_CLASS);
    cluster_paired.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, Colors.HOVER_PAIRED.getColor()+" !important");
    cluster_paired.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
    svgp.addCSSClassOrLogError(cluster_paired);    
    
    // CLUSTER SELECT
    CSSClass cluster_selected = new CSSClass(this, CCConstants.CLR_SELECTED_CLASS);
    cluster_selected.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, Colors.HOVER_INCLUSTER.getColor()+" !important");
    cluster_selected.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.002");
    svgp.addCSSClassOrLogError(cluster_selected);
    
/*   
    // simple shadow
    //    <defs>
    //    <filter id="filter" x="0" y="0">
    //      <feGaussianBlur stdDeviation="5"/>
    //      <feOffset dx="5" dy="5"/>
    //    </filter>
    //    </defs>
    
    Element def = (Element) svgp.getDefs();
    Element shadow = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_FILTER_TAG);
    shadow.setAttribute(SVGConstants.SVG_ID_ATTRIBUTE, "shadow");
    
    Element blur = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_FE_GAUSSIAN_BLUR_TAG);
    blur.setAttribute(SVGConstants.SVG_STD_DEVIATION_ATTRIBUTE, "0.0025");
    
    Element offset = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_FE_OFFSET_TAG);
    offset.setAttribute(SVGConstants.SVG_DY_ATTRIBUTE, "0.003");
    
    shadow.appendChild(blur);
    shadow.appendChild(offset);
    def.appendChild(shadow);
*/   
  }
  
  
  /**
   * Creates a gradient over a set of colors
   * 
   * @param shades  number of colors in the gradient
   * @param colors  colors for the gradient
   * @return        array of colors for css: "rgb(red, green, blue)"
   */
  private String[] getGradient(int shades, int[][] colors) {
    
    // only even shades
    shades += shades%2;
    
    int colorCount = colors.length; 
    String[] colorShades = new String[shades];
    
    if (shades <= 1 || colorCount <= 1) {
      
      colorShades[0] = "rgb("+colors[0][0]+","+colors[0][1]+","+colors[0][2]+")";
      return colorShades;
    }
    
    int colorDelta = shades/(colorCount-1);
    
    for (int s=0; s<shades; s++) {
      
      int from = s/colorDelta;
      int to = (s/colorDelta)+1;
      int step = s%colorDelta;
      
      int r = colors[from][0] - ((colors[from][0] - colors[to][0]) / colorDelta) * step;
      int g = colors[from][1] - ((colors[from][1] - colors[to][1]) / colorDelta) * step;
      int b = colors[from][2] - ((colors[from][2] - colors[to][2]) / colorDelta) * step;
      
      colorShades[s] = Color.toCSS(r, g, b);
    }
    
    return colorShades;
  }
  
  
  /**
   * Returns a single Segment as a Path
   * 
   * @param segment
   * @param center
   * @param alpha
   * @param innerRadius
   * @param outerRadius
   * @return
   *
  private SVGPath getSegment(int segment, Point2D.Double center, double alpha, double innerRadius, double outerRadius) {
    
    double sin1st = Math.sin((segment)*alpha);
    double cos1st = Math.cos((segment)*alpha);
    
    double sin2nd = Math.sin((segment+1)*alpha);
    double cos2nd = Math.cos((segment+1)*alpha);
    
    Point2D.Double inner1st = new Point2D.Double(center.x + (innerRadius * sin1st), center.y - (innerRadius * cos1st));
    Point2D.Double outer1st = new Point2D.Double(center.x + (outerRadius * sin1st), center.y - (outerRadius * cos1st));
    
    Point2D.Double inner2nd = new Point2D.Double(center.x + (innerRadius * sin2nd), center.y - (innerRadius * cos2nd));
    Point2D.Double outer2nd = new Point2D.Double(center.x + (outerRadius * sin2nd), center.y - (outerRadius * cos2nd));
     
    SVGPath path = new SVGPath(inner1st.x, inner1st.y);
    path.lineTo(outer1st.x, outer1st.y);
    path.ellipticalArc(outerRadius, outerRadius, 0, 0, 1, outer2nd.x, outer2nd.y);
    path.lineTo(inner2nd.x, inner2nd.y);
    path.ellipticalArc(innerRadius, innerRadius, 0, 0, 0, inner1st.x, inner1st.y);
    
    return path;
  }*/
  
  @Override
  public Class<? extends Projection> getProjectionType() {
    return null;
  }

  private SVGPath getSegment(double angleOffset, Point2D.Double center, double alpha, double innerRadius, double outerRadius) {
    
    double sin1st = Math.sin(angleOffset);
    double cos1st = Math.cos(angleOffset);
    
    double sin2nd = Math.sin(angleOffset+alpha);
    double cos2nd = Math.cos(angleOffset+alpha);
    
    Point2D.Double inner1st = new Point2D.Double(center.x + (innerRadius * sin1st), center.y - (innerRadius * cos1st));
    Point2D.Double outer1st = new Point2D.Double(center.x + (outerRadius * sin1st), center.y - (outerRadius * cos1st));
    
    Point2D.Double inner2nd = new Point2D.Double(center.x + (innerRadius * sin2nd), center.y - (innerRadius * cos2nd));
    Point2D.Double outer2nd = new Point2D.Double(center.x + (outerRadius * sin2nd), center.y - (outerRadius * cos2nd));
     
    SVGPath path = new SVGPath(inner1st.x, inner1st.y);
    path.lineTo(outer1st.x, outer1st.y);
    path.ellipticalArc(outerRadius, outerRadius, 0, 0, 1, outer2nd.x, outer2nd.y);
    path.lineTo(inner2nd.x, inner2nd.y);
    path.ellipticalArc(innerRadius, innerRadius, 0, 0, 0, inner1st.x, inner1st.y);
    
    return path;
  }
  
  /*
  private void createSegments() {
  
    Point2D.Double firstStart   = new Point2D.Double(center.x, center.y-radiusStart);
    Point2D.Double firstEnd     = new Point2D.Double(center.x, center.y-radius);
    Point2D.Double secondStart  = new Point2D.Double();
    Point2D.Double secondEnd    = new Point2D.Double();  
    
    for (int i=0; i<segments; i++) {
      
      double sin = Math.sin((i+1)*alpha);
      double cos = Math.cos((i+1)*alpha);
      
      secondStart.x = center.x + (radiusStart * sin);
      secondStart.y = center.y - (radiusStart * cos);
      
      secondEnd.x = center.x + (radius * sin);
      secondEnd.y = center.y - (radius * cos);
       
      SVGPath path = new SVGPath(firstStart.x, firstStart.y);
      path.lineTo(firstEnd.x, firstEnd.y);
      path.ellipticalArc(radius, radius, 0, 0, 1, secondEnd.x, secondEnd.y);
      path.lineTo(secondStart.x, secondStart.y);
      path.ellipticalArc(radiusStart, radiusStart, 0, 0, 0, firstStart.x, firstStart.y);
      
      Element line = path.makeElement(svgp);
      line.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, csscls.getName());
      
      layer.appendChild(line);
      
      firstEnd.x    = secondEnd.x;
      firstEnd.y    = secondEnd.y;
      firstStart.x  = secondStart.x;
      firstStart.y  = secondStart.y;
    }
  }
  */
}

// TODO Ghost und Liste animieren
class MouseDragAndDrop implements EventListener {
  
  private SVGPlot svgp;
  private double lastMouseY;
  
  public MouseDragAndDrop(SVGPlot svgp) {
    super();
    
    this.svgp = svgp;
  }
  
  @Override
  public void handleEvent(Event evt) {

    if ( ! (evt instanceof MouseEvent)) {
      return;
    }
    
    Element rectangle = (Element) evt.getTarget();
    SVGPoint location = svgp.elementCoordinatesFromEvent(rectangle, evt);
    
    MouseEvent mouse = (MouseEvent) evt;
    
    if ( ! (mouse.getButton() == 0)) {
      lastMouseY = location.getY();
      return;
    }

    //java.awt.PointerInfo info = java.awt.MouseInfo.getPointerInfo();
    //java.awt.Point location = info.getLocation();

    double rectangleY = Double.valueOf(rectangle.getAttribute(SVGConstants.SVG_Y_ATTRIBUTE));
    rectangle.setAttribute(SVGConstants.SVG_Y_ATTRIBUTE, ""+(rectangleY+(location.getY()-lastMouseY)));
    lastMouseY = location.getY();
  }
}


class MouseOverSegmentCluster implements EventListener {
  
  public MouseOverSegmentCluster() {
    super();
  }
  
  @Override
  public void handleEvent(Event evt) {
    
    // hovered segment cluster
    Element thisSegmentCluster = (Element) evt.getTarget();
    SVGUtil.addCSSClass(thisSegmentCluster, CCConstants.CLR_HOVER_CLASS);
    SVGUtil.addCSSClass(thisSegmentCluster, CCConstants.CLR_HOVER_SELECTION_CLASS);
    
    // hovered clustering
    int thisClusteringID = Integer.valueOf(thisSegmentCluster.getAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE)).intValue();
    
    // hovered clusterID
    int thisClusterID = Integer.valueOf(thisSegmentCluster.getAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE)).intValue();
    
    // List of all segments (and others)
    NodeList segmentClusters = thisSegmentCluster.getParentNode().getChildNodes();
    
    // SegmentID
    String thisSegment = thisSegmentCluster.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE);
    SegmentID thisSegmentID = new SegmentID(thisSegment);

    
    //
    // flag segment clusters as followed:
    //
    // - current (hovered) segment : green => matching pairs
    // - cluster of hovered clustering : light green => complete pairset of selection
    // for every segment of hovered cluster
    // - different cluster : red => not in pairset of hovered cluster
    // - same cluster : light green => matching pairs
    // and
    // - cluster of selected segment : black (?) => visibility of cluster fragmentation
    //
    
    for (int i=0; i<segmentClusters.getLength(); i++) {
      
      Element current = (Element) segmentClusters.item(i);

      // just segments
      if (current.hasAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE)) {
        
        int currentClusteringID = Integer.valueOf(current.getAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE)).intValue();
        int currentClusterID = Integer.valueOf(current.getAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE)).intValue();
        
        // element = this clustering
        if (thisClusteringID == currentClusteringID) {
          
          // element = this cluster
          if (thisClusterID == currentClusterID) {
            
            if (current.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE) != thisSegment) {
              
              SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_CLASS);
              SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_PAIRED_CLASS);
            }
          }
          
        } else {
          
          SegmentID currentSegmentID = new SegmentID(current.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE));
          
          // element = selected cluster
          if (thisSegmentID.get(currentClusteringID) == currentClusterID) {
            
            // element = selected segment
            if (thisSegment.equals(current.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE))) {
              
              SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_CLASS);
              SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_SELECTION_CLASS);
              
            } else {
              
              // element = in cluster of selected clustering & element = selected cluster
              if (currentSegmentID.get(thisClusteringID) == thisClusterID) {
                
                SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_CLASS);
                SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_PAIRED_CLASS);
                
              } else {

                // outside selection but selected cluster
                SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_CLASS);
                SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_INCLUSTER_CLASS);
              }
            }
            
          } else {

            // element != selected cluster & element = selected cluster of selected clustering => unpaired
            if (currentSegmentID.get(thisClusteringID) == thisClusterID) {
             
              SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_CLASS);
              SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_UNPAIRED_CLASS);
            }            
          }
        }
      }
    }
  }
}

class MouseOutSegmentCluster implements EventListener {
  
  public MouseOutSegmentCluster() {
    super();
  }
  
  @Override
  public void handleEvent(Event evt) {
    
    Element segment = (Element) evt.getTarget();
    
    NodeList segments = segment.getParentNode().getChildNodes();
    for (int i=0; i<segments.getLength(); i++) {
      
      Element current = (Element) segments.item(i);
      SVGUtil.removeCSSClass(current, CCConstants.CLR_HOVER_CLASS);
      SVGUtil.removeCSSClass(current, CCConstants.CLR_HOVER_SELECTION_CLASS);
      SVGUtil.removeCSSClass(current, CCConstants.CLR_HOVER_INCLUSTER_CLASS);
      SVGUtil.removeCSSClass(current, CCConstants.CLR_HOVER_PAIRED_CLASS);
  
      SVGUtil.removeCSSClass(current, CCConstants.CLR_HOVER_UNPAIRED_CLASS);
    }
  }
}

/**
 * TODO CTRL for add/substract selections
 */
class MouseClickSegmentCluster implements EventListener {
  
  private VisualizerContext context;
  private Segments segments;
  private SortedSet<String> selection;
  
  private int selectionID = 0;
  private long lastClick = 0;
  
  MouseClickSegmentCluster(VisualizerContext context, Segments segments) {
    
    this.context = context;
    this.segments = segments;
    this.selection = new TreeSet<String>();
  }

  @Override
  public void handleEvent(Event evt) {
    
    // Check Double Click
    boolean dblClick = false;
    long time = java.util.Calendar.getInstance().getTimeInMillis();
    if (time-lastClick <= CCConstants.EVT_DBLCLICK_DELAY) dblClick = true;
    lastClick = time;
    
    // clicked segment cluster
    Element thisSegmentCluster = (Element) evt.getTarget();
    
    // clicked clustering
    int thisClusteringID = Integer.valueOf(thisSegmentCluster.getAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE)).intValue();
    
    // clicked clusterID
    int thisClusterID = Integer.valueOf(thisSegmentCluster.getAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE)).intValue();
    
    // List of all segments (and others)
    NodeList segmentClusters = thisSegmentCluster.getParentNode().getChildNodes();
    
    if (dblClick) {
      
      // whole cluster
      for (int i=0; i<segmentClusters.getLength(); i++) {
        
        Element current = (Element) segmentClusters.item(i);

        // just segments
        if (current.hasAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE)) {
          
          int currentClusteringID = Integer.valueOf(current.getAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE)).intValue();
          int currentClusterID = Integer.valueOf(current.getAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE)).intValue();
          
          // element = this clustering
          if (thisClusteringID == currentClusteringID) {
            
            // element = this cluster
            if (thisClusterID == currentClusterID) {
              
              if (thisSegmentCluster.hasAttribute(CCConstants.CLR_SELECTED_ATTRIBUTE)) {
                
                // select
                
                SVGUtil.addCSSClass(current, CCConstants.CLR_SELECTED_CLASS);
                current.setAttribute(CCConstants.CLR_SELECTED_ATTRIBUTE, String.valueOf(selectionID));
                
                selection.add(current.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE));
                
              } else {
                
                // deselect
                
                SVGUtil.removeCSSClass(current, CCConstants.CLR_SELECTED_CLASS);
                current.removeAttribute(CCConstants.CLR_SELECTED_ATTRIBUTE);
                current.removeAttribute(SVGConstants.SVG_STROKE_LINECAP_ATTRIBUTE);
              }
            }
          }
        }
      }
      
      if (thisSegmentCluster.hasAttribute(CCConstants.CLR_SELECTED_ATTRIBUTE)) selectionID++;
        
    } else {

      if (thisSegmentCluster.hasAttribute(CCConstants.CLR_SELECTED_ATTRIBUTE)) {
        
        // deselect
        
        SVGUtil.removeCSSClass(thisSegmentCluster, CCConstants.CLR_SELECTED_CLASS);
        thisSegmentCluster.removeAttribute(CCConstants.CLR_SELECTED_ATTRIBUTE);
        
        selection.remove(thisSegmentCluster.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE));
        
      } else {
        
        // select
        
        SVGUtil.addCSSClass(thisSegmentCluster, CCConstants.CLR_SELECTED_CLASS);
        thisSegmentCluster.setAttribute(CCConstants.CLR_SELECTED_ATTRIBUTE, String.valueOf(selectionID));
        
        selection.add(thisSegmentCluster.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE));
        
        selectionID++;
      }
    }

    /*
    
    // Set Objects selected
    DBIDSelection selContext = context.getSelection();
    HashSetModifiableDBIDs selectionToModify;
    
    selectionToModify = DBIDUtil.newHashSet();
    
    /*
    if(selContext == null) {
      selectionToModify = DBIDUtil.newHashSet();
    }
    else {
      selectionToModify = DBIDUtil.newHashSet(selContext.getSelectedIds());
    }
    *
    
    // get Objects in Selection   
    for (String segment : selection) {
      
      DBIDs objects = segments.getDBIDs(new SegmentID(segment));
      
      for (DBID id : objects) {
        
        selectionToModify.add(id);
      }
    }
    
    context.setSelection(new DBIDSelection(selectionToModify));
    */
  }
}
  
/*
@Deprecated
class MouseOverSegmentCluster_doitall implements EventListener {
  
  private String[] alphaClasses;
  private TreeMap<Integer, TreeMap<Integer, Integer>> participants;
  
  public MouseOverSegmentCluster_doitall(String[] alphaClasses) {
    super();
    
    this.alphaClasses = alphaClasses;
  }
  
  @Override
  public void handleEvent(Event evt) {
    
    participants = new TreeMap<Integer, TreeMap<Integer, Integer>>();

    Element thisSegmentCluster = (Element) evt.getTarget();
    //SVGUtil.addCSSClass(thisSegmentCluster, CCConstants.CLR_HOVER_CLASS); 
    
    int thisCluster = Integer.valueOf(thisSegmentCluster.getAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE)).intValue();
    int thisClustering = Integer.valueOf(thisSegmentCluster.getAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE)).intValue();
    String thisSegment = thisSegmentCluster.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE);
    SegmentID thisSegmentID = new SegmentID(thisSegment);
    
    NodeList segmentClusters = thisSegmentCluster.getParentNode().getChildNodes();


    // get all segments of this cluster and flag selected segment clusters
    for (int i=0; i<segmentClusters.getLength(); i++) {
      
      Element current = (Element) segmentClusters.item(i);

      if (current.hasAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE)) {
        
        int currentClustering = Integer.valueOf(current.getAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE)).intValue();
        int currentCluster = Integer.valueOf(current.getAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE)).intValue();
        
        // flag all segment clusters matching current cluster of hovered clustering
        if (currentClustering == thisClustering) {
          
          if (currentCluster == thisCluster) {

            SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_IDENTICAL_CLASS);
            
            // get participating cluster and their pair count
            SegmentID id = new SegmentID(current.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE));

            //
            // count Pairs in Clusters of selected cluster segments
            //
            for (int clusteringID=0; clusteringID<id.size(); clusteringID++) {
              
              TreeMap<Integer, Integer> clusterList;
              
              // pairs in this segment
              int pairs = Integer.valueOf(current.getAttribute(CCConstants.SEG_PAIRCOUNT_ATTRIBUTE)).intValue();
              
              // cluster of clustering 
              int clusterID = id.get(clusteringID);
              
              if (clusterID != 0) {
                
                // add pairs to clustering-cluster list
                if (participants.containsKey(clusteringID)) {
                  
                  clusterList = participants.get(clusteringID);
                  
                  if (clusterList.containsKey(clusterID)) {
                    
                    int currentPairs = clusterList.get(clusterID);
                    clusterList.put(clusterID, pairs+currentPairs);
                    
                  } else {
                    
                    clusterList.put(clusterID, pairs);
                  }
                  
                } else {
                  
                  clusterList = new TreeMap<Integer, Integer>();
                  clusterList.put(clusterID, pairs);
                  participants.put(clusteringID, clusterList);
                }
              }
            }
          }

        // flag selected clusters of other clusterings
        } else {
          
          if (thisSegmentID.get(currentClustering) == currentCluster) {

            SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_SELECTION_CLASS);
          }                   
        }
      }
    }
    
    // flag all clusters participating in selected cluster
    for (int i=0; i<segmentClusters.getLength(); i++) {
      
      Element current = (Element) segmentClusters.item(i);
      
      if (current.hasAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE)) {

        int currentClustering = Integer.valueOf(current.getAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE)).intValue();
        int currentCluster = Integer.valueOf(current.getAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE)).intValue();
        int currentPaircount = Integer.valueOf(current.getAttribute(CCConstants.CLR_PAIRCOUNT_ATTRIBUTE)).intValue();
        
        // flag all segmemt clusters matching cluster of hovered clustering
        if ((currentClustering != thisClustering)) {

          if (participants.containsKey(currentClustering)) {
            
            if(participants.get(currentClustering).containsKey(currentCluster)) {
              
              //*
              SVGUtil.addCSSClass(current, CCConstants.PRE_COLOR_CLASS+(currentClustering*(currentCluster)%(CCConstants.AvailableColors+1)));
              /*
              SVGUtil.addCSSClass(current, CCConstants.PRE_COLOR_CLASS+currentClustering);
              /
              
              int currentClusteredPairs = participants.get(currentClustering).get(currentCluster);
               
              if (currentClusteredPairs == currentPaircount) {
                
                if (participants.get(thisClustering).get(thisCluster) == currentClusteredPairs) {
                  
                  SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_IDENTICAL_CLASS);
                  
                } else {
                  
                  SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_SUBSET_CLASS);
                }
                
              } else {
                
                double ratio = ((double)currentClusteredPairs / currentPaircount);
                int alphaID = (int)((ratio*alphaClasses.length));
                /*
                if(currentClusteredPairs > currentPaircount) {
                  
                  System.out.println(currentClustering+"-"+currentCluster+": currentClusteredPairs: "+currentClusteredPairs+" - currentPaircount:"+currentPaircount);
                }

                System.out.println(currentClusteredPairs + ":" + currentPaircount + " = "+ratio+" => "+alphaID);
                *
                SVGUtil.addCSSClass(current, CCConstants.CLR_HOVER_INTERSECTION_CLASS);
                SVGUtil.addCSSClass(current, alphaClasses[alphaID]);
              }
            }            
          }
        }
      }
    }
  }  
}
*/