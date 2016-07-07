package de.lmu.ifi.dbs.elki.algorithm.timeseries;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.result.ChangePointDetectionResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import java.util.*;

public class OfflineChangePointDetectionAlgorithm extends AbstractAlgorithm<ChangePointDetectionResult> {

    private int confidence, bootstrap_steps;

    public OfflineChangePointDetectionAlgorithm(int confidence, int bootstrap_steps) {
        this.confidence = confidence;
        this.bootstrap_steps = bootstrap_steps;
    }

    public ChangePointDetectionResult run(Database database) {
        Relation<DoubleVector> relation = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
        Relation<LabelList> labellist = database.getRelation(TypeUtil.LABELLIST);

        List<ChangePoints> result = new ArrayList<>();

        for(DBIDIter realtion_iter = relation.getDBIDs().iter(), label_iter = labellist.getDBIDs().iter()
            ; realtion_iter.valid()
            ; realtion_iter.advance(), label_iter.advance()) {

            result.add(new ChangePoints(
                    multiple_changepoints_with_confidence(relation.get(realtion_iter).getValues(), confidence, bootstrap_steps)
                    , labellist.get(label_iter).toString()));
        }

        return new ChangePointDetectionResult("Change Point List", "changepoints", result);
    }

    private double[] likelihood_ratio_change_in_mean(double[] values){
        double[] result = new double[values.length];

        // vector containing means for all different vector lengths, last index contains mean over all elements
        double[] means_left = new double[values.length];
        Mean current_mean_left = new Mean();
        for(int i = 0; i < means_left.length; i++){
            current_mean_left.put(values[i]);
            means_left[i] = current_mean_left.getMean();
        }
        // first index contains mean over all elements coming from the other side
        double[] means_right = new double[values.length];
        Mean current_mean_right = new Mean();
        for(int i = means_right.length-1; i >= 0; i--){
            current_mean_right.put(values[i]);
            means_right[i] = current_mean_right.getMean();
        }

        result[0] = -(VMath.sum_elements(VMath.square(VMath.minus(values, means_right[0]))));
        for(int i = 1; i < values.length; i++){
            result[i] = -(  (VMath.sum_elements(VMath.square(VMath.minus(Arrays.copyOfRange(values, 0, i), means_left[i-1]))))
                        +   (VMath.sum_elements(VMath.square(VMath.minus(Arrays.copyOfRange(values, i, values.length), means_right[i]))))
                        );
        }

        return result;
    }

    // DOES NOT WORK - REASON NOT YET INVESTIGATED
    private double[] likelihood_ratio_change_in_mean_optimised(double[] values){
        double[] result = new double[values.length];

        // vector containing means for all different vector lengths, last index contains mean over all elements
        double[] means_left = new double[values.length];
        Mean current_mean_left = new Mean();
        for(int i = 0; i < means_left.length; i++){
            current_mean_left.put(values[i]);
            means_left[i] = current_mean_left.getMean();
        }
        // first index contains mean over all elements coming from the other side
        double[] means_right = new double[values.length];
        Mean current_mean_right = new Mean();
        for(int i = means_right.length-1; i >= 0; i--){
            current_mean_right.put(values[i]);
            means_right[i] = current_mean_right.getMean();
        }

        result[0]=0;
        double tmp_mean_dif;
        for(int i = 1; i < values.length; i++) {
            tmp_mean_dif = means_left[i-1] - means_right[i];
            result[i]= -i*(values.length - i)*tmp_mean_dif*tmp_mean_dif;
        }

        return result;

    }

    private List<ChangePoint> multiple_changepoints_with_confidence(double[] values, int confidence, int bootstrap_steps){
        List<ChangePoint> result = new ArrayList<>();
        result = multiple_changepoints_with_confidence(result, values, confidence, bootstrap_steps, 0);
        return result;
    }

