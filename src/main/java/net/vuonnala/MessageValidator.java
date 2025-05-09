package net.vuonnala;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class MessageValidator {
    private JsonSchema schema;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public MessageValidator(InputStream schemaStream) throws IOException {
        loadSchema(schemaStream);
    }


    public void loadSchema(InputStream schemaStream) throws IOException {
        JsonNode schemaNode = MAPPER.readTree(schemaStream);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        this.schema = factory.getSchema(schemaNode);
    }

    /**
     * Validate the given JSON string against the schema.
     *
     * @param jsonContent The input JSON string to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validate(String jsonContent) {
        try {
            JsonNode inputNode = MAPPER.readTree(jsonContent);
            Set<ValidationMessage> errors = schema.validate(inputNode);

            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder("JSON validation error(s):\n");
                for (ValidationMessage error : errors) {
                    sb.append(" - ").append(error.getMessage()).append("\n");
                }
                throw new IllegalArgumentException(sb.toString());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse JSON content.", e);
        }
    }
}

