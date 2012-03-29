package experimentalcode.students.goldhofa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments.Segment;
import de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments.Segments;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;

/**
 * Die CS Vis will also eine eigene stylingpolicy basierend auf
 * singleobjectstyling? Genau so ist das gedacht. Das erzeugt eine eigene
 * styling policy, und setzt diese dann im StylingResult als aktiv. Dann löst
 * sie ein "ResultChanged" für das StylingResult aus, und die anderen Visualizer
 * zeichnen neu
 * 
 * @author Sascha Goldhofer
 */
public class CSStylingPolicy implements ClassStylingPolicy {
  // all segments
  Segments segments;

  // selection
  protected ArrayList<Segment> selectedSegments;

  protected TreeSet<Segment> unselectedSegments;

  protected ModifiableDBIDs unselectedObjects;

  public CSStylingPolicy(Segments segments, StyleLibrary style) {
    super();
    this.segments = segments;

    // get all selectable segments
    unselectedSegments = new TreeSet<Segment>();
    unselectedObjects = DBIDUtil.newHashSet();
    for(Segment segment : segments) {
      // store segmentID
      if(!segment.isUnpaired()) {
        unselectedSegments.add(segment);
        // and store their get all objects
        if(segment.getDBIDs() != null) {
          unselectedObjects.addDBIDs(segment.getDBIDs());
        }
      }
    }

    selectedSegments = new ArrayList<Segment>();
  }

  public void selectObjects(Segment segment) {
    if(selectedSegments.contains(segment)) {
      return;
    }
    selectedSegments.add(segment);
    unselectedSegments.remove(segment);
    if(segment.getDBIDs() != null) {
      unselectedObjects.removeDBIDs(segment.getDBIDs());
    }
  }

  public boolean hasSegmentSelected(Segment segment) {
    return selectedSegments.contains(segment);
  }

  public ArrayList<Segment> getSelectedSegments() {
    return selectedSegments;
  }

  public void deselectObjects(Segment segment) {
    if(unselectedSegments.contains(segment)) {
      return;
    }
    selectedSegments.remove(segment);
    unselectedSegments.add(segment);
    if(segment.getDBIDs() != null) {
      unselectedObjects.addDBIDs(segment.getDBIDs());
    }
  }

  public void deselectAllObjects() {
    for(Segment segment : selectedSegments) {
      unselectedSegments.add(segment);
      if(segment.getDBIDs() != null) {
        unselectedObjects.addDBIDs(segment.getDBIDs());
      }
    }
    selectedSegments.clear();
  }

  @Override
  public int getStyleForDBID(DBID id) {
    /*
     * System.out.println("policy: serving style"); for(int i = 0; i <
     * ids.size(); i++) { if(ids.get(i).contains(id)) { return i; } }
     */
    return -2;
  }

  @Override
  public int getColorForDBID(DBID id) {
    /*
     * System.out.println("policy: serving color"); for(int i = 0; i <
     * ids.size(); i++) { if(ids.get(i).contains(id)) { return colors.get(i); }
     * }
     */
    return 0;
  }

  @Override
  // -2=grau, -1=schwarz, 0+=farben
  public int getMinStyle() {
    return -2;
  }

  @Override
  public int getMaxStyle() {
    return selectedSegments.size();// ids.size();
  }

  @Override
  public Iterator<DBID> iterateClass(int cnum) {
    // unselected
    if(cnum == -2) {
      return unselectedObjects.iterator();
    }
    else if(cnum == -1) {
      return DBIDUtil.EMPTYDBIDS.iterator();
    }
    // colors
    DBIDs ids = selectedSegments.get(cnum).getDBIDs();
    return (ids != null) ? ids.iterator() : DBIDUtil.EMPTYDBIDS.iterator();
  }
}