package com.recipebookpro.data.local.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.recipebookpro.domain.model.LocalizedText;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LocalizedTextListConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<LocalizedText> fromString(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<LocalizedText>>() {}.getType();
        List<LocalizedText> list = gson.fromJson(value, type);
        return list != null ? list : new ArrayList<>();
    }

    @TypeConverter
    public static String toString(List<LocalizedText> list) {
        return gson.toJson(list != null ? list : new ArrayList<>());
    }
}
