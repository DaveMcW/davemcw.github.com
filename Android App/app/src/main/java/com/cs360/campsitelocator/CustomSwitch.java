package com.cs360.campsitelocator;

import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Switch with a method to adjust it without triggering the listener
 * https://stackoverflow.com/questions/7187287/
 */
public class CustomSwitch extends SwitchCompat {
    CompoundButton.OnCheckedChangeListener _listener;

    public CustomSwitch(@NonNull android.content.Context context) {
        super(context);
    }

    public CustomSwitch(@NonNull android.content.Context context, @Nullable android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSwitch(@NonNull android.content.Context context, @Nullable android.util.AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnCheckedChangeListener(@Nullable CompoundButton.OnCheckedChangeListener listener) {
        // No call to superclass!
        _listener = listener;
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (_listener != null) {
            _listener.onCheckedChanged(this, checked);
        }
    }

    public void setCheckedSilent(boolean checked) {
        // Call super method to avoid triggering listener
        super.setChecked(checked);
    }
}