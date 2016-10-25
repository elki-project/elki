package de.lmu.ifi.dbs.elki.result;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.algorithm.timeseries.ChangePoints;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

import java.util.List;

/**
 * Change point detection result
 * Used by change or trend detection algorithms
 *
 * @author Sebastian Rühl
 */
public class ChangePointDetectionResult extends BasicResult implements TextWriteable {

    private List<ChangePoints> results;
    private Relation<LabelList> labellist;

    public ChangePointDetectionResult(String name, String shortname, List<ChangePoints> results, Relation<LabelList> labellist) {
        super(name, shortname);
        this.results = results;
        this.labellist = labellist;
    }

    public void writeToText(TextWriterStream out, String label) {
        DBIDIter labeliter = labellist.iterDBIDs();
        for(ChangePoints cp : results) {
            out.inlinePrintNoQuotes(cp.appendTo(new StringBuilder(), labellist.get(labeliter)));
            out.flush();
            labeliter.advance();
        }
    }
}
