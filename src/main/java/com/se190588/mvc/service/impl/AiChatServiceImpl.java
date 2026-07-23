package com.se190588.mvc.service.impl;

import com.se190588.mvc.dto.AiChatDto;
import com.se190588.mvc.entity.Tour;
import com.se190588.mvc.repository.TourRepository;
import com.se190588.mvc.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public AiChatDto processUserMessage(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return new AiChatDto("Xin chào! Tôi là Trợ lý AI du lịch của TourVerse. Bạn cần tìm thông tin tour nào ạ?", getDefaultSuggestions());
        }

        List<Tour> allTours = tourRepository.findAllByOrderByTourNameAsc();

        // 1. Nếu có Gemini API Key, gọi trực tiếp Gemini API
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
            try {
                String aiReply = callGeminiApi(userMessage, allTours);
                if (aiReply != null && !aiReply.trim().isEmpty()) {
                    return new AiChatDto(aiReply, generateDynamicSuggestions(userMessage, allTours));
                }
            } catch (Exception e) {
                // Nếu gọi API thất bại, tự động chuyển sang Smart RAG Fallback Engine
            }
        }

        // 2. Smart RAG Fallback Engine (Truy vấn DB trực tiếp trả về kết quả thông minh)
        String fallbackReply = generateSmartResponseFromDb(userMessage, allTours);
        return new AiChatDto(fallbackReply, generateDynamicSuggestions(userMessage, allTours));
    }

    private String callGeminiApi(String userMessage, List<Tour> tours) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

        String dbContext = buildDbContextPrompt(tours);

        String systemPrompt = "Bạn là AI Trợ lý Tư Vấn Du Lịch của website TourVerse. "
                + "Dưới đây là DỮ LIỆU THỰC TẾ các Tour du lịch đang có trong hệ thống Database của chúng tôi:\n\n"
                + dbContext + "\n\n"
                + "QUY TẮC PHẢN HỒI:\n"
                + "1. Chỉ tư vấn dựa trên thông tin các tour thực tế có trong Database nêu trên.\n"
                + "2. Trả lời bằng tiếng Việt thân thiện, lịch sự, sử dụng định dạng Markdown đẹp mắt (in đậm, danh sách bullet).\n"
                + "3. Bao gồm thông tin Điểm đến, Giá ($), Thời lượng (ngày), Sức chứa và Trạng thái khi giới thiệu tour.\n"
                + "4. Nếu người dùng hỏi tour không có trong DB, hãy lịch sự thông báo và gợi ý các tour hiện có.";

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

    private String buildDbContextPrompt(List<Tour> tours) {
        if (tours.isEmpty()) {
            return "Hiện tại hệ thống chưa có dữ liệu tour nào trong database.";
        }

        StringBuilder sb = new StringBuilder();
        for (Tour t : tours) {
            sb.append(String.format("- ID: %d | Tên Tour: %s | Điểm đến: %s | Giá: $%.2f | Thời lượng: %d ngày | Sức chứa: %d khách | Trạng thái: %s\n",
                    t.getId(), t.getTourName(), t.getDestination(), t.getPrice(), t.getDuration(), t.getCapacity(), getStatusText(t.getStatus())));
        }
        return sb.toString();
    }

    private String generateSmartResponseFromDb(String query, List<Tour> tours) {
        String q = query.toLowerCase().trim();

        if (tours.isEmpty()) {
            return "⚠️ Hiện tại hệ thống chưa có dữ liệu tour du lịch nào trong cơ sở dữ liệu.";
        }

        // Tìm kiếm theo địa điểm / tên tour
        List<Tour> matchedTours = tours.stream()
                .filter(t -> t.getDestination().toLowerCase().contains(q)
                        || t.getTourName().toLowerCase().contains(q)
                        || q.contains(t.getDestination().toLowerCase()))
                .collect(Collectors.toList());

        if (!matchedTours.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("🎯 **Tìm thấy ").append(matchedTours.size()).append(" tour phù hợp với yêu cầu của bạn:**\n\n");
            for (Tour t : matchedTours) {
                appendTourInfo(sb, t);
            }
            return sb.toString();
        }

        // Hỏi về giá rẻ nhất
        if (q.contains("rẻ nhất") || q.contains("giá rẻ") || q.contains("thấp nhất")) {
            Tour cheapest = tours.stream().min((t1, t2) -> Double.compare(t1.getPrice(), t2.getPrice())).orElse(null);
            if (cheapest != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("💡 **Tour có giá tốt nhất hiện tại:**\n\n");
                appendTourInfo(sb, cheapest);
                return sb.toString();
            }
        }

        // Hỏi về thời lượng / tour dài ngày / ngắn ngày
        if (q.contains("ngắn") || q.contains("ít ngày")) {
            List<Tour> shortest = tours.stream()
                    .sorted((t1, t2) -> Integer.compare(t1.getDuration(), t2.getDuration()))
                    .limit(2)
                    .toList();
            StringBuilder sb = new StringBuilder("⏱️ **Các tour có thời lượng ngắn nhất:**\n\n");
            for (Tour t : shortest) {
                appendTourInfo(sb, t);
            }
            return sb.toString();
        }

        // Tổng quan danh sách tất cả tour trong DB
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 **Trợ lý TourVerse:** Tôi đã tìm kiếm trong Database và dưới đây là toàn bộ danh sách các Tour du lịch đang khả dụng:\n\n");
        for (Tour t : tours) {
            appendTourInfo(sb, t);
        }
        sb.append("\n👉 *Bạn có thể nhấp vào 'View Detail' trên danh sách để xem chi tiết hoặc hỏi tôi thêm về điểm đến cụ thể!*");
        return sb.toString();
    }

    private void appendTourInfo(StringBuilder sb, Tour t) {
        sb.append("📍 **").append(t.getTourName()).append("**\n");
        sb.append("• **Điểm đến:** ").append(t.getDestination()).append("\n");
        sb.append("• **Giá tour:** $").append(String.format("%.2f", t.getPrice())).append("\n");
        sb.append("• **Thời lượng:** ").append(t.getDuration()).append(" ngày\n");
        sb.append("• **Sức chứa:** ").append(t.getCapacity()).append(" khách\n");
        sb.append("• **Trạng thái:** ").append(getStatusBadge(t.getStatus())).append("\n\n");
    }

    private String getStatusText(String status) {
        if ("AC".equalsIgnoreCase(status)) return "Active (Hoạt động)";
        if ("IN".equalsIgnoreCase(status)) return "Inactive (Tạm ngưng)";
        if ("DR".equalsIgnoreCase(status)) return "Draft (Bản nháp)";
        return status;
    }

    private String getStatusBadge(String status) {
        if ("AC".equalsIgnoreCase(status)) return "🟢 Active";
        if ("IN".equalsIgnoreCase(status)) return "🔴 Inactive";
        if ("DR".equalsIgnoreCase(status)) return "🟡 Draft";
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

    private List<String> generateDynamicSuggestions(String userMessage, List<Tour> tours) {
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
