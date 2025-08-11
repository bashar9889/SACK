package com.example.sack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class TimeSetPage extends AppCompatActivity {

    private TimePicker timePicker;
    private TextView tvSelectedDays;
    private Spinner spinnerAlarmSound;
    private Switch switchVibrate;
    private Button btnSave, btnCancel;
    private ArrayList<String> selectedDays;
    private DatabaseHelper databaseHelper;
    private int userId = 1;
    private String selectedSound = "Default"; // Default selected sound

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timesetpage);

        // Initialize UI components
        timePicker = findViewById(R.id.time_picker);
        tvSelectedDays = findViewById(R.id.tv_selected_days);
        spinnerAlarmSound = findViewById(R.id.spinner_alarm_sound);
        switchVibrate = findViewById(R.id.switch_vibrate);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel_set);
        databaseHelper = new DatabaseHelper(this);
        selectedDays = new ArrayList<>();

        // Ensure TimePicker uses spinner mode and AM/PM selection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            timePicker.setIs24HourView(false); // Ensure AM/PM is available
            timePicker.setDescendantFocusability(TimePicker.FOCUS_BLOCK_DESCENDANTS); // Prevent manual input
        }

        // Apply white text color manually on startup
        applyTimePickerTextColor(timePicker);

        // 🔥 Add listener to dynamically fix colors when scrolling
        timePicker.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                applyTimePickerTextColor(timePicker); // Reapply colors on layout updates
            }
        });

        // Setup click listeners for day selection
        setupDaySelection();

        // Setup Alarm Sound Selection
        setupAlarmSoundSpinner();

        // Handle Cancel Button
        btnCancel.setOnClickListener(v -> finish());

        // Handle Save Button
        btnSave.setOnClickListener(v -> saveAlarm());
    }

    /**
     * Recursively applies white text color to TimePicker elements.
     */
    private void applyTimePickerTextColor(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(Color.WHITE);  // Force white text
            } else if (child instanceof ViewGroup) {
                applyTimePickerTextColor((ViewGroup) child);  // Recursively apply to subviews
            }
        }
    }

    private void setupDaySelection() {
        int[] dayIds = {R.id.btn_sun, R.id.btn_mon, R.id.btn_tue, R.id.btn_wed, R.id.btn_thu, R.id.btn_fri, R.id.btn_sat};
        String[] dayLabels = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        for (int i = 0; i < dayIds.length; i++) {
            TextView dayView = findViewById(dayIds[i]);
            String day = dayLabels[i];

            dayView.setOnClickListener(v -> {
                if (selectedDays.contains(day)) {
                    selectedDays.remove(day);
                    dayView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    selectedDays.add(day);
                    dayView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }
                updateSelectedDaysText();
            });
        }
    }

    private void updateSelectedDaysText() {
        tvSelectedDays.setText(String.join(", ", selectedDays));
    }


    //Sets up the spinner for selecting the alarm sound.
    private void setupAlarmSoundSpinner() {
        // Create an ArrayAdapter using alarm sounds
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.alarm_sounds, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlarmSound.setAdapter(adapter);

        // Reference the TextView to display selected sound
        TextView tvSelectedSound = findViewById(R.id.tv_selected_alarm_sound);

        // Handle user selection
        spinnerAlarmSound.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSound = parent.getItemAtPosition(position).toString(); // Save selected sound
                tvSelectedSound.setText("Selected Sound: " + selectedSound); // 🔹 Update UI
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSound = "Default"; // Default sound
                tvSelectedSound.setText("Selected Sound: Default"); // 🔹 Update UI
            }
        });
    }


    //Save the Alarm in the Database
    private void saveAlarm() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String time = String.format("%02d:%02d", hour, minute);
        String repeatDays = selectedDays.isEmpty() ? "None" : String.join(", ", selectedDays);
        boolean vibrateEnabled = switchVibrate.isChecked();

        // Store the selected alarm sound type
        String alarmSound = selectedSound;
        String alarmLabel = "Alarm"; // You can modify this if needed

        //Pass parameters in the correct order
        long result = databaseHelper.insertAlarm(userId, time, alarmLabel, repeatDays, alarmSound, vibrateEnabled);

        if (result > 0) {
            System.out.println("DEBUG: Alarm successfully saved - Time: " + time + ", Sound: " + alarmSound + ", Repeat: " + repeatDays);
            Toast.makeText(this, "Alarm Saved!", Toast.LENGTH_SHORT).show();

            Intent returnIntent = new Intent();
            setResult(RESULT_OK, returnIntent);
            finish();
        } else {
            System.out.println("DEBUG: Failed to save alarm.");
            Toast.makeText(this, "Failed to save alarm", Toast.LENGTH_SHORT).show();
        }
    }

}
