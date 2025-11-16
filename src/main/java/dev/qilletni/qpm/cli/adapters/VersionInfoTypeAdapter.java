package dev.qilletni.qpm.cli.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.qilletni.qpm.cli.models.VersionInfo;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Gson TypeAdapter for {@link VersionInfo} that handles JSON serialization/deserialization
 * including proper parsing of Instant fields and optional dependencies map.
 */
public class VersionInfoTypeAdapter extends TypeAdapter<VersionInfo> {

    @Override
    public void write(JsonWriter out, VersionInfo value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginObject();
        out.name("version").value(value.version());
        out.name("integrity").value(value.integrity());
        out.name("size").value(value.size());

        if (value.uploadedAt() != null) {
            out.name("uploadedAt").value(value.uploadedAt().toString());
        }

        if (value.dependencies() != null && !value.dependencies().isEmpty()) {
            out.name("dependencies");
            out.beginObject();
            for (Map.Entry<String, String> entry : value.dependencies().entrySet()) {
                out.name(entry.getKey()).value(entry.getValue());
            }
            out.endObject();
        }

        out.endObject();
    }

    @Override
    public VersionInfo read(JsonReader in) throws IOException {
        String version = null;
        String integrity = null;
        long size = 0;
        Instant uploadedAt = null;
        Map<String, String> dependencies = new HashMap<>();

        in.beginObject();
        while (in.hasNext()) {
            String fieldName = in.nextName();

            switch (fieldName) {
                case "version" -> version = in.nextString();
                case "integrity" -> integrity = in.nextString();
                case "size" -> size = in.nextLong();
                case "uploadedAt" -> {
                    if (in.peek() != JsonToken.NULL) {
                        String uploadedAtStr = in.nextString();
                        uploadedAt = Instant.parse(uploadedAtStr);
                    } else {
                        in.nextNull();
                    }
                }
                case "dependencies" -> {
                    if (in.peek() != JsonToken.NULL) {
                        in.beginObject();
                        while (in.hasNext()) {
                            String depName = in.nextName();
                            String depVersion = in.nextString();
                            dependencies.put(depName, depVersion);
                        }
                        in.endObject();
                    } else {
                        in.nextNull();
                    }
                }
                default -> in.skipValue();
            }
        }
        in.endObject();

        return new VersionInfo(version, integrity, size, uploadedAt, dependencies);
    }
}