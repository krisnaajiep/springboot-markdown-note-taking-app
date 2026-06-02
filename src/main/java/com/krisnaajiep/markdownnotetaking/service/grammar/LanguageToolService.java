package com.krisnaajiep.markdownnotetaking.service.grammar;

import com.krisnaajiep.markdownnotetaking.controller.BadGatewayException;
import com.krisnaajiep.markdownnotetaking.dto.LanguageToolCheckResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LanguageToolService {
    private final RestClient restClient;

    public LanguageToolService(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.languagetool.org/v2").build();
    }

    public LanguageToolCheckResponse check(String text, String language) {

        return restClient.get()
                .uri("/check", uriBuilder -> uriBuilder
                        .queryParam("text", text)
                        .queryParam("language", language)
                        .build())
                .exchange((clientRequest, clientResponse) -> {
                    MediaType contentType = clientResponse.getHeaders().getContentType();
                    LanguageToolCheckResponse response = clientResponse.bodyTo(LanguageToolCheckResponse.class);
                    boolean isBadGateway = !clientResponse.getStatusCode().is2xxSuccessful()
                            || contentType == null
                            || !contentType.isCompatibleWith(MediaType.APPLICATION_JSON)
                            || response == null;

                    if (isBadGateway) {
                        throw new BadGatewayException();
                    }

                    return response;
                });
    }
}
