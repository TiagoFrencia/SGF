package com.sgf.integrations.vademecum.web;

import com.sgf.integrations.vademecum.DrugInteractionService;
import com.sgf.integrations.vademecum.DrugInteractionService.InteractionResult;
import com.sgf.integrations.vademecum.GenericSuggestionService;
import com.sgf.integrations.vademecum.GenericSuggestionService.GenericAlternative;
import com.sgf.integrations.vademecum.GenericSuggestionService.SuggestionResult;
import com.sgf.integrations.vademecum.VademecumSyncScheduler;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified REST controller for vademecum operations.
 *
 * Endpoints:
 * - GET  /vademecum/interactions — Check drug interactions for a list of GTINs
 * - GET  /vademecum/interactions/{gtin1}/{gtin2} — Pairwise interaction check
 * - GET  /vademecum/alternatives/{gtin} — Generic substitution suggestions
 * - GET  /vademecum/cheapest/{ifa} — Cheapest alternative for an active ingredient
 * - GET  /vademecum/equivalent/{gtin1}/{gtin2} — Therapeutic equivalence check
 * - POST /vademecum/sync — Trigger manual sync
 */
@RestController
@RequestMapping("/vademecum")
public class VademecumController {

    private final DrugInteractionService interactionService;
    private final GenericSuggestionService suggestionService;
    private final VademecumSyncScheduler syncScheduler;

    public VademecumController(DrugInteractionService interactionService,
                                GenericSuggestionService suggestionService,
                                VademecumSyncScheduler syncScheduler) {
        this.interactionService = interactionService;
        this.suggestionService = suggestionService;
        this.syncScheduler = syncScheduler;
    }

    /**
     * Check drug interactions for a cart or prescription.
     */
    @PostMapping("/interactions")
    public ResponseEntity<InteractionCheckResponse> checkInteractions(
            @RequestBody InteractionCheckRequest request) {
        InteractionResult result = interactionService.checkInteractions(request.gtins());
        return ResponseEntity.ok(InteractionCheckResponse.from(result));
    }

    /**
     * Quick pairwise check: can these two be taken together?
     */
    @GetMapping("/interactions/{gtin1}/{gtin2}")
    public ResponseEntity<PairwiseCheckResponse> checkPairwise(
            @PathVariable String gtin1, @PathVariable String gtin2) {
        boolean safe = interactionService.canBeTakenTogether(gtin1, gtin2);
        InteractionResult result = interactionService.checkInteractions(List.of(gtin1, gtin2));
        return ResponseEntity.ok(new PairwiseCheckResponse(gtin1, gtin2, safe, InteractionCheckResponse.from(result)));
    }

    /**
     * Find generic alternatives for a prescribed product.
     */
    @GetMapping("/alternatives/{gtin}")
    public ResponseEntity<AlternativesResponse> findAlternatives(
            @PathVariable String gtin,
            @RequestParam(defaultValue = "false") boolean bioequivalentOnly) {
        SuggestionResult result = suggestionService.findAlternatives(gtin);
        List<GenericAlternative> alternatives = bioequivalentOnly
                ? result.bioequivalentOnly()
                : result.alternatives();
        return ResponseEntity.ok(new AlternativesResponse(
                result.prescribedGtin(),
                result.prescribedName(),
                alternatives,
                result.message(),
                alternatives.size()
        ));
    }

    /**
     * Find cheapest alternative for an active ingredient.
     */
    @GetMapping("/cheapest/{activeIngredient}")
    public ResponseEntity<CheapestResponse> findCheapest(
            @PathVariable String activeIngredient) {
        GenericAlternative cheapest = suggestionService.findCheapest(activeIngredient);
        if (cheapest == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new CheapestResponse(activeIngredient, cheapest));
    }

    /**
     * Check if two products are therapeutically equivalent.
     */
    @GetMapping("/equivalent/{gtin1}/{gtin2}")
    public ResponseEntity<EquivalenceResponse> checkEquivalence(
            @PathVariable String gtin1, @PathVariable String gtin2) {
        boolean equivalent = suggestionService.areTherapeuticallyEquivalent(gtin1, gtin2);
        return ResponseEntity.ok(new EquivalenceResponse(gtin1, gtin2, equivalent));
    }

    /**
     * Trigger manual vademecum synchronization.
     */
    @PostMapping("/sync")
    public ResponseEntity<SyncResponse> triggerSync() {
        syncScheduler.syncNow();
        return ResponseEntity.accepted().body(new SyncResponse("ACCEPTED",
                "Sincronización de vademécums iniciada"));
    }

    // --- Request/Response DTOs ---

    public record InteractionCheckRequest(List<String> gtins) {}

    public record InteractionCheckResponse(
            List<InteractionWarningResponse> warnings,
            String riskLevel,
            boolean safe,
            boolean requiresConsult,
            int warningCount
    ) {
        public static InteractionCheckResponse from(InteractionResult result) {
            return new InteractionCheckResponse(
                    result.warnings().stream()
                            .map(w -> new InteractionWarningResponse(
                                    w.ingredientA(), w.ingredientB(),
                                    w.riskLevel().name(),
                                    w.description(), w.recommendation()
                            )).toList(),
                    result.maxRisk().name(),
                    result.isSafe(),
                    result.requiresPrescriberConsult(),
                    result.warningCount()
            );
        }
    }

    public record InteractionWarningResponse(
            String ingredientA,
            String ingredientB,
            String riskLevel,
            String description,
            String recommendation
    ) {}

    public record PairwiseCheckResponse(
            String gtin1, String gtin2,
            boolean safe,
            InteractionCheckResponse details
    ) {}

    public record AlternativesResponse(
            String prescribedGtin,
            String prescribedName,
            List<GenericAlternative> alternatives,
            String message,
            int totalFound
    ) {}

    public record CheapestResponse(
            String activeIngredient,
            GenericAlternative cheapest
    ) {}

    public record EquivalenceResponse(
            String gtin1, String gtin2, boolean therapeuticallyEquivalent
    ) {}

    public record SyncResponse(String status, String message) {}
}