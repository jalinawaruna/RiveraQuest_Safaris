package com.boatsafari.controller;

import com.boatsafari.model.Promotion;
import com.boatsafari.model.Trip;
import com.boatsafari.repository.TripRepository;
import com.boatsafari.exception.ResourceNotFoundException;
import com.boatsafari.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin(origins = "*")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private TripRepository tripRepository;

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateVoucher(
            @RequestParam("code") String code, @RequestParam("tripId") Long tripId, @RequestParam("seatCount") int seatCount) {
        Promotion promo = promotionService.getPromotionByCode(code.trim());
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with ID: " + tripId));
        double total = trip.getPrice() * seatCount;
        double discount = promotionService.calculateDiscount(promo, trip, total);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", promo.getCode());
        result.put("discountAmount", discount);
        result.put("finalPrice", total - discount);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Promotion> createPromotion(@RequestParam(value = "marketingOfficerId", required = false) Long marketingOfficerId, @RequestBody Promotion promotion) {
        return new ResponseEntity<>(promotionService.createPromotion(marketingOfficerId, promotion), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Promotion>> getAllPromotions(HttpServletRequest request) {
        List<Promotion> all = promotionService.getAllPromotions();
        String role = request.getHeader("X-User-Role");
        boolean manager = role != null && ("ADMIN".equalsIgnoreCase(role.trim()) || "MARKETING_OFFICER".equalsIgnoreCase(role.trim()));
        if (!manager) {
            // Everyone else only sees vouchers that can currently be used
            all = all.stream()
                    .filter(p -> p.getComputedStatus() == com.boatsafari.model.VoucherStatus.ACTIVE)
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(all);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Promotion>> searchPromotions(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(promotionService.searchPromotions(keyword, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Promotion> getPromotionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Promotion> getPromotionByCode(@PathVariable("code") String code) {
        return ResponseEntity.ok(promotionService.getPromotionByCode(code));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Promotion> updatePromotion(@PathVariable("id") Long id, @RequestBody Promotion promotion) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, promotion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable("id") Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/trips")
    public ResponseEntity<Promotion> setApplicableTrips(@PathVariable("id") Long id, @RequestBody List<Long> tripIds) {
        return ResponseEntity.ok(promotionService.setApplicableTrips(id, tripIds));
    }

    @GetMapping("/{id}/trips")
    public ResponseEntity<List<Long>> getApplicableTrips(@PathVariable("id") Long id) {
        return ResponseEntity.ok(promotionService.getApplicableTripIds(id));
    }
}
