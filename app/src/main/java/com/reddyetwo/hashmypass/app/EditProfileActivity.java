package com.reddyetwo.hashmypass.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.reddyetwo.hashmypass.app.data.DataOpenHelper;
import com.reddyetwo.hashmypass.app.data.PasswordType;
import com.reddyetwo.hashmypass.app.data.ProfileSettings;

public class EditProfileActivity extends Activity {

    public static final String EXTRA_PROFILE_ID = "profile_id";

    // State bundle keys
    private static final String KEY_PASSWORD_LENGTH = "password_length";

    // UI Widgets
    private EditText mNameEditText;
    private EditText mPrivateKeyEditText;
    private Spinner mPasswordLengthSpinner;
    private Spinner mPasswordTypeSpinner;
    private Button mDiscardButton;
    private Button mSaveButton;

    private ArrayAdapter<String> mPasswordLengthAdapter;

    // Activity status
    private long mProfileID;
    private String mOriginalName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_profile);
        mProfileID = getIntent().getLongExtra(EXTRA_PROFILE_ID, -1);

        DataOpenHelper helper = new DataOpenHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();
        String query = "SELECT * FROM " + DataOpenHelper.PROFILES_TABLE_NAME +
                " WHERE _id=" + mProfileID;
        Cursor cursor = db.rawQuery(query, null);
        if (!cursor.moveToFirst()) {
            // TODO This should be an error
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setSubtitle(cursor.getString(
                cursor.getColumnIndex(DataOpenHelper.COLUMN_PROFILES_NAME)));

        /* Get UI widgets */
        mNameEditText = (EditText) findViewById(R.id.profile_name_text);
        mPrivateKeyEditText = (EditText) findViewById(R.id.private_key_text);
        mPasswordLengthSpinner =
                (Spinner) findViewById(R.id.password_length_spinner);
        mPasswordTypeSpinner =
                (Spinner) findViewById(R.id.password_type_spinner);
        mDiscardButton = (Button) findViewById(R.id.discard_button);
        mSaveButton = (Button) findViewById(R.id.save_button);

        /* Populate text fields */
        mOriginalName = cursor.getString(
                cursor.getColumnIndex(DataOpenHelper.COLUMN_PROFILES_NAME));
        mNameEditText.setText(mOriginalName);

        mPrivateKeyEditText.setText(cursor.getString(cursor.getColumnIndex(
                DataOpenHelper.COLUMN_PROFILES_PRIVATE_KEY)));

        /* Populate password type spinner */
        ArrayAdapter<CharSequence> adapter = ArrayAdapter
                .createFromResource(this, R.array.password_types_array,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mPasswordTypeSpinner.setAdapter(adapter);
        mPasswordTypeSpinner.setSelection(cursor.getInt(cursor.getColumnIndex(
                DataOpenHelper.COLUMN_PROFILES_PASSWORD_TYPE)));

        /* Populate password length spinner */
        int passwordLength;
        if (savedInstanceState != null) {
            passwordLength = savedInstanceState.getInt(KEY_PASSWORD_LENGTH);
        } else {
            passwordLength = cursor.getInt(cursor.getColumnIndex(
                    DataOpenHelper.COLUMN_PROFILES_PASSWORD_LENGTH));
        }
        populatePasswordLengthSpinner(passwordLength);

        /* Open number picker dialog when the password length spinner is
           touched */
        mPasswordLengthSpinner.setOnTouchListener(
                new MovementTouchListener(this,
                        new MovementTouchListener.OnPressedListener() {
                            @Override
                            public void onPressed() {
                                showDialog();
                            }
                        })
        );

        /* Add profile to database when Add button is pressed */
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileSettings.updateProfileSettings(EditProfileActivity
                                .this, mProfileID,
                        mNameEditText.getText().toString(),
                        mPrivateKeyEditText.getText().toString(),
                        Integer.decode((String) mPasswordLengthSpinner
                                .getSelectedItem()),
                        PasswordType.values()[mPasswordTypeSpinner
                                .getSelectedItemPosition()]
                );

                /* Navigate to previous activity */
                NavUtils.navigateUpFromSameTask(EditProfileActivity.this);
            }
        });

        mDiscardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(EditProfileActivity.this);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_PASSWORD_LENGTH, Integer.parseInt(
                        (String) mPasswordLengthSpinner.getSelectedItem())
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (id == R.id.action_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.confirm_delete, mOriginalName));
            builder.setPositiveButton(R.string.action_delete,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DataOpenHelper helper =
                                    new DataOpenHelper(EditProfileActivity
                                            .this);
                            SQLiteDatabase db = helper.getWritableDatabase();
                            db.delete(DataOpenHelper.PROFILES_TABLE_NAME,
                                    "_id=" + mProfileID, null);
            /* TODO Check delete return value */
                            NavUtils.navigateUpFromSameTask(
                                    EditProfileActivity.this);
                        }
                    }
            );
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create().show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Shows a number picker dialog for choosing the password length */
    private void showDialog() {
        PasswordLengthDialogFragment dialogFragment =
                new PasswordLengthDialogFragment();
        dialogFragment.setPasswordLength(Integer.parseInt(
                (String) mPasswordLengthSpinner.getSelectedItem()));
        dialogFragment.setOnSelectedListener(
                new PasswordLengthDialogFragment.OnSelectedListener() {
                    @Override
                    public void onPasswordLengthSelected(int length) {
                        populatePasswordLengthSpinner(length);
                    }
                }
        );

        dialogFragment.show(getFragmentManager(), "passwordLength");
    }

    private void populatePasswordLengthSpinner(int length) {
        mPasswordLengthAdapter =
                new ArrayAdapter<String>(EditProfileActivity.this,
                        android.R.layout.simple_spinner_item,
                        new String[]{String.valueOf(length)});
        mPasswordLengthAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mPasswordLengthSpinner.setAdapter(mPasswordLengthAdapter);
    }

}
