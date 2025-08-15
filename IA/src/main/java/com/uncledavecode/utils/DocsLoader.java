package com.uncledavecode.utils;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document; // <-- AÑADE ESTE IMPORT
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;




@Component
public class DocsLoader {
	
	 private static final Logger log = LoggerFactory.getLogger(DocsLoader.class);

    private final JdbcClient jdbcClient;
    private final VectorStore vectorStore;

    @Value("classpath:docs/jls21.pdf")
    private Resource pdfResource;

    public DocsLoader(JdbcClient jdbcClient, VectorStore vectorStore) {
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadDocs(){
        var count = jdbcClient.sql("select count(*) from vector_store")
                .query(Integer.class)
                .single();

        if(count == 0){
            log.info("Loading docs into vector store");
            var config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopTextLinesToDelete(0)
                            .build()
                    )
                    .withPagesPerDocument(1)
                    .build();

            var pdfReader = new PagePdfDocumentReader(pdfResource, config);
            var result = pdfReader.get().stream()
                    .peek(doc -> log.info("Loading doc: {}", doc.getText())) //Sufrio cambio
                    .toList();

            vectorStore.accept(result);

            log.info("Loaded {} docs into vector store", result.size());
        }
    }
}