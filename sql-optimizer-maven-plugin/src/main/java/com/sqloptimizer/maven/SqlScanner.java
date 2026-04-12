package com.sqloptimizer.maven;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlScanner {

    private static final Pattern SQL_PATTERN = Pattern.compile(
            "(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|GRANT|REVOKE)[\\s\\S]*?;",
            Pattern.CASE_INSENSITIVE
    );

    private final List<String> scanPaths;
    private final List<String> includes;
    private final List<String> excludes;

    public SqlScanner(List<String> scanPaths, List<String> includes, List<String> excludes) {
        this.scanPaths = scanPaths;
        this.includes = includes;
        this.excludes = excludes;
    }

    public List<SqlFile> scan(File baseDir) {
        List<SqlFile> sqlFiles = new ArrayList<>();

        for (String scanPath : scanPaths) {
            File path = new File(baseDir, scanPath);
            if (path.exists() && path.isDirectory()) {
                scanDirectory(path, sqlFiles);
            }
        }

        return sqlFiles;
    }

    private void scanDirectory(File directory, List<SqlFile> sqlFiles) {
        // 创建包含过滤器
        List<IOFileFilter> includeFilters = new ArrayList<>();
        for (String include : includes) {
            if (include.startsWith("**/*")) {
                includeFilters.add(new SuffixFileFilter(include.substring(3)));
            }
        }
        IOFileFilter includeFilter = new OrFileFilter(includeFilters);

        // 扫描文件
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                // 检查是否需要排除
                boolean exclude = false;
                for (String excludePattern : excludes) {
                    if (file.getPath().contains(excludePattern)) {
                        exclude = true;
                        break;
                    }
                }
                if (!exclude) {
                    scanDirectory(file, sqlFiles);
                }
            } else if (includeFilter.accept(file)) {
                // 读取文件内容并提取SQL
                try {
                    String content = FileUtils.readFileToString(file, "UTF-8");
                    List<String> sqlStatements = extractSqlStatements(content);
                    if (!sqlStatements.isEmpty()) {
                        sqlFiles.add(new SqlFile(file.getPath(), sqlStatements));
                    }
                } catch (IOException e) {
                    // 忽略读取错误
                }
            }
        }
    }

    private List<String> extractSqlStatements(String content) {
        List<String> sqlStatements = new ArrayList<>();
        Matcher matcher = SQL_PATTERN.matcher(content);
        while (matcher.find()) {
            sqlStatements.add(matcher.group());
        }
        return sqlStatements;
    }
}
