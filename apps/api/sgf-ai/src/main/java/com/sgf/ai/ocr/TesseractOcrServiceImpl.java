package com.sgf.ai.ocr;

import com.sgf.ai.ocr.dto.OcrResult;
import com.sgf.ai.ocr.dto.OcrResult.OcrMedicationItem;
import com.sgf.ai.ocr.service.PrescriptionOcrService;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementación real del OCR de recetas usando Tesseract.
 * Requiere que el paquete 'tesseract-ocr' esté instalado en el sistema operativo
 * junto con el language pack 'spa' (tesseract-ocr-spa).
 */
@Service
public class TesseractOcrServiceImpl implements PrescriptionOcrService {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrServiceImpl.class);

    // Patrones de extracción para recetas PAMI/ADESFA
    private static final Pattern PRESCRIPTION_ID_PATTERN =
            Pattern.compile("(?:formulario|receta)(?:\\s+(?:nro\\.?|n°|numero))?[:\\s#]+([A-Z0-9\\-]+)|(?:nro\\.?|n°|numero)\\s+receta[:\\s#]+([A-Z0-9\\-]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern BENEFICIARY_PATTERN =
            Pattern.compile("(?:afiliado|credencial|dni|nro\\.?\\s*beneficiario)[:\\s#]+([0-9\\-]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern DOCTOR_LICENSE_PATTERN =
            Pattern.compile("(?:mat\\.?|matrícula|matricula|m\\.p\\.?|m\\.n\\.?)[:\\s#]+([0-9]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern DOCTOR_NAME_PATTERN =
            Pattern.compile("(?:dr\\.?|dra\\.?|med\\.?)[\\s]+([A-ZÁÉÍÓÚ][a-záéíóú]+\\s+[A-ZÁÉÍÓÚ][a-záéíóú]+)", Pattern.UNICODE_CASE);

    private static final Pattern MEDICATION_PATTERN =
            Pattern.compile("^\\d+[xX]?\\s+([A-ZÁÉÍÓÚ][A-Za-záéíóúñ\\s]+(?:\\d+\\s*mg|ml|mcg|g|ui)?)", Pattern.MULTILINE);

    @Value("${app.ai.tesseract.data-path:/usr/share/tesseract-ocr/4.00/tessdata}")
    private String tessDataPath;

    @Value("${app.ai.tesseract.language:spa+eng}")
    private String language;

    @Override
    public OcrResult extractFromImage(InputStream imageStream, String filename) {
        log.info("OCR: Processing prescription image: {}", filename);

        try {
            BufferedImage image = ImageIO.read(imageStream);
            if (image == null) {
                log.warn("OCR: Could not read image from stream for file: {}", filename);
                return OcrResult.failed("Cannot read image");
            }

            String rawText = runTesseract(image);
            log.debug("OCR raw text:\n{}", rawText);

            return parseRawText(rawText);

        } catch (IOException e) {
            log.error("OCR: IO error reading image {}: {}", filename, e.getMessage());
            return OcrResult.failed("IO Error: " + e.getMessage());
        }
    }

    private String runTesseract(BufferedImage image) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage(language);
            tesseract.setPageSegMode(6);  // Assume a single uniform block of text
            tesseract.setOcrEngineMode(1); // LSTM engine
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            log.warn("OCR: Tesseract engine error (is tesseract-ocr installed?): {}", e.getMessage());
            return "";
        }
    }

    private OcrResult parseRawText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return OcrResult.failed(rawText);
        }

        String prescriptionId = extractFirst(PRESCRIPTION_ID_PATTERN, rawText);
        String beneficiaryId  = extractFirst(BENEFICIARY_PATTERN, rawText);
        String doctorLicense  = extractFirst(DOCTOR_LICENSE_PATTERN, rawText);
        String doctorName     = extractFirst(DOCTOR_NAME_PATTERN, rawText);
        List<OcrMedicationItem> medications = extractMedications(rawText);

        // Confianza basada en cuántos campos críticos se encontraron
        int fieldsFound = 0;
        if (prescriptionId != null) fieldsFound++;
        if (beneficiaryId != null) fieldsFound++;
        if (doctorLicense != null) fieldsFound++;
        if (!medications.isEmpty()) fieldsFound++;

        double confidence = fieldsFound / 4.0;
        boolean success   = fieldsFound >= 2;

        log.info("OCR: Parsed fields={}, confidence={:.2f}, success={}",
                fieldsFound, confidence, success);

        return new OcrResult(
                rawText,
                prescriptionId,
                beneficiaryId,
                doctorLicense,
                doctorName,
                medications,
                confidence,
                success
        );
    }

    private String extractFirst(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) {
            return null;
        }
        for (int i = 1; i <= m.groupCount(); i++) {
            String value = m.group(i);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private List<OcrMedicationItem> extractMedications(String text) {
        List<OcrMedicationItem> items = new ArrayList<>();
        Matcher m = MEDICATION_PATTERN.matcher(text);
        while (m.find()) {
            String desc = m.group(1).trim();
            items.add(new OcrMedicationItem(desc, desc, null));
        }
        return items;
    }
}
