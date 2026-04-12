package com.sqloptimizer.maven;

import com.sqloptimizer.core.model.OptimizationReport;
import com.sqloptimizer.core.service.SqlOptimizerService;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ReportGenerator {

    private final String outputDirectory;
    private final String reportFormat;

    public ReportGenerator(String outputDirectory, String reportFormat) {
        this.outputDirectory = outputDirectory;
        this.reportFormat = reportFormat;
    }

    public void generateReport(List<SqlFile> sqlFiles, SqlOptimizerService optimizerService) {
        switch (reportFormat.toLowerCase()) {
            case "html":
                generateHtmlReport(sqlFiles, optimizerService);
                break;
            case "json":
                generateJsonReport(sqlFiles, optimizerService);
                break;
            case "xml":
                generateXmlReport(sqlFiles, optimizerService);
                break;
            default:
                generateHtmlReport(sqlFiles, optimizerService);
        }
    }

    private void generateHtmlReport(List<SqlFile> sqlFiles, SqlOptimizerService optimizerService) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<title>SQL优化报告</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("h1 { color: #333; }");
        html.append("h2 { color: #666; }");
        html.append(".file { margin: 20px 0; padding: 10px; border: 1px solid #ddd; }");
        html.append(".sql { background-color: #f5f5f5; padding: 10px; margin: 10px 0; }");
        html.append(".issues { color: #d9534f; }");
        html.append(".advice { color: #5cb85c; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<h1>SQL优化报告</h1>");
        html.append("<p>共分析了 " + sqlFiles.size() + " 个文件</p>");

        for (SqlFile sqlFile : sqlFiles) {
            html.append("<div class='file'>");
            html.append("<h2>文件: " + sqlFile.getPath() + "</h2>");

            for (String sql : sqlFile.getSqlStatements()) {
                html.append("<div class='sql'>");
                html.append("<p><strong>SQL:</strong><br>" + sql + "</p>");

                // 这里简化处理，实际需要连接数据库进行分析
                // OptimizationReport report = optimizerService.analyze(sql, dataSource);
                // 暂时使用空报告
                html.append("<p class='issues'>问题: 未连接数据库，无法分析</p>");
                html.append("<p class='advice'>建议: 请在应用中使用SqlOptimizerService进行详细分析</p>");
                html.append("</div>");
            }

            html.append("</div>");
        }

        html.append("</body>");
        html.append("</html>");

        try {
            FileUtils.writeStringToFile(new File(outputDirectory, "sql-optimizer-report.html"), html.toString(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateJsonReport(List<SqlFile> sqlFiles, SqlOptimizerService optimizerService) {
        // 简化实现，实际需要生成完整的JSON格式
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"files\": [");

        for (int i = 0; i < sqlFiles.size(); i++) {
            SqlFile sqlFile = sqlFiles.get(i);
            json.append("{");
            json.append("\"path\": \"" + sqlFile.getPath() + "\",");
            json.append("\"sqlStatements\": [");

            for (int j = 0; j < sqlFile.getSqlStatements().size(); j++) {
                String sql = sqlFile.getSqlStatements().get(j);
                json.append("\"" + sql.replace("\"", "\\\"") + "\"");
                if (j < sqlFile.getSqlStatements().size() - 1) {
                    json.append(",");
                }
            }

            json.append("]");
            json.append("}");
            if (i < sqlFiles.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");
        json.append("}");

        try {
            FileUtils.writeStringToFile(new File(outputDirectory, "sql-optimizer-report.json"), json.toString(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateXmlReport(List<SqlFile> sqlFiles, SqlOptimizerService optimizerService) {
        // 简化实现，实际需要生成完整的XML格式
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<sql-optimizer-report>");
        xml.append("<file-count>" + sqlFiles.size() + "</file-count>");

        for (SqlFile sqlFile : sqlFiles) {
            xml.append("<file>");
            xml.append("<path>" + sqlFile.getPath() + "</path>");
            xml.append("<sql-statements>");

            for (String sql : sqlFile.getSqlStatements()) {
                xml.append("<sql>" + sql + "</sql>");
            }

            xml.append("</sql-statements>");
            xml.append("</file>");
        }

        xml.append("</sql-optimizer-report>");

        try {
            FileUtils.writeStringToFile(new File(outputDirectory, "sql-optimizer-report.xml"), xml.toString(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
