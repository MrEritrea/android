package com.codepath.chefster.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.codepath.chefster.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProgressItemViewHolder extends RecyclerView.ViewHolder{
    @BindView(R.id.relative_layout_step_item) RelativeLayout mainLayout;
    @BindView(R.id.text_view_dish_title) TextView stepDishTextView;
    @BindView(R.id.text_view_step_description) TextView stepDescriptionTextView;
    @BindView(R.id.text_view_step_type) TextView stepTypeTextView;
    @BindView(R.id.button_step_details) Button stepDetailsButton;

    public ProgressItemViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public TextView getStepDishTextView() {
        return stepDishTextView;
    }

    public TextView getStepDescriptionTextView() {
        return stepDescriptionTextView;
    }

    public TextView getStepTypeTextView() {
        return stepTypeTextView;
    }

    public Button getStepDetailsButton() {
        return stepDetailsButton;
    }

    public RelativeLayout getMainLayout() {
        return mainLayout;
    }
}
