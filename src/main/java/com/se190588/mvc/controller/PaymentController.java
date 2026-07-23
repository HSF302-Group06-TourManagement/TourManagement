package com.se190588.mvc.controller;

import com.se190588.mvc.dto.CheckoutDto;
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
}
