package com.sgf.integrations.vademecum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Checks for drug interactions between active ingredients in a prescription or cart.
 *
 * Combines data from both AlfaBeta and Kairos vademecums:
 * - AlfaBeta provides known interaction pairs with ATC classification
 * - Kairos adds severity levels and clinical recommendations
 *
 * Typical use case: when a patient brings multiple prescriptions, the pharmacist
 * scans all products and this service warns about potential interactions before dispensing.
 */
@Service
public class DrugInteractionService {

    private static final Logger log = LoggerFactory.getLogger(DrugInteractionService.class);

    private final AlfaBetaConnector alfaBetaConnector;
    private final KairosConnector kairosConnector;

    /**
     * In-memory cache of known severe interactions for offline/quick checks.
     * In production, this would be populated from a DB table synced from vademécums.
     */
    private static final Map<String, Set<String>> KNOWN_SEVERE_INTERACTIONS = new HashMap<>();

    static {
        // Example well-known interactions (would be loaded from DB in prod)
        addInteraction("WARFARINA", "ASPIRINA");
        addInteraction("WARFARINA", "IBUPROFENO");
        addInteraction("METOTREXATO", "TRIMETOPRIM");
        addInteraction("INHIBIDOR_MAO", "ISRS");
        addInteraction("SIMVASTATINA", "CLARITROMICINA");
        addInteraction("ATORVASTATINA", "CLARITROMICINA");
        addInteraction("METFORMINA", "CONTRASTE_YODADO");
        addInteraction("ENALAPRIL", "ESPIRONOLACTONA");
        addInteraction("LITIO", "DIURETICOS_TIAZIDICOS");
        addInteraction("DIGOXINA", "AMIODARONA");
        addInteraction("SILDENAFILO", "NITRATOS");
        addInteraction("OMEPRAZOL", "CLOPIDOGREL");
        addInteraction("WARFARINA", "PARACETAMOL");
    }

    private static void addInteraction(String a, String b) {
        KNOWN_SEVERE_INTERACTIONS
                .computeIfAbsent(normalize(a), k -> new HashSet<>())
                .add(normalize(b));
        KNOWN_SEVERE_INTERACTIONS
                .computeIfAbsent(normalize(b), k -> new HashSet<>())
                .add(normalize(a));
    }

    public DrugInteractionService(AlfaBetaConnector alfaBetaConnector,
                                   KairosConnector kairosConnector) {
        this.alfaBetaConnector = alfaBetaConnector;
        this.kairosConnector = kairosConnector;
    }

    /**
     * Check for interactions among a set of products (by GTIN).
     * Merges results from local cache and Kairos online API.
     *
     * @param productGtins List of GTINs currently in the cart/prescription
     * @return List of interaction warnings, empty if safe
     */
    public InteractionResult checkInteractions(List<String> productGtins) {
        if (productGtins == null || productGtins.size() < 2) {
            return new InteractionResult(List.of(), InteractionRiskLevel.NONE);
        }

        // Resolve GTINs to active ingredients
        List<ProductWithIngredients> products = new ArrayList<>();
        for (String gtin : productGtins) {
            ProductWithIngredients pwi = resolveIngredients(gtin);
            if (pwi != null) {
                products.add(pwi);
            }
        }

        if (products.size() < 2) {
            return new InteractionResult(List.of(), InteractionRiskLevel.NONE);
        }

        List<InteractionWarning> warnings = new ArrayList<>();
        InteractionRiskLevel maxRisk = InteractionRiskLevel.NONE;

        // Pairwise ingredient check
        List<String> allIngredients = new ArrayList<>();
        for (ProductWithIngredients product : products) {
            allIngredients.addAll(product.activeIngredients());
        }

        for (int i = 0; i < allIngredients.size(); i++) {
            for (int j = i + 1; j < allIngredients.size(); j++) {
                String ingA = normalize(allIngredients.get(i));
                String ingB = normalize(allIngredients.get(j));

                if (ingA.equals(ingB)) continue;

                // Check local cache first
                Set<String> interactions = KNOWN_SEVERE_INTERACTIONS.get(ingA);
                if (interactions != null && interactions.contains(ingB)) {
                    warnings.add(new InteractionWarning(
                            ingA, ingB,
                            InteractionRiskLevel.SEVERE,
                            "Interacción conocida entre " + ingA + " y " + ingB,
                            "Consultar al médico antes de dispensar. Evaluar ajuste de dosis o alternativa terapéutica."
                    ));
                    maxRisk = InteractionRiskLevel.SEVERE;
                }
            }
        }

        // Try online check via Kairos for more detailed info
        if (!allIngredients.isEmpty()) {
            try {
                List<KairosConnector.DrugInteraction> onlineInteractions =
                        kairosConnector.checkInteractions(allIngredients);
                for (var interaction : onlineInteractions) {
                    InteractionRiskLevel level = mapSeverity(interaction.severity());
                    warnings.add(new InteractionWarning(
                            interaction.ingredientA(), interaction.ingredientB(),
                            level, interaction.description(), interaction.recommendation()
                    ));
                    if (level.compareTo(maxRisk) > 0) {
                        maxRisk = level;
                    }
                }
            } catch (Exception e) {
                log.warn("Online interaction check failed, using local cache only: {}", e.getMessage());
            }
        }

        return new InteractionResult(warnings, maxRisk);
    }

