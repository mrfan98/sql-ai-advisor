package com.sqloptimizer.maven;

import com.sqloptimizer.core.service.SqlOptimizerService;
import com.sqloptimizer.core.service.impl.SqlOptimizerServiceImpl;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "analyze")
public class SqlOptimizerMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/sql-optimizer-reports")
    private String outputDirectory;

    @Parameter(defaultValue = "src/main/resources,src/main/java")
    private List<String> scanPaths;

    @Parameter(defaultValue = "**/*.xml,**/*.sql,**/*.java")
    private List<String> includes;

    @Parameter(defaultValue = "**/target/**")
    private List<String> excludes;

    @Parameter(defaultValue = "html")
    private String reportFormat;

    @Parameter
    private String openAiApiKey;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("开始SQL优化分析...");

        // 创建输出目录
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 初始化SQL优化服务
        SqlOptimizerService optimizerService;
        if (openAiApiKey != null && !openAiApiKey.isEmpty()) {
            optimizerService = new SqlOptimizerServiceImpl(openAiApiKey);
            getLog().info("启用AI优化建议功能");
        } else {
            optimizerService = new SqlOptimizerServiceImpl();
            getLog().info("使用默认优化规则");
        }

        // 扫描SQL文件
        SqlScanner scanner = new SqlScanner(scanPaths, includes, excludes);
        List<SqlFile> sqlFiles = scanner.scan(project.getBasedir());

        getLog().info("找到 " + sqlFiles.size() + " 个包含SQL的文件");

        // 分析SQL并生成报告
        ReportGenerator reportGenerator = new ReportGenerator(outputDirectory, reportFormat);
        reportGenerator.generateReport(sqlFiles, optimizerService);

        getLog().info("SQL优化分析完成，报告生成在: " + outputDirectory);
    }
}
