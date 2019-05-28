package com.parrish.android.portfolio.fragments.movie.detail;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.parrish.android.portfolio.BuildConfig;
import com.parrish.android.portfolio.R;
import com.parrish.android.portfolio.activities.movie.details.MovieDetailsActivity;
import com.parrish.android.portfolio.adaptors.movie.details.MovieReviewsAdaptor;
import com.parrish.android.portfolio.interfaces.MovieService;
import com.parrish.android.portfolio.models.movie.reviews.Result;
import com.parrish.android.portfolio.models.movie.reviews.MovieReviewResponse;
import com.parrish.android.portfolio.network.ApiUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ReviewFragment extends Fragment {
    private MovieReviewsAdaptor movieReviewsAdaptor;

    @BindView(R.id.reviews_rv)
    public RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.review_fragment, null, false);
        ButterKnife.bind(this, view);
        setupRecyclerView();

        assert getArguments() != null;
        com.parrish.android.portfolio.models.movie.Result result =
                getArguments().getParcelable(MovieDetailsActivity.RESULT_CACHE_KEY);
        assert result != null;
        loadReviews(result.getId());

        return view;
    }

    private void setupRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        movieReviewsAdaptor = new MovieReviewsAdaptor(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(movieReviewsAdaptor);
    }

    private void loadReviews(Integer id) {
        MovieService movieService = ApiUtils.getMovieService();
        movieService.getMovieReviews(id, BuildConfig.MOVIE_API_KEY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<MovieReviewResponse>() {
                private List<Result> resultCache;

                @Override
                public void onSubscribe(Disposable d) {
                    movieReviewsAdaptor.setResults(null);
                }

                @Override
                public void onNext(MovieReviewResponse movieReviewResponse) {
                    setResultCache(movieReviewResponse);
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
                    movieReviewsAdaptor.setResults(resultCache
                            .toArray(new com.parrish.android.portfolio.models.movie.reviews.Result[resultCache.size()]));
                }

                private void setResultCache(MovieReviewResponse movieReviewResponse) {
                    resultCache = movieReviewResponse.getResults();
                }
            });
    }
}
