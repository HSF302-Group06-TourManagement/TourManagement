package com.se190588.mvc.controller;

import com.se190588.mvc.dto.TourDto;
import com.se190588.mvc.service.TourService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

@Controller
@RequestMapping
public class TourController {

    @Autowired
    private TourService tourService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Cho phep bind startDate tu textbox dd/MM/yyyy tren form add/update.
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                    return;
                }

                String value = text.trim();
                try {
                    setValue(LocalDate.parse(value, dateFormatter));
                } catch (DateTimeParseException exception) {
                    throw new IllegalArgumentException("Start date must be in dd/MM/yyyy format");
                }
            }
        });
    }

    @GetMapping({"/", "/home", "/landing"})
    public String home(Model model) {
        model.addAttribute("tours", tourService.searchToursByDestination(null));
        return "landingPage";
    }


    @GetMapping("/tours")
    public String listTours(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
        // Neu co keyword thi tim theo Destination, neu khong co thi lay toan bo tour.
        model.addAttribute("tours", tourService.searchToursByDestination(keyword));
        // Gui keyword nguoc lai view de input search giu gia tri nguoi dung vua nhap.
        model.addAttribute("keyword", keyword);
        return "list";
    }

    @GetMapping("/tours/{id}")
    public String detailTour(@PathVariable Integer id, Model model) {
        // Lay chi tiet mot tour theo id tren URL, vi du /tours/1.
        model.addAttribute("tour", tourService.getTourById(id));
        return "detail";
    }

    @GetMapping("/tours/add")
    public String showAddForm(Model model) {
        // Tao DTO rong de Thymeleaf bind cac field tren form add.html.
        model.addAttribute("tour", new TourDto());
        model.addAttribute("statuses", tourService.getAllTourStatus());
        return "add";
    }

    @PostMapping("/tours/add")
    public String addTour(@Valid @ModelAttribute("tour") TourDto tourDto,
                          BindingResult bindingResult,
                          @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        validateStartDate(tourDto, bindingResult);

        // Neu validation loi, tra lai form cu kem danh sach status de nguoi dung sua.
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", tourService.getAllTourStatus());
            return "add";
        }

        tourService.createTour(tourDto, imageFile);
        // Flash attribute chi ton tai trong lan redirect ke tiep, phu hop de show thong bao thanh cong.
        redirectAttributes.addFlashAttribute("successMessage", "Add successfully");
        return "redirect:/tours";
    }

    @GetMapping("/tours/{id}/update")
    public String showUpdateForm(@PathVariable Integer id, Model model) {
        // Nap du lieu cu cua tour de hien thi len form update.html.
        model.addAttribute("tour", tourService.getTourById(id));
        model.addAttribute("statuses", tourService.getAllTourStatus());
        return "update";
    }

    @PostMapping("/tours/{id}/update")
    public String updateTour(@PathVariable Integer id,
                             @Valid @ModelAttribute("tour") TourDto tourDto,
                             BindingResult bindingResult,
                             @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        validateStartDate(tourDto, bindingResult);

        // Khi form update co loi, can gan lai id va statuses de trang render day du.
        if (bindingResult.hasErrors()) {
            tourDto.setId(id);
            model.addAttribute("statuses", tourService.getAllTourStatus());
            return "update";
        }

        tourService.updateTour(id, tourDto, imageFile);
        // Sau khi update thanh cong, quay ve list va hien thong bao theo yeu cau.
        redirectAttributes.addFlashAttribute("successMessage", "Update successfully");
        return "redirect:/tours";
    }

    @PostMapping("/tours/{id}/delete")
    public String deleteTour(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        // Xoa bang POST de tranh xoa du lieu chi bang viec truy cap link GET.
        tourService.deleteTour(id);
        redirectAttributes.addFlashAttribute("successMessage", "Delete successfully");
        return "redirect:/tours";
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException exception, Model model) {
        // Hien thi thong bao than thien neu nguoi dung truy cap id khong ton tai.
        model.addAttribute("errorMessage", exception.getMessage());
        return "detail";
    }

    private void validateStartDate(TourDto tourDto, BindingResult bindingResult) {
        // Neu startDate da bi loi required/format thi khong validate range nua de tranh hien trung nhieu loi.
        if (bindingResult.hasFieldErrors("startDate") || tourDto.getStartDate() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate maxStartDate = today.plusDays(360);

        // Theo de bai: Start Date phai o tuong lai, tuc la lon hon ngay hien tai.
        if (!tourDto.getStartDate().isAfter(today)) {
            bindingResult.rejectValue("startDate", "tour.startDate.future",
                    "Start Date must be in the future");
            return;
        }

        // Theo de bai: Start Date phai truoc current + 360 ngay.
        if (!tourDto.getStartDate().isBefore(maxStartDate)) {
            bindingResult.rejectValue("startDate", "tour.startDate.max",
                    "Start Date must be before current date + 360 days");
        }
    }
}
