package InfrastructureManager.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class FileParser {

    public static String YAMLtoJSON(String pathToYAML) {
        String pathToJSON = pathToYAML.replaceAll("\\.yaml", "\\.json");
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        try {
            Object aux = yamlReader.readValue(new File(pathToYAML), Object.class);
            ObjectMapper jsonWriter = new ObjectMapper();
            jsonWriter.writeValue(new File(pathToJSON), aux);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pathToJSON;
    }
}
