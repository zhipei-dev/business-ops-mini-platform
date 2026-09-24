package dev.zhipei.businessops.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class Requests {
    private Requests() { }

    public record Product(
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 160) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
        BigDecimal unitPrice) { }

    public record Item(
        @NotNull @Positive Long productId,
        @Positive int quantity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
        BigDecimal unitPrice) { }

    public record PurchaseOrder(
        @NotBlank @Size(max = 160) String supplierName,
        @NotEmpty @Size(max = 100) List<@Valid Item> items) { }

    public record SalesOrder(
        @NotBlank @Size(max = 160) String customerName,
        @NotEmpty @Size(max = 100) List<@Valid Item> items) { }
}
