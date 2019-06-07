package com.parrish.android.portfolio.db;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.CursorLoader;

public class FavoriteMovieLoader extends CursorLoader {

    private FavoriteMovieLoader(@NonNull Context context, @NonNull Uri uri) {
        super(context, uri, Query.PROJECTION,
                null, null, null);
    }

    public static FavoriteMovieLoader favoriteMovieIds(Context context) {
        return new FavoriteMovieLoader(context,
                FavoriteMovieContract.FavoriteMovieEntry.CONTENT_URI);
    }

    public static FavoriteMovieLoader favoriteMovieByMovieId(Context context,
                                                             long id) {
        return new FavoriteMovieLoader(context,
                ContentUris.withAppendedId(
                        FavoriteMovieContract.FavoriteMovieEntry.CONTENT_URI,
                        id));
    }

    public interface Query {
        String[] PROJECTION = {
                FavoriteMovieContract.FavoriteMovieEntry._ID,
                FavoriteMovieContract.FavoriteMovieEntry.COLUMN_ID
        };

        int _ID = 0;
        int MOVIE_ID = 1;
    }
}
