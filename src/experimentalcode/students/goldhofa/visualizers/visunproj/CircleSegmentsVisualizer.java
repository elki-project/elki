package experimentalcode.students.goldhofa.visualizers.visunproj;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MouseEvent;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import experimentalcode.students.goldhofa.CCConstants;
import experimentalcode.students.goldhofa.ClusteringComparison;
import experimentalcode.students.goldhofa.ClusteringComparisonResult;
import experimentalcode.students.goldhofa.Color;
import experimentalcode.students.goldhofa.SegmentID;
import experimentalcode.students.goldhofa.SegmentSelection;
import experimentalcode.students.goldhofa.Segments;
import experimentalcode.students.goldhofa.visualization.batikutil.BarChart;
import experimentalcode.students.goldhofa.visualization.batikutil.CheckBox;
import experimentalcode.students.goldhofa.visualization.batikutil.CheckBoxListener;
import experimentalcode.students.goldhofa.visualization.batikutil.SwitchEvent;
import experimentalcode.students.goldhofa.visualization.batikutil.UnorderedList;


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
public class CircleSegmentsVisualizer extends AbstractVisFactory implements /*ContextChangeListener,*/ ResultListener {
  
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
  private Element layer, visLayer, ctrlLayer;
  
  /**
   * CSS class name for the clusterings.
   */
  private static final String CLUSTERID = "cluster";
  
  /**
   * Center of CircleSegments
   */
  private Point2D.Double center = new Point2D.Double(0.5, 0.5);
  
  protected EventListener mouseOver;
  protected EventListener mouseOut;
  protected EventListener mouseClick;
  
  protected CSSClass[] cssClr;
  
  /**
   * context
   */
  public VisualizerContext context;
  
  /**
   * The plot
   */
  public SVGPlot svgp;
  
  /**
   * Segment selection manager
   */
  public SegmentSelection selection;
 
  /**
   * Properties of a Segment
   */
  private static enum Properties {
    
    // Constant values
    CLUSTERING_DISTANCE(0.01),    // Margin between two rings
    CLUSTER_MIN_WIDTH(0.01),      // Minimum width (radian) of Segment
    CLUSTER_DISTANCE(0.01),       // Margin (radian) between segments
    RADIUS_INNER(0.05),           // Offset from center to first ring
    RADIUS_OUTER(0.46),           // Radius of whole CircleSegments except selection border
    RADIUS_SELECTION(0.02),       // Radius of highlight selection (outer ring)
    
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
   * Show unclustered Pairs in CircleSegments
   */
  boolean showUnclusteredPairs = false;
  
  UnorderedList selectionInfo;
  
  /**
   * Constructor
   */
  public CircleSegmentsVisualizer() {
    super();
  }
  
  public void showUnclusteredPairs(boolean show) {
    
    if (showUnclusteredPairs == show) return;
    showUnclusteredPairs = show;
    
    // recalculate values
    calculateSegmentProperties();  
    
    // store attributes & delete old visLayer
    Node parent = visLayer.getParentNode();
    String attTranslate = ((Element)parent).getAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE);
    parent.removeChild(visLayer);
    
    // add new node to draw
    visLayer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    visLayer.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE, attTranslate);
    parent.appendChild(visLayer);
    
    redraw();
    
    selection.reselect();
    
