package experimentalcode.students.goldhofa.visualizers.visunproj;

import java.awt.geom.Point2D;
import java.util.ArrayList;
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

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
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
import experimentalcode.students.goldhofa.visualization.gui.SVGWindow;
import experimentalcode.students.goldhofa.visualization.visualizers.vis2d.ClusteringComparisonVisualization;


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
public class CircleSegmentsVisualizer extends AbstractVisFactory implements ContextChangeListener, ResultListener {
  
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
  public Segments segments;
  
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
   * context
   */
  public VisualizerContext context;
  
  /**
   * The plot
   */
  public SVGPlot svgp;
  
  //
  // SELECTION HELPERS
  //
  
  /**
   * currently selected objects
   */
  public ArrayList<DBIDs> currentSelection = new ArrayList<DBIDs>();
  /**
   * currently visible segment labels as <SegmentID, cssClass>
   */
  public TreeMap<String, String> selectedSegmentLabels = new TreeMap<String, String>();
  /**
   * currently highlighted segment clusters
   */
  public ArrayList<Element> selectedSegments = new ArrayList<Element>();
  
  //
  // ---
  //
  
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
  private static enum Colors { BORDER("#FF0073"), CLUSTER_UNPAIRED("#ffffff"),
    HOVER_ALPHA("1.0"), HOVER_INCLUSTER("#008e9e"), HOVER_SELECTION("#73ff00"), HOVER_PAIRED("#4ba600"), HOVER_UNPAIRED("#b20000"),
    HOVER_SUBSET("#009900"), HOVER_INTERSECTION("#990000"),
    SELECTED_SEGMENT("#009900"), SELECTED_BORDER("#000000");
  
    // getter/setter
    String color;  
    Colors(String color) { this.color = color; }
    public String getColor() { return this.color; }
  }
  
  /**
   * Constructor
   */
  public CircleSegmentsVisualizer() {
    super();
    
    //svgContext = SVGWindow.svgContext;
    //svgContext.addListener(this);
  }
  
  public void redraw() {
    //System.out.println("CircleSegments: redraw");
    this.contextChanged(null);
  }
  
