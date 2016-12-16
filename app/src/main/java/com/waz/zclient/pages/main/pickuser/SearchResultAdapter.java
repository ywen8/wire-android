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
package com.waz.zclient.pages.main.pickuser;

import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.waz.api.Contact;
import com.waz.api.ContactDetails;
import com.waz.api.Contacts;
import com.waz.api.IConversation;
import com.waz.api.User;
import com.waz.zclient.R;
import com.waz.zclient.pages.main.pickuser.controller.IPickUserController;
import com.waz.zclient.pages.main.pickuser.views.ContactRowView;
import com.waz.zclient.pages.main.pickuser.views.viewholders.AddressBookContactViewHolder;
import com.waz.zclient.pages.main.pickuser.views.viewholders.AddressBookSectionHeaderViewHolder;
import com.waz.zclient.pages.main.pickuser.views.viewholders.ConversationViewHolder;
import com.waz.zclient.pages.main.pickuser.views.viewholders.SectionHeaderViewHolder;
import com.waz.zclient.pages.main.pickuser.views.viewholders.TopUsersViewHolder;
import com.waz.zclient.pages.main.pickuser.views.viewholders.UserViewHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @IntDef({ITEM_TYPE_TOP_USER,
             ITEM_TYPE_INITIAL,
             ITEM_TYPE_CONTACT,
             ITEM_TYPE_CONNECTED_USER,
             ITEM_TYPE_OTHER_USER,
             ITEM_TYPE_CONVERSATION,
             ITEM_TYPE_SECTION_HEADER
    })
    @interface ItemType { }
    public static final int ITEM_TYPE_TOP_USER = 0;
    public static final int ITEM_TYPE_INITIAL = 1;
    public static final int ITEM_TYPE_CONTACT = 2;
    public static final int ITEM_TYPE_CONNECTED_USER = 3;
    public static final int ITEM_TYPE_OTHER_USER = 4;
    public static final int ITEM_TYPE_CONVERSATION = 5;
    public static final int ITEM_TYPE_SECTION_HEADER = 6;

    public static final int ROW_COUNT_SECTION_HEADER = 1;
    private Callback callback;
    private User[] connectedUsers;
    private User[] otherUsers;
    private User[] topUsers;
    private Contacts contacts;
    private IConversation[] conversations;
    private boolean showSearch;
    private boolean darkTheme;
    private SearchResultOnItemTouchListener topUsersOnItemTouchListener;
    private int itemCount;
    private int accentColor;
    private Map<Integer, int[]> positionsMap;
    private ContactRowView.Callback contactsCallback;

    public SearchResultAdapter(final Callback callback) {
        positionsMap = new HashMap<>();
        if (callback == null) {
            return;
        }
        this.callback = callback;
        this.contactsCallback = new ContactRowView.Callback() {
            @Override
            public void onContactListUserClicked(User user) {
                callback.onContactListUserClicked(user);
            }

            @Override
            public void onContactListContactClicked(ContactDetails contactDetails) {
                callback.onContactListContactClicked(contactDetails);
            }

            @Override
            public int getDestination() {
                return callback.getDestination();
            }

            @Override
            public boolean isUserSelected(User user) {
                if (callback.getSelectedUsers() == null) {
                    return false;
                }
                return callback.getSelectedUsers().contains(user);
            }
        };
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, @ItemType int viewType) {
        View view;
        switch (viewType) {
            case ITEM_TYPE_TOP_USER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.startui_top_users, parent, false);
                TopUserAdapter topUserAdapter = new TopUserAdapter(new TopUserAdapter.Callback() {
                    @Override
                    public Set<User> getSelectedUsers() {
                        return callback.getSelectedUsers();
                    }
                });
                return new TopUsersViewHolder(view, topUserAdapter, parent.getContext());
            case ITEM_TYPE_OTHER_USER:
            case ITEM_TYPE_CONNECTED_USER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.startui_user, parent, false);
                return new UserViewHolder(view, darkTheme, true);
            case ITEM_TYPE_CONVERSATION:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.startui_conversation, parent, false);
                return new ConversationViewHolder(view);
            case ITEM_TYPE_SECTION_HEADER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.startui_section_header, parent, false);
                return new SectionHeaderViewHolder(view);
            case ITEM_TYPE_INITIAL:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.startui_section_header, parent, false);
                return new AddressBookSectionHeaderViewHolder(view, darkTheme);
            case ITEM_TYPE_CONTACT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contactlist_user, parent, false);
                return new AddressBookContactViewHolder(view, darkTheme);
        }
        return null;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        @ItemType int itemType = getItemViewType(position);

        switch (itemType) {
            case ITEM_TYPE_TOP_USER:
                ((TopUsersViewHolder) holder).bind(topUsers);
                ((TopUsersViewHolder) holder).bindOnItemTouchListener(topUsersOnItemTouchListener);
                break;
            case ITEM_TYPE_CONVERSATION:
                IConversation conversation = conversations[getConversationInternalPosition(position) - ROW_COUNT_SECTION_HEADER];
                ((ConversationViewHolder) holder).bind(conversation);
                break;
            case ITEM_TYPE_OTHER_USER:
                User otherUser = otherUsers[getOtherUserInternalPosition(position) - ROW_COUNT_SECTION_HEADER];
                boolean otherIsSelected = callback.getSelectedUsers().contains(otherUser);
                ((UserViewHolder) holder).bind(otherUser, otherIsSelected);
                break;
            case ITEM_TYPE_CONNECTED_USER:
                User connectedUser = connectedUsers[position - ROW_COUNT_SECTION_HEADER];
                boolean contactIsSelected = callback.getSelectedUsers().contains(connectedUser);
                ((UserViewHolder) holder).bind(connectedUser, contactIsSelected);
                break;
            case ITEM_TYPE_SECTION_HEADER:
                int type = getSectionItemType(position);
                ((SectionHeaderViewHolder) holder).bind(type);
                break;
            case ITEM_TYPE_INITIAL:
                if (contacts == null ||
                    contacts.getInitials() == null ||
                    contacts.getInitials().isEmpty()) {
                    break;
                }
                position = showSearch ? position - ROW_COUNT_SECTION_HEADER : position;
                String initial = getContactInitial(position);
                ((AddressBookSectionHeaderViewHolder) holder).bind(initial);
                break;
            case ITEM_TYPE_CONTACT:
                if (contacts == null ||
                    contacts.getInitials() == null ||
                    contacts.getInitials().isEmpty()) {
                    break;
                }
                position = showSearch ? position - ROW_COUNT_SECTION_HEADER : position;
                int[] contactMapping = getContactMapping(position);
                String contactInitial = getContactInitial(position);
                int contactInternalPosition = contactMapping[2];
                Contact contact = contacts.getContactForInitial(contactInitial, contactInternalPosition);
                ((AddressBookContactViewHolder) holder).bind(contact, contactsCallback, accentColor);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    @Override
    public @ItemType int getItemViewType(int position) {
        @ItemType int type = -1;
        if (showSearch) {
            if (hasConnectedUsers() &&
                position < connectedUsers.length + ROW_COUNT_SECTION_HEADER) {
                // Connected users
                type = position == 0 ? ITEM_TYPE_SECTION_HEADER : ITEM_TYPE_CONNECTED_USER;
            } else if (hasConversations() &&
                       getConversationInternalPosition(position) < conversations.length + ROW_COUNT_SECTION_HEADER) {
                // Conversations
                type = getConversationInternalPosition(position) ==  0 ? ITEM_TYPE_SECTION_HEADER : ITEM_TYPE_CONVERSATION;
            } else if (hasContacts() &&
                       getSearchContactInternalPosition(position) < positionsMap.size() + ROW_COUNT_SECTION_HEADER) {
                int contactsPos = getSearchContactInternalPosition(position);
                if (contactsPos == 0) {
                    type = ITEM_TYPE_SECTION_HEADER;
                } else {
                    contactsPos -= 1;
                    type = getContactItemViewType(contactsPos);
                }
            } else if (hasOtherUsers() &&
                       getOtherUserInternalPosition(position) < otherUsers.length + ROW_COUNT_SECTION_HEADER) {
                // Other users
                type = getOtherUserInternalPosition(position) == 0 ? ITEM_TYPE_SECTION_HEADER : ITEM_TYPE_OTHER_USER;
            }
        } else {
            if (hasTopUsers() && position < 2) {
                // Top users
                type = position == 0 ? ITEM_TYPE_SECTION_HEADER : ITEM_TYPE_TOP_USER;
            } else {
                int start = hasTopUsers() ? 2 : 0;
                if (position == start) {
                    type = ITEM_TYPE_SECTION_HEADER;
                } else {
                    int contactsPos = getContactInternalPosition(position);
                    type = getContactItemViewType(contactsPos);
                }
            }
        }
        return type;
    }

    public void setAccentColor(int color) {
        accentColor = color;
    }

    public void setTopUsersOnItemTouchListener(SearchResultOnItemTouchListener topUsersOnItemTouchListener) {
        this.topUsersOnItemTouchListener = topUsersOnItemTouchListener;
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
    }

    public void setTopUsers(User[] users) {
        showSearch = false;
        this.topUsers = users;
        updateItemCount();
        notifyDataSetChanged();
    }

    public void setContacts(Contacts contacts) {
        this.contacts = contacts;
        updateContactsPositionMapping();
        updateItemCount();
        notifyDataSetChanged();
    }

    public void setSearchResult(User[] connectedUsers, User[] otherUsers, IConversation[] conversations) {
        showSearch = true;
        this.connectedUsers = connectedUsers;
        this.otherUsers = otherUsers;
        this.conversations = conversations;
        updateItemCount();
        notifyDataSetChanged();
    }

    public void reset() {
        connectedUsers = null;
        conversations = null;
        otherUsers = null;
        contacts = null;
    }

    public boolean hasTopUsers() {
        if (topUsers == null) {
            return false;
        }
        return topUsers.length > 0;
    }

    public boolean hasContacts() {
        return positionsMap.size() > 0;
    }

    public boolean hasConnectedUsers() {
        return connectedUsers != null && connectedUsers.length > 0;
    }

    public boolean hasOtherUsers() {
        return otherUsers != null && otherUsers.length > 0;
    }

    public boolean hasConversations() {
        return conversations != null && conversations.length > 0;
    }

    private int getConversationInternalPosition(int position) {
        if (hasConnectedUsers()) {
            position = position - connectedUsers.length - ROW_COUNT_SECTION_HEADER;
        }
        return position;
    }

    private int getContactInternalPosition(int position) {
        if (hasTopUsers()) {
            // 2 section headers + 1 row for top users
            return position - 3;
        }
        // 1 for section header
        return position - 1;
    }

    private int getSearchContactInternalPosition(int position) {
        if (hasConnectedUsers()) {
            position = position - connectedUsers.length - ROW_COUNT_SECTION_HEADER;
        }
        if (hasConversations()) {
            position = position - conversations.length - ROW_COUNT_SECTION_HEADER;
        }
        return position;
    }

    private int getOtherUserInternalPosition(int position) {
        if (hasConnectedUsers()) {
            position = position - connectedUsers.length - ROW_COUNT_SECTION_HEADER;
        }
        if (hasConversations()) {
            position = position - conversations.length - ROW_COUNT_SECTION_HEADER;
        }
        if (hasContacts()) {
            position = position - positionsMap.size() - ROW_COUNT_SECTION_HEADER;
        }
        return position;
    }

    private int getSectionItemType(int position) {
        int type = -1;
        if (showSearch) {
            if (hasConnectedUsers() &&
                position == 0) {
                type = ITEM_TYPE_CONNECTED_USER;
            } else if (hasConversations() &&
                       getConversationInternalPosition(position) == 0) {
                type = ITEM_TYPE_CONVERSATION;
            } else if (hasContacts() &&
                       getSearchContactInternalPosition(position) == 0) {
                type = ITEM_TYPE_CONTACT;
            }
            else if (hasOtherUsers() &&
                       getOtherUserInternalPosition(position) == 0) {
                type = ITEM_TYPE_OTHER_USER;
            }
        } else {
            if (hasTopUsers() && position < 2) {
                type = ITEM_TYPE_TOP_USER;
            } else {
                type = ITEM_TYPE_CONTACT;
            }
        }
        return type;
    }

    private void updateItemCount() {
        itemCount = 0;

        if (showSearch) {
            if (hasConnectedUsers()) {
                itemCount += connectedUsers.length + ROW_COUNT_SECTION_HEADER;
            }

            if (hasConversations()) {
                itemCount += conversations.length + ROW_COUNT_SECTION_HEADER;
            }

            if (hasContacts()) {
                itemCount += positionsMap.size() + ROW_COUNT_SECTION_HEADER;
            }

            if (hasOtherUsers()) {
                itemCount += otherUsers.length + ROW_COUNT_SECTION_HEADER;
            }
        } else {
            if (hasTopUsers()) {
                // If top users are visible, are extra row and section header = 2
                itemCount += 2;
            }

            if (hasContacts()) {
                itemCount += ROW_COUNT_SECTION_HEADER + positionsMap.size();
            }
        }
    }

    public @ItemType int getContactItemViewType(int position) {
        int[] mapping = positionsMap.get(position);
        if (mapping[0] == ITEM_TYPE_CONTACT) {
            return ITEM_TYPE_CONTACT;
        }
        return ITEM_TYPE_INITIAL;
    }

    private void updateContactsPositionMapping() {
        positionsMap.clear();
        if (contacts == null) {
            return;
        }
        int pos = 0;
        int initialPos = 0;
        for (String initial : contacts.getInitials()) {
            positionsMap.put(pos, new int[] {ITEM_TYPE_INITIAL, initialPos, -1});

            int numContactsForInitial = contacts.getNumberOfContactsForInitial(initial);
            for (int contactPos = 0; contactPos < numContactsForInitial; contactPos++) {
                pos++;
                positionsMap.put(pos, new int[] {ITEM_TYPE_CONTACT, initialPos, contactPos});
            }
            pos++;
            initialPos++;
        }
    }

    private int[] getContactMapping(int position) {
        position = showSearch ? getSearchContactInternalPosition(position) : getContactInternalPosition(position);
        int[] mapping = positionsMap.get(position);
        return mapping;
    }

    private String getContactInitial(int position) {
        String[] initials = contacts.getInitials().toArray(new String[contacts.getInitials().size()]);
        int[] mapping = getContactMapping(position);
        return initials[mapping[1]];
    }

    public interface Callback {
        Set<User> getSelectedUsers();

        void onContactListUserClicked(User user);

        void onContactListContactClicked(ContactDetails contactDetails);

        @IPickUserController.ContactListDestination int getDestination();
    }
}
