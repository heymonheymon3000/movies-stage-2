package com.parrish.android.portfolio.db;

import android.net.Uri;
import android.provider.BaseColumns;

public class FavoriteMovieContract {

    public static final String AUTHORITY  = "com.parrish.android.portfolio.movies";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://"+AUTHORITY);
    public static final String PATH_FAVORITE_MOVIES = "favorite_movies";

    private FavoriteMovieContract(){}

    public static final class FavoriteMovieEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITE_MOVIES).build();

        public static final String TABLE_NAME = "favorite_movie";
        public static final String COLUMN_ID = "movieId";
    }
}
