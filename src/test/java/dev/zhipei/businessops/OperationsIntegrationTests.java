package dev.zhipei.businessops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.zhipei.businessops.api.ApiException;
import dev.zhipei.businessops.api.Requests;
import dev.zhipei.businessops.service.OperationsService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OperationsIntegrationTests {
    @Autowired OperationsService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM audit_events");
        jdbc.update("DELETE FROM inventory_movements");
        jdbc.update("DELETE FROM purchase_order_items");
        jdbc.update("DELETE FROM sales_order_items");
        jdbc.update("DELETE FROM purchase_orders");
        jdbc.update("DELETE FROM sales_orders");
        jdbc.update("DELETE FROM products");
    }

    private long product() {
        return service.createProduct(
            new Requests.Product(
                "SKU-" + System.nanoTime(),
                "Demo product",
                new BigDecimal("12.50")));
    }

    private Requests.Item item(long product, int quantity) {
        return new Requests.Item(product, quantity, new BigDecimal("12.50"));
    }

    private long received(long product, int quantity) {
        long purchaseOrder = service.createPurchase(
            new Requests.PurchaseOrder("Supplier", List.of(item(product, quantity))));
        service.purchaseTransition(purchaseOrder, "SUBMIT");
        service.purchaseTransition(purchaseOrder, "APPROVE");
        service.purchaseTransition(purchaseOrder, "RECEIVE");
        return purchaseOrder;
    }

    @Test
    void freshMigrationAndPurchaseReceiptCreatesLedgerBalance() {
        long product = product();
        received(product, 10);

        assertEquals(
            10,
            ((Number) service.inventory().getFirst().get("onHand")).intValue());
        assertTrue(
            service.audit().stream()
                .anyMatch(event -> "RECEIVE".equals(event.get("action"))));
    }

    @Test
    void duplicateReceiveIsRejected() {
        long product = product();
        long purchaseOrder = received(product, 10);

        assertThrows(
            ApiException.class,
            () -> service.purchaseTransition(purchaseOrder, "RECEIVE"));
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements",
                Integer.class));
    }

    @Test
    void fulfillmentDecreasesLedgerDerivedBalanceAndCannotRepeat() {
        long product = product();
        received(product, 10);

        long salesOrder = service.createSales(
            new Requests.SalesOrder(
                "Customer",
                List.of(item(product, 4))));
        service.salesTransition(salesOrder, "CONFIRM");
        service.salesTransition(salesOrder, "FULFILL");

        assertEquals(
            6,
            ((Number) service.inventory().getFirst().get("onHand")).intValue());
        assertThrows(
            ApiException.class,
            () -> service.salesTransition(salesOrder, "FULFILL"));
    }

    @Test
    void insufficientStockRollsBackSuccessWrites() {
        long product = product();
        received(product, 3);

        long salesOrder = service.createSales(
            new Requests.SalesOrder(
                "Customer",
                List.of(item(product, 4))));
        service.salesTransition(salesOrder, "CONFIRM");

        int auditsBefore = jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_events",
            Integer.class);
        int movementsBefore = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inventory_movements",
            Integer.class);

        assertThrows(
            ApiException.class,
            () -> service.salesTransition(salesOrder, "FULFILL"));

        assertEquals(
            3,
            ((Number) service.inventory().getFirst().get("onHand")).intValue());
        assertEquals(
            auditsBefore,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events",
                Integer.class));
        assertEquals(
            movementsBefore,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements",
                Integer.class));
    }

    @Test
    void duplicateProductLinesCannotOversell() {
        long product = product();
        received(product, 5);

        long salesOrder = service.createSales(
            new Requests.SalesOrder(
                "Customer",
                List.of(
                    item(product, 3),
                    item(product, 3))));
        service.salesTransition(salesOrder, "CONFIRM");

        int movementsBefore = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inventory_movements",
            Integer.class);
        int auditsBefore = jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_events",
            Integer.class);

        assertThrows(
            ApiException.class,
            () -> service.salesTransition(salesOrder, "FULFILL"));

        assertEquals(
            5,
            ((Number) service.inventory().getFirst().get("onHand")).intValue());
        assertEquals(
            movementsBefore,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements",
                Integer.class));
        assertEquals(
            auditsBefore,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events",
                Integer.class));
    }

    @Test
    void apiListJsonUsesStableCamelCaseFieldNames() throws Exception {
        long product = service.createProduct(
            new Requests.Product(
                "CONTRACT-SKU",
                "Contract product",
                new BigDecimal("12.50")));
        received(product, 2);
        service.createSales(
            new Requests.SalesOrder(
                "Contract customer",
                List.of(item(product, 1))));

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].sku").value("CONTRACT-SKU"))
            .andExpect(jsonPath("$[0].name").value("Contract product"))
            .andExpect(jsonPath("$[0].unitPrice").value(12.50));

        mockMvc.perform(get("/api/purchase-orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].supplierName").value("Supplier"))
            .andExpect(jsonPath("$[0].status").value("RECEIVED"))
            .andExpect(jsonPath("$[0].totalQuantity").value(2));

        mockMvc.perform(get("/api/sales-orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].customerName").value("Contract customer"))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
            .andExpect(jsonPath("$[0].totalQuantity").value(1));

        mockMvc.perform(get("/api/inventory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].productId").value(product))
            .andExpect(jsonPath("$[0].sku").value("CONTRACT-SKU"))
            .andExpect(jsonPath("$[0].name").value("Contract product"))
            .andExpect(jsonPath("$[0].onHand").value(2));

        mockMvc.perform(get("/api/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].entityType").exists())
            .andExpect(jsonPath("$[0].entityId").exists())
            .andExpect(jsonPath("$[0].action").exists())
            .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void httpValidationAndWorkflowErrorsUseClientStatusCodes() throws Exception {
        mockMvc.perform(
                post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"sku":"","name":"","unitPrice":10}
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid request"));

        mockMvc.perform(
                post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{bad"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid request"));

        long product = product();
        long purchaseOrder = service.createPurchase(
            new Requests.PurchaseOrder(
                "Supplier",
                List.of(item(product, 1))));

        mockMvc.perform(
                post("/api/purchase-orders/{id}/approve", purchaseOrder))
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.error")
                    .value("Workflow transition is not allowed"));

        mockMvc.perform(
                post("/api/purchase-orders/{id}/submit", 0))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    void oversizedJsonIsRejectedBeforeControllerBinding() throws Exception {
        String payload = """
            {"sku":"%s","name":"Large","unitPrice":10}
            """.formatted("x".repeat(70_000));

        mockMvc.perform(
                post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(
                jsonPath("$.error")
                    .value("Request body too large"));
    }
}
