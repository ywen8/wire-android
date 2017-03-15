/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.pages.main.popup;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.waz.api.IConversation;
import com.waz.api.Message;
import com.waz.api.MessagesList;
import com.waz.api.UpdateListener;
import com.waz.api.User;
import com.waz.zclient.R;
import com.waz.zclient.ui.utils.TextViewUtils;
import com.waz.zclient.utils.StringUtils;
import com.waz.zclient.utils.ViewUtils;

public class ContentAdapter extends RecyclerView.Adapter<com.waz.zclient.pages.main.popup.ContentAdapter.ViewHolder> {

    private Context context;
    private int lastReadIndex;
    private int itemCount;
    private MessagesList messagesList;

    public ContentAdapter(Context context, MessagesList messagesList) {
        this.context = context;
        this.messagesList = messagesList;
        this.lastReadIndex = messagesList.getLastReadIndex();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.layout_quick_reply_content, parent, false);
        return new ViewHolder(context, view);
    }

    public void setLastRead(int lastReadIndex) {
        // We want to skip the new last read position if it is caused due to a new message
        if (itemCount == getItemCount() && lastReadIndex + 1 < getItemCount()) {
            this.lastReadIndex = lastReadIndex;
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public Message getItem(int position) {
        return messagesList.get(messagesList.size() - 1 - position);
    }

    @Override
    public int getItemCount() {
        // Want to know the old itemcount, so without asking the messagesList
        itemCount = messagesList.size() - 1 - lastReadIndex;
        return itemCount;
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder implements UpdateListener {

        private Context context;
        private TextView content;
        private Message message;
        private User user;
        private IConversation conversation;

        public ViewHolder(Context context, View itemView) {
            super(itemView);
            this.context = context;
            this.content = ViewUtils.getView(itemView, R.id.ttv__quick_reply__content);
        }

        public void bind(Message message) {
            this.message = message;
            this.message.addUpdateListener(this);

            this.user = message.getUser();
            this.user.addUpdateListener(this);

            this.conversation = message.getConversation();
            this.conversation.addUpdateListener(this);

            updated();
        }

        public void recycle() {
            if (message != null) {
                message.removeUpdateListener(this);
                message = null;
            }

            if (user != null) {
                user.removeUpdateListener(this);
                user = null;
            }

            if (conversation != null) {
                conversation.removeUpdateListener(this);
                conversation = null;
            }
        }

        @Override
        public void updated() {
            String messageBody = getMessageBody(message);
            if (conversation.getType() == IConversation.Type.ONE_TO_ONE) {
                content.setText(messageBody);
            } else {
                content.setText(context.getString(R.string.quick_reply__message_group,
                                                  user.getDisplayName(),
                                                  messageBody));
                TextViewUtils.boldText(content);
            }
        }

        private String getMessageBody(Message message) {
            switch (message.getMessageType()) {
                case TEXT:
                case CONNECT_REQUEST:
                    return message.getBody();
                case MISSED_CALL:
                    return context.getString(R.string.notification__message__one_to_one__wanted_to_talk);
                case KNOCK:
                    return context.getString(R.string.notification__message__one_to_one__pinged);
                case ASSET:
                    return context.getString(R.string.notification__message__one_to_one__shared_picture);
                case RENAME:
                    return StringUtils.capitalise(context.getString(R.string.notification__message__group__renamed_conversation, message.getBody()));
                case MEMBER_LEAVE:
                    return StringUtils.capitalise(context.getString(R.string.notification__message__group__remove));
                case MEMBER_JOIN:
                    return StringUtils.capitalise(context.getString(R.string.notification__message__group__add));
                case CONNECT_ACCEPTED:
                    return context.getString(R.string.notification__message__single__accept_request, user.getDisplayName());
                case ANY_ASSET:
                    return context.getString(R.string.notification__message__one_to_one__shared_file);
            }
            return message.getBody();
        }
    }
}