    // notify parent
    // FIXME: signal 
  }

  /**
   * Create the segments
   */
  public void redraw() {

    int refClustering   = 0;
    int refSegment      = 0;
    double offsetAngle  = 0.0;

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
        // Add border if the next segment is a different cluster in the reference clustering
        //
        
        if ((refSegment != id.get(refClustering)) && refClustering==i) {
          
          Element border = getSegment(offsetAngle-Properties.CLUSTER_DISTANCE.getValue(), center, Properties.BORDER_WIDTH.getValue(), currentRadius, Properties.RADIUS_OUTER.getValue()-Properties.CLUSTERING_DISTANCE.getValue()).makeElement(svgp);
          border.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_BORDER_CLASS);
          visLayer.appendChild(border);  
          
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
        // if its an unpaired cluster set color to white 
        else segment.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_UNPAIRED_CLASS);
        
        
        visLayer.appendChild(segment);
      }
      
      //
      // Add a extended strip for each segment to emphasis selection
      // (easier to track thin segments and their color coding and differentiates them from cluster border lines)
      //
      
      int i = id.size();
      double currentRadius = i*(Properties.RADIUS_DELTA.getValue()+Properties.CLUSTERING_DISTANCE.getValue())+Properties.RADIUS_INNER.getValue();
      Element extension = getSegment(offsetAngle, center, alpha, currentRadius, currentRadius+(Properties.RADIUS_SELECTION.getValue())).makeElement(svgp);
      extension.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      extension.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_UNPAIRED_CLASS);
      svgp.putIdElement(CCConstants.SEG_EXTENSION_ID_PREFIX+id.toString(), extension);
      visLayer.appendChild(extension);
      
      // calculate angle for next segment
      offsetAngle += alpha+Properties.CLUSTER_DISTANCE.getValue();
    }
  }
  
  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    // If no comparison result found abort
    List<ClusteringComparisonResult> ccr = ResultUtil.filterResults(result, ClusteringComparisonResult.class);
    if (ccr.size() != 1) return;
    
    final VisualizationTask task = new VisualizationTask(NAME, ccr.get(0), null, this);
    task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
    baseResult.getHierarchy().add(ccr.get(0), task);
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    
    context = task.getContext();
    svgp    = task.getPlot();
    ccr     = task.getResult();

    this.segments   = ccr.getSegments();
    this.layer      = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    this.visLayer   = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    this.ctrlLayer  = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    
    this.selectionInfo = new UnorderedList(svgp);
    this.selection  = new SegmentSelection(svgp, visLayer, segments, selectionInfo);
    
    // Listen for context changes
    //context.addContextChangeListener(this);
    
    // Listen for result changes (Selection changed)
    context.addResultListener(this);
    
    // calculate properties for drawing
    calculateSegmentProperties();  
    
    // add css
    addCSSClasses(svgp);
    
    // create Color shades for clusters
    // TODO: Auskommentieren falls Probleme mit Beamer
    //*
    String[] clusterColorShades = getGradient(clusterSize, Color.getColorSet(Color.ColorSet.GREY));
    /*/
    String[] clusterColorShades = getGradient(clusterSize, Color.getColorSet(Color.ColorSet.BLUE));
    //*/
    
    
    cssClr = new CSSClass[clusterSize];
    for (int i=0; i<clusterSize; i++) {
      cssClr[i] = new CSSClass(this, CLUSTERID+"_"+(i+1));
      cssClr[i].setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, clusterColorShades[i]);
      cssClr[i].setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      svgp.addCSSClassOrLogError(cssClr[i]);
    }
    
    // initialize events
    mouseOver   = new MouseOverSegmentCluster(this);
    mouseOut    = new MouseOutSegmentCluster();
    mouseClick  = new MouseClickSegmentCluster(selection);
    
    // and create svg elements
    redraw();
    
    //
    // Build Interface
    //
    
    CheckBox checkbox = new CheckBox(svgp, showUnclusteredPairs, "Show unclustered pairs");
    checkbox.addCheckBoxListener(new CheckBoxListener() {
      public void switched(SwitchEvent evt) {
        showUnclusteredPairs(evt.isOn());
      }
    });
    
    // list to store all elements
    UnorderedList info = new UnorderedList(svgp);
    
    // Add ring:clustering info
    Element clrInfo = getClusteringInfo();    
    info.addItem(clrInfo, Integer.valueOf(clrInfo.getAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE)));
    // checkbox
    info.addItem(checkbox.asElement(), 20);
    // pairs:clusteredpairs ratio
    BarChart barchart = new BarChart(svgp, 200.0, 20.0, true);
    barchart.setSize(segments.getPairCount(true));
    barchart.setFill(segments.getPairCount(false));
    barchart.showValues();
    barchart.addLabel("Total paircount : clustered pairs");
    info.addItem(barchart.asElement(), 20);
    // and add selection info
    // ! TODO VARIABLE LENGTH
    info.addItem(selectionInfo.asElement(), 50);
    
    ctrlLayer.setAttribute(CCConstants.UI_CONTROL_LAYER, "");
    ctrlLayer.appendChild(info.asElement());
    
    visLayer.setAttribute(CCConstants.UI_VISUALIZATION_LAYER, "");
    SVGUtil.setAtt(visLayer, SVGConstants.SVG_ID_ATTRIBUTE, CCConstants.UI_VISUALIZATION_LAYER);
    
    layer.appendChild(ctrlLayer);
    layer.appendChild(visLayer);

    return new StaticVisualization(task, layer);
  }
  
  /**
   * Calculates segment properties like radius, width, etc. Values currently vary
   * only with "showUnclusteredPairs".
   */
  protected void calculateSegmentProperties() {
    
    pairSegments = segments.getSegments(showUnclusteredPairs);
    
    clusterSize = segments.getHighestClusterCount();
    clusterings = this.segments.getClusterings();
    
    Properties.ANGLE_PAIR.setValue((MathUtil.TWOPI-(Properties.CLUSTER_DISTANCE.getValue()*pairSegments.size()))/segments.getPairCount(showUnclusteredPairs));
    Properties.PAIR_MIN_COUNT.setValue(Math.ceil(Properties.CLUSTER_MIN_WIDTH.getValue()/Properties.ANGLE_PAIR.getValue()));
    
    // number of segments needed to be resized
    int segMinCount = 0;
    for (Integer size : pairSegments.values()) {
      if (size <= Properties.PAIR_MIN_COUNT.getValue()) segMinCount++;
    }
    
    Properties.CLUSTER_MIN_COUNT.setValue(segMinCount);
    
    // update width of a pair
    Properties.ANGLE_PAIR.setValue((MathUtil.TWOPI-(Properties.CLUSTER_DISTANCE.getValue()*pairSegments.size()+segMinCount*Properties.CLUSTER_MIN_WIDTH.getValue()))/(segments.getPairCount(showUnclusteredPairs)-Properties.CLUSTER_MIN_COUNT.getValue()));
    Properties.RADIUS_DELTA.setValue((Properties.RADIUS_OUTER.getValue()-Properties.RADIUS_INNER.getValue()-clusterings*Properties.CLUSTERING_DISTANCE.getValue())/(clusterings));
    Properties.BORDER_WIDTH.setValue(Properties.CLUSTER_DISTANCE.getValue());
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
    
    // steps to use between each colour
    int colorDelta = shades/(colorCount-1);
    
    for (int s=0; s<shades; s++) {
      
      int from = s/colorDelta;
      int to = ((s/colorDelta)+1)%colorCount;
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
  
  protected Element getClusteringInfo() {
    
    Element thumbnail = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    
    // build thumbnail
    int startRadius = 4;
    int singleHeight = 12;
    int margin = 4;
    int radius = clusterings * (singleHeight + margin) + startRadius;
    
    thumbnail.setAttribute("height", ""+radius);
    
    for (int i=0; i<clusterings; i++) {
      
      double innerRadius = i*singleHeight+margin*i+startRadius;
      Element clr = getSegment(Math.PI*1.5, new Point2D.Double(radius - startRadius, radius - startRadius), Math.PI*0.5, innerRadius, innerRadius+singleHeight).makeElement(svgp);
      clr.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#d4e4f1");
      clr.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
      clr.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "1.0");
      
      String labelText = segments.getClusteringDescription(i);
      Element label = svgp.svgText(radius + startRadius, radius - innerRadius - startRadius, labelText);
      SVGUtil.addCSSClass(label,  CCConstants.CSS_TEXT);
      thumbnail.appendChild(label);
      
      thumbnail.appendChild(clr);
    }
    
    return thumbnail;
  }

  protected SVGPath getSegment(double angleOffset, Point2D.Double center, double alpha, double innerRadius, double outerRadius) {
    
    double sin1st = Math.sin(angleOffset);
    double cos1st = Math.cos(angleOffset);
    
    double sin2nd = Math.sin(angleOffset+alpha);
    double cos2nd = Math.cos(angleOffset+alpha);
    
    Point2D.Double inner1st = new Point2D.Double(center.x + (innerRadius * sin1st), center.y - (innerRadius * cos1st));
    Point2D.Double outer1st = new Point2D.Double(center.x + (outerRadius * sin1st), center.y - (outerRadius * cos1st));
    
    Point2D.Double inner2nd = new Point2D.Double(center.x + (innerRadius * sin2nd), center.y - (innerRadius * cos2nd));
    Point2D.Double outer2nd = new Point2D.Double(center.x + (outerRadius * sin2nd), center.y - (outerRadius * cos2nd));
    
    double largeArc = 0;
    if (alpha >= Math.PI) largeArc = 1;
    
    SVGPath path = new SVGPath(inner1st.x, inner1st.y);
    path.lineTo(outer1st.x, outer1st.y);
    path.ellipticalArc(outerRadius, outerRadius, 0, largeArc, 1, outer2nd.x, outer2nd.y);
    path.lineTo(inner2nd.x, inner2nd.y);
    path.ellipticalArc(innerRadius, innerRadius, 0, largeArc, 0, inner1st.x, inner1st.y);
    
    return path;
  }
  
  @Override
  public void resultAdded(Result child, Result parent) {}

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
      NodeList elements = visLayer.getChildNodes();
      
      
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
  public void resultRemoved(Result child, Result parent) {}
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
  
  private SegmentSelection selection;
  //private long lastClick = 0;
  
  MouseClickSegmentCluster(SegmentSelection selection) {
    
    this.selection = selection;
  }

  public void handleEvent(Event evt) {
    
    MouseEvent mouse = (MouseEvent) evt;
    
    // Check Double Click
    /*
    boolean dblClick = false;
    long time = java.util.Calendar.getInstance().getTimeInMillis();
    if (time-lastClick <= CCConstants.EVT_DBLCLICK_DELAY) dblClick = true;
    lastClick = time;
    */
    
    // CTRL (add) pressed?
    boolean ctrl = false;
    if (mouse.getCtrlKey()) {
      System.out.println("CTRL");
      ctrl = true;
    }
    
    // clicked segment cluster
    Element thisSegmentElement = (Element) evt.getTarget();
    
   selection.select(thisSegmentElement, ctrl);
  }
}