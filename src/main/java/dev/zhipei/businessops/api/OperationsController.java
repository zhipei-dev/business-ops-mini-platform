package dev.zhipei.businessops.api;

import dev.zhipei.businessops.service.OperationsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OperationsController {
    private final OperationsService service;
    public OperationsController(OperationsService service) { this.service = service; }
    @GetMapping("/health") Map<String, String> health() { return Map.of("status", "ok", "mode", "synthetic-demo"); }
    @GetMapping("/products") List<Map<String, Object>> products() { return service.products(); }
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED) Map<String, Long> product(@Valid @RequestBody Requests.Product body) { return Map.of("id", service.createProduct(body)); }
    @GetMapping("/purchase-orders") List<Map<String, Object>> purchases() { return service.purchaseOrders(); }
    @PostMapping("/purchase-orders") @ResponseStatus(HttpStatus.CREATED) Map<String, Long> purchase(@Valid @RequestBody Requests.PurchaseOrder body) { return Map.of("id", service.createPurchase(body)); }
    @PostMapping("/purchase-orders/{id}/submit") @ResponseStatus(HttpStatus.NO_CONTENT) void submit(@PathVariable @Positive long id) { service.purchaseTransition(id, "SUBMIT"); }
    @PostMapping("/purchase-orders/{id}/approve") @ResponseStatus(HttpStatus.NO_CONTENT) void approve(@PathVariable @Positive long id) { service.purchaseTransition(id, "APPROVE"); }
    @PostMapping("/purchase-orders/{id}/receive") @ResponseStatus(HttpStatus.NO_CONTENT) void receive(@PathVariable @Positive long id) { service.purchaseTransition(id, "RECEIVE"); }
    @GetMapping("/sales-orders") List<Map<String, Object>> sales() { return service.salesOrders(); }
    @PostMapping("/sales-orders") @ResponseStatus(HttpStatus.CREATED) Map<String, Long> sales(@Valid @RequestBody Requests.SalesOrder body) { return Map.of("id", service.createSales(body)); }
    @PostMapping("/sales-orders/{id}/confirm") @ResponseStatus(HttpStatus.NO_CONTENT) void confirm(@PathVariable @Positive long id) { service.salesTransition(id, "CONFIRM"); }
    @PostMapping("/sales-orders/{id}/fulfill") @ResponseStatus(HttpStatus.NO_CONTENT) void fulfill(@PathVariable @Positive long id) { service.salesTransition(id, "FULFILL"); }
    @GetMapping("/inventory") List<Map<String, Object>> inventory() { return service.inventory(); }
    @GetMapping("/audit") List<Map<String, Object>> audit() { return service.audit(); }
}
