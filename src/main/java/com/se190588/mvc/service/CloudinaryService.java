package com.se190588.mvc.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    // Upload anh tour len Cloudinary va tra ve secure URL de luu vao database.
    String uploadTourImage(MultipartFile imageFile);
}
