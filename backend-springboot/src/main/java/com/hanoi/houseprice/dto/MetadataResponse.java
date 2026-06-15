package com.hanoi.houseprice.dto;

import java.util.List;
import java.util.Map;

public class MetadataResponse {
    private final List<String> districts;
    private final int trainingRows;
    private final int featureCount;
    private final double averagePriceMillion;
    private final double averageArea;
    private final double minPriceMillion;
    private final double maxPriceMillion;
    private final List<Map<String, Object>> districtAverages;
    private final List<Map<String, Object>> priceBuckets;
    private final List<Map<String, Object>> areaPricePoints;
    private final List<ModelEvaluationMetric> metrics;

    public MetadataResponse(
            List<String> districts,
            int trainingRows,
            int featureCount,
            double averagePriceMillion,
            double averageArea,
            double minPriceMillion,
            double maxPriceMillion,
            List<Map<String, Object>> districtAverages,
            List<Map<String, Object>> priceBuckets,
            List<Map<String, Object>> areaPricePoints,
            List<ModelEvaluationMetric> metrics
    ) {
        this.districts = districts;
        this.trainingRows = trainingRows;
        this.featureCount = featureCount;
        this.averagePriceMillion = averagePriceMillion;
        this.averageArea = averageArea;
        this.minPriceMillion = minPriceMillion;
        this.maxPriceMillion = maxPriceMillion;
        this.districtAverages = districtAverages;
        this.priceBuckets = priceBuckets;
        this.areaPricePoints = areaPricePoints;
        this.metrics = metrics;
    }

    public List<String> getDistricts() {
        return districts;
    }

    public int getTrainingRows() {
        return trainingRows;
    }

    public int getFeatureCount() {
        return featureCount;
    }

    public double getAveragePriceMillion() {
        return averagePriceMillion;
    }

    public double getAverageArea() {
        return averageArea;
    }

    public double getMinPriceMillion() {
        return minPriceMillion;
    }

    public double getMaxPriceMillion() {
        return maxPriceMillion;
    }

    public List<Map<String, Object>> getDistrictAverages() {
        return districtAverages;
    }

    public List<Map<String, Object>> getPriceBuckets() {
        return priceBuckets;
    }

    public List<Map<String, Object>> getAreaPricePoints() {
        return areaPricePoints;
    }

    public List<ModelEvaluationMetric> getMetrics() {
        return metrics;
    }
}
