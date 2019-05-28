package com.parrish.android.portfolio.activities.movie.details;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parrish.android.portfolio.BuildConfig;
import com.parrish.android.portfolio.adaptors.movie.details.TabAdapter;
import com.parrish.android.portfolio.fragments.movie.detail.ReviewFragment;
import com.parrish.android.portfolio.fragments.movie.detail.TrailerFragment;
import com.squareup.picasso.Callback;

import com.parrish.android.portfolio.helpers.Helper;
import com.parrish.android.portfolio.interfaces.MovieService;
import com.parrish.android.portfolio.models.movie.details.MovieDetailsResponse;
import com.parrish.android.portfolio.models.movie.Result;
import com.parrish.android.portfolio.R;
import com.parrish.android.portfolio.network.ApiUtils;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MovieDetailsActivity extends AppCompatActivity {

    private final String TAG = MovieDetailsActivity.class.getSimpleName();

    @BindView(R.id.movie_details_title)
    public TextView movieDetailsTitleTextView;

    @BindView(R.id.movie_thumbnail)
    public ImageView movieThumbnailImageView;

    @BindView(R.id.movie_year)
    public TextView movieYear;

    @BindView(R.id.movie_duration)
    public TextView movieDuration;

    @BindView(R.id.movie_rating)
    public TextView movieRating;

    @BindView(R.id.movie_favorite_image_view)
    public ImageView movieFavoriteImageView;

    @BindView(R.id.movie_description)
    public TextView movieDescription;

    @BindView(R.id.tabLayout)
    public TabLayout tabLayout;

    @BindView(R.id.viewPager)
    public ViewPager viewPager;

    private TabAdapter adapter;

    public final static String RESULT_CACHE_KEY = "RESULT_CACHE_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        supportPostponeEnterTransition();
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if(intent.hasExtra(Intent.EXTRA_TEXT)) {
            Result result = intent.getParcelableExtra(Intent.EXTRA_TEXT);

            movieDetailsTitleTextView.setText(result.getTitle());
            movieThumbnailImageView.setTransitionName(result.getTitle());

            Picasso.get().load(Helper.getThumbNailURL(result))
                .into(movieThumbnailImageView, new Callback(){
                    @Override
                    public void onSuccess() {
                        supportStartPostponedEnterTransition();
                        scheduleStartPostponedTransition(movieThumbnailImageView);
                    }

                    @Override
                    public void onError(Exception e) {
                        supportStartPostponedEnterTransition();
                        scheduleStartPostponedTransition(movieThumbnailImageView);
                    }

                    /**
                     * Schedules the shared element transition to be started immediately
                     * after the shared element has been measured and laid out within the
                     * activity's view hierarchy. Some common places where it might make
                     * sense to call this method are:
                     *
                     * (1) Inside a Fragment's onCreateView() method (if the shared element
                     *     lives inside a Fragment hosted by the called Activity).
                     *
                     * (2) Inside a Picasso Callback object (if you need to wait for Picasso to
                     *     asynchronously load/scale a bitmap before the transition can begin).
                     **/
                    private void scheduleStartPostponedTransition(final View sharedElement) {
                        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                            new ViewTreeObserver.OnPreDrawListener() {
                                @Override
                                public boolean onPreDraw() {
                                    sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                                    startPostponedEnterTransition();
                                    return true;
                                }
                            });
                    }
                });
            movieYear.setText(getYear(result.getReleaseDate()));
            setMovieDuration(result.getId());
            movieRating.setText(getVoteAverage(result.getVoteAverage()));
            movieDescription.setText(result.getOverview());

            adapter = new TabAdapter(getSupportFragmentManager());
            Bundle bundle = new Bundle();
            bundle.putParcelable(RESULT_CACHE_KEY, result);

            TrailerFragment trailerFragment = new TrailerFragment();
            trailerFragment.setArguments(bundle);

            ReviewFragment reviewFragment = new ReviewFragment();
            reviewFragment.setArguments(bundle);

            adapter.addFragment(trailerFragment, getString(R.string.trailers));
            adapter.addFragment(reviewFragment, getString(R.string.reviews));

            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);


            //TODO: Here I need to get the movie id and check to see it if exist in the favorite table.
            //      Then set resource and tag accordingly
            movieFavoriteImageView.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            movieFavoriteImageView.setTag(R.drawable.ic_favorite_border_black_24dp);

            movieFavoriteImageView.setOnClickListener(v -> {
                if((int)((movieFavoriteImageView.getTag())) == R.drawable.ic_favorite_border_black_24dp) {
                    movieFavoriteImageView.setImageResource(R.drawable.ic_favorite_black_24dp);
                    movieFavoriteImageView.setTag(R.drawable.ic_favorite_black_24dp);
                } else {
                    movieFavoriteImageView.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                    movieFavoriteImageView.setTag(R.drawable.ic_favorite_border_black_24dp);
                }
            });

            Animation animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            View view = findViewById(R.id.container);
            view.startAnimation(animFadeIn);

            setTitle(getString(R.string.movie_details));
        }

        ActionBar actionBar = this.getSupportActionBar();

        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }

    private String getYear(String releaseDate) {
        return releaseDate.split("-")[0];
    }

    private void setMovieDuration(Integer id) {
        MovieService movieService = ApiUtils.getMovieService();
        movieService.getMovieDetails(id, BuildConfig.MOVIE_API_KEY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<MovieDetailsResponse>() {
                private Integer runTime;

                @Override
                public void onSubscribe(Disposable d) {}

                @Override
                public void onNext(MovieDetailsResponse movieDetailsResponse) {
                    runTime = movieDetailsResponse.getRuntime();
                }

                @Override
                public void onError(Throwable e) {
                    Toast.makeText(MovieDetailsActivity.this,
                            getString(R.string.no_internet_connection),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onComplete() {
                    String sb = runTime +
                            "min ";
                    movieDuration.setText(sb);
                }
            });
    }

    private String getVoteAverage(Double voteAverage) {
        return voteAverage +
                "/" +
                10;
    }
}
