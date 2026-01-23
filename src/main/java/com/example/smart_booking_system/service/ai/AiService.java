package com.example.smart_booking_system.service.ai;

import com.example.smart_booking_system.dto.ChatResponseDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.repository.PropertyDetailRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    private final UserRepository userRepository;
    private final PropertyDetailRepository propertyDetailRepository;
    private final ObjectMapper objectMapper;
    private final BookingSessionManager sessionManager;

    private WebClient webClient;

    public AiService(
            UserRepository userRepository,
            PropertyDetailRepository propertyDetailRepository,
            ObjectMapper objectMapper,
            BookingSessionManager sessionManager
    ) {
        this.userRepository = userRepository;
        this.propertyDetailRepository = propertyDetailRepository;
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
    }

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final String BASE_INSTRUCTION = """
            You are Travel Mate, a Vietnamese travel & hotel assistant. You must ALWAYS follow this output specification exactly.
                    
            ====================== GLOBAL RULES ======================
            1) Tuyệt đối KHÔNG dùng bất kỳ markdown nào:
               Không *, **, -, _, #, ~, `, emoji, danh sách bullet.
            2) Không tự thêm tiêu đề, tag, dấu ngoặc vuông hoặc ký tự lạ.
            3) Không bao giờ viết thêm “(JSON only)” hoặc bất kỳ mô tả gì xung quanh JSON.
            4) Không bao giờ trộn text vào trong JSON hoặc viết JSON trong chuỗi.
            5) Không bao giờ sinh mã code, markdown code block, hoặc ký tự backtick.
                    
            ====================== TRAVEL GUIDE MODE ======================
            KÍCH HOẠT khi người dùng hỏi:
            - "ở <city> đi đâu đẹp"
            - "gợi ý chỗ chơi"
            - hỏi địa điểm du lịch, danh lam, thắng cảnh
                    
            KHI Ở TRAVEL GUIDE MODE:
            Bạn phải trả về CHÍNH XÁC 2 phần, theo đúng thứ tự:
                    
            RESPONSE:
            <một đoạn văn tiếng Việt tự nhiên, truyền cảm, không markdown, không bullet, không quá 5 câu, không tiêu đề>
                    
            PAYLOAD:
            {
              "city": "<tên thành phố>",
              "places": [
                { "name": "<tên địa điểm>", "type": "<loại>" },
                ...
              ]
            }
                    
            YÊU CẦU BẮT BUỘC:
            - Không để bất kỳ ký tự nào ngoài JSON sau dòng PAYLOAD:.
            - JSON phải hợp lệ tuyệt đối.
            - Không thêm text trước, trong, hoặc sau JSON.
            - Không xuống dòng lung tung bên trong JSON trừ spacing mặc định.
            - RESPONSE phải là đoạn văn đơn giản, không chứa danh sách bullet.
            - Ngắn gọn, mượt, tự nhiên.
                    
            ====================== BOOKING MODE ======================
            Hoàn toàn do backend điều khiển.
            Trong booking mode tuyệt đối KHÔNG sinh gợi ý du lịch.
                    
            ====================== Q&A MODE ======================
            Nếu không thuộc Travel Guide hay Booking:
            → chỉ trả RESPONSE:, không PAYLOAD.
                    
            ====================== SELF CHECK (THỰC THI BẮT BUỘC) ======================
            Trước khi trả kết quả, bạn PHẢI tự kiểm tra:
            - RESPONSE không chứa bất kỳ ký tự markdown nào.
            - Không có bullet.
            - Không có ký tự *, -, #, _, `, ~, emoji.
            - Không có text sau JSON.
            - JSON hợp lệ 100%.
            - Format đúng EXACT như mô tả trên.
                    
            Nếu vi phạm, tự sửa lại trước khi gửi về.
                    
            ====================== EXTRA STRICT FORMAT RULES ======================
            1) Trong RESPONSE:
               - Không được chứa từ PAYLOAD (dù viết thường hay viết hoa).
               - Không được chứa từ JSON.
               - Không được bắt đầu hoặc kết thúc bằng dấu : hoặc ký tự lạ.
               - Không được tự tạo heading, prefix, suffix, hoặc bất kỳ chú thích nào.
                    
            2) Giữa RESPONSE và PAYLOAD:
               - Chỉ được phép có đúng 1 dòng trống duy nhất.
               - Không được có ký tự, chữ cái, dấu cách thừa, hoặc dấu xuống dòng kép.
               - Tuyệt đối không được có text mô tả như “PAYLOAD bên dưới”, “tiếp theo là JSON”, “dữ liệu như sau”, hoặc bất kỳ dạng giải thích nào.
                    
            3) Trong PAYLOAD (JSON):
               - Không được thêm dấu hai chấm hoặc bất kỳ text nào sau dấu đóng }.
               - Không bao giờ được bao toàn bộ JSON trong chuỗi văn bản.
               - Không được escape dấu ngoặc hoặc sinh JSON ảo.
               - Không được viết JSON với dấu tròn, dấu vuông hoặc ký tự lạ phía ngoài.
                    
            4) Thứ tự bắt buộc:
               - Luôn luôn: RESPONSE: <đoạn văn> → 1 dòng trống → JSON.
               - Không được đảo thứ tự.
                    
            5) Không được trả văn bản không liên quan, không được sáng tạo thêm phần mới.
                        
                    
            """;



    @PostConstruct
    private void init() {
        webClient = WebClient.builder().baseUrl(apiUrl).build();
    }

    // ==========================================================
    // MAIN ENTRY
    // ==========================================================
    @Transactional
    public ChatResponseDTO processChat(String userId, String userMessage) {

        if (userMessage == null || userMessage.isEmpty()) {
            return new ChatResponseDTO("Dạ anh/chị gửi nội dung giúp em ạ.", "NORMAL_CHAT", null);
        }

        String clean = preprocess(userMessage);
        BookingSession session = sessionManager.getOrCreate(userId);

        // ---------------- BOOKING FLOW ----------------
        if (session.isActive()) {
            return handleBookingFlow(userId, session, clean);
        }

        if (detectBookingIntent(clean)) {
            session.setActive(true);

            Integer cap = extractCapacity(clean);
            String city = extractCity(clean);

            if (city != null) session.setCity(city);
            if (cap != null) session.setCapacity(cap);

            if (session.getCity() == null) {
                session.setStep(BookingSession.Step.WAITING_CITY);
                return new ChatResponseDTO("Bạn muốn tìm phòng ở thành phố nào ạ?", "NORMAL_CHAT", null);
            }
            if (session.getCapacity() == null) {
                session.setStep(BookingSession.Step.WAITING_CAPACITY);
                return new ChatResponseDTO("Bạn muốn tìm phòng cho bao nhiêu người ạ?", "NORMAL_CHAT", null);
            }

            session.setStep(BookingSession.Step.READY);
            return executeSearch(userId, session.getCity(), session.getCapacity(), session);
        }

        // ---------------- NORMAL CHAT ----------------
        String aiReply = callGemini(clean);
        if (aiReply == null) {
            return new ChatResponseDTO("Xin lỗi, em chưa hiểu ý anh/chị ạ.", "NORMAL_CHAT", null);
        }

        String responseText = aiReply;
        JsonNode payloadJson = null;

        int payloadIndex = aiReply.indexOf("{");
        if (payloadIndex > 0) {
            responseText = aiReply.substring(0, payloadIndex).trim();
            String jsonPart = aiReply.substring(payloadIndex).trim();

            try {
                payloadJson = objectMapper.readTree(jsonPart);
            } catch (Exception ignored) {}
        }

        responseText = sanitize(responseText);

        return new ChatResponseDTO(responseText, "NORMAL_CHAT", payloadJson);
    }

    // ==========================================================
    // BOOKING FLOW
    // ==========================================================
    private ChatResponseDTO handleBookingFlow(String userId, BookingSession session, String msg) {

        // ⚠ FIX: bỏ CityExtractor — dùng extractCity của bạn
        String newCity = extractCity(msg);

        if (newCity != null) {
            session.setCity(newCity);
            session.setStep(BookingSession.Step.WAITING_CAPACITY);
            return new ChatResponseDTO(
                    "Dạ mình chuyển sang tìm phòng ở " + newCity + " ạ. Bạn muốn tìm phòng cho bao nhiêu người ạ?",
                    "NORMAL_CHAT",
                    null
            );
        }

        Integer newCap = extractCapacity(msg);
        if (newCap != null && newCap > 0 && session.getStep() == BookingSession.Step.WAITING_CAPACITY) {
            session.setCapacity(newCap);
            session.setStep(BookingSession.Step.READY);
            return executeSearch(userId, session.getCity(), newCap, session);
        }

        if (session.getStep() == BookingSession.Step.WAITING_CITY) {
            if (newCity != null) {
                session.setCity(newCity);
                session.setStep(BookingSession.Step.WAITING_CAPACITY);
                return new ChatResponseDTO("Bạn muốn tìm phòng cho bao nhiêu người ạ?", "NORMAL_CHAT", null);
            }
            return new ChatResponseDTO("Anh/chị cho em biết thành phố mình muốn tìm phòng ạ.", "NORMAL_CHAT", null);
        }

        if (session.getStep() == BookingSession.Step.WAITING_CAPACITY) {
            if (newCap != null && newCap > 0) {
                session.setCapacity(newCap);
                session.setStep(BookingSession.Step.READY);
                return executeSearch(userId, session.getCity(), newCap, session);
            }
            return new ChatResponseDTO("Anh/chị cho em biết số lượng người ạ?", "NORMAL_CHAT", null);
        }

        if (session.getStep() == BookingSession.Step.READY) {
            return executeSearch(userId, session.getCity(), session.getCapacity(), session);
        }

        session.reset();
        return new ChatResponseDTO("Mình bắt đầu lại giúp em được không ạ?", "NORMAL_CHAT", null);
    }

    // ==========================================================
    // SEARCH
    // ==========================================================
    private ChatResponseDTO executeSearch(String userId, String cityRaw, int capacity, BookingSession session) {

        // ❗ FIX: bỏ normalize – dùng y nguyên city của user
        String cityKeyword = cityRaw.toLowerCase().trim();

        List<Property> results = propertyDetailRepository.findAvailableProperties(cityKeyword, capacity);

        String reply = results.isEmpty()
                ? "Dạ hiện tại em chưa tìm được chỗ phù hợp ạ."
                : String.format("Dạ em đã tìm được %d chỗ ở phù hợp tại %s cho %d người ạ.",
                results.size(), cityRaw, capacity);

        session.reset();

        JsonNode json = null;
        try {
            json = objectMapper.readTree(buildJsonDataset(results, cityRaw, capacity));
        } catch (Exception ignored) {}

        return new ChatResponseDTO(reply, "SEARCH_ROOM_RESULT", json);
    }

    // ==========================================================
    // GEMINI CALL
    // ==========================================================
    private String callGemini(String userMessage) {
        try {
            ObjectNode req = objectMapper.createObjectNode();

            ObjectNode sys = objectMapper.createObjectNode();
            sys.putArray("parts").addObject().put("text",
                    BASE_INSTRUCTION + "\nToday: " + LocalDateTime.now()
            );
            req.set("system_instruction", sys);

            ArrayNode contents = req.putArray("contents");
            ObjectNode msg = contents.addObject();
            msg.put("role", "user");
            msg.putArray("parts").addObject().put("text", userMessage);

            String raw = webClient.post()
                    .uri(uri -> uri.queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(300)))
                    .block();

            JsonNode root = objectMapper.readTree(raw);
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText("");

        } catch (Exception e) {
            return "Xin lỗi anh/chị, hệ thống đang quá tải ạ.";
        }
    }

    // ==========================================================
    // HELPERS
    // ==========================================================
    private String preprocess(String s) {
        return s.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String sanitize(String s) {
        return s.replace("```", "").replace("---", "").trim();
    }

    private boolean detectBookingIntent(String text) {
        String s = text.toLowerCase();
        return s.matches(".*(tìm phòng|đặt phòng|thuê phòng|book).*")
                || s.matches(".*\\d+\\s*(người|khách).*");
    }

    private Integer extractCapacity(String text) {
        Matcher m = Pattern.compile("(\\d+)").matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));
        return null;
    }

    // ❗ FIX: extract city KHÔNG lấy dư chữ “cho”, “2 người”, “với”…
    private String extractCity(String text) {
        Matcher m = Pattern.compile("\\b(?:ở|tại)\\s+([\\p{L}\\s]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            String raw = m.group(1).trim();
            // cắt chữ nối
            raw = raw.replaceAll("\\b(cho|để|với|cần|muốn)\\b.*", "").trim();
            raw = raw.replaceAll("\\d+", "").trim();
            return raw;
        }

        if (text.matches("[\\p{L}\\s]+") && text.length() <= 30) {
            return text.trim();
        }

        return null;
    }

    // bỏ normalize – chỉ dùng lowercase
    private String normalizeCity(String s) {
        return s.toLowerCase().trim();
    }

    private String buildJsonDataset(List<Property> properties, String city, int capacity) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("city", city);
        root.put("capacity", capacity);

        if (properties.isEmpty()) {
            root.put("type", "NO_RESULT");
            root.put(
                    "message",
                    String.format("Xin lỗi anh chị em không tìm thấy dữ liệu ở %s ạ.", city)
            );
            return root.toString();
        }

        root.put("type", "RESULT");
        ArrayNode arr = root.putArray("properties");

        for (Property p : properties) {
            ObjectNode node = arr.addObject();

            node.put("propertyId", p.getPropertyId());
            node.put("name", p.getPropertyName());
            node.put("rating", p.getRating() != null ? p.getRating().doubleValue() : 0);
            node.put("reviewCount", p.getReviewCount());
            node.put("address", p.getAddress());
            node.put("bookingUrl", frontendUrl + "/hotels/" + p.getPropertyId());

            ArrayNode rooms = node.putArray("rooms");

            p.getRooms().stream()
                    .filter(Room::isActive)
                    .filter(r -> r.getCapacity() >= capacity)
                    .sorted(Comparator.comparing(r -> r.getPricePerNight()))
                    .limit(3)
                    .forEach(r -> {
                        ObjectNode rn = rooms.addObject();
                        rn.put("roomName", r.getRoomName());
                        rn.put("capacity", r.getCapacity());
                        rn.put("price", r.getPricePerNight().longValue());
                    });
        }

        return root.toString();
    }
}
