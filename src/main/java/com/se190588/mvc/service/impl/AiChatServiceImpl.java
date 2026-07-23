package com.se190588.mvc.service.impl;

import com.se190588.mvc.dto.AiChatDto;
import com.se190588.mvc.entity.Tour;
import com.se190588.mvc.repository.TourRepository;
import com.se190588.mvc.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private Environment environment;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public AiChatDto processUserMessage(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return new AiChatDto("Xin chào! Tôi là Trợ lý AI du lịch của TourVerse. Bạn cần tìm thông tin tour nào?",
                    getDefaultSuggestions());
        }

        List<Tour> allTours = tourRepository.findAllByOrderByTourNameAsc();
        String geminiApiKey = getGeminiApiKey();

        // Nếu có Gemini API key thì gọi Gemini; nếu lỗi thì fallback sang dữ liệu tour trong database.
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
            try {
                String aiReply = callGeminiApi(userMessage, allTours, geminiApiKey);
                if (aiReply != null && !aiReply.trim().isEmpty()) {
                    return new AiChatDto(aiReply, generateDynamicSuggestions(allTours));
                }
            } catch (Exception exception) {
                // Không chặn người dùng khi Gemini lỗi; hệ thống vẫn trả lời bằng dữ liệu nội bộ.
            }
        }

        String fallbackReply = generateSmartResponseFromDb(userMessage, allTours);
        return new AiChatDto(fallbackReply, generateDynamicSuggestions(allTours));
    }

    private String callGeminiApi(String userMessage, List<Tour> tours, String geminiApiKey) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";
        String dbContext = buildDbContextPrompt(tours);

        String systemPrompt = "Bạn là AI Trợ lý Tư vấn Du lịch của website TourVerse. "
                + "Dưới đây là dữ liệu thực tế các tour du lịch đang có trong database:\n\n"
                + dbContext + "\n\n"
                + "Quy tắc phản hồi:\n"
                + "1. Chỉ tư vấn dựa trên thông tin tour có trong database nêu trên.\n"
                + "2. Trả lời bằng tiếng Việt thân thiện, lịch sự, có định dạng Markdown dễ đọc.\n"
                + "3. Khi giới thiệu tour, hãy nêu điểm đến, giá, thời lượng, sức chứa và trạng thái.\n"
                + "4. Nếu người dùng hỏi tour không có trong database, hãy báo rõ và gợi ý tour hiện có.";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", systemPrompt),
                                Map.of("text", "Câu hỏi của khách hàng: " + userMessage)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", geminiApiKey.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map body = response.getBody();
            List candidates = (List) body.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map candidate = (Map) candidates.get(0);
                Map content = (Map) candidate.get("content");
                if (content != null) {
                    List parts = (List) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map part = (Map) parts.get(0);
                        return (String) part.get("text");
                    }
                }
            }
        }
        return null;
    }

    private String getGeminiApiKey() {
        try {
            String apiKey = environment.getProperty("GEMINI_API_KEY", "");
            return apiKey == null ? "" : apiKey.trim();
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private String buildDbContextPrompt(List<Tour> tours) {
        if (tours.isEmpty()) {
            return "Hiện tại hệ thống chưa có dữ liệu tour nào trong database.";
        }

        StringBuilder sb = new StringBuilder();
        for (Tour tour : tours) {
            sb.append(String.format("- ID: %d | Tên tour: %s | Điểm đến: %s | Giá: %.2f VND | Thời lượng: %d ngày | Sức chứa: %d khách | Trạng thái: %s\n",
                    tour.getId(), tour.getTourName(), tour.getDestination(), tour.getPrice(), tour.getDuration(),
                    tour.getCapacity(), getStatusText(tour.getStatus())));
        }
        return sb.toString();
    }

    private String generateSmartResponseFromDb(String query, List<Tour> tours) {
        String q = query.toLowerCase().trim();

        if (tours.isEmpty()) {
            return "Hiện tại hệ thống chưa có dữ liệu tour du lịch nào trong cơ sở dữ liệu.";
        }

        // Tìm theo tên tour hoặc destination để trả lời nhanh không cần gọi AI bên ngoài.
        List<Tour> matchedTours = tours.stream()
                .filter(tour -> tour.getDestination().toLowerCase().contains(q)
                        || tour.getTourName().toLowerCase().contains(q)
                        || q.contains(tour.getDestination().toLowerCase()))
                .collect(Collectors.toList());

        if (!matchedTours.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("**Tìm thấy ").append(matchedTours.size())
                    .append(" tour phù hợp với yêu cầu của bạn:**\n\n");
            for (Tour tour : matchedTours) {
                appendTourInfo(sb, tour);
            }
            return sb.toString();
        }

        if (q.contains("rẻ nhất") || q.contains("giá rẻ") || q.contains("thấp nhất")) {
            Tour cheapest = tours.stream()
                    .min((first, second) -> Double.compare(first.getPrice(), second.getPrice()))
                    .orElse(null);
            if (cheapest != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("**Tour có giá tốt nhất hiện tại:**\n\n");
                appendTourInfo(sb, cheapest);
                return sb.toString();
            }
        }

        if (q.contains("ngắn") || q.contains("ít ngày")) {
            List<Tour> shortestTours = tours.stream()
                    .sorted((first, second) -> Integer.compare(first.getDuration(), second.getDuration()))
                    .limit(2)
                    .toList();
            StringBuilder sb = new StringBuilder("**Các tour có thời lượng ngắn nhất:**\n\n");
            for (Tour tour : shortestTours) {
                appendTourInfo(sb, tour);
            }
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Trợ lý TourVerse:** Tôi đã tìm trong database. Đây là danh sách các tour đang có:\n\n");
        for (Tour tour : tours) {
            appendTourInfo(sb, tour);
        }
        sb.append("\nBạn có thể bấm View Detail trên danh sách để xem chi tiết, hoặc hỏi thêm về điểm đến cụ thể.");
        return sb.toString();
    }

    private void appendTourInfo(StringBuilder sb, Tour tour) {
        sb.append("**").append(tour.getTourName()).append("**\n");
        sb.append("- **Điểm đến:** ").append(tour.getDestination()).append("\n");
        sb.append("- **Giá tour:** ").append(String.format("%.2f", tour.getPrice())).append(" VND\n");
        sb.append("- **Thời lượng:** ").append(tour.getDuration()).append(" ngày\n");
        sb.append("- **Sức chứa:** ").append(tour.getCapacity()).append(" khách\n");
        sb.append("- **Trạng thái:** ").append(getStatusBadge(tour.getStatus())).append("\n\n");
    }

    private String getStatusText(String status) {
        if ("AC".equalsIgnoreCase(status)) {
            return "Active (Hoạt động)";
        }
        if ("IN".equalsIgnoreCase(status)) {
            return "Inactive (Tạm ngưng)";
        }
        if ("DR".equalsIgnoreCase(status)) {
            return "Draft (Bản nháp)";
        }
        return status;
    }

    private String getStatusBadge(String status) {
        if ("AC".equalsIgnoreCase(status)) {
            return "[status:active]Active[/status]";
        }
        if ("IN".equalsIgnoreCase(status)) {
            return "[status:inactive]Inactive[/status]";
        }
        if ("DR".equalsIgnoreCase(status)) {
            return "[status:draft]Draft[/status]";
        }
        return status;
    }

    private List<String> getDefaultSuggestions() {
        return List.of(
                "Các tour đi Đà Nẵng?",
                "Tour nào giá rẻ nhất?",
                "Có tour đi Hạ Long không?",
                "Danh sách tất cả tour"
        );
    }

    private List<String> generateDynamicSuggestions(List<Tour> tours) {
        List<String> suggestions = new ArrayList<>();
        if (!tours.isEmpty()) {
            suggestions.add("Tour đi " + tours.get(0).getDestination());
        }
        suggestions.add("Tour nào giá rẻ nhất?");
        suggestions.add("Tour ngắn ngày nhất?");
        suggestions.add("Xem tất cả tour");
        return suggestions;
    }
}
