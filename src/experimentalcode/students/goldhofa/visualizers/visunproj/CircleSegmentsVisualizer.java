package experimentalcode.students.goldhofa.visualizers.visunproj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MouseEvent;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import experimentalcode.students.goldhofa.CCConstants;
import experimentalcode.students.goldhofa.ClusteringComparison;
import experimentalcode.students.goldhofa.ClusteringComparisonResult;
import experimentalcode.students.goldhofa.Color;
import experimentalcode.students.goldhofa.Segment;
import experimentalcode.students.goldhofa.SegmentSelection;
import experimentalcode.students.goldhofa.Segments;
import experimentalcode.students.goldhofa.visualization.batikutil.CheckBox;
import experimentalcode.students.goldhofa.visualization.batikutil.CheckBoxListener;
import experimentalcode.students.goldhofa.visualization.batikutil.SwitchEvent;
import experimentalcode.students.goldhofa.visualization.batikutil.UnorderedList;

/**
 * Visualizer to draw circle segments of clusterings
 * 
 * BUG: double precision not enough for complete circle (?)
 * 
 * TODO - remove completely unpaired segments
 * 
 * @author Sascha Goldhofer
 */
public class CircleSegmentsVisualizer extends AbstractVisualization implements ResultListener {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(CircleSegmentsVisualizer.class);

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
  private Collection<Segment> pairSegments;

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
  private Element visLayer, ctrlLayer;

  /**
   * CSS class name for the clusterings.
   */
  private static final String CLUSTERID = "cluster";

  /**
   * Center of CircleSegments
   * 
   * TODO: scale with StyleLibrary.SCALE
   */
  private final double centerx = 0.5 * StyleLibrary.SCALE;

  private final double centery = 0.5 * StyleLibrary.SCALE;

  protected CSSClass[] cssClr;

  public Map<Segment, List<Element>> segmentToElements = new HashMap<Segment, List<Element>>();

  /**
   * Segment selection manager
   */
  public SegmentSelection selection;

  /**
   * Properties of a Segment
   */
  private static enum Properties {
    // Constant values
    CLUSTERING_DISTANCE(0.01 * StyleLibrary.SCALE), // Margin between two rings
    CLUSTER_MIN_WIDTH(0.01), // Minimum width (radian) of Segment
    CLUSTER_DISTANCE(0.01), // Margin (radian) between segments
    RADIUS_INNER(0.05 * StyleLibrary.SCALE), // Offset from center to first ring
    RADIUS_OUTER(0.46 * StyleLibrary.SCALE), // Radius of whole CircleSegments
                                             // except selection border
    RADIUS_SELECTION(0.02 * StyleLibrary.SCALE), // Radius of highlight
                                                 // selection (outer ring)

    // Calculated Values
    ANGLE_PAIR(0.0), // width of a pair (radian)
    BORDER_WIDTH(0.0), // Width of cluster borders
    CLUSTER_MIN_COUNT(0.0), // Count of clusters needed to be resized
    PAIR_MIN_COUNT(0.0), // Less Paircount needs resizing
    RADIUS_DELTA(0.0); // Height of a clustering (ring)

    // getter/setter
    double value;

    Properties(double value) {
      this.value = value;
    }

    public double getValue() {
      return this.value;
    }

    public void setValue(double newval) {
      this.value = newval;
    }
  }

  /**
   * Color coding of CircleSegments
   */
  private static enum Colors {
    BORDER("#FF0073"), CLUSTER_UNPAIRED("#ffffff"), //
    HOVER_ALPHA("1.0"), HOVER_INCLUSTER("#008e9e"), HOVER_SELECTION("#73ff00"), //
    HOVER_PAIRED("#4ba600"), HOVER_UNPAIRED("#b20000"), //
    SELECTED_SEGMENT("#009900"), SELECTED_BORDER("#000000"), SELECTED_UNPAIRED_SEGMENT("#bababa");

    // getter/setter
    String color;

    Colors(String color) {
      this.color = color;
    }

    public String getColor() {
      return this.color;
    }
  }

