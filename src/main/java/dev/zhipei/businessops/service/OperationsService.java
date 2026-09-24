package dev.zhipei.businessops.service;

import dev.zhipei.businessops.api.ApiException;
import dev.zhipei.businessops.api.Requests;
import dev.zhipei.businessops.domain.Workflow;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsService {
    private final JdbcTemplate jdbc;

    public OperationsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> products() {
        return jdbc.queryForList(
            "SELECT id AS \"id\", sku AS \"sku\", name AS \"name\", "
                + "unit_price AS \"unitPrice\" FROM products ORDER BY id DESC");
    }

    public List<Map<String, Object>> inventory() {
        return jdbc.queryForList(
            """
            SELECT p.id AS "productId",
                   p.sku AS "sku",
                   p.name AS "name",
                   COALESCE(SUM(m.quantity_delta), 0) AS "onHand"
            FROM products p
            LEFT JOIN inventory_movements m ON m.product_id = p.id
            GROUP BY p.id, p.sku, p.name
            ORDER BY p.id
            """);
    }

    public List<Map<String, Object>> audit() {
        return jdbc.queryForList(
            """
            SELECT id AS "id",
                   entity_type AS "entityType",
                   entity_id AS "entityId",
                   action AS "action",
                   created_at AS "createdAt"
            FROM audit_events
            ORDER BY id DESC
            """);
    }

    public List<Map<String, Object>> purchaseOrders() {
        return orders(
            "purchase_orders",
            "purchase_order_items",
            "purchase_order_id",
            "supplier_name",
            "supplierName");
    }

    public List<Map<String, Object>> salesOrders() {
        return orders(
            "sales_orders",
            "sales_order_items",
            "sales_order_id",
            "customer_name",
            "customerName");
    }

    @Transactional
    public long createProduct(Requests.Product request) {
        return insert(
            "products",
            Map.of(
                "sku", request.sku().trim(),
                "name", request.name().trim(),
                "unit_price", request.unitPrice(),
                "created_at", Timestamp.from(Instant.now())));
    }

    @Transactional
    public long createPurchase(Requests.PurchaseOrder request) {
        return createOrder(
            "purchase_orders",
            "purchase_order_items",
            "supplier_name",
            request.supplierName(),
            request.items());
    }

    @Transactional
    public long createSales(Requests.SalesOrder request) {
        return createOrder(
            "sales_orders",
            "sales_order_items",
            "customer_name",
            request.customerName(),
            request.items());
    }

    @Transactional
    public void purchaseTransition(long id, String action) {
        String status = requiredStatus("purchase_orders", id);
        String next = Workflow.purchaseNext(status, action);

        jdbc.update("UPDATE purchase_orders SET status=? WHERE id=?", next, id);

        if ("RECEIVE".equals(action)) {
            List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT product_id, quantity FROM purchase_order_items WHERE purchase_order_id=?",
                id);

            for (Map<String, Object> item : items) {
                movement(
                    ((Number) item.get("product_id")).longValue(),
                    ((Number) item.get("quantity")).intValue(),
                    "PURCHASE_RECEIPT",
                    id);
            }
        }

        audit("PURCHASE_ORDER", id, action);
    }

    @Transactional
    public void salesTransition(long id, String action) {
        String status = requiredStatus("sales_orders", id);
        String next = Workflow.salesNext(status, action);

        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT product_id, quantity FROM sales_order_items WHERE sales_order_id=?",
            id);

        if ("FULFILL".equals(action)) {
            Map<Long, Long> requiredByProduct = new LinkedHashMap<>();
            for (Map<String, Object> item : items) {
                long productId = ((Number) item.get("product_id")).longValue();
                long quantity = ((Number) item.get("quantity")).longValue();
                requiredByProduct.merge(productId, quantity, Long::sum);
            }

            for (Map.Entry<Long, Long> entry : requiredByProduct.entrySet()) {
                long required = entry.getValue();
                if (required > Integer.MAX_VALUE) {
                    throw new ApiException(400, "Requested quantity is too large");
                }

                Long onHand = jdbc.queryForObject(
                    """
                    SELECT COALESCE(SUM(quantity_delta), 0)
                    FROM inventory_movements
                    WHERE product_id=?
                    """,
                    Long.class,
                    entry.getKey());

                if (onHand == null || onHand < required) {
                    throw new ApiException(409, "Insufficient inventory");
                }
            }

            for (Map.Entry<Long, Long> entry : requiredByProduct.entrySet()) {
                movement(
                    entry.getKey(),
                    -Math.toIntExact(entry.getValue()),
                    "SALES_FULFILLMENT",
                    id);
            }
        }

        jdbc.update("UPDATE sales_orders SET status=? WHERE id=?", next, id);
        audit("SALES_ORDER", id, action);
    }

    private long createOrder(
        String table,
        String itemTable,
        String partyColumn,
        String party,
        List<Requests.Item> items) {

        for (Requests.Item item : items) {
            existsProduct(item.productId());
        }

        long orderId = insert(
            table,
            Map.of(
                partyColumn, party.trim(),
                "status", "DRAFT",
                "created_at", Timestamp.from(Instant.now())));

        String foreignKey =
            table.equals("purchase_orders") ? "purchase_order_id" : "sales_order_id";

        for (Requests.Item item : items) {
            jdbc.update(
                "INSERT INTO " + itemTable
                    + " (" + foreignKey + ",product_id,quantity,unit_price) VALUES (?,?,?,?)",
                orderId,
                item.productId(),
                item.quantity(),
                item.unitPrice());
        }

        audit(
            table.equals("purchase_orders") ? "PURCHASE_ORDER" : "SALES_ORDER",
            orderId,
            "CREATE");

        return orderId;
    }

    private List<Map<String, Object>> orders(
        String orderTable,
        String itemTable,
        String foreignKey,
        String partyColumn,
        String alias) {

        return jdbc.queryForList(
            "SELECT o.id AS \"id\","
                + "o." + partyColumn + " AS \"" + alias + "\","
                + "o.status AS \"status\","
                + "o.created_at AS \"createdAt\","
                + "COALESCE(SUM(i.quantity),0) AS \"totalQuantity\" "
                + "FROM " + orderTable + " o "
                + "LEFT JOIN " + itemTable + " i ON i." + foreignKey + "=o.id "
                + "GROUP BY o.id,o." + partyColumn + ",o.status,o.created_at "
                + "ORDER BY o.id DESC");
    }

    private String requiredStatus(String table, long id) {
        List<String> states = jdbc.query(
            "SELECT status FROM " + table + " WHERE id=?",
            (rs, row) -> rs.getString(1),
            id);

        if (states.isEmpty()) {
            throw new ApiException(404, "Resource not found");
        }

        return states.getFirst();
    }

    private void existsProduct(long id) {
        if (jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE id=?",
                Integer.class,
                id) != 1) {
            throw new ApiException(404, "Product not found");
        }
    }

    private void movement(long productId, int delta, String referenceType, long referenceId) {
        jdbc.update(
            """
            INSERT INTO inventory_movements(
                product_id, quantity_delta, reference_type, reference_id, created_at
            ) VALUES(?,?,?,?,?)
            """,
            productId,
            delta,
            referenceType,
            referenceId,
            Timestamp.from(Instant.now()));
    }

    private void audit(String entityType, long entityId, String action) {
        jdbc.update(
            """
            INSERT INTO audit_events(entity_type,entity_id,action,created_at)
            VALUES(?,?,?,?)
            """,
            entityType,
            entityId,
            action,
            Timestamp.from(Instant.now()));
    }

    private long insert(String table, Map<String, Object> values) {
        return new SimpleJdbcInsert(jdbc)
            .withTableName(table)
            .usingGeneratedKeyColumns("id")
            .executeAndReturnKey(values)
            .longValue();
    }
}
