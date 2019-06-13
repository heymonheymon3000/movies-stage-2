package com.parrish.android.portfolio.activities.movie;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import android.support.v4.app.LoaderManager;

import com.parrish.android.portfolio.BuildConfig;
import com.parrish.android.portfolio.R;
import com.parrish.android.portfolio.activities.movie.details.MovieDetailsActivity;
import com.parrish.android.portfolio.activities.settings.SettingsActivity;
import com.parrish.android.portfolio.adaptors.movie.MovieAdaptor;

import android.support.v4.util.Pair;

import com.parrish.android.portfolio.db.FavoriteMovieContract;
import com.parrish.android.portfolio.db.FavoriteMovieLoader;
import com.parrish.android.portfolio.helpers.Helper;
import com.parrish.android.portfolio.interfaces.MovieService;
import com.parrish.android.portfolio.models.movie.MovieResponse;
import com.parrish.android.portfolio.models.movie.Result;
import com.parrish.android.portfolio.models.movie.details.MovieDetailsResponse;
import com.parrish.android.portfolio.network.ApiUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MovieActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        MovieAdaptor.MovieClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private final static String TAG = MovieActivity.class.getSimpleName();
    private Cursor mCursor;

    @BindView(R.id.rv_movies)
    public RecyclerView recyclerView;

    private MovieAdaptor movieAdaptor;

    private String sortOrder;

    private final static String RESULT_CACHE_KEY = "RESULT_CACHE_KEY";
    private final static String SORT_ORDER_KEY = "SORT_ORDER_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie);
        ButterKnife.bind(this);
        setupSharedPreferences();
        setupRecyclerView();

        if(savedInstanceState == null || !savedInstanceState.containsKey(RESULT_CACHE_KEY)) {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            sortOrder = sharedPreferences.getString(getString(R.string.pref_sort_order_key), getString(R.string.pref_sort_order_default));
            if(sortOrder.equals(getString(R.string.pref_sort_order_popular_value))) {
                loadMovies();
            } else if(sortOrder.equals(getString(R.string.pref_sort_order_top_rated_value))) {
                loadMovies();
            } else if(sortOrder.equals(getString(R.string.pref_sort_order_favorites_value))) {
                LoaderManager.getInstance(this).initLoader(0, null, this);
            }
        } else {
            sortOrder = savedInstanceState.getString(SORT_ORDER_KEY);
            loadMoviesFromCache(savedInstanceState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sort_order_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            Intent startSettingActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getString(R.string.pref_sort_order_key))) {
            sortOrder = sharedPreferences.getString(getString(R.string.pref_sort_order_key), getString(R.string.pref_sort_order_default));
            if(sortOrder.equals(getString(R.string.pref_sort_order_popular_value))) {
                loadMovies();
            } else if(sortOrder.equals(getString(R.string.pref_sort_order_top_rated_value))) {
                loadMovies();
            } else if(sortOrder.equals(getString(R.string.pref_sort_order_favorites_value))) {
                LoaderManager.getInstance(this).restartLoader(0, null, this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArray(RESULT_CACHE_KEY, movieAdaptor.getResults());
        outState.putString(SORT_ORDER_KEY, sortOrder);
        super.onSaveInstanceState(outState);
    }

    private void setupRecyclerView() {
        int numberOfColumns = Helper.calculateNoOfColumns(this, 100);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, numberOfColumns);
        movieAdaptor = new MovieAdaptor(this, this);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(movieAdaptor);
    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        loadSortOrderFromSharedPreferences(sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void loadSortOrderFromSharedPreferences(SharedPreferences sharedPreferences) {
        sortOrder = sharedPreferences.getString(getString(R.string.pref_sort_order_key), getString(R.string.pref_sort_order_default));
    }

    private void loadMovies() {
        MovieService movieService = ApiUtils.getMovieService();
        movieService.getMovies(sortOrder, BuildConfig.MOVIE_API_KEY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<MovieResponse>() {
                List<Result> results = new ArrayList<>();

                @Override
                public void onSubscribe(Disposable d) {}

                @Override
                public void onNext(MovieResponse movieResponse) {
                    results = movieResponse.getResults();
                }

                @Override
                public void onError(Throwable e) {
                    Toast.makeText(MovieActivity.this,
                        getString(R.string.no_internet_connection),
                        Toast.LENGTH_LONG).show();
                }

                @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
                @Override
                public void onComplete() {
                    movieAdaptor.setResults(results.toArray(new Result[results.size()]));
                    setTitle(getTitle());
                }

                private String getTitle() {
                    if(sortOrder.equals(getString(R.string.pref_sort_order_popular_value))) {
                        return getString(R.string.pref_sort_order_popular_label);
                    } else if(sortOrder.equals(getString(R.string.pref_sort_order_top_rated_value))) {
                        return getString(R.string.pref_sort_order_top_rated_label);
                    } else {
                        return getString(R.string.pref_sort_order_favorites_label);
                    }
                }
            });
    }

    private void loadMoviesFromCache(Bundle savedInstanceState) {
        (new Thread() {
            public void run() {
                movieAdaptor.setResults((Result[])
                        savedInstanceState.getParcelableArray(RESULT_CACHE_KEY));
                sortOrder = savedInstanceState.getString(SORT_ORDER_KEY);

                if(sortOrder != null) {
                    if(sortOrder.equals(getString(R.string.pref_sort_order_popular_value))) {
                        setTitle(getString(R.string.pref_sort_order_popular_label));
                    } else if (sortOrder.equals(getString(R.string.pref_sort_order_top_rated_value))) {
                        setTitle(getString(R.string.pref_sort_order_top_rated_label));
                    } else {
                        setTitle(getString(R.string.pref_sort_order_favorites_label));
                    }
                } else {
                    setTitle(getString(R.string.pref_sort_order_default));
                }

                Animation animFadeIn = AnimationUtils.loadAnimation(
                        MovieActivity.this, R.anim.fade_in);
                View view = findViewById(R.id.container);
                view.startAnimation(animFadeIn);
            }
        }).start();
    }

    @Override
    public void onMovieClickListener(Result result, View view) {
        Pair<View, String> p1 = Pair.create(view, result.getTitle());
        @SuppressWarnings("unchecked")
        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, p1);
        Intent startMovieDetailsActivity = new Intent(this, MovieDetailsActivity.class);
        startMovieDetailsActivity.putExtra(Intent.EXTRA_TEXT, result);
        startActivity(startMovieDetailsActivity, options.toBundle());
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        return FavoriteMovieLoader.favoriteMovieIds(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if(sortOrder.equals(getString(R.string.pref_sort_order_top_rated_value)) || sortOrder.equals(getString(R.string.pref_sort_order_popular_value)) ) {
            return;
        }

        mCursor = cursor;
        List<Integer> favoriteMovieIds = new ArrayList<>();

        mCursor.moveToFirst();
        while (!mCursor.isAfterLast()) {
            favoriteMovieIds.add(mCursor.
                    getInt(mCursor.getColumnIndex(
                            FavoriteMovieContract.
                                    FavoriteMovieEntry.COLUMN_ID)));
            mCursor.moveToNext();
        }

        MovieService movieService = ApiUtils.getMovieService();
        List<Result> results = new ArrayList<>();
        for(Integer id : favoriteMovieIds) {
            movieService.getMovieDetails(id, BuildConfig.MOVIE_API_KEY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<MovieDetailsResponse>() {
                private Result result = null;

                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onNext(MovieDetailsResponse movieDetailsResponse) {
                    result = convertMovieDetailToMovieResult(movieDetailsResponse);
                }

                @Override
                public void onError(Throwable e) {
                    Log.i(TAG, "Error ");

                    Log.i(TAG, "Error " + e.getMessage());
                    Toast.makeText(MovieActivity.this,
                        getString(R.string.no_internet_connection),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onComplete() {
                    movieAdaptor.setResults(null);
                    results.add(result);
                    movieAdaptor.setResults(results.toArray(new Result[results.size()]));

                    setTitle("Favorite Movies");
                }

                private Result convertMovieDetailToMovieResult(
                        MovieDetailsResponse movieDetailsResponse) {
                    Result result = new Result();
                    result.setVoteCount(movieDetailsResponse.getVoteCount());

                    result.setId(id);

                    result.setVideo(movieDetailsResponse.getVideo());
                    result.setVoteAverage(movieDetailsResponse.getVoteAverage());
                    result.setTitle(movieDetailsResponse.getTitle());
                    result.setPopularity(movieDetailsResponse.getPopularity());
                    result.setPosterPath(movieDetailsResponse.getPosterPath());
                    result.setOriginalLanguage(movieDetailsResponse.getOriginalLanguage());
                    result.setOriginalTitle(movieDetailsResponse.getOriginalTitle());
                    result.setGenreIds(null);
                    result.setBackdropPath(movieDetailsResponse.getBackdropPath());
                    result.setAdult(movieDetailsResponse.getAdult());
                    result.setOverview(movieDetailsResponse.getOverview());
                    result.setReleaseDate(movieDetailsResponse.getReleaseDate());

                    Log.i(TAG, "TITLE -> " + movieDetailsResponse.getTitle());

                    return result;
                }
            });
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        movieAdaptor.setResults(null);
        mCursor = null;
    }
}
