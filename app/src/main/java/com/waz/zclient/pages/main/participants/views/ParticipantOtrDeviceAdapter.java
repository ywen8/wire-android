/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
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
package com.waz.zclient.pages.main.participants.views;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.waz.api.CoreList;
import com.waz.api.OtrClient;
import com.waz.api.UpdateListener;
import com.waz.api.Verification;
import com.waz.zclient.R;
import com.waz.zclient.ui.utils.TextViewUtils;
import com.waz.zclient.utils.OtrUtils;
import com.waz.zclient.utils.ViewUtils;

import java.util.Locale;

public class ParticipantOtrDeviceAdapter extends RecyclerView.Adapter<ParticipantOtrDeviceAdapter.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_OTR_CLIENT = 1;
    private static final String OTR_CLIENT_TEXT_TEMPLATE = "[[%s]]\n%s";
    private ViewHolder.ViewHolderClicks viewHolderClicks;
    private CoreList<OtrClient> otrClients;
    private String userDisplayName;
    private int accentColor;

    public ParticipantOtrDeviceAdapter(ViewHolder.ViewHolderClicks viewHolderClicks) {
        this.viewHolderClicks = viewHolderClicks;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_participant_otr_header, parent, false);
                return new OtrHeaderViewHolder(view, viewHolderClicks);
            case VIEW_TYPE_OTR_CLIENT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_participant_otr_device, parent, false);
                return new OtrClientViewHolder(view, viewHolderClicks);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindOtrClient(getOtrClient(position), accentColor, userDisplayName);
        holder.setIsLastItem(position == getItemCount() - 1);
    }

    private OtrClient getOtrClient(int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_OTR_CLIENT:
                return otrClients.get(position - 1);
            default:
                return null;
        }
    }

    @Override
    public int getItemViewType(int position) {
        switch (position) {
            case 0:
                return VIEW_TYPE_HEADER;
            default:
                return VIEW_TYPE_OTR_CLIENT;
        }
    }

    @Override
    public int getItemCount() {
        return otrClients == null || otrClients.size() == 0 ? 1 : otrClients.size() + 1;
    }

    public void setOtrClients(CoreList<OtrClient> otrClients) {
        this.otrClients = otrClients;
        notifyDataSetChanged();
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
        notifyDataSetChanged();
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
        notifyDataSetChanged();
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {

        protected ViewHolderClicks viewHolderClicks;

        public ViewHolder(View itemView, ViewHolderClicks viewHolderClicks) {
            super(itemView);
            this.viewHolderClicks = viewHolderClicks;
        }

        public abstract void bindOtrClient(OtrClient otrClient, int accentColor, String userDisplayName);
        public abstract void recycle();
        public abstract void setIsLastItem(boolean isLastItem);

        public interface ViewHolderClicks {
            void onOtrClientClick(OtrClient otrClient);
            void onOtrHeaderClick();
        }
    }

    public static class OtrClientViewHolder extends ViewHolder implements UpdateListener {

        private OtrClient otrClient;
        private View divider;
        private TextView textView;
        private ImageView imageView;

        public OtrClientViewHolder(View itemView, final ViewHolderClicks viewHolderClicks) {
            super(itemView, viewHolderClicks);
            textView = ViewUtils.getView(itemView, R.id.ttv__row_otr_device);
            imageView = ViewUtils.getView(itemView, R.id.iv__row_otr_icon);
            divider = ViewUtils.getView(itemView, R.id.v__row_otr__divider);
        }

        @Override
        public void bindOtrClient(final OtrClient otrClient, int accentColor, String userDisplayName) {
            if (this.otrClient != null) {
                this.otrClient.removeUpdateListener(this);
            }

            this.otrClient = otrClient;

            if (otrClient != null) {
                otrClient.addUpdateListener(this);
            }
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (viewHolderClicks != null) {
                        viewHolderClicks.onOtrClientClick(otrClient);
                    }
                }
            });
            updated();
        }

        @Override
        public void recycle() {
            if (otrClient != null) {
                otrClient.removeUpdateListener(this);
                otrClient = null;
            }
            textView.setText("");
            imageView.setImageDrawable(null);
        }

        @Override
        public void setIsLastItem(boolean isLastItem) {
            divider.setVisibility(isLastItem ? View.GONE : View.VISIBLE);
        }

        @Override
        public void updated() {
            String otrClientText = String.format(OTR_CLIENT_TEXT_TEMPLATE,
                                                 OtrUtils.getDeviceClassName(textView.getContext(), otrClient),
                                                 textView.getContext().getString(R.string.pref_devices_device_id, otrClient.getDisplayId()));
            CharSequence boldText = TextViewUtils.getBoldText(textView.getContext(),
                                                        otrClientText.toUpperCase(Locale.getDefault()));
            textView.setText(boldText);
            if (otrClient.getVerified() == Verification.VERIFIED) {
                imageView.setImageResource(R.drawable.shield_full);
            } else {
                imageView.setImageResource(R.drawable.shield_half);
            }
        }
    }

    public static class OtrHeaderViewHolder extends ViewHolder {

        private final TextView headerTextView;
        private final TextView linkTextView;
        private String userDisplayName;

        public OtrHeaderViewHolder(View itemView, final ViewHolderClicks viewHolderClicks) {
            super(itemView, viewHolderClicks);
            headerTextView = ViewUtils.getView(itemView, R.id.ttv__row__otr_header);
            linkTextView = ViewUtils.getView(itemView, R.id.ttv__row__otr_details_link);
        }

        @Override
        public void bindOtrClient(OtrClient otrClient, int accentColor, String userDisplayName) {
            this.userDisplayName = userDisplayName;
            headerTextView.setText("");
            linkTextView.setText(
                TextViewUtils.getHighlightText(linkTextView.getContext(),
                                               linkTextView.getContext().getString(R.string.otr__participant__device_header__link_text),
                                               accentColor,
                                               false));
            linkTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (viewHolderClicks != null) {
                        viewHolderClicks.onOtrHeaderClick();
                    }
                }
            });
        }

        @Override
        public void setIsLastItem(boolean isLastItem) {
            if (isLastItem) {
                headerTextView.setText(headerTextView.getContext().getString(R.string.otr__participant__device_header__no_devices, userDisplayName));
                linkTextView.setVisibility(View.GONE);
            } else {
                headerTextView.setText(headerTextView.getContext().getString(R.string.otr__participant__device_header, userDisplayName));
                linkTextView.setVisibility(View.VISIBLE);
            }
            ViewUtils.setPaddingTop(linkTextView, isLastItem ? linkTextView.getContext().getResources().getDimensionPixelSize(R.dimen.wire__padding__small) : 0);
        }

        @Override
        public void recycle() {
            headerTextView.setText("");
        }

    }

}
