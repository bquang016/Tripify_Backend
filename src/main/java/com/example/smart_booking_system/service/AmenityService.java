package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.repository.AmenityRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AmenityService {

    private final AmenityRepository amenityRepository;

    public AmenityService(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    // ADD
    public Amenity addAmenity(Amenity amenity) {

        if (amenityRepository.existsByNameAndType(
                amenity.getAmenityName(),
                amenity.getAmenityType()
        )) {
            throw new IllegalArgumentException("Amenity name already exists for this type");
        }

        return amenityRepository.save(amenity);
    }

    // GET ALL ACTIVE
    public List<Amenity> findAll() {
        return amenityRepository.findAllActive();
    }

    // GET BY TYPE
    public List<Amenity> findByType(AmenityType type) {
        return amenityRepository.findByType(type);
    }

    // UPDATE
    public Amenity updateAmenity(int id, Amenity updatedAmenity) {

        Amenity existing = amenityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + id));

        // Lấy giá trị mới hoặc giữ nguyên
        String newName = updatedAmenity.getAmenityName() != null && !updatedAmenity.getAmenityName().trim().isEmpty()
                ? updatedAmenity.getAmenityName()
                : existing.getAmenityName();

        AmenityType newType = updatedAmenity.getAmenityType() != null
                ? updatedAmenity.getAmenityType()
                : existing.getAmenityType();

        // Validate type
        if (updatedAmenity.getAmenityType() != null) {
            boolean validType = Arrays.stream(AmenityType.values())
                    .anyMatch(t -> t.equals(updatedAmenity.getAmenityType()));
            if (!validType) {
                throw new IllegalArgumentException("Invalid amenity type: " + updatedAmenity.getAmenityType());
            }
        }

        // Check trùng bản ghi khác
        boolean exists = amenityRepository.existsByNameTypeExcept(newName, newType, id);
        if (exists) {
            throw new IllegalArgumentException("Amenity name already exists for this type");
        }

        // Update
        existing.setAmenityName(newName);
        existing.setAmenityType(newType);
        existing.setActive(updatedAmenity.isActive());

        return amenityRepository.save(existing);
    }

    // DELETE (SOFT)
    public void deactivateAmenity(int id) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + id));

        if (!amenity.isActive()) {
            throw new IllegalArgumentException("Amenity is already inactive");
        }

        amenity.setActive(false);
        amenityRepository.save(amenity);
    }
}
