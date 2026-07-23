package com.se190588.mvc.service;

import com.se190588.mvc.dto.TourDto;
import com.se190588.mvc.dto.TourStatus;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface TourService {

    // Lay danh sach tour de show tren trang list.html.
    // Service tra ve DTO de controller/template khong phu thuoc truc tiep vao entity.
    List<TourDto> getAllTours();

    // Tim tour theo Destination, dung cho thanh search tren trang list.html.
    List<TourDto> searchToursByDestination(String keyword);

    // Lay chi tiet mot tour theo id, dung cho detail.html va form update.html.
    TourDto getTourById(Integer id);

    // Tao moi tour tu du lieu nguoi dung nhap o add.html.
    TourDto createTour(TourDto tourDto, MultipartFile imageFile);

    // Cap nhat tour dang co. Id lay tu URL de dam bao update dung ban ghi.
    TourDto updateTour(Integer id, TourDto tourDto, MultipartFile imageFile);

    // Xoa tour theo id.
    void deleteTour(Integer id);

    // Dua vao AC/IN/DR va tra ve Active/Inactive/Draft de hien thi.
    String getTourStatus(String status);

    // Lay toan bo status hard-code de render dropdown tren form add/update.
    List<TourStatus> getAllTourStatus();
}
