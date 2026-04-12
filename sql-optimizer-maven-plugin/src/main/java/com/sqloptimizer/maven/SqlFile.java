package com.sqloptimizer.maven;

import java.util.List;

public class SqlFile {

    private final String path;
    private final List<String> sqlStatements;

    public SqlFile(String path, List<String> sqlStatements) {
        this.path = path;
        this.sqlStatements = sqlStatements;
    }

    public String getPath() {
        return path;
    }

    public List<String> getSqlStatements() {
        return sqlStatements;
    }
}
