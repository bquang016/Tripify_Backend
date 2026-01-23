package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.service.AmenityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/amenity")
public class AmenityController {

    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    // GET ALL
    @GetMapping("/all")
    public List<Amenity> getAll() {
        return amenityService.findAll();
    }

    // ADD
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addAmenity(@RequestBody Amenity amenity) {
        try {
            Amenity saved = amenityService.addAmenity(amenity);
            return ResponseEntity.status(201).body(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error adding amenity: " + e.getMessage());
        }
    }

    // UPDATE
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAmenity(
            @PathVariable int id,
            @RequestBody Amenity amenity) {

        try {
            Amenity updated = amenityService.updateAmenity(id, amenity);
            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error updating amenity: " + e.getMessage());
        }
    }

    // GET BY TYPE
    @GetMapping("/type/{type}")
    public ResponseEntity<?> getByType(@PathVariable String type) {
        try {
            AmenityType amenityType;

            try {
                amenityType = AmenityType.valueOf(type.toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid amenity type: " + type);
            }

            return ResponseEntity.ok(amenityService.findByType(amenityType));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching amenity by type: " + e.getMessage());
        }
    }

    // DELETE (SOFT)
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAmenity(@PathVariable int id) {
        try {
            amenityService.deactivateAmenity(id);
            return ResponseEntity.ok("Amenity deactivated successfully");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error deactivating amenity: " + e.getMessage());
        }
    }
}
