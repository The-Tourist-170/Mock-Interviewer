package com.tourist.server.config;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "envFileProperties";
    private static final String ENV_FILE_PROPERTY = "env.file";
    private static final String DEFAULT_ENV_FILE = ".env";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, org.springframework.boot.SpringApplication application) {
        String envFilePath = environment.getProperty(ENV_FILE_PROPERTY, DEFAULT_ENV_FILE);
        File envFile = new File(envFilePath);
        if (!envFile.exists() || !envFile.isFile()) {
            return;
        }
        Map<String, Object> envVars = parseEnvFile(envFile);
        if (envVars.isEmpty()) {
            return;
        }
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, envVars));
    }

    private Map<String, Object> parseEnvFile(File envFile) {
        Map<String, Object> envVars = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(envFile.toPath())) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();
                if (value.length() >= 2 &&
                        ((value.startsWith("\"") && value.endsWith("\"")) ||
                         (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (StringUtils.hasText(key)) {
                    envVars.put(key, value);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read .env file: " + envFile.getAbsolutePath(), e);
        }
        return envVars;
    }
}
