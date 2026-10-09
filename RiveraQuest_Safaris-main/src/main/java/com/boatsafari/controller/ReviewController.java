package com.boatsafari.controller;

import com.boatsafari.model.FeedbackStatus;
import com.boatsafari.model.Review;
import com.boatsafari.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {
  @Autowired
  private ReviewService reviewService;
  
  @PostMapping
  public ResponseEntity<Review> createReview(
      @RequestParam("userId") Long userId,
      @RequestParam("tripId") Long tripId,
      @RequestParam("rating") Integer rating,
      @RequestParam("comment") String comment) {
    return new ResponseEntity<>(reviewService.createReview(userId, tripId, rating, comment), HttpStatus.CREATED);
  }
  @GetMapping
  public ResponseEntity<List<Review>> getAllReviews(@RequestParam(value = "tripId", required = false) Long tripId) {
    if (tripId != null) {
      return ResponseEntity.ok(reviewService.getReviewsForTrip(tripId));
    }
    return ResponseEntity.ok(reviewService.getAllReviews());
  }
  @GetMapping("/search")
  public ResponseEntity<List<Review>> searchReviews(
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "rating", required = false) Integer rating,
      @RequestParam(value = "status", required = false) String status) {
    return ResponseEntity.ok(reviewService.searchReviews(keyword, rating, status));
  }
  @GetMapping("/trip/{tripId}/rating")
  public ResponseEntity<Double> getAverageRating(@PathVariable("tripId") Long tripId) {
    return ResponseEntity.ok(reviewService.getAverageRatingForTrip(tripId));
  }
  @GetMapping("/summary")
  public ResponseEntity<Map<String, Object>> getRatingSummary() {
    return ResponseEntity.ok(Map.of(
      "averageRating", reviewService.getOverallAverageRating(),
      "breakdown", reviewService.getRatingBreakdown(),
      "totalReviews", reviewService.getAllReviews().size()
    ));
  }
  @PutMapping("/{id}/status")
  public ResponseEntity<Review> updateStatus(@PathVariable("id") Long id, @RequestParam("status") FeedbackStatus status) {
    return ResponseEntity.ok(reviewService.updateStatus(id, status));
  }
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteReview(@PathVariable("id") Long id) {
    reviewService.deleteReview(id);
    return ResponseEntity.noContent().build();
  }
}
