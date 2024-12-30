import android.content.Context
import android.util.Log

object SharedPreferencesHelper {
    private const val PREFS_NAME = "EmergencyPrefs"
    private const val KEY_NAME = "Name"
    private const val KEY_SENDER_EMAIL = "SenderEmail"
    private const val KEY_RECEIVER_EMAIL = "ReceiverEmail"
    private const val LAST_EMAIL_DATE_KEY = "lastEmailDate"

    fun getLastEmailSentTime(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LAST_EMAIL_DATE_KEY, 0L)
    }

    fun setLastEmailSentTime(context: Context, timestamp: Long) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong(LAST_EMAIL_DATE_KEY, timestamp)
            apply()
        }
    }

    fun saveEmails(context: Context, name: String, sender: String, receiver: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_NAME, name)
            putString(KEY_SENDER_EMAIL, sender)
            putString(KEY_RECEIVER_EMAIL, receiver)
            apply()
            Log.d("SharedPreferences", "Saved: Name=$name, Sender=$sender, Receiver=$receiver")
        }
    }

    fun getEmails(context: Context): Triple<String?, String?, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_NAME, null)
        val sender = prefs.getString(KEY_SENDER_EMAIL, null)
        val receiver = prefs.getString(KEY_RECEIVER_EMAIL, null)

        Log.d("SharedPreferences", "Retrieved: Name=$name, Sender=$sender, Receiver=$receiver")
        return Triple(name, sender, receiver)
    }

    fun clearEmails(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d("SharedPreferences", "Preferences cleared")
    }
}
