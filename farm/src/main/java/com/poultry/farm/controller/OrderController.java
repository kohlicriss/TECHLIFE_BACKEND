package com.poultry.farm.controller;

import com.poultry.farm.dto.AdminDeliveryRequest;
import com.poultry.farm.dto.AdminStatusUpdateRequest;
import com.poultry.farm.dto.VendorOrderRequest;
import com.poultry.farm.entity.Batch;
import com.poultry.farm.entity.Order;
import com.poultry.farm.entity.OrderStatus;
import com.poultry.farm.service.BatchService;
import com.poultry.farm.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farm")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private BatchService batchService;

    // GET: Vendor views available batches
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR')")
    @GetMapping("/orders/vendor/available-batches")
    public ResponseEntity<List<Batch>> getAvailableBatches() {
        List<Batch> batches = batchService.getAvailableBatches();
        return ResponseEntity.ok(batches);
    }

    // POST: Vendor places order (creates PENDING order)
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR')")
    @PostMapping("/orders/vendor/place-order")
    public ResponseEntity<Order> placeVendorOrder(@RequestBody VendorOrderRequest request) {
        Order order = orderService.placeVendorOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // GET: Admin views all pending orders
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR')")
    @GetMapping("/orders/admin/allOrders")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getOrders();
        return ResponseEntity.ok(orders);
    }

    // POST: Admin approves/rejects order
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR')")
    @PostMapping("/orders/admin/update-status/{orderId}")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody AdminStatusUpdateRequest request) {

        Order order = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(order);
    }

    // POST: Admin adds delivery details (when dispatching)
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR')")
    @PostMapping("/orders/admin/add-delivery/{orderId}")
    public ResponseEntity<Order> addDeliveryDetails(
            @PathVariable Long orderId,
            @RequestBody AdminDeliveryRequest request) {

        Order order = orderService.addDeliveryDetails(orderId, request);
        return ResponseEntity.ok(order);
    }

    // POST: Admin marks order as delivered
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR')")
    @PostMapping("/orders/admin/mark-delivered/{orderId}")
    public ResponseEntity<Order> markAsDelivered(@PathVariable Long orderId) {
        Order order = orderService.markAsDelivered(orderId);
        return ResponseEntity.ok(order);
    }

    // GET: Get order by ID
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR')")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId) {
        Order order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    // GET: Vendor views their orders
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR')")
    @GetMapping("/orders/vendor/my-orders/{phoneNumber}")
    public ResponseEntity<List<Order>> getVendorOrders(@PathVariable String phoneNumber) {
        List<Order> orders = orderService.getOrdersByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(orders);
    }
}