package com.codepath.chefster.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.azoft.carousellayoutmanager.CarouselLayoutManager;
import com.azoft.carousellayoutmanager.CarouselZoomPostLayoutListener;
import com.azoft.carousellayoutmanager.CenterScrollListener;
import com.codepath.chefster.ChefsterApplication;
import com.codepath.chefster.R;
import com.codepath.chefster.adapters.ProgressAdapter;
import com.codepath.chefster.models.Dish;
import com.codepath.chefster.models.Step;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ProgressActivity extends BaseActivity implements ProgressAdapter.OnStepInteractionListener {
    @Nullable @BindViews({ R.id.recycler_view_main_1, R.id.recycler_view_main_2, R.id.recycler_view_main_3 })
    List<RecyclerView> recyclerViewsList;

    ProgressAdapter[] adapter;

    List<HashSet<Integer>> stepIndexHashSetList;
    List<Dish> chosenDishes;
    List<List<Step>> stepsLists;
    List<Integer> nextStepPerDish;
    boolean[] finished;
    // A HashMap that points on an index for each dish name
    HashMap<String,Integer> dishNameToIndexHashMap;

    List<HashMap<Integer, CountDownTimer>> timersListPerDish;
    int numberOfPeople, numberOfPans, numberOfPots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        ButterKnife.bind(this);

        chosenDishes = Parcels.unwrap(getIntent().getParcelableExtra(ChefsterApplication.SELECTED_DISHES_KEY));
        finished = new boolean[chosenDishes.size()];
        stepsLists = new ArrayList<>(chosenDishes.size());
        timersListPerDish = new ArrayList<>();
        stepIndexHashSetList = new ArrayList<>(chosenDishes.size());
        for (Dish dish : chosenDishes) {
            stepsLists.add(dish.getSteps());
            stepIndexHashSetList.add(new HashSet<Integer>());
            timersListPerDish.add(new HashMap<Integer, CountDownTimer>());
        }

        Intent incomingIntent = getIntent();
        numberOfPeople = incomingIntent.getIntExtra("number_of_people", 1);
        numberOfPans = incomingIntent.getIntExtra("number_of_pans", 1);
        numberOfPots = incomingIntent.getIntExtra("number_of_pots", 1);
        adapter = new ProgressAdapter[chosenDishes.size()];
        dishNameToIndexHashMap = new HashMap<>();

        setupRecyclerViews();
        markInitialActiveSteps();
    }

    private void markInitialActiveSteps() {
        nextStepPerDish = new ArrayList<>(chosenDishes.size());
        for (int i = 0; i < stepsLists.size(); i++) {
            nextStepPerDish.add(0);
        }

        for (int i = 0; i < numberOfPeople; i++) {
            Step thisStep = getNextBestStep();
            if (thisStep != null) {
                int dishIndex = dishNameToIndexHashMap.get(thisStep.getDishName());
                // increment the next step for this list
                thisStep.setStatus(Step.Status.ACTIVE);
                adapter[dishIndex].notifyDataSetChanged();
            }
        }
    }

    private Step getNextBestStep() {
        PriorityQueue<Step> potentialInitialSteps = new PriorityQueue<>(chosenDishes.size());
        for (int i = 0; i < stepsLists.size(); i++) {
            int nextStepOrderForThisList = nextStepPerDish.get(i);
            // We want to protect from out of bound exception. This dish might be finished
            if (nextStepOrderForThisList < stepsLists.get(i).size()) {
                Step nextStep = stepsLists.get(i).get(nextStepPerDish.get(i));
                if (nextStep.getPreRequisite() == -1
                        || !stepIndexHashSetList.isEmpty() && stepIndexHashSetList.get(i).contains(nextStep.getPreRequisite())) {
                    potentialInitialSteps.add(stepsLists.get(i).get(nextStepPerDish.get(i)));
                }
            }
        }
        if (potentialInitialSteps.isEmpty()) return null;
        else {
            Step chosenStep = potentialInitialSteps.peek();
            int index = dishNameToIndexHashMap.get(chosenStep.getDishName());
            nextStepPerDish.set(index, nextStepPerDish.get(index) + 1);

            return chosenStep;
        }
    }

    protected void setupRecyclerViews() {
        for (int i = 0; i < stepsLists.size(); i++) {
            dishNameToIndexHashMap.put(stepsLists.get(i).get(0).getDishName(), i);
            List<Step> currentList = stepsLists.get(i);
            for (Step step : currentList) {
                step.setStatus(Step.Status.READY);
            }
            adapter[i] = new ProgressAdapter(currentList, this, i);
            final CarouselLayoutManager layoutManager = new CarouselLayoutManager(CarouselLayoutManager.HORIZONTAL, false);
            layoutManager.setPostLayoutListener(new CarouselZoomPostLayoutListener());
            recyclerViewsList.get(i).setAdapter(adapter[i]);
            recyclerViewsList.get(i).setLayoutManager(layoutManager);
            recyclerViewsList.get(i).setHasFixedSize(true);
            recyclerViewsList.get(i).addOnScrollListener(new CenterScrollListener());
            recyclerViewsList.get(i).setVisibility(View.VISIBLE);
        }
    }

    public void scrollToStep(int list, int step) {
        recyclerViewsList.get(list).smoothScrollToPosition(step);
    }

    @OnClick(R.id.text_view_meals_in_progress)
    public void startScrolling() {
        try {
            Thread.sleep(1000);
            scrollToStep(0,4);
            Thread.sleep(1000);
            scrollToStep(1,2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStepDone(String dish, int step) {
        int index = dishNameToIndexHashMap.get(dish);
        stepIndexHashSetList.get(index).add(step);
        List<Step> currentStepsList = stepsLists.get(index);
        // Set this step as DONE and update UI
        currentStepsList.get(step).setStatus(Step.Status.DONE);
        adapter[index].notifyItemChanged(step);
        if (step == currentStepsList.size() - 1) {
            finished[index] = true;
            YoYo.with(Techniques.Tada)
                    .duration(1500)
                    .playOn(recyclerViewsList.get(index));
            if (mealIsFinished()) {
                finishCooking();
            }
        } else {
            // When there are 2 people cooking, there could be 2 steps that should be done in the
            // same list
        }

        Step chosenStep = getNextBestStep();
        if (chosenStep == null) {
            return;
        }
        chosenStep.setStatus(Step.Status.ACTIVE);
        int newStepListIndex = dishNameToIndexHashMap.get(chosenStep.getDishName());
        int order = chosenStep.getOrder();
        adapter[newStepListIndex].notifyItemChanged(order);
        scrollToStep(newStepListIndex, order);
    }

    @Override
    public void onPausePlayButtonClick(String dish, final int step, long timeLeftForStep) {
        final int index = dishNameToIndexHashMap.get(dish);
        if (timersListPerDish.get(index).get(step) == null) {
            timersListPerDish.get(index).put(step, new CountDownTimer(timeLeftForStep * 60000, 60000) {
                public void onTick(long millisUntilFinished) {
                    adapter[index].setTimeLeftForStep(step, millisUntilFinished / 1000 / 60);
                    adapter[index].setStepTimerRunning(step, true);
                    adapter[index].notifyItemChanged(step);
                }

                public void onFinish() {
                    adapter[index].setTimeLeftForStep(step, 0);
                    adapter[index].setStepTimerRunning(step, false);
                    adapter[index].notifyItemChanged(step);
                }
            });
            timersListPerDish.get(index).get(step).start();
        } else {
            timersListPerDish.get(index).get(step).cancel();
            timersListPerDish.get(index).remove(step);
            adapter[index].setStepTimerRunning(step, false);
            adapter[index].notifyItemChanged(step);
        }
    }

    private boolean mealIsFinished() {
        for (boolean isFinished : finished) {
            if (!isFinished) return false;
        }
        return true;
    }


    @Override
    public void onDetailsButtonClick(int index) {
        boolean isExpanded = adapter[index].isExpanded();
        expandIndex(index, !isExpanded);
        adapter[index].setExpanded(!isExpanded);
        adapter[index].notifyDataSetChanged();
    }

    public void expandIndex(int index, boolean expand) {
        if (expand) {
            for (int i = 0; i < chosenDishes.size(); i++) {
                recyclerViewsList.get(i).setVisibility(i == index ? View.VISIBLE : View.GONE);
            }
        } else {
            for (int i = 0; i < chosenDishes.size(); i++) {
                recyclerViewsList.get(i).setVisibility(View.VISIBLE);
            }
        }
    }

    @OnClick(R.id.button_finish_cooking)
    public void finishCooking() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.finish_cooking_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(getBaseContext(), ShareActivity.class);
                        intent.putExtra(ChefsterApplication.SELECTED_DISHES_KEY, Parcels.wrap(chosenDishes));
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
       builder.create().show();
    }
}
