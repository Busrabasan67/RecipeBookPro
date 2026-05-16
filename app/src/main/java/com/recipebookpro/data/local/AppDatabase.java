package com.recipebookpro.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.recipebookpro.data.local.converter.LocalizedTextListConverter;
import com.recipebookpro.data.local.converter.MapConverter;
import com.recipebookpro.data.local.converter.StringListConverter;
import com.recipebookpro.data.local.dao.UserDao;
import com.recipebookpro.data.local.entity.UserEntity;

@Database(entities = {UserEntity.class}, version = 5, exportSchema = false)
@TypeConverters({StringListConverter.class, MapConverter.class, LocalizedTextListConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "recipe_book_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
