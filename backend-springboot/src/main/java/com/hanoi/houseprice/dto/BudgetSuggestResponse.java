package com.hanoi.houseprice.dto;

import java.util.List;

public class BudgetSuggestResponse {
    private final List<DistrictOption> options;
    private final double budgetMillion;

    public BudgetSuggestResponse(List<DistrictOption> options, double budgetMillion) {
        this.options = options;
        this.budgetMillion = budgetMillion;
    }

    public List<DistrictOption> getOptions() { return options; }
    public double getBudgetMillion() { return budgetMillion; }

    public static class DistrictOption {
        private final String district;
        private final double suggestedAreaMin;
        private final double suggestedAreaMax;
        private final double estimatedPrice;
        private final double pricePerSqm;
        private final String valueTag; // "Tốt nhất", "Cân bằng", "Tiện nghi"

        public DistrictOption(String district, double suggestedAreaMin, double suggestedAreaMax,
                              double estimatedPrice, double pricePerSqm, String valueTag) {
            this.district = district;
            this.suggestedAreaMin = suggestedAreaMin;
            this.suggestedAreaMax = suggestedAreaMax;
            this.estimatedPrice = estimatedPrice;
            this.pricePerSqm = pricePerSqm;
            this.valueTag = valueTag;
        }

        public String getDistrict() { return district; }
        public double getSuggestedAreaMin() { return suggestedAreaMin; }
        public double getSuggestedAreaMax() { return suggestedAreaMax; }
        public double getEstimatedPrice() { return estimatedPrice; }
        public double getPricePerSqm() { return pricePerSqm; }
        public String getValueTag() { return valueTag; }
    }
}