    /**
     * Quick check: can these two products be taken together?
     */
    public boolean canBeTakenTogether(String gtin1, String gtin2) {
        InteractionResult result = checkInteractions(List.of(gtin1, gtin2));
        return result.maxRisk() == InteractionRiskLevel.NONE;
    }

    private ProductWithIngredients resolveIngredients(String gtin) {
        try {
            // Try AlfaBeta first
            var alfabeta = alfaBetaConnector.findByGtin(gtin);
            if (alfabeta.isPresent()) {
                var p = alfabeta.get();
                return new ProductWithIngredients(
                        p.gtin(), p.commercialName(),
                        p.activeIngredient() != null ? List.of(p.activeIngredient()) : List.of()
                );
            }
            // Try Kairos
            var kairos = kairosConnector.findByGtin(gtin);
            if (kairos.isPresent()) {
                var p = kairos.get();
                return new ProductWithIngredients(
                        p.gtin(), p.commercialName(),
                        p.activeIngredient() != null ? List.of(p.activeIngredient()) : List.of()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to resolve ingredients for {}: {}", gtin, e.getMessage());
        }
        return null;
    }

    private InteractionRiskLevel mapSeverity(KairosConnector.DrugInteractionSeverity severity) {
        return switch (severity) {
            case MILD -> InteractionRiskLevel.LOW;
            case MODERATE -> InteractionRiskLevel.MODERATE;
            case SEVERE -> InteractionRiskLevel.SEVERE;
            case CONTRAINDICATED -> InteractionRiskLevel.CONTRAINDICATED;
        };
    }

    private static String normalize(String ingredient) {
        return ingredient.toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("/", "_");
    }

    // --- Types ---

    private record ProductWithIngredients(
            String gtin, String name, List<String> activeIngredients
    ) {}

    public enum InteractionRiskLevel {
        NONE,           // No interaction detected
        LOW,            // Minor interaction, usually no action needed
        MODERATE,       // May require monitoring or dose adjustment
        SEVERE,         // Potentially serious — consult prescriber
        CONTRAINDICATED // Do not dispense together — call prescriber immediately
    }

    public record InteractionWarning(
            String ingredientA,
            String ingredientB,
            InteractionRiskLevel riskLevel,
            String description,
            String recommendation
    ) {}

    public record InteractionResult(
            List<InteractionWarning> warnings,
            InteractionRiskLevel maxRisk
    ) {
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public boolean isSafe() {
            return maxRisk == InteractionRiskLevel.NONE;
        }

        public boolean requiresPrescriberConsult() {
            return maxRisk == InteractionRiskLevel.SEVERE
                    || maxRisk == InteractionRiskLevel.CONTRAINDICATED;
        }

        public int warningCount() {
            return warnings.size();
        }
    }
}