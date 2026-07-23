package com.se190588.mvc.service.impl;

import com.se190588.mvc.dto.CheckoutDto;
import com.se190588.mvc.dto.BookedTourDetailDto;
import com.se190588.mvc.dto.PaymentResultDto;
import com.se190588.mvc.dto.TourDto;
import com.se190588.mvc.entity.Tour;
import com.se190588.mvc.repository.TourRepository;
import com.se190588.mvc.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VnPayPaymentService implements PaymentService {

    private static final Pattern TRANSACTION_REF_PATTERN = Pattern.compile("^TOUR(\\d+)Q(\\d+)T\\d+$");

    private final Set<String> processedTransactionRefs = ConcurrentHashMap.newKeySet();
    private final Map<String, BookedTourDetailDto> bookedTourDetails = new ConcurrentHashMap<>();

    @Autowired
    private TourRepository tourRepository;

    @Value("${payment.vnpay.enabled:false}")
    private boolean vnPayEnabled;

    @Value("${payment.vnpay.pay-url:}")
    private String payUrl;

    @Value("${payment.vnpay.return-url:}")
    private String returnUrl;

    @Value("${payment.vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${payment.vnpay.hash-secret:}")
    private String hashSecret;

    @Override
    public String createPaymentUrl(TourDto tour, CheckoutDto checkoutDto, HttpServletRequest request) {
        BigDecimal totalAmount = BigDecimal.valueOf(tour.getPrice())
                .multiply(BigDecimal.valueOf(checkoutDto.getQuantity()));
        String transactionRef = createTransactionRef(tour.getId(), checkoutDto.getQuantity());
        savePendingBookedTourDetail(transactionRef, tour, checkoutDto,
                totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString() + " VND");

        if (!isVnPayEnabled()) {
            return UriComponentsBuilder.fromPath("/payment/demo-success")
                    .queryParam("txnRef", transactionRef)
                    .queryParam("amount", totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString())
                    .toUriString();
        }

        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", totalAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_OrderInfo", "Payment for tour " + tour.getTourName());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", resolveReturnUrl(request));
        params.put("vnp_IpAddr", getClientIp(request));
        params.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        params.put("vnp_ExpireDate", LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        String query = buildQuery(params, true);
        String secureHash = hmacSha512(hashSecret, query);

        return payUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    public PaymentResultDto verifyVnPayReturn(Map<String, String[]> parameterMap) {
        TreeMap<String, String> params = new TreeMap<>();
        parameterMap.forEach((key, values) -> {
            if (values != null && values.length > 0 && values[0] != null && !values[0].isBlank()
                    && !"vnp_SecureHash".equals(key) && !"vnp_SecureHashType".equals(key)) {
                params.put(key, values[0]);
            }
        });

        String providedHash = firstValue(parameterMap.get("vnp_SecureHash"));
        String calculatedHash = hmacSha512(hashSecret, buildQuery(params, true));
        boolean validSignature = providedHash != null && providedHash.equalsIgnoreCase(calculatedHash);
        String responseCode = params.get("vnp_ResponseCode");

        PaymentResultDto result = new PaymentResultDto();
        result.setValidSignature(validSignature);
        result.setSuccess(validSignature && "00".equals(responseCode));
        result.setTransactionRef(params.get("vnp_TxnRef"));
        result.setResponseCode(responseCode);
        result.setAmount(formatVnPayAmount(params.get("vnp_Amount")));
        parseBookingInfo(result.getTransactionRef()).ifPresent(info -> result.setTourId(info.tourId()));
        result.setMessage(result.isSuccess() ? "Payment successfully" : "Payment failed or invalid signature");
        if (result.isSuccess()) {
            markBookingPaid(result);
            decreaseTourCapacity(result.getTransactionRef());
        }
        return result;
    }

    @Override
    public PaymentResultDto createDemoResult(String transactionRef, String amount) {
        PaymentResultDto result = new PaymentResultDto();
        result.setSuccess(true);
        result.setValidSignature(true);
        result.setTransactionRef(transactionRef);
        result.setResponseCode("DEMO");
        result.setAmount(amount + " VND");
        parseBookingInfo(transactionRef).ifPresent(info -> result.setTourId(info.tourId()));
        result.setMessage("Payment successfully in demo mode");
        markBookingPaid(result);
        decreaseTourCapacity(transactionRef);
        return result;
    }

    @Override
    public BookedTourDetailDto getBookedTourDetail(String transactionRef) {
        return bookedTourDetails.get(transactionRef);
    }

    @Override
    public List<BookedTourDetailDto> getPaidBookedTourDetails() {
        // Chi hien thi cac booking da thanh toan thanh cong, khong hien cac giao dich dang pending.
        return bookedTourDetails.values().stream()
                .filter(BookedTourDetailDto::isPaid)
                .toList();
    }

    @Override
    public boolean isVnPayEnabled() {
        return vnPayEnabled && !payUrl.isBlank() && !tmnCode.isBlank() && !hashSecret.isBlank();
    }

    private void savePendingBookedTourDetail(String transactionRef, TourDto tour, CheckoutDto checkoutDto, String amount) {
        BookedTourDetailDto detail = new BookedTourDetailDto();
        detail.setTransactionRef(transactionRef);
        detail.setCustomerName(checkoutDto.getCustomerName());
        detail.setCustomerEmail(checkoutDto.getCustomerEmail());
        detail.setCustomerPhone(checkoutDto.getCustomerPhone());
        detail.setQuantity(checkoutDto.getQuantity());
        detail.setAmount(amount);
        detail.setPaid(false);
        detail.setTour(tour);
        bookedTourDetails.put(transactionRef, detail);
    }

    private void markBookingPaid(PaymentResultDto result) {
        BookedTourDetailDto detail = bookedTourDetails.get(result.getTransactionRef());
        if (detail != null) {
            detail.setAmount(result.getAmount());
            detail.setPaid(true);
        }
    }

    private String createTransactionRef(Integer tourId, Integer quantity) {
        // Ma giao dich luu tourId va quantity de khi thanh toan thanh cong co the tru capacity tuong ung.
        return "TOUR" + tourId + "Q" + quantity + "T" + System.currentTimeMillis();
    }

    private void decreaseTourCapacity(String transactionRef) {
        Optional<BookingInfo> bookingInfo = parseBookingInfo(transactionRef);
        if (bookingInfo.isEmpty() || !processedTransactionRefs.add(transactionRef)) {
            return;
        }

        tourRepository.findById(bookingInfo.get().tourId()).ifPresent(tour -> {
            Integer currentCapacity = tour.getCapacity();
            Integer bookedQuantity = bookingInfo.get().quantity();
            if (currentCapacity != null && bookedQuantity != null && currentCapacity >= bookedQuantity) {
                tour.setCapacity(currentCapacity - bookedQuantity);
                tourRepository.save(tour);
                updateBookedTourCapacity(transactionRef, tour);
            }
        });
    }

    private void updateBookedTourCapacity(String transactionRef, Tour tour) {
        BookedTourDetailDto detail = bookedTourDetails.get(transactionRef);
        if (detail == null || detail.getTour() == null) {
            return;
        }
        detail.getTour().setCapacity(tour.getCapacity());
    }

    private Optional<BookingInfo> parseBookingInfo(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = TRANSACTION_REF_PATTERN.matcher(transactionRef);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new BookingInfo(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveReturnUrl(HttpServletRequest request) {
        // Neu cau hinh VNPAY_RETURN_URL thi uu tien dung cau hinh do.
        if (returnUrl != null && !returnUrl.isBlank()) {
            return returnUrl;
        }

        // Khi chay qua ngrok/proxy, cac header nay giup lay dung public URL.
        String protocol = firstHeaderValue(request, "X-Forwarded-Proto", request.getScheme());
        String host = firstHeaderValue(request, "X-Forwarded-Host", request.getHeader("Host"));

        if (host == null || host.isBlank()) {
            int port = request.getServerPort();
            boolean defaultPort = ("http".equals(protocol) && port == 80) || ("https".equals(protocol) && port == 443);
            host = request.getServerName() + (defaultPort ? "" : ":" + port);
        }

        return protocol + "://" + host + request.getContextPath() + "/payment/vnpay-return";
    }

    private String firstHeaderValue(HttpServletRequest request, String headerName, String fallback) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.split(",")[0].trim();
    }

    private String firstValue(String[] values) {
        return values == null || values.length == 0 ? null : values[0];
    }

    private String buildQuery(TreeMap<String, String> params, boolean encoded) {
        StringBuilder query = new StringBuilder();
        params.forEach((key, value) -> {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(encode(key));
            query.append('=');
            query.append(encoded ? encode(value) : value);
        });
        return query.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String formatVnPayAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return "";
        }

        try {
            BigDecimal amount = new BigDecimal(rawAmount)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return amount.toPlainString() + " VND";
        } catch (NumberFormatException exception) {
            return rawAmount;
        }
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create payment signature", exception);
        }
    }

    private record BookingInfo(Integer tourId, Integer quantity) {
    }
}
