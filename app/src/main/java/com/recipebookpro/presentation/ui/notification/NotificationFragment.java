package com.recipebookpro.presentation.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.recipebookpro.R;
import com.recipebookpro.domain.model.Notification;

public class NotificationFragment extends Fragment {

    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;
    private RecyclerView rvNotifications;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvEmpty = view.findViewById(R.id.tvEmptyNotifications);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(new NotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onAccept(Notification notification) {
                String invitationId = notification.getData().get("invitationId");
                if (invitationId != null) {
                    viewModel.respondToInvitation(invitationId, true);
                    Toast.makeText(getContext(), R.string.invitation_accepted, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onReject(Notification notification) {
                String invitationId = notification.getData().get("invitationId");
                if (invitationId != null) {
                    viewModel.respondToInvitation(invitationId, false);
                    Toast.makeText(getContext(), R.string.invitation_declined, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onClick(Notification notification) {
                if (!notification.isRead()) {
                    viewModel.markAsRead(notification.getId());
                }
                // Handle navigation based on type if needed
            }
        });

        rvNotifications.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications == null || notifications.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvNotifications.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvNotifications.setVisibility(View.VISIBLE);
                adapter.submitList(notifications);
            }
        });

        return view;
    }
}
