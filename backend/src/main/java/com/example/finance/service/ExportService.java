package com.example.finance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

 @Autowired
 private TransactionService transactionService;
 
 @Autowired
 private BudgetService budgetService;
 
 @Autowired
 private GoalService goalService;

 /**
 * Xuất báo cáo Excel
 */
 public byte[] exportToExcel(Long userId, String reportType, LocalDate startDate, LocalDate endDate) throws IOException {
 try (Workbook workbook = new XSSFWorkbook()) {
 Sheet sheet = workbook.createSheet("Báo cáo tài chính");
 
 // Tạo style cho header
 CellStyle headerStyle = createHeaderStyle(workbook);
 CellStyle dataStyle = createDataStyle(workbook);
 
 int rowNum = 0;
 
 // Tiêu đề báo cáo
 Row titleRow = sheet.createRow(rowNum++);
 Cell titleCell = titleRow.createCell(0);
 titleCell.setCellValue("BÁO CÁO TÀI CHÍNH");
 titleCell.setCellStyle(headerStyle);
 sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
 
 // Thông tin thời gian
 Row dateRow = sheet.createRow(rowNum++);
 dateRow.createCell(0).setCellValue("Thời gian:");
 dateRow.createCell(1).setCellValue(startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
 
 rowNum++; // Dòng trống
 
 if ("transaction".equals(reportType) || "summary".equals(reportType)) {
 rowNum = exportTransactionsToExcel(sheet, userId, startDate, endDate, rowNum, headerStyle, dataStyle);
 }
 
 if ("budget".equals(reportType) || "summary".equals(reportType)) {
 rowNum = exportBudgetsToExcel(sheet, userId, startDate, endDate, rowNum, headerStyle, dataStyle);
 }
 
 if ("goal".equals(reportType) || "summary".equals(reportType)) {
 rowNum = exportGoalsToExcel(sheet, userId, rowNum, headerStyle, dataStyle);
 }
 
 // Tự động điều chỉnh cột
 for (int i = 0; i < 6; i++) {
 sheet.autoSizeColumn(i);
 }
 
 ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
 workbook.write(outputStream);
 return outputStream.toByteArray();
 }
 }

 /**
 * Xuất báo cáo PDF
 */
 public byte[] exportToPDF(Long userId, String reportType, LocalDate startDate, LocalDate endDate) throws DocumentException, IOException {
 ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
 Document document = new Document(PageSize.A4);
 PdfWriter.getInstance(document, outputStream);
 
 document.open();
 
 // Tiêu đề
 com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
 Paragraph title = new Paragraph("BÁO CÁO TÀI CHÍNH", titleFont);
 title.setAlignment(Element.ALIGN_CENTER);
 document.add(title);
 document.add(new Paragraph(" "));
 
 // Thông tin thời gian
 com.itextpdf.text.Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
 Paragraph dateInfo = new Paragraph("Thời gian: " + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
 " - " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), infoFont);
 document.add(dateInfo);
 document.add(new Paragraph(" "));
 
 if ("transaction".equals(reportType) || "summary".equals(reportType)) {
 exportTransactionsToPDF(document, userId, startDate, endDate);
 }
 
 if ("budget".equals(reportType) || "summary".equals(reportType)) {
 exportBudgetsToPDF(document, userId, startDate, endDate);
 }
 
 if ("goal".equals(reportType) || "summary".equals(reportType)) {
 exportGoalsToPDF(document, userId);
 }
 
 document.close();
 return outputStream.toByteArray();
 }

 /**
 * Xuất báo cáo Excel với date range string
 */
 public byte[] generateExcelReport(Long userId, String startDateStr, String endDateStr) throws IOException {
 LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : LocalDate.now().withDayOfMonth(1);
 LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : LocalDate.now();
 
 return exportToExcel(userId, "summary", startDate, endDate);
 }

 /**
 * Xuất báo cáo PDF với date range string
 */
 public byte[] generatePdfReport(Long userId, String startDateStr, String endDateStr) throws IOException, DocumentException {
 LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : LocalDate.now().withDayOfMonth(1);
 LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : LocalDate.now();
 
 return exportToPDF(userId, "summary", startDate, endDate);
 }

 private int exportTransactionsToExcel(Sheet sheet, Long userId, LocalDate startDate, LocalDate endDate, 
 int startRow, CellStyle headerStyle, CellStyle dataStyle) {
 int rowNum = startRow;
 
 // Header giao dịch
 Row headerRow = sheet.createRow(rowNum++);
 String[] headers = {"Ngày", "Loại", "Danh mục", "Mô tả", "Số tiền", "Ví"};
 for (int i = 0; i < headers.length; i++) {
 Cell cell = headerRow.createCell(i);
 cell.setCellValue(headers[i]);
 cell.setCellStyle(headerStyle);
 }
 
 // Dữ liệu giao dịch
 List<Map<String, Object>> transactions = transactionService.getRecentTransactions(userId, 100);
 for (Map<String, Object> tx : transactions) {
 Row dataRow = sheet.createRow(rowNum++);
 dataRow.createCell(0).setCellValue(tx.get("date").toString());
 dataRow.createCell(1).setCellValue(tx.get("type").toString());
 dataRow.createCell(2).setCellValue(tx.get("categoryName") != null ? tx.get("categoryName").toString() : "");
 dataRow.createCell(3).setCellValue(tx.get("description") != null ? tx.get("description").toString() : "");
 dataRow.createCell(4).setCellValue(tx.get("amount").toString());
 dataRow.createCell(5).setCellValue(tx.get("walletName") != null ? tx.get("walletName").toString() : "");
 }
 
 rowNum++; // Dòng trống
 return rowNum;
 }

 private int exportBudgetsToExcel(Sheet sheet, Long userId, LocalDate startDate, LocalDate endDate, 
 int startRow, CellStyle headerStyle, CellStyle dataStyle) {
 int rowNum = startRow;
 
 // Header ngân sách
 Row headerRow = sheet.createRow(rowNum++);
 String[] headers = {"Danh mục", "Ngân sách", "Đã chi", "Còn lại", "Tỷ lệ sử dụng"};
 for (int i = 0; i < headers.length; i++) {
 Cell cell = headerRow.createCell(i);
 cell.setCellValue(headers[i]);
 cell.setCellStyle(headerStyle);
 }
 
 // Dữ liệu ngân sách
 List<Map<String, Object>> budgets = budgetService.getBudgetVsActualByDate(userId, startDate, endDate);
 for (Map<String, Object> budget : budgets) {
 Row dataRow = sheet.createRow(rowNum++);
 
 // Kiểm tra null và xử lý an toàn
 String categoryName = budget.get("categoryName") != null ? budget.get("categoryName").toString() : "Không xác định";
 String budgetAmount = budget.get("budgetAmount") != null ? budget.get("budgetAmount").toString() : "0";
 String actualAmount = budget.get("actualAmount") != null ? budget.get("actualAmount").toString() : "0";
 String remainingAmount = budget.get("remainingAmount") != null ? budget.get("remainingAmount").toString() : "0";
 String usagePercent = budget.get("usagePercent") != null ? budget.get("usagePercent").toString() : "0";
 
 dataRow.createCell(0).setCellValue(categoryName);
 dataRow.createCell(1).setCellValue(budgetAmount);
 dataRow.createCell(2).setCellValue(actualAmount);
 dataRow.createCell(3).setCellValue(remainingAmount);
 dataRow.createCell(4).setCellValue(usagePercent + "%");
 }
 
 rowNum++; // Dòng trống
 return rowNum;
 }

 private int exportGoalsToExcel(Sheet sheet, Long userId, int startRow, CellStyle headerStyle, CellStyle dataStyle) {
 int rowNum = startRow;
 
 // Header mục tiêu
 Row headerRow = sheet.createRow(rowNum++);
 String[] headers = {"Tên mục tiêu", "Mục tiêu", "Hiện tại", "Tiến độ", "Ngày hoàn thành"};
 for (int i = 0; i < headers.length; i++) {
 Cell cell = headerRow.createCell(i);
 cell.setCellValue(headers[i]);
 cell.setCellStyle(headerStyle);
 }
 
 // Dữ liệu mục tiêu
 List<Map<String, Object>> goals = goalService.getGoalProgress(userId);
 for (Map<String, Object> goal : goals) {
 Row dataRow = sheet.createRow(rowNum++);
 
 // Kiểm tra null và xử lý an toàn
 String name = goal.get("name") != null ? goal.get("name").toString() : "Không xác định";
 String targetAmount = goal.get("targetAmount") != null ? goal.get("targetAmount").toString() : "0";
 String currentAmount = goal.get("currentAmount") != null ? goal.get("currentAmount").toString() : "0";
 String progressPercent = goal.get("progressPercent") != null ? goal.get("progressPercent").toString() : "0";
 String targetDate = goal.get("targetDate") != null ? goal.get("targetDate").toString() : "";
 
 dataRow.createCell(0).setCellValue(name);
 dataRow.createCell(1).setCellValue(targetAmount);
 dataRow.createCell(2).setCellValue(currentAmount);
 dataRow.createCell(3).setCellValue(progressPercent + "%");
 dataRow.createCell(4).setCellValue(targetDate);
 }
 
 return rowNum;
 }

 private void exportTransactionsToPDF(Document document, Long userId, LocalDate startDate, LocalDate endDate) throws DocumentException {
 // Tiêu đề phần giao dịch
 com.itextpdf.text.Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
 Paragraph sectionTitle = new Paragraph("GIAO DỊCH", sectionFont);
 document.add(sectionTitle);
 document.add(new Paragraph(" "));
 
 // Tạo bảng giao dịch
 PdfPTable table = new PdfPTable(6);
 table.setWidthPercentage(100);
 
 // Header
 String[] headers = {"Ngày", "Loại", "Danh mục", "Mô tả", "Số tiền", "Ví"};
 for (String header : headers) {
 PdfPCell cell = new PdfPCell(new Phrase(header));
 cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
 cell.setHorizontalAlignment(Element.ALIGN_CENTER);
 table.addCell(cell);
 }
 
 // Dữ liệu
 List<Map<String, Object>> transactions = transactionService.getRecentTransactions(userId, 100);
 for (Map<String, Object> tx : transactions) {
 // Kiểm tra null và xử lý an toàn
 String date = tx.get("date") != null ? tx.get("date").toString() : "";
 String type = tx.get("type") != null ? tx.get("type").toString() : "";
 String categoryName = tx.get("categoryName") != null ? tx.get("categoryName").toString() : "";
 String description = tx.get("description") != null ? tx.get("description").toString() : "";
 String amount = tx.get("amount") != null ? tx.get("amount").toString() : "0";
 String walletName = tx.get("walletName") != null ? tx.get("walletName").toString() : "";
 
 table.addCell(date);
 table.addCell(type);
 table.addCell(categoryName);
 table.addCell(description);
 table.addCell(amount);
 table.addCell(walletName);
 }
 
 document.add(table);
 document.add(new Paragraph(" "));
 }

 private void exportBudgetsToPDF(Document document, Long userId, LocalDate startDate, LocalDate endDate) throws DocumentException {
 // Tiêu đề phần ngân sách
 com.itextpdf.text.Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
 Paragraph sectionTitle = new Paragraph("NGÂN SÁCH", sectionFont);
 document.add(sectionTitle);
 document.add(new Paragraph(" "));
 
 // Tạo bảng ngân sách
 PdfPTable table = new PdfPTable(5);
 table.setWidthPercentage(100);
 
 // Header
 String[] headers = {"Danh mục", "Ngân sách", "Đã chi", "Còn lại", "Tỷ lệ"};
 for (String header : headers) {
 PdfPCell cell = new PdfPCell(new Phrase(header));
 cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
 cell.setHorizontalAlignment(Element.ALIGN_CENTER);
 table.addCell(cell);
 }
 
 // Dữ liệu
 List<Map<String, Object>> budgets = budgetService.getBudgetVsActualByDate(userId, startDate, endDate);
 for (Map<String, Object> budget : budgets) {
 // Kiểm tra null và xử lý an toàn
 String categoryName = budget.get("categoryName") != null ? budget.get("categoryName").toString() : "Không xác định";
 String budgetAmount = budget.get("budgetAmount") != null ? budget.get("budgetAmount").toString() : "0";
 String actualAmount = budget.get("actualAmount") != null ? budget.get("actualAmount").toString() : "0";
 String remainingAmount = budget.get("remainingAmount") != null ? budget.get("remainingAmount").toString() : "0";
 String usagePercent = budget.get("usagePercent") != null ? budget.get("usagePercent").toString() : "0";
 
 table.addCell(categoryName);
 table.addCell(budgetAmount);
 table.addCell(actualAmount);
 table.addCell(remainingAmount);
 table.addCell(usagePercent + "%");
 }
 
 document.add(table);
 document.add(new Paragraph(" "));
 }

 private void exportGoalsToPDF(Document document, Long userId) throws DocumentException {
 // Tiêu đề phần mục tiêu
 com.itextpdf.text.Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
 Paragraph sectionTitle = new Paragraph("MỤC TIÊU TÀI CHÍNH", sectionFont);
 document.add(sectionTitle);
 document.add(new Paragraph(" "));
 
 // Tạo bảng mục tiêu
 PdfPTable table = new PdfPTable(5);
 table.setWidthPercentage(100);
 
 // Header
 String[] headers = {"Tên mục tiêu", "Mục tiêu", "Hiện tại", "Tiến độ", "Ngày hoàn thành"};
 for (String header : headers) {
 PdfPCell cell = new PdfPCell(new Phrase(header));
 cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
 cell.setHorizontalAlignment(Element.ALIGN_CENTER);
 table.addCell(cell);
 }
 
 // Dữ liệu
 List<Map<String, Object>> goals = goalService.getGoalProgress(userId);
 for (Map<String, Object> goal : goals) {
 // Kiểm tra null và xử lý an toàn
 String name = goal.get("name") != null ? goal.get("name").toString() : "Không xác định";
 String targetAmount = goal.get("targetAmount") != null ? goal.get("targetAmount").toString() : "0";
 String currentAmount = goal.get("currentAmount") != null ? goal.get("currentAmount").toString() : "0";
 String progressPercent = goal.get("progressPercent") != null ? goal.get("progressPercent").toString() : "0";
 String targetDate = goal.get("targetDate") != null ? goal.get("targetDate").toString() : "";
 
 table.addCell(name);
 table.addCell(targetAmount);
 table.addCell(currentAmount);
 table.addCell(progressPercent + "%");
 table.addCell(targetDate);
 }
 
 document.add(table);
 }

 private CellStyle createHeaderStyle(Workbook workbook) {
 CellStyle style = workbook.createCellStyle();
 org.apache.poi.ss.usermodel.Font font = workbook.createFont();
 font.setBold(true);
 font.setColor(IndexedColors.WHITE.getIndex());
 style.setFont(font);
 style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
 style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
 style.setAlignment(HorizontalAlignment.CENTER);
 style.setBorderTop(BorderStyle.THIN);
 style.setBorderBottom(BorderStyle.THIN);
 style.setBorderLeft(BorderStyle.THIN);
 style.setBorderRight(BorderStyle.THIN);
 return style;
 }

 private CellStyle createDataStyle(Workbook workbook) {
 CellStyle style = workbook.createCellStyle();
 style.setAlignment(HorizontalAlignment.CENTER);
 style.setBorderTop(BorderStyle.THIN);
 style.setBorderBottom(BorderStyle.THIN);
 style.setBorderLeft(BorderStyle.THIN);
 style.setBorderRight(BorderStyle.THIN);
 return style;
 }
}
