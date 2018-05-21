package com.petclump.petclump.views;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.petclump.petclump.R;
import com.petclump.petclump.models.BaseMessage;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ChatRecycleViewAdapter extends RecyclerView.Adapter{
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private List<BaseMessage> MessageList;
    private Context mContext;

    public ChatRecycleViewAdapter(Context context, List<BaseMessage> messageList){
        this.mContext = context;
        this.MessageList = messageList;
    }
    @Override
    public int getItemCount() {
        return MessageList.size();
    }

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        String message = MessageList.get(position).getMessage();
        int sender = MessageList.get(position).getWhich_side();
        return sender ;
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }

        return null;
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BaseMessage message = MessageList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.chatview_sent_message);
            timeText = (TextView) itemView.findViewById(R.id.chatview_sent_time);
        }

        void bind(BaseMessage message) {
            messageText.setText(message.getMessage());
            Date currentTime = Calendar.getInstance().getTime();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            String time = timeFormat.format(currentTime);
            // Format the stored timestamp into a readable String using method.
            timeText.setText(time);
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, nameText;
        ImageView profileImage;

        ReceivedMessageHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.chatview_receive_message);
            timeText = (TextView) itemView.findViewById(R.id.chatview_receive_time);

            profileImage = (ImageView) itemView.findViewById(R.id.chatview_receive_image);
        }

        void bind(BaseMessage message) {
            messageText.setText(message.getMessage());

            // Format the stored timestamp into a readable String using method.
            Date currentTime = Calendar.getInstance().getTime();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            String time = timeFormat.format(currentTime);
            timeText.setText(time);
            //nameText.setText(message.getUser());

            // Insert the profile image from the URL into the ImageView.
            profileImage.setImageResource(R.drawable.dog_placeholder);
        }
    }
}
