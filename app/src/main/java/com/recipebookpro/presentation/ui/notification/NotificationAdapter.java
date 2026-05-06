package com.recipebookpro.presentation.ui.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.Notification;
import com.recipebookpro.util.LocaleUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    private final OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onAccept(Notification notification);
        void onReject(Notification notification);
        void onClick(Notification notification);
    }

    public NotificationAdapter(OnNotificationActionListener listener) {
        super(new DiffUtil.ItemCallback<Notification>() {
            @Override
            public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
                return oldItem.isRead() == newItem.isRead() && 
                       oldItem.getTimestamp() == newItem.getTimestamp();
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        LinearLayout layoutActions;
        MaterialButton btnAccept, btnReject;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            layoutActions = itemView.findViewById(R.id.layoutInvitationActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        public void bind(Notification notification) {
            boolean isTr = LocaleUtils.isTurkish(itemView.getContext());
            tvTitle.setText(isTr ? notification.getTitleTr() : notification.getTitle());
            tvMessage.setText(isTr ? notification.getMessageTr() : notification.getMessage());

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", 
                    isTr ? new Locale("tr") : Locale.ENGLISH);
            tvTime.setText(sdf.format(new Date(notification.getTimestamp())));

            if (Notification.TYPE_INVITATION.equals(notification.getType())) {
                layoutActions.setVisibility(View.VISIBLE);
                btnAccept.setOnClickListener(v -> listener.onAccept(notification));
                btnReject.setOnClickListener(v -> listener.onReject(notification));
            } else {
                layoutActions.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onClick(notification));
            
            // Highlight unread notifications
            itemView.setAlpha(notification.isRead() ? 0.7f : 1.0f);
        }
    }
}
