package com.jwtauth.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@Profile("!test")
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/jwtauthdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true}")
    private String defaultUrl;

    @Value("${spring.datasource.username:root}")
    private String defaultUsername;

    @Value("${spring.datasource.password:tiger}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setConnectionTimeout(30000);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        String mysqlUrl = System.getenv("MYSQL_URL");
        if (mysqlUrl == null || mysqlUrl.isBlank()) {
            mysqlUrl = System.getenv("DATABASE_URL");
        }
        if (mysqlUrl == null || mysqlUrl.isBlank()) {
            mysqlUrl = System.getenv("MYSQL_PUBLIC_URL");
        }

        String mysqlHost = System.getenv("MYSQLHOST");
        String mysqlPort = System.getenv("MYSQLPORT");
        String mysqlUser = System.getenv("MYSQLUSER");
        String mysqlPassword = System.getenv("MYSQLPASSWORD");
        String mysqlDatabase = System.getenv("MYSQLDATABASE");

        if (mysqlUrl != null && !mysqlUrl.isBlank()) {
            try {
                log.info("Configuring DataSource from Railway MYSQL_URL");
                URI uri = URI.create(mysqlUrl.replace("jdbc:", ""));
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 3306 : uri.getPort();
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "railway";

                String userInfo = uri.getUserInfo();
                String user = defaultUsername;
                String pass = defaultPassword;
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    user = parts[0];
                    pass = parts[1];
                }

                String jdbcUrl = String.format(
                        "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true",
                        host, port, dbName);

                config.setJdbcUrl(jdbcUrl);
                config.setUsername(user);
                config.setPassword(pass);
                return new HikariDataSource(config);
            } catch (Exception e) {
                log.warn("Failed to parse MYSQL_URL, fallback to host/properties: {}", e.getMessage());
            }
        }

        if (mysqlHost != null && !mysqlHost.isBlank()) {
            log.info("Configuring DataSource from Railway MYSQLHOST: {}", mysqlHost);
            int port = 3306;
            if (mysqlPort != null && !mysqlPort.isBlank()) {
                try {
                    port = Integer.parseInt(mysqlPort);
                } catch (NumberFormatException ignored) {}
            }
            String db = (mysqlDatabase != null && !mysqlDatabase.isBlank()) ? mysqlDatabase : "railway";
            String user = (mysqlUser != null && !mysqlUser.isBlank()) ? mysqlUser : defaultUsername;
            String pass = (mysqlPassword != null) ? mysqlPassword : defaultPassword;

            String jdbcUrl = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true",
                    mysqlHost, port, db);

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(pass);
            return new HikariDataSource(config);
        }

        log.info("Configuring default DataSource from application.properties");
        config.setJdbcUrl(defaultUrl);
        config.setUsername(defaultUsername);
        config.setPassword(defaultPassword);
        return new HikariDataSource(config);
    }
}
