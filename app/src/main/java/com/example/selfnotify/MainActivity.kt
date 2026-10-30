package com.example.selfnotify

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.selfnotify.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Epoch millis chosen with the date/time pickers; 0 means nothing chosen yet. */
    private var selectedTime: Long = 0L

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied_hint),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createChannels(this)
        requestNotificationPermissionIfNeeded()

        binding.btnPickTime.setOnClickListener { pickDateTime() }
        binding.btnSendNow.setOnClickListener { sendNow() }
        binding.btnSchedule.setOnClickListener { scheduleNotification() }
        binding.btnCancelAll.setOnClickListener { cancelAll() }

        refreshUpcoming()
    }

    // ---------------------------------------------------------------- Actions

    private fun sendNow() {
        val title = titleText()
        if (title.isEmpty()) {
            binding.inputTitle.error = getString(R.string.error_title_empty)
            return
        }

        NotificationHelper.show(
            context = this,
            id = newId(),
            title = title,
            text = textBody(),
            channelId = selectedChannel(),
        )
        Toast.makeText(this, getString(R.string.toast_sent), Toast.LENGTH_SHORT).show()
    }

    private fun scheduleNotification() {
        val title = titleText()
        if (title.isEmpty()) {
            binding.inputTitle.error = getString(R.string.error_title_empty)
            return
        }

        if (selectedTime == 0L) {
            // Nothing picked yet: open the pickers right away.
            pickDateTime()
            return
        }

        requestExactAlarmPermissionIfNeeded()

        val sn = ScheduledNotification(
            id = newId(),
            title = title,
            text = textBody(),
            channelId = selectedChannel(),
            time = selectedTime,
        )
        NotificationHelper.schedule(this, sn)

        refreshUpcoming()
        Toast.makeText(this, getString(R.string.toast_scheduled), Toast.LENGTH_SHORT).show()
    }

    private fun cancelAll() {
        NotificationHelper.cancelAll(this)
        refreshUpcoming()
        Toast.makeText(this, getString(R.string.toast_cancelled_all), Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------------- Pickers

    private fun pickDateTime() {
        val now = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val cal = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (cal.timeInMillis <= System.currentTimeMillis()) {
                            Toast.makeText(
                                this,
                                getString(R.string.error_time_past),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TimePickerDialog
                        }
                        selectedTime = cal.timeInMillis
                        binding.tvScheduleTime.text = formatTime(selectedTime)
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true,
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    // ---------------------------------------------------------------- Helpers

    private fun titleText(): String = binding.inputTitle.text?.toString()?.trim().orEmpty()

    private fun textBody(): String = binding.inputText.text?.toString()?.trim().orEmpty()

    private fun selectedChannel(): String = when (binding.typeGroup.checkedRadioButtonId) {
        R.id.radioHigh -> NotificationHelper.CHANNEL_HIGH
        R.id.radioLow -> NotificationHelper.CHANNEL_LOW
        R.id.radioSilent -> NotificationHelper.CHANNEL_SILENT
        else -> NotificationHelper.CHANNEL_DEFAULT
    }

    private fun newId(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

    private fun refreshUpcoming() {
        val upcoming = NotificationHelper.listScheduled(this).sortedBy { it.time }
        binding.tvUpcoming.text = if (upcoming.isEmpty()) {
            getString(R.string.no_upcoming)
        } else {
            upcoming.joinToString(separator = "\n") { sn ->
                "${formatTime(sn.time)} — ${sn.title}"
            }
        }
    }

    private fun formatTime(epochMillis: Long): String =
        SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault()).format(epochMillis)

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) return

        Toast.makeText(this, getString(R.string.exact_alarm_hint), Toast.LENGTH_LONG).show()
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName"),
                )
            )
        } catch (_: Exception) {
            // The exact alarm setting screen is unavailable; scheduling still works (inexact).
        }
    }
}
