package com.hanoi.houseprice.controller;

import com.hanoi.houseprice.dto.BudgetSuggestRequest;
import com.hanoi.houseprice.dto.BudgetSuggestResponse;
import com.hanoi.houseprice.dto.FeatureImportanceResponse;
import com.hanoi.houseprice.dto.MetadataResponse;
import com.hanoi.houseprice.dto.PredictionRequest;
import com.hanoi.houseprice.dto.PredictionResponse;
import com.hanoi.houseprice.service.HousePriceModelService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PredictionController {
    private final HousePriceModelService modelService;

    public PredictionController(HousePriceModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping("/metadata")
    public MetadataResponse metadata() {
        return modelService.metadata();
    }

    @PostMapping("/predict")
    public PredictionResponse predict(@Valid @RequestBody PredictionRequest request) {
        return modelService.predict(request);
    }

    @GetMapping("/feature-importance")
    public FeatureImportanceResponse featureImportance() {
        return modelService.featureImportance();
    }

    @PostMapping("/budget-suggest")
    public BudgetSuggestResponse budgetSuggest(@Valid @RequestBody BudgetSuggestRequest request) {
        return modelService.budgetSuggest(request);
    }
}
