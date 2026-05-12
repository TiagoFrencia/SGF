package com.sgf.app.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.sgf.ai.ocr.TesseractOcrServiceImpl;
import com.sgf.ai.ocr.dto.OcrResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Tests de la lógica de parseo OCR (sin necesitar Tesseract instalado).
 * Inyecta texto crudo simulando la salida del motor Tesseract.
 */
class PrescriptionOcrParserTest {

    private TesseractOcrServiceImpl ocrService;

    // Simula una receta PAMI típica tal como la leería Tesseract
    private static final String SAMPLE_PAMI_PRESCRIPTION = """
            OBRA SOCIAL: PAMI
            Formulario Nro: REC-20260510-7890
            
            Afiliado DNI: 28123456
            Plan: GENERAL
            
            Prescriptor: DR. Carlos Rodriguez
            Mat. 56789
            
            Diagnóstico: Hipertensión arterial
            
            1x ENALAPRIL 10mg - Cantidad: 2 cajas
            2x ASPIRINA 100mg - Cantidad: 1 caja
            
            Fecha: 10/05/2026
            Firma y sello
            """;

    private static final String SAMPLE_PAMI_PRESCRIPTION_ES = """
            INSTITUTO NACIONAL DE SERVICIOS SOCIALES
            PARA JUBILADOS Y PENSIONADOS - PAMI
            
            Receta Nro: 2026-ABC-001
            Credencial: 12-33445566-7
            
            Dra. Maria Lopez
            Matricula: 99321
            
            1x METFORMINA 850mg
            """;

    @BeforeEach
    void setUp() {
        ocrService = new TesseractOcrServiceImpl();
        // Inyecta valores de config sin necesitar Spring context
        ReflectionTestUtils.setField(ocrService, "tessDataPath", "/usr/share/tesseract-ocr/4.00/tessdata");
        ReflectionTestUtils.setField(ocrService, "language", "spa+eng");
    }

    @Test
    void shouldExtractPrescriptionIdFromPamiRecipe() {
        OcrResult result = simulateOcrParsing(SAMPLE_PAMI_PRESCRIPTION);

        assertThat(result.prescriptionId()).isEqualTo("REC-20260510-7890");
    }

    @Test
    void shouldExtractBeneficiaryIdFromPamiRecipe() {
        OcrResult result = simulateOcrParsing(SAMPLE_PAMI_PRESCRIPTION);

        assertThat(result.beneficiaryId()).isEqualTo("28123456");
    }

    @Test
    void shouldExtractDoctorLicenseFromPamiRecipe() {
        OcrResult result = simulateOcrParsing(SAMPLE_PAMI_PRESCRIPTION);

        assertThat(result.doctorLicense()).isEqualTo("56789");
    }

    @Test
    void shouldDetectMedicationsInRecipe() {
        OcrResult result = simulateOcrParsing(SAMPLE_PAMI_PRESCRIPTION);

        assertThat(result.medications()).isNotEmpty();
        assertThat(result.medications())
            .anyMatch(m -> m.detectedName().contains("ENALAPRIL"));
    }

    @Test
    void shouldCalculateHighConfidenceWhenAllFieldsFound() {
        OcrResult result = simulateOcrParsing(SAMPLE_PAMI_PRESCRIPTION);

        assertThat(result.confidenceScore()).isGreaterThan(0.5);
        assertThat(result.parsedSuccessfully()).isTrue();
    }

    @Test
    void shouldHandleAlternativeMatriculaFormat() {
        OcrResult result = simulateOcrParsing(SAMPLE_PAMI_PRESCRIPTION_ES);

        assertThat(result.prescriptionId()).isEqualTo("2026-ABC-001");
        assertThat(result.doctorLicense()).isEqualTo("99321");
    }

    @Test
    void shouldReturnFailedResultForEmptyText() {
        OcrResult result = simulateOcrParsing("");

        assertThat(result.parsedSuccessfully()).isFalse();
        assertThat(result.confidenceScore()).isEqualTo(0.0);
    }

    /**
     * Simula el output del parser de Tesseract inyectando texto crudo directamente.
     * Esto permite validar la lógica de Regex sin depender del motor OCR.
     */
    private OcrResult simulateOcrParsing(String rawText) {
        // Usa el método público de la interfaz con un stream que simula salida Tesseract
        // Aprovechamos que ImageIO.read devuelve null para streams de texto,
        // lo que activa el path de fallback - aquí preferimos testear parseRawText via reflexión
        return (OcrResult) ReflectionTestUtils.invokeMethod(ocrService, "parseRawText", rawText);
    }
}
