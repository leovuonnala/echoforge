package net.vuonnala;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

public class MessageValidator {
    private final JsonSchema schema;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public MessageValidator(String schemaPath) throws IOException {
        // 1) Load the JSON Schema as a JsonNode
        String schemaContent = Files.readString(Paths.get(schemaPath));
        JsonNode schemaNode = MAPPER.readTree(schemaContent);

        // 2) Create the JSON schema using the factory
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        this.schema = factory.getSchema(schemaNode);
    }

    /**
     * Validate the given JSON string against the schema.
     * @param jsonContent The input JSON string to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validate(String jsonContent) {
        try {
            // 3) Parse the input JSON
            JsonNode inputNode = MAPPER.readTree(jsonContent);

            // 4) Validate the input JSON against the schema
            Set<ValidationMessage> errors = schema.validate(inputNode);

            // 5) If there are validation errors, throw an exception or handle them
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
