package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.algorithm.timeseries.ChangePoints;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

import java.util.List;

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
