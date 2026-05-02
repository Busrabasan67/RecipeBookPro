package com.recipebookpro.presentation.ui.planner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.recipebookpro.R;
import com.recipebookpro.data.repository.MealPlanRepositoryImpl;
import com.recipebookpro.domain.model.MealPlan;
import com.recipebookpro.domain.repository.MealPlanRepository;

import java.util.ArrayList;
import java.util.List;

public class SavedPlansBottomSheet extends BottomSheetDialogFragment {

    public interface OnPlanSelectedListener {
        void onSelected(MealPlan plan);
    }

    private OnPlanSelectedListener listener;
    private final List<MealPlan> plans = new ArrayList<>();
    private SavedPlansAdapter adapter;
    private ProgressBar progressBar;
    private MaterialTextView tvEmpty;

    public void setOnPlanSelectedListener(OnPlanSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_saved_plans, container, false);
        
        RecyclerView rv = view.findViewById(R.id.rvSavedPlans);
        progressBar = view.findViewById(R.id.progressSavedPlans);
        tvEmpty = view.findViewById(R.id.tvEmptySavedPlans);

        adapter = new SavedPlansAdapter(plans, plan -> {
            if (listener != null) listener.onSelected(plan);
            dismiss();
        });
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        loadPlans();
        
        return view;
    }

    private void loadPlans() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        progressBar.setVisibility(View.VISIBLE);
        new MealPlanRepositoryImpl().getUserMealPlans(uid, new MealPlanRepository.OnMealPlansLoadedListener() {
            @Override
            public void onLoaded(List<MealPlan> loadedPlans) {
                progressBar.setVisibility(View.GONE);
                plans.clear();
                plans.addAll(loadedPlans);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(plans.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Simple Adapter internal to the class
    private static class SavedPlansAdapter extends RecyclerView.Adapter<SavedPlansAdapter.ViewHolder> {
        private final List<MealPlan> plans;
        private final OnPlanSelectedListener listener;

        SavedPlansAdapter(List<MealPlan> plans, OnPlanSelectedListener listener) {
            this.plans = plans;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MealPlan p = plans.get(position);
            holder.text1.setText(p.getName());
            holder.text2.setText(p.getDuration() + " Days - " + p.getTotalCalories() + " kcal");
            holder.itemView.setOnClickListener(v -> listener.onSelected(p));
        }

        @Override
        public int getItemCount() { return plans.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