  @Override
  public void contextChanged(ContextChangedEvent e) {}
  
  
  @Override
  public void addVisualizers(VisualizerContext context, Result result) {
    
    // If no comparison result found abort
    List<ClusteringComparisonResult> ccr = ResultUtil.filterResults(result, ClusteringComparisonResult.class);
    if (ccr.size() != 1) return;
    
    final VisualizationTask task = new VisualizationTask(NAME, context, ccr.get(0), null, this, null);
    task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
    context.addVisualizer(ccr.get(0), task);
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    
    context = task.getContext();
    svgp = task.getPlot();
    ccr = task.getResult();
    
    //
    // init
    //

    this.segments = ccr.getSegments();
    this.layer    = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    
    // Listen for context changes
    context.addContextChangeListener(this);
    // Listen for result changes (Selection changed)
    context.addResultListener(this);
    
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
    
    EventListener mouseOver = new MouseOverSegmentCluster(this);
    EventListener mouseOut = new MouseOutSegmentCluster();
    EventListener mouseClick = new MouseClickSegmentCluster(context, segments, this);
    
   
    //
    // Draw Circle Segments
    //
    
    int refClustering = 0;
    int refSegment = 0;
    double offsetAngle = 0;

    // ITERATE OVER ALL SEGMENTS
    
    for (SegmentID id : pairSegments.descendingKeySet()) {
      
      int currentPairCount = pairSegments.get(id);
      
      // resize small segments if below minimum
      double alpha = Properties.CLUSTER_MIN_WIDTH.getValue();
      if (currentPairCount > Properties.PAIR_MIN_COUNT.getValue()) alpha = Properties.ANGLE_PAIR.getValue()*currentPairCount;

      // ITERATE OVER ALL SEGMENT-CLUSTERS
      
      // draw segment for every clustering
      
      for (int i=0; i<id.size(); i++) {
        
        double currentRadius = i*(Properties.RADIUS_DELTA.getValue()+Properties.CLUSTERING_DISTANCE.getValue())+Properties.RADIUS_INNER.getValue();
        
        //
        // Add border if next cluster of reference clustering
        //
        
        if ((refSegment != id.get(refClustering)) && refClustering==i) {
          
          Element border = getSegment(offsetAngle-Properties.CLUSTER_DISTANCE.getValue(), center, Properties.BORDER_WIDTH.getValue(), currentRadius, Properties.RADIUS_OUTER.getValue()-Properties.CLUSTERING_DISTANCE.getValue()).makeElement(svgp);
          border.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_BORDER_CLASS);
          layer.appendChild(border);  
          
          if (id.get(refClustering) == 0) refClustering = Math.min(refClustering+1, clusterings-1);
          
          refSegment = id.get(refClustering);                    
        }
        
        int cluster = id.get(i);
        
        //
        // create ring segment
        //
        
        Element segment = getSegment(offsetAngle, center, alpha, currentRadius, currentRadius+Properties.RADIUS_DELTA.getValue()).makeElement(svgp);
        
        segment.setAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE, ""+cluster);
        segment.setAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE, ""+i);
        segment.setAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE, id.toString());
        //segment.setAttribute(CCConstants.SEG_PAIRCOUNT_ATTRIBUTE, pairSegments.get(id).toString());
        //segment.setAttribute(CCConstants.CLR_PAIRCOUNT_ATTRIBUTE, ""+segments.getPairCount(i, cluster));

        //
        // MouseEvents on segment cluster
        //
        
        EventTarget targ = (EventTarget) segment;
        targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, mouseOver, false);
        targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, mouseOut, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, mouseClick, false);
        
        //
        // Coloring based on clusterID
        //
        
        if (cluster != 0) segment.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, cssClr[id.get(i)-1].getName());
        // if its an unpaired cluster set colour to white 
        else segment.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_UNPAIRED_CLASS);
        
        
        layer.appendChild(segment);
      }
      
      //
      // Add a extended strip for each segment to emphasis selection
      // (makes it easier to track thin segments and their color coding and differentiates them from cluster border lines)
      //
      
      int i = id.size();
      double currentRadius = i*(Properties.RADIUS_DELTA.getValue()+Properties.CLUSTERING_DISTANCE.getValue())+Properties.RADIUS_INNER.getValue();
      Element extension = getSegment(offsetAngle, center, alpha, currentRadius, currentRadius+(0.5 - Properties.RADIUS_OUTER.getValue())).makeElement(svgp);
      extension.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      extension.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_UNPAIRED_CLASS);
      svgp.putIdElement(CCConstants.SEG_EXTENSION_ID_PREFIX+id.toString(), extension);
      layer.appendChild(extension);
      
      // calculate angle for next segment
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
    cluster_hover.setStatement(SVGConstants.SVG_CURSOR_TAG, SVGConstants.SVG_POINTER_VALUE+" !important" );
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
    
    // UNPAIRED CLUSTER 
    CSSClass clusterUnpaired = new CSSClass(this, CCConstants.CLR_UNPAIRED_CLASS);
    clusterUnpaired.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, Colors.CLUSTER_UNPAIRED.getColor());
    clusterUnpaired.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
    svgp.addCSSClassOrLogError(clusterUnpaired);
    
    // CLUSTER SELECT
    CSSClass cluster_selected = new CSSClass(this, CCConstants.CLR_SELECTED_CLASS);
    cluster_selected.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, Colors.SELECTED_BORDER.getColor()+" !important");
    cluster_selected.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.003");
    svgp.addCSSClassOrLogError(cluster_selected);
    
    // SEGMENT SELECT
    CSSClass segment_selected = new CSSClass(this, CCConstants.SEG_SELECTED_CLASS);
    segment_selected.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, Colors.SELECTED_SEGMENT.getColor()+" !important" );
    svgp.addCSSClassOrLogError(segment_selected);
    
    //
    // SELECTION CLASSES
    // TODO refactor: mixed by classes in ClusteringComparisonVisualization & CCMarkers
    //
    
    // Color classes for differentiation of segments
    int index = 0;
    for ( String colorValue : CCConstants.ColorArray) {
      
      CSSClass bordercolor = new CSSClass(this, CCConstants.PRE_STROKE_COLOR_CLASS+index);
      bordercolor.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, colorValue+" !important");
      svgp.addCSSClassOrLogError(bordercolor);
      
      CSSClass fillcolor = new CSSClass(this, CCConstants.PRE_FILL_COLOR_CLASS+index);
      fillcolor.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, colorValue+" !important");
      svgp.addCSSClassOrLogError(fillcolor);
      
      index++;
    } 
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

  @Override
  public void resultAdded(Result child, Result parent) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void resultChanged(Result currentResult) {
    
    if (currentResult.getLongName() != "Selection") {
      
      //System.out.println(" # CircleSegments.resultChanged: "+currentResult.getLongName()+" => ignored");
      return;
    }

    // get the current selection
    DBIDSelection selContext = context.getSelection();
    
    if (selContext != null) {
      
      // and get all selected DB IDs
      DBIDs selection = selContext.getSelectedIds();
      
      //final double linewidth = 3 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT);
      
      // segments
      NodeList elements = layer.getChildNodes();
      
      
      // Iterate over IDs and select segments containing object
      // TODO for pairs (?)
      
      // Get List of segments
      SortedSet<String> selectedSegments = new TreeSet<String>();
      for (DBID id : selection) {
        selectedSegments.add(segments.getSegmentID(id).toString());        
      }
      
      // And iterate over segments
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

        /*
        double[] v = proj.fastProjectDataToRenderSpace(database.get(i));
        Element dot = svgp.svgCircle(v[0], v[1], linewidth);
        SVGUtil.addCSSClass(dot, MARKER);
        layer.appendChild(dot);
        */
    }
    
    //currentSelection = (short)(currentSelection % 32766);
  }

  @Override
  public void resultRemoved(Result child, Result parent) {
    // TODO Auto-generated method stub
    
  }
  
  /**
   * Get the DB objects of a segmentID.
   * 
   * @param selectedSegment   selected segment of a clustering
   * return                   (paired) objects in segment
   */
  public DBIDs getSegmentObjects(SegmentID id) {
    
    // retrieve associated objects
    DBIDs segmentObjects = segments.getDBIDs(id);
    
    return segmentObjects;
  }
  
  /**
   * Select Objects of a single Segment.
   * 
   * Currently highlights objects on its own. Requires Scatterplot to be present.
   * This is a basic selection for highlighting objects. Further analysis is done
   * by Scatterplot selection tools.
   * 
   * @param selectedObjects   Set of objects to highlight in Scatterplot
   */
  public void selectObjects(DBIDs selectedObjects) {
    
    // remove current Selection
    //deselectObjects();
    
    this.currentSelection.add(selectedObjects);
    
    // Iterate over selection and fetch corresponding elements by its id
    // TODO objectID prefix
    for (DBID id : selectedObjects) {
      
      Element object = svgp.getIdElement(id.toString());
      SVGUtil.removeCSSClass(object, CCConstants.OBJ_UNSELECTED_CLASS);
    }
  }
  
  /**
   * Select Objects of a single Segment and add a specified css class.
   * 
   * Currently highlights objects on its own. Requires Scatterplot to be present.
   * This is a basic selection for highlighting objects. Further analysis is done
   * by Scatterplot selection tools.
   * 
   * @param selectedObjects   Set of objects to highlight in Scatterplot
   * @param cssClass          the CSS class to apply to selection
   */
  public void selectObjects(DBIDs selectedObjects, String cssClass) {
    
    // remove current Selection
    //deselectObjects();
    
    this.currentSelection.add(selectedObjects);
    
    // Iterate over selection and fetch corresponding elements by its id
    // TODO objectID prefix
    for (DBID id : selectedObjects) {
      
      Element object = svgp.getIdElement(id.toString());
      SVGUtil.removeCSSClass(object, CCConstants.OBJ_UNSELECTED_CLASS);
      SVGUtil.addCSSClass(object, cssClass);
    }
  }
  
  public void selectSegment(SegmentID segment, String objectCSSClass, String segmentCSSClass) {
    
    // get DB objects of segment
    DBIDs objects = this.getSegmentObjects(segment);
    // and mark them as selected
    this.selectObjects(objects, objectCSSClass);
    // highlight segment in same color
    Element extension = svgp.getIdElement(CCConstants.SEG_EXTENSION_ID_PREFIX+segment.toString());
    SVGUtil.addCSSClass(extension, segmentCSSClass);
    // and add segment to selection list
    this.selectedSegmentLabels.put(segment.toString(), segmentCSSClass);
  }
  
  /**
   * Select Objects of multiple Segments resulting in different coloring of DB objects,
   * representing the different segments.
   * 
   * Currently highlights objects on its own. Requires Scatterplot to be present.
   * This is a basic selection for highlighting objects. Further analysis is done
   * by Scatterplot selection tools.
   * 
   * @param selectedObjects   Set of segments to highlight in Scatterplot
   */
  public void selectObjects(ArrayList<SegmentID> selectedSegments) {
    
    // if its just one segment use default highlighting
    if (selectedSegments.size() == 1) {
      
      DBIDs selectedObjects = this.getSegmentObjects(selectedSegments.get(0));
      selectObjects(selectedObjects);
      return;
    }
    
    // select all segments
    int index = 0;    
    for (SegmentID segment : selectedSegments) {
      selectSegment(segment, CCConstants.PRE_STROKE_COLOR_CLASS+index, CCConstants.PRE_FILL_COLOR_CLASS+index);     
      index++;
    }
  }
  
  /**
   * Get all elements of a segment by its segmentID
   * 
   * @param segmentID
   */
  public ArrayList<Element> getSegments(SegmentID segmentID) {
    
    ArrayList<Element> segment = new ArrayList<Element>();
    
    String searchString = segmentID.toString();
    
    // iteraters over all nodes (ugly)
    NodeList clusterSegments = layer.getChildNodes();
    
    for (int i=0; i<clusterSegments.getLength(); i++) {
      
      Element clusterSegment = (Element) clusterSegments.item(i);

      // collect all Elements of requested Segment
      if (clusterSegment.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE).compareTo(searchString) == 0) {
        segment.add(clusterSegment);
      }
    }
    
    return segment; 
  }
  
  public void deselectObjects() {
    
    // if nothing is selected -> abort 
    if (this.currentSelection == null) return;
    
    // else iterate over selection
    for (DBIDs ids : this.currentSelection) {
      
      for (DBID id : ids) {
      
        Element object = svgp.getIdElement(id.toString());
        // and set as unselected, [!!] removing all CSS Classes 
        //SVGUtil.addCSSClass(object, CCConstants.OBJ_UNSELECTED_CLASS);
        object.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.OBJ_STANDARD_CLASS);
        SVGUtil.addCSSClass(object, CCConstants.OBJ_UNSELECTED_CLASS);
      }
    }
    
    // and clear current selection list
    this.currentSelection.clear();
    
    
    // TODO: move to "deselectSegments" ?
    
    // clear segment labels
    for(String segmentID : this.selectedSegmentLabels.keySet()) {
      Element selection = svgp.getIdElement(CCConstants.SEG_EXTENSION_ID_PREFIX+segmentID);
      SVGUtil.removeCSSClass(selection, this.selectedSegmentLabels.get(segmentID));
    }
    this.selectedSegmentLabels.clear();
    
    // clear selected segment clusters
    for (Element elem : this.selectedSegments) {
      SVGUtil.removeCSSClass(elem, CCConstants.SEG_SELECTED_CLASS);
    }
    this.selectedSegments.clear();
    
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
  
  CircleSegmentsVisualizer cs;
  
  public MouseOverSegmentCluster(CircleSegmentsVisualizer cs) {
    super();
    
    this.cs = cs;
  }
  
  @Override
  public void handleEvent(Event evt) {
    
    // hovered segment cluster
    Element thisSegmentCluster = (Element) evt.getTarget();
    
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
    // STANDARD CLUSTER SEGMENT
    // highlight all ring segments in this clustering and this cluster
    //
    
    if ( ! thisSegmentID.isUnpaired()) {
      
      // highlight current hovered ring segment
      SVGUtil.addCSSClass(thisSegmentCluster, CCConstants.CLR_HOVER_CLASS);
      SVGUtil.addCSSClass(thisSegmentCluster, CCConstants.CLR_HOVER_SELECTION_CLASS);
      
      // and all corresponding ring Segments
      for (int i=0; i<segmentClusters.getLength(); i++) {
        
        Element ringSegment = (Element) segmentClusters.item(i);
        // just segments
        if (ringSegment.hasAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE)) {
          // only this selected clustering
          if (ringSegment.getAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE).compareTo(String.valueOf(thisClusteringID)) == 0) {
            // and same cluster
            if (ringSegment.getAttribute(CCConstants.SEG_CLUSTER_ATTRIBUTE).compareTo(String.valueOf(thisClusterID)) == 0) {
              
              // mark as selected
              SVGUtil.addCSSClass(ringSegment, CCConstants.CLR_HOVER_CLASS);
              SVGUtil.addCSSClass(ringSegment, CCConstants.CLR_HOVER_SELECTION_CLASS);
            }
          }
          
        }
      }
    }
    //
    // UNPAIRED SEGMENT
    // highlight all ring segments in this clustering responsible for unpaired segment
    //
    else {
      
      // get the paired segments corresponding to the unpaired segment
      ArrayList<SegmentID> segments = cs.segments.getPairedSegments(thisSegmentID);
      
      // and all corresponding ring Segments
      for (int i=0; i<segmentClusters.getLength(); i++) {
        
        Element ringSegment = (Element) segmentClusters.item(i);
        // just segments
        if (ringSegment.hasAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE)) {
          
          SegmentID segment = new SegmentID(ringSegment.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE));
          if (segments.contains(segment) && ringSegment.getAttribute(CCConstants.SEG_CLUSTERING_ATTRIBUTE).compareTo(String.valueOf(thisClusteringID)) == 0) {
            
            // mark as selected
            SVGUtil.addCSSClass(ringSegment, CCConstants.CLR_HOVER_CLASS);
            SVGUtil.addCSSClass(ringSegment, CCConstants.CLR_HOVER_SELECTION_CLASS);
          }
        }
      }
    }
    
    /*
    
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
    }*/
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
  
  private CircleSegmentsVisualizer cs;
  
  private VisualizerContext context;
  private Segments segments;
  private SortedSet<String> selection;
  
  private int selectionID = 0;
  private long lastClick = 0;
  
  MouseClickSegmentCluster(VisualizerContext context, Segments segments, CircleSegmentsVisualizer cs) {
    
    this.cs = cs;
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
    Element thisSegmentElement = (Element) evt.getTarget();
    
    // Get the segmentID String representation
    // A segmentID String consists of the cluster id of each clustering,
    // ordered by clustering and separated by a character. Thus a segment ID describes
    // the common pairs in all clusterings and cluster.
    // i.e. clusteringID 0 & clusterID 2, clusteringID 1 & clusterID 0 => segmentID: 2-0
    String segmentIDString = thisSegmentElement.getAttribute(CCConstants.SEG_SEGMENT_ATTRIBUTE);
    
    // convert String representation to SegmentID
    SegmentID selectedSegment = new SegmentID(segmentIDString);
   
    // check if this segment has unpaired objects in a clustering
    if (selectedSegment.isUnpaired()) {
      
      // segment with an unpaired cluster selected
      
      ArrayList<SegmentID> selectedSegments = segments.getPairedSegments(selectedSegment);
      
      // deselect old selection
      cs.deselectObjects();
      
      // create selection of segments
      cs.selectObjects(selectedSegments);
      
    } else {
      
      // segment with common paired objects selected
      
      // get DBIDs of Segment
      DBIDs selectedObjects = cs.getSegmentObjects(selectedSegment);
      
      // deselect old selection
      cs.deselectObjects();
      
      // and set them as selected
      cs.selectObjects(selectedObjects);
    }
    
    // Highlight current Selected segment
    SVGUtil.addCSSClass(thisSegmentElement, CCConstants.SEG_SELECTED_CLASS);
    cs.selectedSegments.add(thisSegmentElement);
  }
}