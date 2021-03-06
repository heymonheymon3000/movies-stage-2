package com.parrish.android.portfolio.adaptors.movie.details;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.ivbaranov.mli.MaterialLetterIcon;
import com.parrish.android.portfolio.R;
import com.parrish.android.portfolio.models.movie.reviews.Result;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MovieReviewsAdaptor extends
        RecyclerView.Adapter<MovieReviewsAdaptor.MovieReviewsViewHolder> {

    private final Context mContext;
    private Result[] mResults;
    private static final Random RANDOM = new Random();
    private int[] mMaterialColors;

    public MovieReviewsAdaptor(Context context) {
        this.mContext = context;
        this.mMaterialColors = context.getResources().getIntArray(R.array.colors);
    }

    public void setResults(Result[] results) {
        this.mResults = results;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieReviewsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        int layoutIdForListItem = R.layout.movie_review_list_item;
        LayoutInflater inflater = LayoutInflater.from(mContext);

        View view = inflater.inflate(layoutIdForListItem, viewGroup, false);
        view.setFocusable(true);

        return new MovieReviewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieReviewsViewHolder movieReviewsViewHolder, int i) {
        Result result = mResults[i];
        movieReviewsViewHolder.materialLetterIcon.setLetter(result.getAuthor());
        movieReviewsViewHolder.materialLetterIcon.setShapeColor(mMaterialColors[RANDOM.nextInt(mMaterialColors.length)]);
        movieReviewsViewHolder.author.setText(result.getAuthor());
        movieReviewsViewHolder.content.setText(result.getContent());
        movieReviewsViewHolder.readMore.setText(Html.fromHtml("<a href='"+result.getUrl()+"'>Read more...</a>", 0));
        movieReviewsViewHolder.readMore.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    @Override
    public int getItemCount() {
        if(mResults == null) return 0;
        return mResults.length;
    }

    public class MovieReviewsViewHolder extends ViewHolder {
        @BindView(R.id.letter_icon)
        public MaterialLetterIcon materialLetterIcon;

        @BindView(R.id.author)
        public TextView author;

        @BindView(R.id.content)
        public TextView content;

        @BindView(R.id.read_more)
        public TextView readMore;

        @SuppressWarnings("WeakerAccess")
        public MovieReviewsViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
