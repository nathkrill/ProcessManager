/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.android.preference;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.preference.DialogPreference;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import nl.adaptivity.process.editor.android.R;


/**
 * A {@link Preference} that allows for string
 * input.
 * <p>
 * It is a subclass of {@link DialogPreference} and shows the {@link EditText}
 * in a dialog. This {@link EditText} can be modified either programmatically
 * via {@link #getEditText()}, or through XML by setting any EditText
 * attributes on the EditTextPreference.
 * <p>
 * This preference will store a string into the SharedPreferences.
 * <p>
 */
public class AutoCompletePreference extends DialogPreference {
  /**
   * The edit text shown in the dialog.
   */
  private AutoCompleteTextView mEditText;

  private String mText;

  @TargetApi(VERSION_CODES.LOLLIPOP)
  public AutoCompletePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    int suggestionsId;
    {
      TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AutoCompletePreference, defStyleAttr, defStyleRes);
      try {
        suggestionsId = a.getResourceId(R.styleable.AutoCompletePreference_candidates, 0);
      } finally {
        a.recycle();
      }
    }

    setDialogLayoutResource(R.layout.auto_complete_preference);

    mEditText = new AutoCompleteTextView(context, attrs);

    // Give it an ID so it can be saved/restored
    mEditText.setId(R.id.edit);

        /*
         * The preference framework and view framework both have an 'enabled'
         * attribute. Most likely, the 'enabled' specified in this XML is for
         * the preference framework, but it was also given to the view framework.
         * We reset the enabled state.
         */
    mEditText.setEnabled(true);

    if (suggestionsId!=0) {
      String[] candidates = context.getResources().getStringArray(suggestionsId);
      mEditText.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, candidates));
    }

  }

  public AutoCompletePreference(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public AutoCompletePreference(Context context, AttributeSet attrs) {
    this(context, attrs, android.R.attr.editTextPreferenceStyle, 0);
  }

  public AutoCompletePreference(Context context) {
    this(context, null, android.R.attr.editTextPreferenceStyle, 0);
  }

  /**
   * Saves the text to the {@link SharedPreferences}.
   *
   * @param text The text to save
   */
  public void setText(String text) {
    final boolean wasBlocking = shouldDisableDependents();

    mText = text;

    persistString(text);

    final boolean isBlocking = shouldDisableDependents();
    if (isBlocking != wasBlocking) {
      notifyDependencyChange(isBlocking);
    }
  }

  /**
   * Gets the text from the {@link SharedPreferences}.
   *
   * @return The current preference value.
   */
  public String getText() {
    return mText;
  }

  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);

    EditText editText = mEditText;
    editText.setText(getText());

    ViewParent oldParent = editText.getParent();
    if (oldParent != view) {
      if (oldParent != null) {
        ((ViewGroup) oldParent).removeView(editText);
      } else {
        ((ViewGroup) view).removeView(view.findViewById(R.id.edit));
      }
      onAddEditTextToDialogView(view, editText);
    }
  }

  /**
   * Adds the EditText widget of this preference to the dialog's view.
   *
   * @param dialogView The dialog view.
   */
  protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
    ViewGroup container = (ViewGroup) dialogView
            .findViewById(R.id.edit_container);
    if (container != null) {
      container.addView(editText, ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
    }
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);

    if (positiveResult) {
      String value = mEditText.getText().toString();
      if (callChangeListener(value)) {
        setText(value);
      }
    }
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getString(index);
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
    setText(restoreValue ? getPersistedString(mText) : (String) defaultValue);
  }

  @Override
  public boolean shouldDisableDependents() {
    return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
  }

  /**
   * Returns the {@link EditText} widget that will be shown in the dialog.
   *
   * @return The {@link EditText} widget that will be shown in the dialog.
   */
  public AutoCompleteTextView getEditText() {
    return mEditText;
  }

  protected boolean needInputMethod() {
    // We want the input method to show, if possible, when dialog is displayed
    return true;
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    final Parcelable superState = super.onSaveInstanceState();
    if (isPersistent()) {
      // No need to save instance state since it's persistent
      return superState;
    }

    final SavedState myState = new SavedState(superState);
    myState.text = getText();
    return myState;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    if (state == null || !state.getClass().equals(SavedState.class)) {
      // Didn't save state for us in onSaveInstanceState
      super.onRestoreInstanceState(state);
      return;
    }

    SavedState myState = (SavedState) state;
    super.onRestoreInstanceState(myState.getSuperState());
    setText(myState.text);
  }

  private static class SavedState extends BaseSavedState {
    String text;

    public SavedState(Parcel source) {
      super(source);
      text = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeString(text);
    }

    public SavedState(Parcelable superState) {
      super(superState);
    }

    public static final Parcelable.Creator<SavedState> CREATOR =
            new Parcelable.Creator<SavedState>() {
              public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
              }

              public SavedState[] newArray(int size) {
                return new SavedState[size];
              }
            };
  }

}