package com.sqloptimizer.core.model;

import java.util.List;

public class OptimizationReport {

    private List<OptimizationIssue> issues;
    private List<OptimizationAdvice> advice;
    private String optimizedSql;
    private long analysisTime;

    public OptimizationReport() {
    }

    public OptimizationReport(List<OptimizationIssue> issues, List<OptimizationAdvice> advice, String optimizedSql, long analysisTime) {
        this.issues = issues;
        this.advice = advice;
        this.optimizedSql = optimizedSql;
        this.analysisTime = analysisTime;
    }

    public List<OptimizationIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<OptimizationIssue> issues) {
        this.issues = issues;
    }

    public List<OptimizationAdvice> getAdvice() {
        return advice;
    }

    public void setAdvice(List<OptimizationAdvice> advice) {
        this.advice = advice;
    }

    public String getOptimizedSql() {
        return optimizedSql;
    }

    public void setOptimizedSql(String optimizedSql) {
        this.optimizedSql = optimizedSql;
    }

    public long getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(long analysisTime) {
        this.analysisTime = analysisTime;
    }

    @Override
    public String toString() {
        return "OptimizationReport{" +
                "issues=" + issues +
                ", advice=" + advice +
                ", optimizedSql='" + optimizedSql + '\'' +
                ", analysisTime=" + analysisTime +
                '}';
    }
}
