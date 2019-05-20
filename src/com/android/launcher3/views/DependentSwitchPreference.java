/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

/**
 * Extension of {@link Preference} which makes the widget layout clickable.
 *
 * @see #setWidgetLayoutResource(int)
 */
public class DependentSwitchPreference extends SwitchPreference {

    private boolean mDependencyResolved = true;

    public DependentSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DependentSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DependentSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DependentSwitchPreference(Context context) {
        super(context);
    }

    public void setDependencyResolved(boolean resolved) {
        if (mDependencyResolved != resolved) {
            mDependencyResolved = resolved;
            this.setChecked(mDependencyResolved);
            notifyChanged();
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        final int id = Utilities.ATLEAST_NOUGAT ? android.R.id.switch_widget : Resources.getSystem().getIdentifier("switch_widget", "id", "android");
        View switchView = view.findViewById(id);
        ImageView warningIcon = view.findViewById(R.id.notification_pref_warning);

        if (mDependencyResolved) {
            if (warningIcon != null) warningIcon.setVisibility(View.GONE);
            switchView.setVisibility(View.VISIBLE);
        } else {
            if (warningIcon == null) {
                ViewGroup frame = view.findViewById(android.R.id.widget_frame);

                warningIcon = new ImageView(getContext());
                warningIcon.setId(R.id.notification_pref_warning);
                warningIcon.setLayoutParams(new ViewGroup.LayoutParams(48, 48));
                warningIcon.getLayoutParams().width = (int) (48 * view.getResources().getDisplayMetrics().density);
                warningIcon.getLayoutParams().height =  ViewGroup.LayoutParams.MATCH_PARENT;
                warningIcon.setImageResource(R.drawable.ic_warning);
                warningIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), android.R.color.tertiary_text_dark)));
                warningIcon.setScaleType(ImageView.ScaleType.CENTER);

                frame.addView(warningIcon);
            }
            warningIcon.setVisibility(View.VISIBLE);
            switchView.setVisibility(View.GONE);
        }
    }
}
