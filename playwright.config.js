const { defineConfig } = require('@playwright/test');
const host = process.env.E2E_HOST || '127.0.0.1';
const port = process.env.E2E_PORT || '8080';
const maven = process.platform === 'win32' ? 'mvn.cmd' : './mvnw';
module.exports = defineConfig({ testDir: './e2e', use: { baseURL: `http://${host}:${port}` }, webServer: { command: `${maven} spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1"`, url: `http://${host}:${port}/api/health`, reuseExistingServer: false, env: { ...process.env, SERVER_ADDRESS: host, SERVER_PORT: port } } });