  /**
   * Show unclustered Pairs in CircleSegments
   */
  boolean showUnclusteredPairs = false;

  UnorderedList selectionInfo;

  /**
   * Constructor
   */
  public CircleSegmentsVisualizer(VisualizationTask task) {
    super(task);
    ccr = task.getResult();
    // Listen for result changes (Selection changed)
    context.addResultListener(this);
  }

  public void showUnclusteredPairs(boolean show) {
    if(showUnclusteredPairs == show) {
      return;
    }
    showUnclusteredPairs = show;

    // recalculate values
    calculateSegmentProperties();

    // store attributes & delete old visLayer
    Node parent = visLayer.getParentNode();
    parent.removeChild(visLayer);

    // add new node to draw
    visLayer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    // visLayer.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE,
    // attTranslate);
    parent.appendChild(visLayer);

    redraw();

    selection.reselect();
  }

  /**
   * Create the segments
   */
  public void redraw() {
    int refClustering = 0;
    int refSegment = Segment.UNCLUSTERED;
    double offsetAngle = 0.0;

    // ITERATE OVER ALL SEGMENTS
    if(pairSegments == null) {
      calculateSegmentProperties();
    }

    this.segmentToElements.clear();

    for(final Segment id : pairSegments) {
      long currentPairCount = id.getPairCount();

      // resize small segments if below minimum
      double alpha = Properties.CLUSTER_MIN_WIDTH.getValue();
      if(currentPairCount > Properties.PAIR_MIN_COUNT.getValue()) {
        alpha = Properties.ANGLE_PAIR.getValue() * currentPairCount;
      }

      // ITERATE OVER ALL SEGMENT-CLUSTERS

      ArrayList<Element> elems = new ArrayList<Element>(clusterings);
      segmentToElements.put(id, elems);
      // draw segment for every clustering

      for(int i = 0; i < clusterings; i++) {
        double currentRadius = i * (Properties.RADIUS_DELTA.getValue() + Properties.CLUSTERING_DISTANCE.getValue()) + Properties.RADIUS_INNER.getValue();

        // Add border if the next segment is a different cluster in the
        // reference clustering
        if((refSegment != id.get(refClustering)) && refClustering == i) {
          Element border = SVGUtil.svgCircleSegment(svgp, centerx, centery, offsetAngle - Properties.CLUSTER_DISTANCE.getValue(), Properties.BORDER_WIDTH.getValue(), currentRadius, Properties.RADIUS_OUTER.getValue() - Properties.CLUSTERING_DISTANCE.getValue());
          border.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_BORDER_CLASS);
          visLayer.appendChild(border);

          if(id.get(refClustering) == Segment.UNCLUSTERED) {
            refClustering = Math.min(refClustering + 1, clusterings - 1);
          }
          refSegment = id.get(refClustering);
        }

        int cluster = id.get(i);

        // create ring segment
        Element segment = SVGUtil.svgCircleSegment(svgp, centerx, centery, offsetAngle, alpha, currentRadius, currentRadius + Properties.RADIUS_DELTA.getValue());
        elems.add(segment);

        // MouseEvents on segment cluster
        EventListener listener = new SegmentListenerProxy(id, i);
        EventTarget targ = (EventTarget) segment;
        targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, listener, false);
        targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, listener, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, listener, false);

