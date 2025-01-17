package gg.moonflower.etched.common.sound.download;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.URISyntaxException;

@FunctionalInterface
public interface SourceRequest<T> {

    T process(JsonObject json) throws IOException, JsonParseException, URISyntaxException;
}
