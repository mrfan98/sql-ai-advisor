package com.sqloptimizer.core.model;

public class OptimizationAdvice {

    private String title;
    private String description;
    private String recommendation;
    private String example;

    public OptimizationAdvice() {
    }

    public OptimizationAdvice(String title, String description, String recommendation, String example) {
        this.title = title;
        this.description = description;
        this.recommendation = recommendation;
        this.example = example;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    @Override
    public String toString() {
        return "OptimizationAdvice{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", recommendation='" + recommendation + '\'' +
                ", example='" + example + '\'' +
                '}';
    }
}
