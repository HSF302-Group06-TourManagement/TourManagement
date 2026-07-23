package com.se190588.mvc.service;

import com.se190588.mvc.dto.CheckoutDto;
import com.se190588.mvc.dto.PaymentResultDto;
import com.se190588.mvc.dto.TourDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentService {

    String createPaymentUrl(TourDto tour, CheckoutDto checkoutDto, HttpServletRequest request);

    PaymentResultDto verifyVnPayReturn(Map<String, String[]> parameterMap);

    PaymentResultDto createDemoResult(String transactionRef, String amount);

    boolean isVnPayEnabled();
}
