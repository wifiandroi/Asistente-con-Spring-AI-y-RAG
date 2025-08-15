package com.uncledavecode.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/docs")
public class DocsController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:prompts/java21.ai.st")
    private Resource stPromptTemplate;

    public DocsController(ChatClient.Builder chatBuilder, VectorStore vectorStore) {
        // Quitar la memoria: construye el ChatClient sin asesores
        this.chatClient = chatBuilder.build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/chat")
    private String generateResponse(@RequestParam String query) {
        PromptTemplate promptTemplate = new PromptTemplate(stPromptTemplate);
        var promptParameters = new HashMap<String, Object>();
        promptParameters.put("input", query);
        promptParameters.put("documents", String.join("\n", this.findSimilarDocuments(query)));

        var prompt = promptTemplate.create(promptParameters);
        var response = this.chatClient.prompt(prompt).call().chatResponse();
        return response.getResult().getOutput().getText(); // Cambiado a getText()
    }

    private List<String> findSimilarDocuments(String query) {
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(3)
            .build();

        List<org.springframework.ai.document.Document> similarDocuments =
            vectorStore.similaritySearch(request);

        return similarDocuments.stream()
            .map(org.springframework.ai.document.Document::getText)
            .toList();
    }

}
