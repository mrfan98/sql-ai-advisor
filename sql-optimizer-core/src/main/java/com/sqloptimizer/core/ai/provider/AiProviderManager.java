package com.sqloptimizer.core.ai.provider;

import com.sqloptimizer.core.ai.model.AiProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

/**
 * AI Provider管理器
 * 管理多个Provider，支持动态切换
 */
public class AiProviderManager {

    private static final Logger log = LoggerFactory.getLogger(AiProviderManager.class);

    private final Map<String, AiProvider> providers = new ConcurrentHashMap<>();
    private volatile String defaultProviderName;

    public AiProviderManager() {
    }

    public AiProviderManager(List<AiProviderConfig> configs) {
        if (configs != null) {
            for (AiProviderConfig config : configs) {
                registerProvider(config);
            }
        }
    }

    /**
     * 注册Provider
     *
     * @param config Provider配置
     * @return 注册的Provider实例
     */
    public AiProvider registerProvider(AiProviderConfig config) {
        AiProvider provider = AiProviderFactory.create(config);
        providers.put(config.getName(), provider);

        if (defaultProviderName == null || config.isEnabled()) {
            defaultProviderName = config.getName();
        }

        log.info("Registered AI provider: {} (default: {})", config.getName(), defaultProviderName);
        return provider;
    }

    /**
     * 移除Provider
     */
    public AiProvider removeProvider(String name) {
        AiProvider removed = providers.remove(name);
        if (removed != null) {
            log.info("Removed AI provider: {}", name);
            if (defaultProviderName != null && defaultProviderName.equals(name)) {
                // 选择一个新的默认Provider
                defaultProviderName = providers.isEmpty() ? null : providers.keySet().iterator().next();
            }
        }
        return removed;
    }

    /**
     * 获取Provider
     */
    public Optional<AiProvider> getProvider(String name) {
        return Optional.ofNullable(providers.get(name));
    }

    /**
     * 获取默认Provider
     */
    public Optional<AiProvider> getDefaultProvider() {
        if (defaultProviderName == null) {
            return Optional.empty();
        }
        return getProvider(defaultProviderName);
    }

    /**
     * 设置默认Provider
     */
    public void setDefaultProvider(String name) {
        if (!providers.containsKey(name)) {
            throw new IllegalArgumentException("Provider not found: " + name);
        }
        this.defaultProviderName = name;
        log.info("Set default provider to: {}", name);
    }

    /**
     * 获取所有Provider
     */
    public List<AiProvider> getAllProviders() {
        return new ArrayList<>(providers.values());
    }

    /**
     * 获取所有启用的Provider
     */
    public List<AiProvider> getEnabledProviders() {
        return providers.values().stream()
                .filter(AiProvider::isAvailable)
                .toList();
    }

    /**
     * 检查是否有可用的Provider
     */
    public boolean hasAvailableProvider() {
        return providers.values().stream().anyMatch(AiProvider::isAvailable);
    }

    /**
     * 获取Provider数量
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * 获取Provider名称列表
     */
    public List<String> getProviderNames() {
        return new ArrayList<>(providers.keySet());
    }

    /**
     * 清空所有Provider
     */
    public void clear() {
        providers.clear();
        defaultProviderName = null;
    }

    /**
     * 验证所有Provider的API Key
     */
    public Map<String, Boolean> validateAllApiKeys() {
        Map<String, Boolean> results = new ConcurrentHashMap<>();
        for (Map.Entry<String, AiProvider> entry : providers.entrySet()) {
            try {
                boolean valid = entry.getValue().validateApiKey();
                results.put(entry.getKey(), valid);
                log.info("Provider {} API key validation: {}", entry.getKey(), valid ? "VALID" : "INVALID");
            } catch (Exception e) {
                results.put(entry.getKey(), false);
                log.warn("Provider {} API key validation failed: {}", entry.getKey(), e.getMessage());
            }
        }
        return results;
    }

    @Override
    public String toString() {
        return "AiProviderManager{" +
                "providers=" + providers.keySet() +
                ", default='" + defaultProviderName + '\'' +
                '}';
    }
}
