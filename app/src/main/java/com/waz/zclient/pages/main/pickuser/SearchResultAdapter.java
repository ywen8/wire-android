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
import android.util.SparseArray;
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
import com.waz.zclient.pages.main.pickuser.views.viewholders.SectionExpanderViewHolder;
import com.waz.zclient.pages.main.pickuser.views.viewholders.SectionHeaderViewHolder;
import com.waz.zclient.pages.main.pickuser.views.viewholders.TopUsersViewHolder;
import com.waz.zclient.pages.main.pickuser.views.viewholders.UserViewHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @IntDef({ITEM_TYPE_TOP_USER,
             ITEM_TYPE_INITIAL,
             ITEM_TYPE_CONTACT,
             ITEM_TYPE_CONNECTED_USER,
             ITEM_TYPE_OTHER_USER,
             ITEM_TYPE_CONVERSATION,
             ITEM_TYPE_SECTION_HEADER,
             ITEM_TYPE_EXPAND_BUTTON
    })
    @interface ItemType { }
    public static final int ITEM_TYPE_TOP_USER = 0;
    public static final int ITEM_TYPE_INITIAL = 1;
    public static final int ITEM_TYPE_CONTACT = 2;
    public static final int ITEM_TYPE_CONNECTED_USER = 3;
    public static final int ITEM_TYPE_OTHER_USER = 4;
    public static final int ITEM_TYPE_CONVERSATION = 5;
    public static final int ITEM_TYPE_SECTION_HEADER = 6;
    public static final int ITEM_TYPE_EXPAND_BUTTON = 7;

    public static final int ROW_COUNT_SECTION_HEADER = 1;
    public static final int COLLAPSED_LIMIT = 4;
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
    private SparseArray<int[]> positionsMap;
    private ContactRowView.Callback contactsCallback;
    private boolean contactsCollapsed = true;
    private boolean groupsCollapsed = true;
    private List<SearchContact> mergedContacts = new ArrayList<>();

    private class SearchContact {
        @ItemType public int itemType;
        public int index;
        public String name;

        SearchContact(int itemType, int index, String name) {
            this.itemType = itemType;
            this.index = index;
            this.name = name;
        }
    }

    private void updateMergedContacts() {
        mergedContacts.clear();
        if (contacts != null) {
            for (int i = 0; i < contacts.size(); i++) {
                ContactDetails details = contacts.get(i).getDetails();
                if (details != null) {
                    mergedContacts.add(new SearchContact(ITEM_TYPE_CONTACT, i, details.getDisplayName()));
                }
            }
        }
        if (connectedUsers != null) {
            for (int i = 0; i < connectedUsers.length; i++) {
                mergedContacts.add(new SearchContact(ITEM_TYPE_CONNECTED_USER, i, connectedUsers[i].getDisplayName()));
            }
        }
        Collections.sort(mergedContacts, new Comparator<SearchContact>() {
            @Override
            public int compare(SearchContact o1, SearchContact o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });
    }

    public SearchResultAdapter(final Callback callback) {
        positionsMap = new SparseArray<>();
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
            case ITEM_TYPE_EXPAND_BUTTON:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.startui_section_expander, parent, false);
                return new SectionExpanderViewHolder(view);
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
                int index = mergedContacts.get(position - ROW_COUNT_SECTION_HEADER).index;
                User connectedUser = connectedUsers[index];
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
                if (showSearch) {
                    position = mergedContacts.get(position - ROW_COUNT_SECTION_HEADER).index;
                    Contact contact = contacts.get(position);
                    ((AddressBookContactViewHolder) holder).bind(contact, contactsCallback, accentColor);
                } else {
                    int[] contactMapping = getContactMapping(position);
                    String contactInitial = getContactInitial(position);
                    int contactInternalPosition = contactMapping[2];
                    Contact contact = contacts.getContactForInitial(contactInitial, contactInternalPosition);
                    ((AddressBookContactViewHolder) holder).bind(contact, contactsCallback, accentColor);
                }
                break;
            case ITEM_TYPE_EXPAND_BUTTON:
                if (getSectionForPosition(position) == ITEM_TYPE_CONNECTED_USER) {
                    ((SectionExpanderViewHolder) holder).bind(mergedContacts.size(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            setContactsCollapsed(false);
                        }
                    });
                } else {
                    ((SectionExpanderViewHolder) holder).bind(conversations.length, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            setGroupsCollapsed(false);
                        }
                    });
                }
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
        if (position < 0) {
            return type;
        }
        if (showSearch) {
            if (hasConnectedUsers() &&
                position < getContactsSectionLength()) {
                // Connected users
                if (position == 0) {
                    type = ITEM_TYPE_SECTION_HEADER;
                } else if (position == getContactsSectionLength() - 1 && isContactsCollapsed()) {
                    type = ITEM_TYPE_EXPAND_BUTTON;
                } else {
                    type = mergedContacts.get(position - ROW_COUNT_SECTION_HEADER).itemType;
                }
            } else if (hasConversations() &&
                       getConversationInternalPosition(position) < getGroupsSectionLength()) {
                // Conversations
                int internalPosition = getConversationInternalPosition(position);
                if (internalPosition == 0) {
                    type = ITEM_TYPE_SECTION_HEADER;
                } else if (internalPosition == getGroupsSectionLength() - 1 && isGroupsCollapsed()) {
                    type = ITEM_TYPE_EXPAND_BUTTON;
                } else {
                    type = ITEM_TYPE_CONVERSATION;
                }
            } else {
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
        updateMergedContacts();
        updateItemCount();
        notifyDataSetChanged();
    }

    public void setSearchResult(User[] connectedUsers, User[] otherUsers, IConversation[] conversations) {
        showSearch = true;
        this.connectedUsers = connectedUsers;
        this.otherUsers = otherUsers;
        this.conversations = conversations;
        updateMergedContacts();
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
        return connectedUsers != null && getContactsListLength() > 0;
    }

    public boolean hasOtherUsers() {
        return otherUsers != null && otherUsers.length > 0;
    }

    public boolean hasConversations() {
        return conversations != null && getGroupsListLength() > 0;
    }

    public int getConversationInternalPosition(int position) {
        if (hasConnectedUsers()) {
            position = position - getContactsSectionLength();
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
            position = position - getContactsSectionLength();
        }
        if (hasConversations()) {
            position = position - getGroupsSectionLength();
        }
        return position;
    }

    public int getOtherUserInternalPosition(int position) {
        if (hasConnectedUsers()) {
            position = position - getContactsSectionLength();
        }
        if (hasConversations()) {
            position = position - getGroupsSectionLength();
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
            } else if (hasOtherUsers() &&
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

    private int getSectionForPosition(int position) {
        int type = -1;
        if (showSearch) {
            if (hasConnectedUsers() &&
                position < getContactsSectionLength()) {
                type = ITEM_TYPE_CONNECTED_USER;
            } else if (hasConversations() &&
                getConversationInternalPosition(position) < getGroupsSectionLength()) {
                type = ITEM_TYPE_CONVERSATION;
            } else if (hasOtherUsers() &&
                getOtherUserInternalPosition(position) < otherUsers.length + ROW_COUNT_SECTION_HEADER) {
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
                itemCount += getContactsSectionLength();
            }

            if (hasConversations()) {
                itemCount += getGroupsSectionLength();
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

    public boolean isContactsCollapsed() {
        return mergedContacts.size() > COLLAPSED_LIMIT && contactsCollapsed;
    }

    public void setContactsCollapsed(boolean collapsed) {
        contactsCollapsed = collapsed;
        updateItemCount();
        notifyDataSetChanged();
    }

    private int getContactsListLength() {
        return isContactsCollapsed() ? COLLAPSED_LIMIT : mergedContacts.size();
    }

    private int getContactsSectionLength() {
        return getContactsListLength() + ROW_COUNT_SECTION_HEADER + (isContactsCollapsed() ? 1 : 0);
    }

    public boolean isGroupsCollapsed() {
        return conversations.length > COLLAPSED_LIMIT && groupsCollapsed;
    }

    public void setGroupsCollapsed(boolean collapsed) {
        groupsCollapsed = collapsed;
        updateItemCount();
        notifyDataSetChanged();
    }

    private int getGroupsListLength() {
        return isGroupsCollapsed() ? COLLAPSED_LIMIT : conversations.length;
    }

    private int getGroupsSectionLength() {
        return getGroupsListLength() + ROW_COUNT_SECTION_HEADER + (isGroupsCollapsed() ? 1 : 0);
    }

    public interface Callback {
        Set<User> getSelectedUsers();

        void onContactListUserClicked(User user);

        void onContactListContactClicked(ContactDetails contactDetails);

        @IPickUserController.ContactListDestination int getDestination();
    }
}