        // Coloring based on clusterID
        if(cluster >= 0) {
          segment.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, cssClr[cluster].getName());
        }
        // if its an unpaired cluster set color to white
        else {
          segment.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_UNPAIRED_CLASS);
        }

        visLayer.appendChild(segment);
      }

      //
      // Add a extended strip for each segment to emphasis selection
      // (easier to track thin segments and their color coding and
      // differentiates them from cluster border lines)
      //

      double currentRadius = clusterings * (Properties.RADIUS_DELTA.getValue() + Properties.CLUSTERING_DISTANCE.getValue()) + Properties.RADIUS_INNER.getValue();
      Element extension = SVGUtil.svgCircleSegment(svgp, centerx, centery, offsetAngle, alpha, currentRadius, currentRadius + (Properties.RADIUS_SELECTION.getValue()));
      extension.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CCConstants.CLR_UNPAIRED_CLASS);
      svgp.putIdElement(CCConstants.SEG_EXTENSION_ID_PREFIX + id.toString(), extension);
      visLayer.appendChild(extension);

      // calculate angle for next segment
      offsetAngle += alpha + Properties.CLUSTER_DISTANCE.getValue();
    }
  }

  @Override
  public void resultChanged(Result current) {
    super.resultChanged(current);
    // Redraw on style result changes.
    if(current == context.getStyleResult()) {
      // FIXME: use the usual redrawing function, to allow other tools to change
      // the segment selection?
      // synchronizedRedraw();
    }
  }

  public void buildSegments() {
    VisualizationTask task = this.task;

    this.segments = ccr.getSegments();

    // Setup scaling for canvas: 0 to StyleLibrary.SCALE (usually 100 to avoid a
    // Java drawing bug!)
    // Plus some margin
    String transform = SVGUtil.makeMarginTransform(task.width, task.height, StyleLibrary.SCALE, StyleLibrary.SCALE, 0.1);
    this.layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.layer.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    this.visLayer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.ctrlLayer = svgp.svgElement(SVGConstants.SVG_G_TAG);

    this.selectionInfo = new UnorderedList(svgp);

    // create selection helper. SegmentSelection initializes and manages
    // CSStylingPolicy. Could completely replace SegmentSelection
    this.selection = new SegmentSelection(task, segments, selectionInfo);

    // calculate properties for drawing
    calculateSegmentProperties();

    // create Color shades for clusters
    String[] clusterColorShades = getGradient(clusterSize, Color.getColorSet(Color.ColorSet.GREY));

    cssClr = new CSSClass[clusterSize];
    for(int i = 0; i < clusterSize; i++) {
      cssClr[i] = new CSSClass(this, CLUSTERID + "_" + (i + 1));
      cssClr[i].setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, clusterColorShades[i]);
      cssClr[i].setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      svgp.addCSSClassOrLogError(cssClr[i]);
    }

    // initialize css
    addCSSClasses();

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
    // and add selection info
    // ! TODO VARIABLE LENGTH
    info.addItem(selectionInfo.asElement(), 50);

    ctrlLayer.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "scale(0.1)");
    ctrlLayer.appendChild(info.asElement());

    layer.appendChild(ctrlLayer);
    layer.appendChild(visLayer);
  }

  /**
   * Calculates segment properties like radius, width, etc. Values currently
   * vary only with "showUnclusteredPairs".
   */
  protected void calculateSegmentProperties() {
    if(segments == null) {
      buildSegments();
    }
    pairSegments = segments.getSegments();

    clusterSize = segments.getHighestClusterCount();
    clusterings = segments.getClusterings();

    Properties.ANGLE_PAIR.setValue((MathUtil.TWOPI - (Properties.CLUSTER_DISTANCE.getValue() * pairSegments.size())) / segments.getPairCount(showUnclusteredPairs));
    Properties.PAIR_MIN_COUNT.setValue(Math.ceil(Properties.CLUSTER_MIN_WIDTH.getValue() / Properties.ANGLE_PAIR.getValue()));

    // number of segments needed to be resized
    int segMinCount = 0;
    for(Segment segment : pairSegments) {
      if(segment.getPairCount() <= Properties.PAIR_MIN_COUNT.getValue()) {
        segMinCount++;
      }
    }

    Properties.CLUSTER_MIN_COUNT.setValue(segMinCount);

    // update width of a pair
    Properties.ANGLE_PAIR.setValue((MathUtil.TWOPI - (Properties.CLUSTER_DISTANCE.getValue() * pairSegments.size() + segMinCount * Properties.CLUSTER_MIN_WIDTH.getValue())) / (segments.getPairCount(showUnclusteredPairs) - Properties.CLUSTER_MIN_COUNT.getValue()));
    Properties.RADIUS_DELTA.setValue((Properties.RADIUS_OUTER.getValue() - Properties.RADIUS_INNER.getValue() - clusterings * Properties.CLUSTERING_DISTANCE.getValue()) / (clusterings));
    Properties.BORDER_WIDTH.setValue(Properties.CLUSTER_DISTANCE.getValue());
  }

  /**
   * Define and add required CSS classes
   */
  protected void addCSSClasses() {
    // CLUSTER BORDER
    CSSClass cssReferenceBorder = new CSSClass(this.getClass(), CCConstants.CLR_BORDER_CLASS);
    cssReferenceBorder.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, Colors.BORDER.getColor());
    svgp.addCSSClassOrLogError(cssReferenceBorder);

    // Note: !important is needed to override the regular color assignment

    // CLUSTER HOVER
    CSSClass cluster_hover = new CSSClass(this.getClass(), CCConstants.CLR_HOVER_CLASS);
    cluster_hover.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, Colors.HOVER_SELECTION.getColor() + " !important");
    cluster_hover.setStatement(SVGConstants.SVG_CURSOR_TAG, SVGConstants.SVG_POINTER_VALUE);
    svgp.addCSSClassOrLogError(cluster_hover);
    
    // Unpaired cluster segment
    CSSClass cluster_unpaired = new CSSClass(this.getClass(), CCConstants.CLR_UNPAIRED_CLASS);
    cluster_unpaired.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.CSS_WHITE_VALUE);
    cluster_unpaired.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.CSS_NONE_VALUE);
    svgp.addCSSClassOrLogError(cluster_unpaired);
  }

  /**
   * Creates a gradient over a set of colors
   * 
   * @param shades number of colors in the gradient
   * @param colors colors for the gradient
   * @return array of colors for css: "rgb(red, green, blue)"
   */
  protected static String[] getGradient(int shades, int[][] colors) {
    // only even shades
    shades += shades % 2;

    int colorCount = colors.length;
    String[] colorShades = new String[shades];

    if(shades <= 1 || colorCount <= 1) {
      colorShades[0] = "rgb(" + colors[0][0] + "," + colors[0][1] + "," + colors[0][2] + ")";
      return colorShades;
    }

    // steps to use between each colour
    int colorDelta = shades / (colorCount - 1);

    for(int s = 0; s < shades; s++) {
      int from = s / colorDelta;
      int to = ((s / colorDelta) + 1) % colorCount;
      int step = s % colorDelta;

      int r = colors[from][0] - ((colors[from][0] - colors[to][0]) / colorDelta) * step;
      int g = colors[from][1] - ((colors[from][1] - colors[to][1]) / colorDelta) * step;
      int b = colors[from][2] - ((colors[from][2] - colors[to][2]) / colorDelta) * step;

      colorShades[s] = Color.toCSS(r, g, b);
    }

    return colorShades;
  }

  protected Element getClusteringInfo() {
    Element thumbnail = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);

    // build thumbnail
    int startRadius = 4;
    int singleHeight = 12;
    int margin = 4;
    int radius = clusterings * (singleHeight + margin) + startRadius;

    SVGUtil.setAtt(thumbnail, SVGConstants.SVG_HEIGHT_ATTRIBUTE, radius);

    for(int i = 0; i < clusterings; i++) {
      double innerRadius = i * singleHeight + margin * i + startRadius;
      Element clr = SVGUtil.svgCircleSegment(svgp, radius - startRadius, radius - startRadius, Math.PI * 1.5, Math.PI * 0.5, innerRadius, innerRadius + singleHeight);
      clr.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#d4e4f1");
      clr.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
      clr.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "1.0");

      String labelText = segments.getClusteringDescription(i);
      Element label = svgp.svgText(radius + startRadius, radius - innerRadius - startRadius, labelText);
      thumbnail.appendChild(label);

      thumbnail.appendChild(clr);
    }

    return thumbnail;
  }

  protected void segmentHover(Segment segment, int ringid, boolean active) {
    if(active) {
      // abort if this are the unclustered pairs
      if(segment.isNone()) {
        return;
      }
      if(logger.isDebugging()) {
        logger.debug("Hover on segment: " + segment + " unpaired: " + segment.isUnpaired());
      }
  
      if(!segment.isUnpaired()) {
        //
        // STANDARD CLUSTER SEGMENT
        // highlight all ring segments in this clustering and this cluster
        //
        // highlight all corresponding ring Segments
        for(Entry<Segment, List<Element>> entry : segmentToElements.entrySet()) {
          Segment other = entry.getKey();
          // Same cluster in same clustering?
          if(other.get(ringid) != segment.get(ringid)) {
            continue;
          }
          Element ringSegment = entry.getValue().get(ringid);
          SVGUtil.addCSSClass(ringSegment, CCConstants.CLR_HOVER_CLASS);
        }
      }
      else {
        //
        // UNPAIRED SEGMENT
        // highlight all ring segments in this clustering responsible for
        // unpaired
        // segment
        //
        // get the paired segments corresponding to the unpaired segment
        List<Segment> paired = segments.getPairedSegments(segment);
        
        for(Segment other : paired) {
          Element ringSegment = segmentToElements.get(other).get(ringid);
          SVGUtil.addCSSClass(ringSegment, CCConstants.CLR_HOVER_CLASS);
        }
      }
    }
    else {
      for(List<Element> elems : segmentToElements.values()) {
        for(Element current : elems) {
          SVGUtil.removeCSSClass(current, CCConstants.CLR_HOVER_CLASS);
        }
      }
    }
  }

  protected void segmentClick(Segment segment, Event evt, boolean dblClick) {
    MouseEvent mouse = (MouseEvent) evt;
  
    // CTRL (add) pressed?
    boolean ctrl = false;
    if(mouse.getCtrlKey()) {
      ctrl = true;
    }
  
    // Unselect others on double click
    if(dblClick) {
      selection.deselectAllSegments();
    }
    selection.select(segment, ctrl);
    // update stylePolicy
    selection.update();
  }

  /**
   * Proxy element to connect signals.
   * 
   * @author Erich Schubert
   */
  private class SegmentListenerProxy implements EventListener {
    /**
     * Segment we are attached to
     */
    private Segment id;
    
    /**
     * Segment ring we are
     */
    private int ringid;
  
    /**
     * For detecting double clicks. TODO: use batik directly?
     */
    private long lastClick = 0;
  
    /**
     * Constructor.
     *
     * @param id Segment id
     * @param ringid Ring id
     */
    public SegmentListenerProxy(Segment id, int ringid) {
      super();
      this.id = id;
      this.ringid = ringid;
    }
    
    @Override
    public void handleEvent(Event evt) {
      if (SVGConstants.SVG_MOUSEOVER_EVENT_TYPE.equals(evt.getType())) {
        segmentHover(id, ringid, true);
      }
      if (SVGConstants.SVG_MOUSEOUT_EVENT_TYPE.equals(evt.getType())) {
        segmentHover(id, ringid, false);
      }
      if (SVGConstants.SVG_CLICK_EVENT_TYPE.equals(evt.getType())) {
        // Check Double Click
        boolean dblClick = false;
        long time = java.util.Calendar.getInstance().getTimeInMillis();
        if(time - lastClick <= CCConstants.EVT_DBLCLICK_DELAY) {
          dblClick = true;
        }
        lastClick = time;
  
        segmentClick(id, evt, dblClick);
      }
    }
  }

  /**
   * Factory for visualizers for a circle segment
   * 
   * @author Sascha Goldhofer
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses CircleSegmentsVisualizer oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new CircleSegmentsVisualizer(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // If no comparison result found abort
      List<ClusteringComparisonResult> ccr = ResultUtil.filterResults(result, ClusteringComparisonResult.class);
      for(ClusteringComparisonResult ccResult : ccr) {
        // create task for visualization
        final VisualizationTask task = new VisualizationTask(NAME, ccResult, null, this);
        task.width = 2.0;
        task.height = 2.0;
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
        baseResult.getHierarchy().add(ccResult, task);
      }
    }
  };
}