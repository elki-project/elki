package de.lmu.ifi.dbs.elki.algorithm.timeseries;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.ChangePointDetectionResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Created by sebastian on 22/06/16.
 */
public class OfflineChangePointDetectionAlgorithm extends AbstractAlgorithm<ChangePointDetectionResult> {

    protected OfflineChangePointDetectionAlgorithm() {
        super();
    }

    @Override
    public ChangePointDetectionResult run(Database database) {
        return super.run(database);
    }

    @Override
    public TypeInformation[] getInputTypeRestriction() {
        return TypeUtil.array(TypeUtil.DOUBLE_VECTOR_FIELD);
    }

    @Override
    protected Logging getLogger() {
        return null;
    }

    public static class Parameterizer extends AbstractParameterizer {

        @Override
        protected void makeOptions(Parameterization config) {
            super.makeOptions(config);
        }

        @Override
        protected OfflineChangePointDetectionAlgorithm makeInstance() {
            return new OfflineChangePointDetectionAlgorithm();
        }
    }
}
