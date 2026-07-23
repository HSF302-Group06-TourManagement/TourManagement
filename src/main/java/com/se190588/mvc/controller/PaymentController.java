package com.se190588.mvc.controller;

import com.se190588.mvc.dto.CheckoutDto;
import com.se190588.mvc.dto.BookedTourDetailDto;
import com.se190588.mvc.dto.PaymentResultDto;
import com.se190588.mvc.dto.TourDto;
import com.se190588.mvc.service.PaymentService;
import com.se190588.mvc.service.TourService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.NoSuchElementException;

@Controller
public class PaymentController {

    @Autowired
    private TourService tourService;

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/tours/{id}/checkout")
    public String showCheckoutForm(@PathVariable Integer id, Model model) {
        model.addAttribute("tour", tourService.getTourById(id));
        model.addAttribute("checkout", new CheckoutDto());
        model.addAttribute("vnPayEnabled", paymentService.isVnPayEnabled());
        return "checkout";
    }

    @PostMapping("/tours/{id}/checkout")
    public String checkout(@PathVariable Integer id,
                           @Valid @ModelAttribute("checkout") CheckoutDto checkoutDto,
                           BindingResult bindingResult,
                           HttpServletRequest request,
                           Model model) {
        TourDto tour = tourService.getTourById(id);
        validateQuantityAgainstCapacity(checkoutDto, tour, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("tour", tour);
            model.addAttribute("vnPayEnabled", paymentService.isVnPayEnabled());
            return "checkout";
        }

        return "redirect:" + paymentService.createPaymentUrl(tour, checkoutDto, request);
    }

    @GetMapping("/payment/vnpay-return")
    public String vnPayReturn(HttpServletRequest request, Model model) {
        PaymentResultDto result = paymentService.verifyVnPayReturn(request.getParameterMap());
        model.addAttribute("result", result);
        return "payment-result";
    }

    @GetMapping("/payment/demo-success")
    public String demoSuccess(@RequestParam String txnRef, @RequestParam String amount, Model model) {
        model.addAttribute("result", paymentService.createDemoResult(txnRef, amount));
        return "payment-result";
    }

    @GetMapping("/payment/booked-tour/{txnRef}")
    public String bookedTourDetail(@PathVariable String txnRef, Model model) {
        BookedTourDetailDto detail = paymentService.getBookedTourDetail(txnRef);
        if (detail == null || !detail.isPaid()) {
            throw new NoSuchElementException("Booked tour detail not found");
        }

        model.addAttribute("booking", detail);
        return "booked-tour-detail";
    }

    @GetMapping("/payment/booked-tours")
    public String bookedTourList(Model model) {
        // Trang nay gom cac booking da thanh toan thanh cong de nguoi dung xem lai tu Tour List.
        model.addAttribute("bookings", paymentService.getPaidBookedTourDetails());
        return "booked-tour-list";
    }

    private void validateQuantityAgainstCapacity(CheckoutDto checkoutDto, TourDto tour, BindingResult bindingResult) {
        // Neu quantity da loi required/type/min thi khong validate tiep de tranh hien trung loi.
        if (bindingResult.hasFieldErrors("quantity") || checkoutDto.getQuantity() == null) {
            return;
        }

        if (tour.getCapacity() == null || tour.getCapacity() <= 0) {
            bindingResult.rejectValue("quantity", "checkout.quantity.soldOut",
                    "This tour is fully booked");
            return;
        }

        if (checkoutDto.getQuantity() > tour.getCapacity()) {
            bindingResult.rejectValue("quantity", "checkout.quantity.capacity",
                    "Quantity must be less than or equal to current capacity");
        }
    }
}
