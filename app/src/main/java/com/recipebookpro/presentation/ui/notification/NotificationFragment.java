package com.recipebookpro.presentation.ui.notification;

import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
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
        setupSwipeToDelete();

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

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final ColorDrawable background = new ColorDrawable();
            private Drawable deleteIcon;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Notification notification = adapter.getNotificationAt(position);
                if (notification == null) {
                    if (position != RecyclerView.NO_POSITION) {
                        adapter.notifyItemChanged(position);
                    }
                    return;
                }

                adapter.removeNotification(notification);
                viewModel.deleteNotification(notification);
                Snackbar.make(rvNotifications, R.string.notification_deleted, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX,
                                    float dY,
                                    int actionState,
                                    boolean isCurrentlyActive) {
                drawDeleteBackground(c, viewHolder, dX);
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            private void drawDeleteBackground(Canvas canvas, RecyclerView.ViewHolder viewHolder, float dX) {
                View itemView = viewHolder.itemView;
                if (deleteIcon == null) {
                    deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_outline_shopping);
                    if (deleteIcon != null) {
                        deleteIcon = DrawableCompat.wrap(deleteIcon).mutate();
                        DrawableCompat.setTint(deleteIcon, MaterialColors.getColor(itemView,
                                com.google.android.material.R.attr.colorOnError));
                    }
                }

                int errorColor = MaterialColors.getColor(itemView,
                        com.google.android.material.R.attr.colorError);
                background.setColor(errorColor);

                if (dX > 0) {
                    background.setBounds(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + (int) dX, itemView.getBottom());
                } else if (dX < 0) {
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                } else {
                    background.setBounds(0, 0, 0, 0);
                }
                background.draw(canvas);

                if (deleteIcon == null || dX == 0) {
                    return;
                }

                int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + iconMargin;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                int iconLeft;
                int iconRight;
                if (dX > 0) {
                    iconLeft = itemView.getLeft() + iconMargin;
                    iconRight = iconLeft + deleteIcon.getIntrinsicWidth();
                } else {
                    iconRight = itemView.getRight() - iconMargin;
                    iconLeft = iconRight - deleteIcon.getIntrinsicWidth();
                }
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(canvas);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(rvNotifications);
    }
}
