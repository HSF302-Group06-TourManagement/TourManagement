package com.se190588.mvc.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TourDto {

    private Integer id;

    // Validate du lieu nguoi dung nhap tren form add/update.
    // DTO giup tach du lieu hien thi/nhap lieu ra khoi entity JPA.
    @NotBlank(message = "Name is required")
    @Size(max = 200, min = 1, message = "The name must be in 1...200 character")
    private String tourName;

    @NotBlank(message = "Destination is required")
    @Size(max = 200, min = 1, message = "The Destination must be in 1...200 character")
    private String destination;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "The Capacity must be in 1...1000")
    @Max(value = 1000, message = "The Capacity must be in 1...1000")
    private Integer capacity;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "The Duration must be in 1...300")
    @Max(value = 300, message = "The Duration must be in 1...300")
    private Integer duration;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = "dd/MM/yyyy") // Khop voi textbox Start Date tren form add/update.
    private LocalDate startDate;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.1", message = "The Price must be in 0.1...100000")
    @DecimalMax(value = "100000", message = "The Price must be in 1...100000")
    private Double price;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "AC|IN|DR", message = "Status must be in Active, Inactive, Draft")
    private String status;

    // Cot bo sung chi de hien thi y nghia cua status tren giao dien.
    // Database chi luu AC/IN/DR, con man hinh se show Active/Inactive/Draft.
    private String statusDesc;

    // Luu URL anh sau khi upload len Cloudinary; database khong luu file anh truc tiep.
    private String imageUrl;

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTourName() {
        return tourName;
    }

    public void setTourName(String tourName) {
        this.tourName = tourName;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
