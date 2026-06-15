package com.hanoi.houseprice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class PredictionRequest {
    @Min(5)
    @Max(500)
    private double area;

    @NotBlank
    private String district;

    @Min(1)
    @Max(31)
    private int day = 5;

    @Min(1)
    @Max(12)
    private int month = 5;

    @Min(2015)
    @Max(2030)
    private int year = 2026;

    @Min(1)
    @Max(20)
    private int numBedrooms = 1;

    @Min(1)
    @Max(20)
    private int numBathrooms = 1;

    @Min(0)
    @Max(1)
    private int hasAirConditioning = 0;

    @Min(0)
    @Max(1)
    private int furnished = 0;

    @Min(1)
    @Max(100)
    private int floor = 1;

    @NotBlank
    private String roomType = "studio";

    @Min(0)
    @Max(100)
    private double distanceToCenter = 5.0;


    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getNumBedrooms() {
        return numBedrooms;
    }

    public void setNumBedrooms(int numBedrooms) {
        this.numBedrooms = numBedrooms;
    }

    public int getNumBathrooms() {
        return numBathrooms;
    }

    public void setNumBathrooms(int numBathrooms) {
        this.numBathrooms = numBathrooms;
    }

    public int getHasAirConditioning() {
        return hasAirConditioning;
    }

    public void setHasAirConditioning(int hasAirConditioning) {
        this.hasAirConditioning = hasAirConditioning;
    }

    public int getFurnished() {
        return furnished;
    }

    public void setFurnished(int furnished) {
        this.furnished = furnished;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getDistanceToCenter() {
        return distanceToCenter;
    }

    public void setDistanceToCenter(double distanceToCenter) {
        this.distanceToCenter = distanceToCenter;
    }
}
