package com.example.smart_booking_system.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    public byte[] generateReport(String templateName, Map<String, Object> parameters, List<?> data, String format) throws Exception {

        // SỬA Ở ĐÂY: Dùng ClassPathResource.getInputStream() để đọc file trong cả file .jar
        InputStream reportStream = new ClassPathResource("reports/" + templateName + ".jrxml").getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // 2. Map dữ liệu vào báo cáo
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        // 3. Xuất file tùy theo định dạng
        if ("pdf".equalsIgnoreCase(format)) {
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } else if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            return exportToExcel(jasperPrint);
        } else {
            throw new IllegalArgumentException("Định dạng báo cáo không được hỗ trợ: " + format);
        }
    }

    private byte[] exportToExcel(JasperPrint jasperPrint) throws JRException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();

        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        configuration.setDetectCellType(true);
        configuration.setCollapseRowSpan(false);
        exporter.setConfiguration(configuration);

        exporter.exportReport();
        return outputStream.toByteArray();
    }
}