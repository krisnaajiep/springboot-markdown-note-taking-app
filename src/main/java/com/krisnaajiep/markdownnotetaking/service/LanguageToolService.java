package com.krisnaajiep.markdownnotetaking.service;

import com.krisnaajiep.markdownnotetaking.controller.BadGatewayException;
import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;
import com.krisnaajiep.markdownnotetaking.dto.LanguageToolCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class LanguageToolService implements GrammarCheckService {
    private final RestClient restClient;

    @Override
    public GrammarCheckResponse check(String text, String language) {
        return restClient.get()
                .uri("https://api.languagetool.org/v2/check", uriBuilder -> uriBuilder
                        .queryParam("text", text)
                        .queryParam("language", language)
                        .build())
                .exchange(((clientRequest, clientResponse) -> {
                    if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                        throw new BadGatewayException("LanguageTool API is currently unavailable.");
                    } else {
                        return convertResponse(clientResponse);
                    }
                }));
    }

    private GrammarCheckResponse convertResponse(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
        LanguageToolCheckResponse languageToolCheckResponse = response.bodyTo(LanguageToolCheckResponse.class);

        if (languageToolCheckResponse == null) {
            throw new BadGatewayException("LanguageTool API is currently unavailable.");
        }

        return GrammarCheckResponse.builder()
                .software(languageToolCheckResponse.getSoftware().getName())
                .language(languageToolCheckResponse.getLanguage().getName())
                .results(languageToolCheckResponse.getMatches().stream()
                        .map(match -> GrammarCheckResponse.Result.builder()
                                .message(match.getMessage())
                                .suggestions(match.getReplacements().stream()
                                        .map(replacement -> replacement.get("value").toString())
                                        .toList())
                                .offset(match.getOffset())
                                .length(match.getLength())
                                .context(match.getContext())
                                .build())
                        .toList())
                .build();
    }
}
