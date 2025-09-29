package com.techelevator;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VendingController {

    private final VendingService service;

    public VendingController(VendingService service) {
        this.service = service;
    }

    @GetMapping("/items")
    public List<InventoryFileProvider.ItemView> items() {
        return service.listItems();
    }

    @GetMapping("/balance")
    public String balance() {
        return service.getBalance().toPlainString();
    }

    @PostMapping("/feed")
    public String feed(@RequestParam BigDecimal amount) {
        return service.feed(amount).toPlainString();
    }

    @PostMapping("/select/{code}")
    public VendingService.PurchaseResult select(@PathVariable String code) {
        return service.select(code);
    }

    @PostMapping("/finish")
    public VendingService.Change finish() {
        return service.finish();
    }

    @PostMapping("/reset")
    public void reset() {
        service.reset();
    }
}
