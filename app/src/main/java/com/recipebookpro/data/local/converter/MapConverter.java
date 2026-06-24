package com.recipebookpro.data.local.converter;

import androidx.room.TypeConverter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static Map<String, List<String>> fromStringToMapList(String value) {
        if (value == null) return new HashMap<>();
        Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
        Map<String, List<String>> map = gson.fromJson(value, type);
        return map != null ? map : new HashMap<>();
    }

    @TypeConverter
    public static String fromMapListToString(Map<String, List<String>> map) {
        return gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, String> fromStringToMapString(String value) {
        if (value == null) return new HashMap<>();
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> map = gson.fromJson(value, type);
        return map != null ? map : new HashMap<>();
    }

    @TypeConverter
    public static String fromMapStringToString(Map<String, String> map) {
        return gson.toJson(map);
    }
}
