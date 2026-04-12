package com.sqloptimizer.core.model;

public class OptimizationIssue {

    public enum IssueType {
        FULL_TABLE_SCAN,
        MISSING_INDEX,
        IMPLICIT_CONVERSION,
        SELECT_ALL_COLUMNS,
        NESTED_LOOP,
        TEMP_TABLE,
        SORT_OPERATION,
        OTHER
    }

    private IssueType type;
    private String description;
    private String severity;
    private String location;

    public OptimizationIssue() {
    }

    public OptimizationIssue(IssueType type, String description, String severity, String location) {
        this.type = type;
        this.description = description;
        this.severity = severity;
        this.location = location;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "OptimizationIssue{" +
                "type=" + type +
                ", description='" + description + '\'' +
                ", severity='" + severity + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