    private List<ChangePoint> multiple_changepoints_with_confidence(List<ChangePoint> result, double[] values, int confidence, int bootstrap_steps, int tmp_arrary_start_index){
        double tmp_conf = confidence_level(values, bootstrap_steps);
        int tmp_max_pos = tmp_arrary_start_index + get_maximum_index(likelihood_ratio_change_in_mean(values)); // return the detected changepoint

        if(!(tmp_conf < confidence || values.length <=3 || (tmp_max_pos - tmp_arrary_start_index + 1 == values.length))){ // cannot split up arrays of size 3, that would make every element a change poin
            multiple_changepoints_with_confidence(result
                                                    , Arrays.copyOfRange(values, 0, tmp_max_pos - tmp_arrary_start_index)
                                                    , confidence
                                                    , bootstrap_steps
                                                    , tmp_arrary_start_index);
            multiple_changepoints_with_confidence(result
                                                    , Arrays.copyOfRange(values, tmp_max_pos - tmp_arrary_start_index + 1, values.length)
                                                    , confidence
                                                    , bootstrap_steps
                                                    , tmp_max_pos);
            result.add(new ChangePoint(tmp_max_pos, tmp_conf));
        }

        return result;
    }

    //TRY OUT RESULT HANDLING
    private ChangePoint single_changepoint_with_confidence(String label, double[] values, int confidence, int bootstrap_steps){
        double conf = confidence_level(values, bootstrap_steps);
        double index = get_maximum_index(likelihood_ratio_change_in_mean(values));
        return new ChangePoint(index, conf, label);
    }


    private double confidence_level(double[] values, int steps){
        double estimator = get_bootstrap_estimator(likelihood_ratio_change_in_mean(values));
        int x = 0;
        for(int i=0; i < steps; i++){
            double[] tmp_values = shuffle_vector(values);
            double tmp_estimator = get_bootstrap_estimator(likelihood_ratio_change_in_mean(tmp_values));
            if (tmp_estimator < estimator){
                x += 1;
            }
        }
        return 100 * ((double)x/(double)steps);
    }

    private double get_bootstrap_estimator(double[] values){
        return get_maximum(values)-get_minimum(values);
    }

    // move to VMath??
    private double[] shuffle_vector(double[] values)
    {
        double[] result= VMath.copy(values);
        Random rnd = new Random();
        for (int i = result.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            double tmp = result[index];
            result[index] = result[i];
            result[i] = tmp;
        }

        return result;
    }

    // move to VMath??
    private int get_maximum_index(double[] values){
        int result = 0;
        for (int i = 0; i < values.length; i++){
            if ((values[i] >= values[result])){
                result = i;
            }
        }
        return result;
    }

    // move to VMath??
    private double get_maximum(double[] values){
        double result = values[0];
        for(double value : values){
            if(value > result){
                result = value;
            }
        }
        return result;
    }

    // move to VMath??
    private double get_minimum(double[] values){
        double result = values[0];
        for(double value : values){
            if(value < result){
                result = value;
            }
        }
        return result;
    }

    @Override
    public TypeInformation[] getInputTypeRestriction() {
        return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
    }

    @Override
    protected Logging getLogger() {
        return null;
    }

    public static class Parameterizer extends AbstractParameterizer {

        public static final OptionID CONFIDENCE_ID = new OptionID("changepointdetection.confidence", //
                "Confidence level for terminating");

        public static final OptionID BOOTSTRAP_ID = new OptionID("changepointdetection.bootstrapsteps", //
                "Steps for bootstrapping");

        private int confidence, bootstrap_steps;

        @Override
        protected void makeOptions(Parameterization config) {
            super.makeOptions(config);
            IntParameter confidence_parameter = new IntParameter(CONFIDENCE_ID) //
                    .setOptional(false) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
            if(config.grab(confidence_parameter)) {
                confidence = confidence_parameter.getValue();
            }
            IntParameter bootstrap_steps_parameter = new IntParameter(BOOTSTRAP_ID) //
                    .setOptional(false) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
            if(config.grab(bootstrap_steps_parameter)) {
                bootstrap_steps = bootstrap_steps_parameter.getValue();
            }
        }

        @Override
        protected OfflineChangePointDetectionAlgorithm makeInstance() {
            return new OfflineChangePointDetectionAlgorithm(confidence, bootstrap_steps);
        }
    }
}
