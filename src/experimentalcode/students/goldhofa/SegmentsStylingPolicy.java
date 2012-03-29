package experimentalcode.students.goldhofa;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments.Segment;
import de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments.Segments;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;

/**
 * Styling policy to communicate the segment selection to other visualizers.
 * 
 * @author Sascha Goldhofer
 */
public class SegmentsStylingPolicy implements ClassStylingPolicy {
  /**
   * The segments we use for visualization
   */
  protected final Segments segments;

  /**
   * Selected segments
   */
  protected ArrayList<Segment> selectedSegments = new ArrayList<Segment>();

  /**
   * Not selected segments.
   */
  protected TreeSet<Segment> unselectedSegments = new TreeSet<Segment>();

  /**
   * Segments indirectly selected
   */
  protected TreeMap<Segment, Segment> selectedUnpairedSegments = new TreeMap<Segment, Segment>();

  /**
   * Not selected IDs that will be drawn in default colors.
   */
  protected ModifiableDBIDs unselectedObjects = DBIDUtil.newHashSet();

  /**
   * Constructor.
   * 
   * @param segments Segments
   */
  public SegmentsStylingPolicy(Segments segments) {
    super();
    this.segments = segments;

    // get all selectable segments
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

  /**
   * Adds or removes the given segment to the selection. Depending on the
   * clustering and cluster selected and the addToSelection option given, the
   * current selection will be modified. This method is called by clicking on a
   * segment and ring and the CTRL-button status.
   * 
   * Adding selections does only work on the same clustering and cluster, else a
   * new selection will be added.
   * 
   * @param segment the selected element representing a segment ring (specific
   *        clustering)
   * @param addToSelection flag for adding segment to current selection
   */
  public void select(Segment segment, boolean addToSelection) {
    // abort if segment represents pairs inNone. Would select all segments...
    if(segment.isNone()) {
      return;
    }

    // get selected segments
    if(segment.isUnpaired()) {
      // (unpaired segments represent multiple segments)
      List<Segment> newSelection = segments.getPairedSegments(segment);

      // check if all segments are selected
      boolean allSegmentsSelected = true;
      for(int i = 0; i < newSelection.size(); ++i) {
        if(!hasSegmentSelected(newSelection.get(i))) {
          allSegmentsSelected = false;
          break;
        }
      }

      // if all are selected, deselect all
      if(allSegmentsSelected) {
        for(int i = 0; i < newSelection.size(); ++i) {
          Segment other = newSelection.get(i);
          deselectSegment(other);
        }
        // and deselect unpaired segment
        deselectSegment(segment);
      }
      else {
        // else, select all
        for(int i = 0; i < newSelection.size(); ++i) {
          Segment other = newSelection.get(i);
          selectSegment(other);
        }
        // and highlight pair segment
        selectSegment(segment);
      }
    }
    else {
      // an object segment was selected
      if(hasSegmentSelected(segment)) {
        // Deselect
        deselectSegment(segment);
      }
      else {
        // highlight segment
        selectSegment(segment);
      }
    }
  }

  /**
   * Deselect all currently selected segments
   */
  public void deselectAllSegments() {
    ArrayList<Segment> selectedSegments = getSelectedSegments();
    for(int i = 0; i < selectedSegments.size(); ++i) {
      deselectSegment(selectedSegments.get(i));
    }
  }

  /**
   * Deselect a segment
   * 
   * @param segment Segment to deselect
   */
  protected void deselectSegment(Segment segment) {
    if(segment.isUnpaired()) {
      ArrayList<Segment> remove = new ArrayList<Segment>();
      // remove all object segments associated with unpaired segment from
      // selection list
      for(Segment objSegment : selectedUnpairedSegments.keySet()) {
        if(selectedUnpairedSegments.get(objSegment).compareTo(segment) == 0) {
          remove.add(objSegment);
        }
      }

      for(int i = 0; i < remove.size(); ++i) {
        selectedUnpairedSegments.remove(remove.get(i));
      }
    }
    else {
      // check if deselected object Segment has a unpaired segment highlighted
      if(selectedUnpairedSegments.containsKey(segment)) {
        Segment unpaired = selectedUnpairedSegments.get(segment);
        // remove highlight
        deselectSegment(unpaired);
      }
      if(!unselectedSegments.contains(segment)) {
        selectedSegments.remove(segment);
        unselectedSegments.add(segment);
        if(segment.getDBIDs() != null) {
          unselectedObjects.addDBIDs(segment.getDBIDs());
        }
      }
    }
  }

  /**
   * Select a segment
   * 
   * @param segment Segment to select
   */
  protected void selectSegment(Segment segment) {
    if(segment.isUnpaired()) {
      // remember selected unpaired segment
      for(Segment other : segments.getPairedSegments(segment)) {
        selectedUnpairedSegments.put(other, segment);
      }
    }
    else {
      if(!selectedSegments.contains(segment)) {
        selectedSegments.add(segment);
        unselectedSegments.remove(segment);
        if(segment.getDBIDs() != null) {
          unselectedObjects.removeDBIDs(segment.getDBIDs());
        }
      }
    }
  }
}