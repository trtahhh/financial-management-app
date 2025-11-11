package com.example.finance.controller;

import com.example.finance.dto.ApiResponse;
import com.example.finance.service.OcrService;
import com.example.finance.service.InvoiceCategoryClassifier;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class OcrController {

 private final OcrService ocrService;
 private final InvoiceCategoryClassifier classifier; // added classifier
 @Value("${app.upload.dir:uploads}")
 private String uploadDir;

 @PostMapping("/parse-invoice")
 public ResponseEntity<ApiResponse> parseInvoice(@RequestParam("file") MultipartFile file) {
 try {
 if (file == null || file.isEmpty()) {
 return ResponseEntity.badRequest().body(new ApiResponse(false, "File không hợp lệ", null));
 }

 log.info("OCR request: file={}, size={} bytes", file.getOriginalFilename(), file.getSize());
 // 1. Lưu lại file ảnh để người dùng có thể xem lại sau khi OCR
 String storedFileName = persistUploadedFile(file);
 String fileUrl = storedFileName != null ? "/api/files/uploads/" + storedFileName : null;

 JSONObject raw = ocrService.parseInvoice(file);
 log.info("OCR response: {}", raw.toString());

 // Trích xuất text từ OCR.space response
 String text = extractText(raw);
 log.info("Extracted text: '{}'", text);

 Map<String, Object> result = new HashMap<>();
 result.put("rawText", text);
 result.put("rawResponse", raw.toMap()); // Thêm raw response để debug
 if (storedFileName != null) {
 result.put("storedFileName", storedFileName);
 result.put("fileUrl", fileUrl); // URL frontend có thể dùng để hiển thị ảnh
 }

 // Heuristic nâng cao: lấy nhiều ứng viên + thông tin dòng chứa để client có thể hiển thị/cho người dùng chọn nếu cần
 RegexUtils.CurrencyCandidate bestAmount = RegexUtils.extractBestCurrency(text);
 RegexUtils.DateCandidate bestDate = RegexUtils.extractBestDate(text);
 String merchant = RegexUtils.extractMerchant(text);

 if (bestAmount != null) {
 result.put("suggestedAmount", bestAmount.valueRaw);
 result.put("amountLine", bestAmount.lineRaw);
 result.put("amountConfidence", bestAmount.confidence);
 }
 if (bestDate != null) {
 result.put("suggestedDate", bestDate.dateRaw);
 result.put("dateNormalized", bestDate.dateIso);
 result.put("dateConfidence", bestDate.confidence);
 }
 if (merchant != null) {
 result.put("merchant", merchant);
 }
 // Gợi ý danh mục rất đơn giản dựa trên từ khóa (có thể refine sau)
 String cat = RegexUtils.suggestCategory(text);
 if (cat != null) result.put("suggestedCategory", cat);

 // Thử chuyển đổi dòng viết bằng chữ ra số: nếu lớn hơn các số từng món => có thể là tổng
 java.math.BigDecimal spelled = RegexUtils.extractSpelledOutVnAmount(text);
 if (spelled != null) {
 result.put("spelledOutAmount", spelled.toPlainString());
 if (bestAmount == null || spelled.compareTo(bestAmount.numeric) > 0) {
 result.put("suggestedAmount", spelled.toPlainString());
 result.put("amountSource", "spelled-out");
 }
 }

 // Rule: nếu có một số lớn xuất hiện >=2 lần ở các dòng khác nhau (ví dụ tổng & tiền mặt) thì ưu tiên.
 String currentSuggested = (String) result.get("suggestedAmount");
 String dupOverride = RegexUtils.findDuplicateLargeAmount(text, currentSuggested);
 if (dupOverride != null && (currentSuggested == null || !dupOverride.equals(currentSuggested))) {
 result.put("suggestedAmount", dupOverride);
 result.putIfAbsent("amountSource", "duplicate-lines");
 result.put("duplicateAmount", dupOverride);
 }

 // Phase 3: chấm điểm theo từ khóa tổng + số xuất hiện nhiều lần để đảm bảo chọn dòng tổng cuối.
 RegexUtils.AmountLineOverride kw = RegexUtils.overrideByKeywordDuplicate(text, (String) result.get("suggestedAmount"));
 if (kw != null && kw.amount != null) {
 result.put("suggestedAmount", kw.amount);
 if (kw.line != null) result.put("amountLine", kw.line.trim());
 result.put("amountSource", kw.source);
 }

 // Log final chosen values (after overrides) for clarity
 log.info("Final OCR suggestions -> amount: {}, date: {}, merchant: {}", 
 result.get("suggestedAmount"),
 result.get("dateNormalized") != null ? result.get("dateNormalized") : result.get("suggestedDate"),
 merchant);

 // After we have raw extracted text 'text'
 var prediction = classifier.predict(text);
 if (prediction != null) {
 result.put("predictedCategoryId", prediction.categoryId);
 result.put("predictedCategoryName", prediction.categoryName);
 result.put("predictedCategoryConfidence", prediction.confidence);
 }

 return ResponseEntity.ok(new ApiResponse(true, "OCR thành công", result));
 } catch (Exception e) {
 log.error("OCR parse error", e);
 String msg = e.getMessage();
 if (e.getCause() instanceof java.net.SocketTimeoutException || (msg != null && msg.toLowerCase().contains("timeout"))) {
 msg = "Hệ thống OCR phản hồi chậm (timeout). Vui lòng thử lại sau vài giây hoặc chọn ảnh rõ nét hơn.";
 } else if (msg == null || msg.isBlank()) {
 msg = "Không thể xử lý OCR. Vui lòng thử lại.";
 }
 return ResponseEntity.internalServerError().body(new ApiResponse(false, msg, null));
 }
 }

 private String persistUploadedFile(MultipartFile file) throws IOException {
 // Chỉ lưu file ảnh
 String contentType = file.getContentType();
 if (contentType == null || !contentType.startsWith("image/")) {
 return null;
 }
 Path uploadPath = Paths.get(uploadDir);
 if (!Files.exists(uploadPath)) {
 Files.createDirectories(uploadPath);
 }
 String original = file.getOriginalFilename();
 String ext = "";
 if (original != null && original.contains(".")) {
 ext = original.substring(original.lastIndexOf('.'));
 }
 String filename = UUID.randomUUID() + ext;
 Files.copy(file.getInputStream(), uploadPath.resolve(filename));
 return filename;
 }

 private String extractText(JSONObject raw) {
 try {
 JSONArray results = raw.optJSONArray("ParsedResults");
 if (results == null) return "";
 StringBuilder sb = new StringBuilder();
 for (int i = 0; i < results.length(); i++) {
 JSONObject item = results.getJSONObject(i);
 String text = item.optString("ParsedText", "");
 if (!text.isEmpty()) {
 sb.append(text).append("\n");
 }
 }
 return sb.toString();
 } catch (Exception e) {
 return raw.toString();
 }
 }

 // ===================== Regex / Heuristics =====================
 static class RegexUtils {
 // ---- Models ----
 static class CurrencyCandidate { String valueRaw; String lineRaw; double confidence; java.math.BigDecimal numeric; }
 static class DateCandidate { String dateRaw; String dateIso; String lineRaw; double confidence; }

 // ---- Public extractors ----
 static CurrencyCandidate extractBestCurrency(String text) {
 String[] keywords = {"tổng", "thành tiền", "tiền", "amount", "total", "cộng", "phải trả", "sum", "gross", "tiền mặt", "cash"};
 String[] lines = text.split("\r?\n");
 java.util.List<CurrencyCandidate> all = new java.util.ArrayList<>();
 for (String line : lines) {
 java.util.List<String> nums = findCurrencies(line);
 boolean keyword = containsKeyword(line, keywords);
 for (String raw : nums) {
 java.math.BigDecimal val = parseCurrencyToBigDecimal(raw);
 if (val == null) continue;
 // Loại bỏ khả năng là số điện thoại (chứa hơn 2 dấu chấm trong raw ban đầu hoặc toàn bộ 9-11 chữ số không phân tách)
 String rawDigits = raw.replaceAll("[^0-9]", "");
 if (rawDigits.length() >= 9 && rawDigits.length() <= 11 && !raw.contains(",") && !raw.contains(".")) continue;
 CurrencyCandidate c = new CurrencyCandidate();
 c.valueRaw = raw; c.lineRaw = line; c.numeric = val;
 double conf = 0.35; // base
 if (keyword) conf += 0.45; // tăng trọng số cho dòng có keyword
 // Nếu keyword và chỉ có đúng 1 số trong dòng -> tăng confidence (thường là dòng tổng)
 if (keyword && nums.size() == 1) conf += 0.1;
 int digits = val.toPlainString().length();
 conf += Math.min(0.2, digits * 0.01);
 // Boost nếu số có >=2 dấu phẩy (dạng 1,233,350) -> thường là tổng
 int commaCount = raw.length() - raw.replace(",", "").length();
 if (commaCount >= 2) conf += 0.08;
 // Phạt nếu số dạng nhỏ với một dấu phẩy cuối (ví dụ 3,35) và tồn tại số lớn hơn nhiều sẽ xử lý ở bước sau
 if (commaCount == 1 && rawDigits.length() <= 3) conf -= 0.1;
 c.confidence = Math.min(0.99, conf);
 all.add(c);
 }
 }
 if (all.isEmpty()) return null;
 // Ứng viên có nhiều nhóm (>=2 dấu phẩy hoặc >=6 digits & có dấu phẩy) lớn nhất
 CurrencyCandidate multiGroupMax = all.stream()
 .filter(c -> {
 String r = c.valueRaw;
 int commas = r.length() - r.replace(",", "").length();
 String digits = r.replaceAll("[^0-9]", "");
 return commas >= 2 || (digits.length() >= 6 && r.contains(","));
 })
 .sorted((a,b) -> b.numeric.compareTo(a.numeric))
 .findFirst().orElse(null);

 // Ứng viên theo confidence chuẩn
 CurrencyCandidate byConfidence = all.stream()
 .sorted((a,b) -> {
 int cmp = Double.compare(b.confidence, a.confidence);
 if (cmp != 0) return cmp;
 return b.numeric.compareTo(a.numeric);
 })
 .findFirst().orElse(null);

 if (multiGroupMax != null && byConfidence != null) {
 // Nếu candidate theo confidence nhỏ hơn nhiều so với multi-group max (gấp 10 lần) thì chọn multi-group
 java.math.BigDecimal tenTimes = byConfidence.numeric.multiply(new java.math.BigDecimal("10"));
 if (multiGroupMax.numeric.compareTo(tenTimes) > 0) {
 return multiGroupMax;
 }
 // Hoặc nếu multiGroup có >=2 dấu phẩy còn candidate không có => ưu tiên multiGroup
 int mc = multiGroupMax.valueRaw.length() - multiGroupMax.valueRaw.replace(",", "").length();
 int bc = byConfidence.valueRaw.length() - byConfidence.valueRaw.replace(",", "").length();
 if (mc >= 2 && bc < 2) return multiGroupMax;
 }
 return byConfidence != null ? byConfidence : multiGroupMax;
 }

 static DateCandidate extractBestDate(String text) {
 String[] lines = text.split("\r?\n");
 java.util.List<DateCandidate> all = new java.util.ArrayList<>();
 int currentYear = java.time.Year.now().getValue();
 for (String originalLine : lines) {
 String line = originalLine;
 // Quick pre-clean: remove double spaces
 line = line.replaceAll("\\s+"," ");
 // Additional heuristics: unify ' V20xx' or 'V20xx' after day/month
 String heuristicLine = line.replaceAll("([0-9])\\s*V(20[0-9]{2})","$1 $2");
 for (java.util.regex.Pattern p : datePatternsExtended()) {
 java.util.regex.Matcher m = p.matcher(heuristicLine);
 while (m.find()) {
 String rawMatch = m.group(0);
 String cleaned = rawMatch
 .replaceAll("V(20[0-9]{2})","$1")
 .replaceAll("v(20[0-9]{2})","$1")
 .replaceAll("\\s+"," ");
 DateCandidate d = new DateCandidate();
 d.dateRaw = rawMatch; d.lineRaw = originalLine;
 d.dateIso = normalizeDate(cleaned);
 if (d.dateIso == null) {
 // salvage patterns like 28/1 V2015 -> 28/1 2015
 String salvage = cleaned.replaceAll("([0-9]{1,2}/[0-9]{1,2}) V(20[0-9]{2})","$1 $2");
 if (!salvage.equals(cleaned)) d.dateIso = normalizeDate(salvage);
 }
 if (d.dateIso == null) {
 // Last resort: insert current year if only day/month present near V201
 if (heuristicLine.matches(".*(0?[0-9]|[12][0-9]|3[01])/[0-9]{1,2} V201[0-9].*")) {
 java.util.regex.Matcher dm = java.util.regex.Pattern.compile("(0?[0-9]|[12][0-9]|3[01])/(0?[0-9]|1[0-2])").matcher(heuristicLine);
 if (dm.find()) {
 int dDay = Integer.parseInt(dm.group(1));
 int dMon = Integer.parseInt(dm.group(2));
 try {
 d.dateIso = java.time.LocalDate.of(currentYear, dMon, dDay).toString();
 } catch (Exception ignore) {}
 }
 }
 }
 if (d.dateIso != null) {
 try {
 java.time.LocalDate parsed = java.time.LocalDate.parse(d.dateIso);
 if (parsed.getYear() < currentYear - 5 && rawMatch.toUpperCase().contains("V201")) {
 parsed = parsed.withYear(currentYear);
 d.dateIso = parsed.toString();
 }
 } catch (Exception ignore) {}
 }
 double conf = 0.5;
 String lower = originalLine.toLowerCase();
 if (lower.contains("ngày") || lower.contains("date")) conf += 0.3;
 if (d.dateIso != null) conf += 0.15;
 d.confidence = Math.min(conf, 0.97);
 all.add(d);
 }
 }
 }
 return all.stream().filter(c -> c.dateIso != null)
 .sorted((a,b) -> Double.compare(b.confidence, a.confidence))
 .findFirst().orElse(null);
 }

 static String extractMerchant(String text) {
 String[] lines = text.split("\r?\n");
 java.util.regex.Pattern pUpper = java.util.regex.Pattern.compile("^[A-ZÀÁÂÃÄÅÈÉÊËÌÍÎÏÒÓÔÕÖÙÚÛÜÝĐƠƯ0-9 &\\-]{6,}$");
 for (String line : lines) {
 String trimmed = line.trim();
 String upper = trimmed.toUpperCase();
 if (upper.contains("CÔNG TY") || upper.contains("CTY") || upper.contains("TNHH") || upper.contains("COMPANY") || pUpper.matcher(trimmed).find()) {
 if (trimmed.length() >= 6 && !upper.startsWith("TỔNG") && !upper.startsWith("TOTAL")) {
 return trimmed;
 }
 }
 }
 return null;
 }

 static String suggestCategory(String text) {
 String lower = text.toLowerCase();
 if (lower.contains("bar") || lower.contains("restaurant") || lower.contains("susi") || lower.contains("sushi") || lower.contains("ăn") ) {
 return "Ăn uống"; // map đơn giản
 }
 if (lower.contains("coffee") || lower.contains("cafe") ) return "Đồ uống";
 if (lower.contains("taxi") || lower.contains("grab") ) return "Di chuyển";
 return null;
 }

 // --------- Spelled-out Vietnamese amount parsing ----------
 static java.math.BigDecimal extractSpelledOutVnAmount(String text) {
 String[] lines = text.split("\r?\n");
 java.math.BigDecimal best = null;
 for (int i=0;i<lines.length;i++) {
 String norm = normalizeSpelledLine(lines[i]);
 if (norm.matches(".*(trieu|ngan|tram|muoi|dong).*")) {
 // Ghép thêm dòng tiếp theo nếu cũng chứa mảnh vỡ
 String combined = norm;
 if (i+1 < lines.length) {
 String next = normalizeSpelledLine(lines[i+1]);
 if (next.matches(".*(trieu|ngan|tram|muoi|dong).*")) {
 combined = combined + " " + next;
 }
 }
 java.math.BigDecimal val = wordsToNumber(combined);
 if (val != null && (best == null || val.compareTo(best) > 0)) best = val;
 }
 }
 return best;
 }

 // Chuyển số viết bằng chữ tiếng Việt đơn giản (không hoàn hảo nhưng đủ cho tổng tiền lớn)
 private static java.math.BigDecimal wordsToNumber(String line) {
 // Chuẩn hóa: loại bỏ ký tự không cần
 String cleaned = line.replaceAll("[^a-z0-9 ]", " ").replaceAll(" +", " ").trim();
 if (cleaned.isEmpty()) return null;
 String[] tokens = cleaned.split(" ");
 java.util.Map<String, Long> base = new java.util.HashMap<>();
 base.put("khong",0L); base.put("mot",1L); base.put("hai",2L); base.put("ba",3L); base.put("bon",4L); base.put("tu",4L); base.put("nam",5L); base.put("lam",5L); base.put("sau",6L); base.put("bay",7L); base.put("tam",8L); base.put("chin",9L);
 java.util.Map<String, Long> mul = new java.util.HashMap<>();
 mul.put("tram",100L); mul.put("ngan",1000L); mul.put("ngàn",1000L); mul.put("trieu",1_000_000L); mul.put("triệu",1_000_000L); mul.put("ty",1_000_000_000L); mul.put("tỷ",1_000_000_000L);
 long current = 0; long total = 0; boolean seen = false;
 for (String rawTok : tokens) {
 String tok = stripDiacritics(rawTok).toLowerCase();
 // Sửa các biến thể thiếu ký tự do '?'
 if (tok.equals("triu")) tok = "trieu"; // tri?u
 if (tok.equals("trm")) tok = "tram"; // tr?m
 if (tok.equals("moi") || tok.equals("muoi")) tok = "muoi"; // m??i -> muoi
 if (tok.equals("ngan") || tok.equals("ngn")) tok = "ngan"; // ngàn
 if (tok.equals("dong") || tok.equals("ng")) tok = "dong"; // ??ng
 if (tok.equals("nm")) tok = "nam"; // n?m
 if (base.containsKey(tok)) { current = current + base.get(tok); seen = true; continue; }
 if (mul.containsKey(tok)) {
 if (!seen) return null; // tránh parse sai câu không phải số
 if (current == 0) current = 1; // "trieu" đứng sau mà không có số -> 1 triệu
 current = current * mul.get(tok);
 total += current; current = 0; continue;
 }
 if (tok.equals("dong") || tok.equals("vnd")) {
 break; // kết thúc
 }
 }
 total += current;
 if (!seen || total == 0) return null;
 return new java.math.BigDecimal(total);
 }

 private static String normalizeSpelledLine(String raw) {
 String s = stripDiacritics(raw).toLowerCase();
 // Thay thế các biến thể với '?'
 s = s.replace("tri?u","trieu").replace("tr?m","tram").replace("m??i","muoi")
 .replace("ngàn","ngan").replace("n?m","nam").replace("??ng","dong").replace("m?t","mot");
 return s;
 }

 // Tìm số lớn xuất hiện >=2 lần ở các dòng khác nhau
 static String findDuplicateLargeAmount(String text, String currentSuggested) {
 java.util.Map<String, Integer> count = new java.util.HashMap<>();
 for (String line : text.split("\r?\n")) {
 java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![0-9])[0-9]{1,3}(?:[.,][0-9]{3})+").matcher(line);
 while (m.find()) {
 String num = m.group();
 String normalized = num.replaceAll("[.,]", "");
 if (normalized.length() < 5) continue; // bỏ số nhỏ
 count.put(normalized, count.getOrDefault(normalized,0)+1);
 }
 }
 String best = null;
 for (var e : count.entrySet()) {
 if (e.getValue() >= 2) {
 if (best == null || e.getKey().length() > best.length() || (e.getKey().length()==best.length() && e.getKey().compareTo(best)>0)) {
 best = e.getKey();
 }
 }
 }
 if (best == null) return null;
 // Format lại giống đầu vào (thêm dấu phẩy mỗi 3 số) chỉ nếu current format khác
 String formatted = formatWithCommas(best);
 if (currentSuggested != null && stripDigits(currentSuggested).equals(best)) return null; // không thay đổi
 return formatted;
 }

 private static String stripDigits(String s) { return s == null ? "" : s.replaceAll("[^0-9]","" ); }
 private static String formatWithCommas(String digits) {
 StringBuilder sb = new StringBuilder(digits);
 for (int i = sb.length()-3; i>0; i-=3) sb.insert(i, ',');
 return sb.toString();
 }

 // --------- Keyword + duplicate scoring override ---------
 static class AmountLineOverride { String amount; String line; String source; }
 static AmountLineOverride overrideByKeywordDuplicate(String text, String currentSuggested) {
 String[] rawLines = text.split("\r?\n");
 java.util.Map<String, Stats> map = new java.util.HashMap<>();
 java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?<![0-9])[0-9]{1,3}(?:[.,][0-9]{3})+");
 for (String line : rawLines) {
 java.util.regex.Matcher m = p.matcher(line);
 boolean hasKeyword = hasTotalKeyword(line);
 while (m.find()) {
 String token = m.group();
 String digits = token.replaceAll("[.,]", "");
 if (digits.length() < 5) continue; // tránh số nhỏ
 Stats st = map.computeIfAbsent(digits, k -> new Stats());
 st.count++;
 st.lines.add(line);
 st.hasKeyword |= hasKeyword;
 st.commaGroups = token.contains(",") ? token.split(",").length : 1;
 }
 }
 if (map.isEmpty()) return null;
 String bestKey = null; double bestScore = -1;
 for (var e : map.entrySet()) {
 Stats s = e.getValue();
 double score = 0;
 score += s.count * 2; // lặp lại càng nhiều càng tốt
 if (s.hasKeyword) score += 4; // dòng chứa từ khóa tổng
 score += e.getKey().length(); // độ lớn tuyệt đối
 if (s.commaGroups > 2) score += 1; // nhiều nhóm phẩy (>= triệu)
 if (score > bestScore) { bestScore = score; bestKey = e.getKey(); }
 }
 if (bestKey == null) return null;
 String formatted = formatWithCommas(bestKey);
 if (currentSuggested != null && stripDigits(currentSuggested).equals(bestKey)) return null; // không đổi
 AmountLineOverride out = new AmountLineOverride();
 out.amount = formatted;
 out.source = "keyword-duplicate";
 // Chọn dòng tốt nhất: ưu tiên có keyword, sau đó độ dài dòng
 Stats st = map.get(bestKey);
 String chosen = null;
 for (String l : st.lines) {
 if (hasTotalKeyword(l)) { chosen = l; break; }
 }
 if (chosen == null && !st.lines.isEmpty()) chosen = st.lines.get(0);
 out.line = chosen;
 return out;
 }

 private static boolean hasTotalKeyword(String line) {
 String s = stripDiacritics(line).toLowerCase();
 return s.contains("tong") || s.contains("t.c") || s.contains("tcong") || s.contains("t cong") || s.contains("tien mat") || s.contains("cong\t") || s.contains("t.c?ng");
 }

 static class Stats { int count; java.util.List<String> lines = new java.util.ArrayList<>(); boolean hasKeyword; int commaGroups; }

 // ---- Helpers ----
 private static boolean containsKeyword(String line, String[] kws) {
 String l = stripDiacritics(line).toLowerCase();
 for (String k: kws) {
 String normK = stripDiacritics(k).toLowerCase();
 if (l.contains(normK)) return true;
 }
 return false;
 }
 private static java.util.List<String> findCurrencies(String s) {
 java.util.List<String> out = new java.util.ArrayList<>();
 String[] patterns = new String[]{
 "(?i)(?:vnd|vnđ|đ)?\\s*([0-9]{1,3}(?:[\\.\\,\u00A0\u202F ]?[0-9]{3})+(?:[\\,\\.][0-9]{1,2})?)\\s*(?:vnd|vnđ|đ)?",
 "(?i)(?:vnd|vnđ|đ)?\\s*([0-9]+(?:[\\.,][0-9]{1,2})?)\\s*(?:vnd|vnđ|đ)?"
 };
 for (String p : patterns) {
 java.util.regex.Matcher m = java.util.regex.Pattern.compile(p).matcher(s);
 while (m.find()) out.add(m.group(1));
 }
 // Lọc bỏ các chuỗi nghi là số điện thoại dạng 3684 777 hoặc 0905 225 868 (có khoảng trắng phân tách nhưng không phải nhóm nghìn chuẩn)
 java.util.List<String> filtered = new java.util.ArrayList<>();
 for (String candidate : out) {
 String digits = candidate.replaceAll("[^0-9]", "");
 // nếu có khoảng trắng giữa và tổng độ dài 6-11 và không có dấu phẩy => khả năng cao là phone fragment => bỏ
 if (candidate.matches(".*\\s+.*") && digits.length() >= 6 && digits.length() <= 11 && !candidate.contains(",") && !candidate.contains(".")) {
 continue;
 }
 filtered.add(candidate);
 }
 return filtered;
 }
 private static java.math.BigDecimal parseCurrencyToBigDecimal(String raw) {
 if (raw == null) return null;
 String s = raw.toLowerCase().replaceAll("[a-zđvnđ\u00A0\u202F ]", "");
 int lastDot = s.lastIndexOf('.');
 int lastComma = s.lastIndexOf(',');
 if (lastComma > lastDot) { s = s.replace(".", ""); s = s.replace(",", "."); }
 else { s = s.replace(",", ""); }
 try { return new java.math.BigDecimal(s); } catch (Exception e) { return null; }
 }
 private static java.util.List<java.util.regex.Pattern> datePatterns() {
 java.util.List<java.util.regex.Pattern> list = new java.util.ArrayList<>();
 list.add(java.util.regex.Pattern.compile("\\b(0?[1-9]|[12][0-9]|3[01])[\\/\\-\\. ](0?[1-9]|1[0-2])[\\/\\-\\. ](20[0-9]{2})\\b"));
 list.add(java.util.regex.Pattern.compile("\\b(20[0-9]{2})[\\/\\-\\. ](0?[1-9]|1[0-2])[\\/\\-\\. ](0?[1-9]|[12][0-9]|3[01])\\b"));
 list.add(java.util.regex.Pattern.compile("(?i)ngày\\s+(0?[1-9]|[12][0-9]|3[01])[\\/\\-\\. ](0?[1-9]|1[0-2])[\\/\\-\\. ](20[0-9]{2})"));
 return list;
 }
 private static java.util.List<java.util.regex.Pattern> datePatternsExtended() {
 java.util.List<java.util.regex.Pattern> list = new java.util.ArrayList<>();
 list.addAll(datePatterns());
 // Thêm mẫu có ký tự 'V' xen giữa trước năm hoặc dính liền: 28/1 V2015 hoặc 28/01V2015
 list.add(java.util.regex.Pattern.compile("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[0-2])\\s*V(20[0-9]{2})"));
 list.add(java.util.regex.Pattern.compile("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[0-2])V(20[0-9]{2})"));
 // Trường hợp có chữ 'V' sau dấu cách trước block năm trong dòng có chữ Ngày:
 list.add(java.util.regex.Pattern.compile("(?i)ngày[^\n]{0,15}?(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[0-2])\\s*V(20[0-9]{2})"));
 return list;
 }
 private static String normalizeDate(String raw) {
 if (raw == null) return null;
 String cleaned = raw.replace('V',' ').replaceAll("\\s+", " "); // sửa lỗi OCR V2015 -> 2015
 java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("(0?[1-9]|[12][0-9]|3[01])[\\/\\-\\. ](0?[1-9]|1[0-2])[\\/\\-\\. ](20[0-9]{2})").matcher(cleaned);
 if (m1.find()) {
 String d = m1.group(1); String mo = m1.group(2); String y = m1.group(3);
 return String.format("%s-%02d-%02d", y, Integer.parseInt(mo), Integer.parseInt(d));
 }
 java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("(20[0-9]{2})[\\/\\-\\. ](0?[1-9]|1[0-2])[\\/\\-\\. ](0?[1-9]|[12][0-9]|3[01])").matcher(cleaned);
 if (m2.find()) {
 String y = m2.group(1); String mo = m2.group(2); String d = m2.group(3);
 return String.format("%s-%02d-%02d", y, Integer.parseInt(mo), Integer.parseInt(d));
 }
 return null;
 }

 private static String stripDiacritics(String input) {
 if (input == null) return "";
 String norm = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
 return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
 }
 }
}



