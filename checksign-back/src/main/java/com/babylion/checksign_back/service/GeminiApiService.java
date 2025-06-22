package com.babylion.checksign_back.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@Service
public class GeminiApiService {

    private final WebClient webClient;
    private final String geminiApiKey;
    private static final String PROMPT = """
            You are an AI assistant specialized in legal document analysis. Your task is to thoroughly review, summarize, and evaluate a provided contract from a single image input. Based on the analysis of this image, generate a structured JSON response strictly adhering to the provided format:

            {
              "total_score": integer (0 to 100, with 100 being the most favorable),
              "overview": string (written in Korean, summarizing clearly and concisely the type, key characteristics, and contractual relationship of the provided contract),
              "terms_guide": string (written in Korean, explaining key or potentially unfamiliar legal terms appearing in the contract, separate terms by line break),
              "risk_grade": number (1 to 5, with 1 indicating very low risk and 5 indicating very high risk)
            }

            Your evaluation criteria for the score and risk grade should include clarity of terms, fairness, potential legal liabilities, obligations, financial risks, and overall balance between contracting parties.

            Important:
            * If the contract provided is partially or entirely blank, clearly state that the contract contains empty fields but do NOT rate it as inherently risky solely due to blank fields.
            * Risk grade should reflect the potential disadvantages, hidden problematic clauses, or subtly unfair terms that could negatively affect the contracting parties, not simply the existence of empty spaces.
            * You can use bold style tag '<b></b>'. Use it to emphasize important points in your overview and terms guide.
            * You MUST use <b> tags to highlight terms item. (ex: '<b>용어:</b> 용어 설명'), but at line break, do not use '<br>' but just use real line break. ('\\n')
            * If there's any specified name in the contract (such as Company name, person name, etc), you must reiterate it in the overview.

            Remember:
            * The input will be provided as a single image containing the contract text.
            * All textual outputs (`overview` and `terms_guide`) MUST be written in clear, professional Korean.
            * Ensure that your explanations are precise, informative, and easy to understand for users who may not have extensive legal knowledge.
            * Again, you should write the output in Korean(한국어) only.
            * Blank fields in the contract should not be considered as a risk factor, but you should still mention that the contract contains empty fields.

            Analyze the provided image carefully, and produce your structured response accordingly.
            """;

    public GeminiApiService(WebClient.Builder webClientBuilder,
                            @Value("${gemini.api.url}") String geminiApiUrl,
                            @Value("${gemini.api.key}") String geminiApiKey) {
        this.webClient = webClientBuilder.baseUrl(geminiApiUrl).build();
        this.geminiApiKey = geminiApiKey;
    }

    public Mono<JsonNode> callGeminiApi(byte[] imageData, String mimeType) {
        String base64ImageData = Base64.getEncoder().encodeToString(imageData);

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", PROMPT),
                                        Map.of("inline_data", Map.of(
                                                "mime_type", mimeType,
                                                "data", base64ImageData
                                        ))
                                }
                        )
                },
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );

        return webClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", geminiApiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }
} 