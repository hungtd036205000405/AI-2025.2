package com.hanoi.houseprice.dto;

import java.util.List;

public class FeatureImportanceResponse {
    private final List<FeatureScore> linear;
    private final List<FeatureScore> rf;
    private final List<FeatureScore> xgboost;
    private final List<FeatureScore> nn;

    public FeatureImportanceResponse(
            List<FeatureScore> linear,
            List<FeatureScore> rf,
            List<FeatureScore> xgboost,
            List<FeatureScore> nn
    ) {
        this.linear = linear;
        this.rf = rf;
        this.xgboost = xgboost;
        this.nn = nn;
    }

    public List<FeatureScore> getLinear() { return linear; }
    public List<FeatureScore> getRf() { return rf; }
    public List<FeatureScore> getXgboost() { return xgboost; }
    public List<FeatureScore> getNn() { return nn; }

    public static class FeatureScore {
        private final String feature;
        private final double importance;
        private final String featureVi;

        public FeatureScore(String feature, double importance, String featureVi) {
            this.feature = feature;
            this.importance = importance;
            this.featureVi = featureVi;
        }

        public String getFeature() { return feature; }
        public double getImportance() { return importance; }
        public String getFeatureVi() { return featureVi; }
    }
}
