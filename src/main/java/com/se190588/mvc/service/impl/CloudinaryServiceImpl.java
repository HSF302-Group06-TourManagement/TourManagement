package com.se190588.mvc.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.se190588.mvc.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public String uploadTourImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        try {
            // Cloudinary tra ve Map ket qua, secure_url la URL HTTPS dung de hien thi anh tren web.
            Map<?, ?> uploadResult = cloudinary.uploader().upload(imageFile.getBytes(), ObjectUtils.asMap(
                    "folder", "tour-management/tours",
                    "resource_type", "image"));
            return String.valueOf(uploadResult.get("secure_url"));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot upload tour image to Cloudinary", exception);
        }
    }
}
