package com.hanoi.houseprice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class BudgetSuggestRequest {
    @Min(1)
    @Max(200)
    private double budgetMillion;

    private String roomType = "studio";

    @Min(0)
    @Max(1)
    private int hasAirConditioning = 0;

    @Min(0)
    @Max(1)
    private int furnished = 0;

    @Min(1)
    @Max(20)
    private int numBedrooms = 1;

    public double getBudgetMillion() { return budgetMillion; }
    public void setBudgetMillion(double budgetMillion) { this.budgetMillion = budgetMillion; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public int getHasAirConditioning() { return hasAirConditioning; }
    public void setHasAirConditioning(int hasAirConditioning) { this.hasAirConditioning = hasAirConditioning; }

    public int getFurnished() { return furnished; }
    public void setFurnished(int furnished) { this.furnished = furnished; }

    public int getNumBedrooms() { return numBedrooms; }
    public void setNumBedrooms(int numBedrooms) { this.numBedrooms = numBedrooms; }
}
