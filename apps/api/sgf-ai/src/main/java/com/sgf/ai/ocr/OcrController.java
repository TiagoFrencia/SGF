package com.sgf.ai.ocr;

import com.sgf.ai.ocr.dto.OcrResult;
import com.sgf.ai.ocr.service.PrescriptionOcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Endpoint para el procesamiento OCR de recetas médicas.
 * Permite al POS subir una foto y obtener los datos pre-cargados automáticamente.
 */
@RestController
@RequestMapping("/api/ai/ocr")
@Tag(name = "OCR - Recetas", description = "Procesamiento de imágenes de recetas médicas")
public class OcrController {

    private static final Logger log = LoggerFactory.getLogger(OcrController.class);

    private final PrescriptionOcrService ocrService;

    public OcrController(PrescriptionOcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * Procesa una imagen de receta y devuelve los datos extraídos.
     * El POS usa la respuesta para pre-llenar el formulario de venta automáticamente.
     */
    @PostMapping(value = "/prescription", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Leer receta médica por OCR",
        description = "Sube una imagen (JPG/PNG/PDF) de la receta y extrae número de receta, " +
                      "credencial del afiliado, matrícula del médico y medicamentos detectados."
    )
    @PreAuthorize("hasAnyRole('PHARMACIST', 'CASHIER', 'ADMIN')")
    public ResponseEntity<OcrScanResponse> scanPrescription(
            @RequestParam("file") MultipartFile file) {

        log.info("OCR: Received prescription scan request, file={}, size={}KB",
                file.getOriginalFilename(), file.getSize() / 1024);

        try {
            OcrResult result = ocrService.extractFromImage(
                    file.getInputStream(),
                    file.getOriginalFilename()
            );

            return ResponseEntity.ok(OcrScanResponse.from(result));

        } catch (IOException e) {
            log.error("OCR: Failed to read uploaded file: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Response simplificada para el frontend del POS.
     */
    public record OcrScanResponse(
            boolean success,
            double confidence,
            String prescriptionId,
            String beneficiaryId,
            String doctorLicense,
            String doctorName,
            java.util.List<String> detectedMedications,
            String rawText
    ) {
        public static OcrScanResponse from(OcrResult result) {
            return new OcrScanResponse(
                    result.parsedSuccessfully(),
                    result.confidenceScore(),
                    result.prescriptionId(),
                    result.beneficiaryId(),
                    result.doctorLicense(),
                    result.doctorName(),
                    result.medications().stream()
                          .map(OcrResult.OcrMedicationItem::detectedName)
                          .toList(),
                    result.rawText()
            );
        }
    }
}
