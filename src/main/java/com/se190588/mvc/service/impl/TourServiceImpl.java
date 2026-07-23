package com.se190588.mvc.service.impl;

import com.se190588.mvc.dto.TourDto;
import com.se190588.mvc.dto.TourStatus;
import com.se190588.mvc.entity.Tour;
import com.se190588.mvc.repository.TourRepository;
import com.se190588.mvc.service.CloudinaryService;
import com.se190588.mvc.service.TourService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TourServiceImpl implements TourService {

    @Autowired
    private TourRepository tourRepo;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Override
    public List<TourDto> getAllTours() {
        // Repository tra ve entity da sort theo ten tour tang dan.
        // Sau do map sang DTO de bo sung statusDesc cho giao dien.
        return tourRepo.findAllByOrderByTourNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<TourDto> searchToursByDestination(String keyword) {
        // Neu nguoi dung khong nhap keyword, coi nhu xem toan bo danh sach.
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTours();
        }

        // trim de tranh viec khoang trang dau/cuoi lam sai ket qua tim kiem.
        return tourRepo.findByDestinationContainingIgnoreCaseOrderByTourNameAsc(keyword.trim()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public TourDto getTourById(Integer id) {
        // Neu khong tim thay id, nem exception ro rang de controller co the xu ly 404.
        Tour tour = tourRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tour not found with id: " + id));
        return toDto(tour);
    }

    @Override
    public TourDto createTour(TourDto tourDto, MultipartFile imageFile) {
        String imageUrl = cloudinaryService.uploadTourImage(imageFile);
        tourDto.setImageUrl(imageUrl);

        // Tao entity moi nen khong set id; database tu sinh id bang IDENTITY.
        Tour tour = toEntity(tourDto);
        tour.setId(null);
        return toDto(tourRepo.save(tour));
    }

    @Override
    public TourDto updateTour(Integer id, TourDto tourDto, MultipartFile imageFile) {
        // Kiem tra ban ghi ton tai truoc khi update de tranh save thanh record moi sai y nghia.
        Tour existingTour = tourRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tour not found with id: " + id));
        String newImageUrl = cloudinaryService.uploadTourImage(imageFile);

        existingTour.setTourName(tourDto.getTourName());
        existingTour.setDestination(tourDto.getDestination());
        existingTour.setCapacity(tourDto.getCapacity());
        existingTour.setDuration(tourDto.getDuration());
        existingTour.setStartDate(tourDto.getStartDate());
        existingTour.setPrice(tourDto.getPrice());
        existingTour.setStatus(tourDto.getStatus());
        if (newImageUrl != null) {
            // Neu nguoi dung chon anh moi thi thay URL moi, neu khong thi giu anh cu.
            existingTour.setImageUrl(newImageUrl);
        }

        return toDto(tourRepo.save(existingTour));
    }

    @Override
    public void deleteTour(Integer id) {
        // Goi getTourById de dung chung logic kiem tra ton tai va thong bao loi.
        getTourById(id);
        tourRepo.deleteById(id);
    }

    @Override
    public String getTourStatus(String status) {
        // Status dang hard-code vi database khong co bang status rieng.
        for (TourStatus x : getAllTourStatus()) {
            if (x.status().equals(status)) {
                return x.statusDesc();
            }
        }
        return "";
    }

    @Override
    public List<TourStatus> getAllTourStatus() {
        return List.of(
                new TourStatus("AC", "Active"),
                new TourStatus("IN", "Inactive"),
                new TourStatus("DR", "Draft"));
    }

    private TourDto toDto(Tour tour) {
        TourDto tourDto = new TourDto();

        tourDto.setId(tour.getId());
        tourDto.setTourName(tour.getTourName());
        tourDto.setDestination(tour.getDestination());
        tourDto.setCapacity(tour.getCapacity());
        tourDto.setDuration(tour.getDuration());
        tourDto.setStartDate(tour.getStartDate());
        tourDto.setPrice(tour.getPrice());
        tourDto.setStatus(tour.getStatus());
        tourDto.setStatusDesc(getTourStatus(tour.getStatus()));
        tourDto.setImageUrl(tour.getImageUrl());

        return tourDto;
    }

    private Tour toEntity(TourDto tourDto) {
        Tour tour = new Tour();

        tour.setId(tourDto.getId());
        tour.setTourName(tourDto.getTourName());
        tour.setDestination(tourDto.getDestination());
        tour.setCapacity(tourDto.getCapacity());
        tour.setDuration(tourDto.getDuration());
        tour.setStartDate(tourDto.getStartDate());
        tour.setPrice(tourDto.getPrice());
        tour.setStatus(tourDto.getStatus());
        tour.setImageUrl(tourDto.getImageUrl());

        return tour;
    }
}
