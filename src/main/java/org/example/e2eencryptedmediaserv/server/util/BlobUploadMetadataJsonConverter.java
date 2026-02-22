package org.example.e2eencryptedmediaserv.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.e2eencryptedmediaserv.server.model.BlobUploadMetadata;


//@Component
@Converter
public class BlobUploadMetadataJsonConverter implements AttributeConverter<BlobUploadMetadata, String> {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

//    private static ObjectMapper createConfiguredMapper() {
//        ObjectMapper om = new ObjectMapper();
//        om.registerModule(new JavaTimeModule());
//        // om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);   // ← раскомментируй, если хочешь строгий ISO без миллисекунд как числа
//        return om;
//    }

    @Override
    public String convertToDatabaseColumn(BlobUploadMetadata attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Cannot serialize BlobUploadMetadata to JSON", e);
        }
    }

    @Override
    public BlobUploadMetadata convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return mapper.readValue(dbData, BlobUploadMetadata.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot deserialize JSON to BlobUploadMetadata", e);
        }
    }
}
