package net.teamio.gtams.server;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ByteArrayBase64Adapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
	private static final Encoder encoder = Base64.getEncoder();
	private static final Decoder decoder = Base64.getDecoder();

	@Override
	public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(encoder.encodeToString(src));
	}

	@Override
	public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		if(json.isJsonNull()) {
			return null;
		}
		return decoder.decode(json.getAsString());
	}
}