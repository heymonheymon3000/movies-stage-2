package com.parrish.android.portfolio.fragments.movie.detail;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.parrish.android.portfolio.BuildConfig;
import com.parrish.android.portfolio.R;
import com.parrish.android.portfolio.activities.movie.details.MovieDetailsActivity;
import com.parrish.android.portfolio.adaptors.movie.details.MovieTrailersAdaptor;
import com.parrish.android.portfolio.interfaces.MovieService;
import com.parrish.android.portfolio.models.movie.details.MovieVideoResponse;
import com.parrish.android.portfolio.models.movie.details.Result;
import com.parrish.android.portfolio.network.ApiUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import io.reactivex.Observer;

public class TrailerFragment extends Fragment
    implements MovieTrailersAdaptor.TrailerClickListener {

    private final static String YOUTUBE_URL = "http://www.youtube.com/watch?v=";
    private final static String YOUTUBE = "vnd.youtube:";
    private MovieTrailersAdaptor movieTrailersAdaptor;

    @BindView(R.id.trailers_rv)
    public RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.trailer_fragment, container, false);
        ButterKnife.bind(this, view);
        setupRecyclerView();

        assert getArguments() != null;
        com.parrish.android.portfolio.models.movie.Result result =
                getArguments().getParcelable(MovieDetailsActivity.RESULT_CACHE_KEY);
        assert result != null;
        loadTrailers(result.getId());

        return view;
    }

    private void setupRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        movieTrailersAdaptor = new MovieTrailersAdaptor(getContext(), this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(movieTrailersAdaptor);
    }

    private void loadTrailers(Integer id) {
        MovieService movieService = ApiUtils.getMovieService();
        movieService.getMovieTrailers(id, BuildConfig.MOVIE_API_KEY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<MovieVideoResponse>() {
                private List<Result> resultCache;

                @Override
                public void onSubscribe(Disposable d) {
                    movieTrailersAdaptor.setResults(null);
                }

                @Override
                public void onNext(MovieVideoResponse movieVideoResponse) {
                    setResultCache(movieVideoResponse);
                }

                @Override
                public void onError(Throwable e) {
                    Toast.makeText(getContext(),
                            getString(R.string.no_internet_connection),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onComplete() {
                    //noinspection ToArrayCallWithZeroLengthArrayArgument
                    movieTrailersAdaptor.setResults(resultCache
                        .toArray(new com.parrish.android.portfolio.models.movie.details.Result[resultCache.size()]));
                }

                private void setResultCache(MovieVideoResponse movieVideoResponse) {
                    resultCache = movieVideoResponse.getResults().stream()
                        .filter(result -> result.getType()
                            .equals(getString(R.string.trailer))).collect(Collectors.toList());
                }
            });
    }

    @Override
    public void onTrailerClickListener(com.parrish.android.portfolio.models.movie.details.Result result) {
        watchYoutubeVideo(Objects.requireNonNull(getContext()), result.getKey());
    }

    private static void watchYoutubeVideo(Context context, String id){
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(YOUTUBE_URL + id));
        try {
            context.startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            context.startActivity(webIntent);
        }
    }
}
