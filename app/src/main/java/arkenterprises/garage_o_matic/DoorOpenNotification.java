package arkenterprises.garage_o_matic;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * Helper class for showing and canceling new message
 * notifications.
 * <p/>
 * This class makes heavy use of the {@link NotificationCompat.Builder} helper
 * class to create notifications in a backward-compatible way.
 */
public class DoorOpenNotification {
    /**
     * The unique identifier for this type of notification.
     */
    private static final String NOTIFICATION_TAG = "DoorOpenNotification";

    public static final Integer DOOR_OPEN_CODE = 0;
    public static final Integer DOOR_AUTO_CLOSED_CODE = 1;
    public static final Integer DOOR_OPEN_TOO_LONG_CODE = 2;


    /**
     * Shows the notification, or updates a previously shown notification of
     * this type, with the given parameters.
     *
     * @see #cancel(Context, Integer)
     */
    public static void notify(final Context context,
                              final String doorOpenTime,
                              final String doorOpenDuration,
                              final int notificationType) {
        final Resources res = context.getResources();

        // This image is used as the notification's large icon (thumbnail).
//        final Bitmap picture = BitmapFactory.decodeResource(res, R.drawable.example_picture);

//        final String ticker = doorOpenTime;

        final String title;
        final String text;
        if (notificationType == DOOR_OPEN_CODE) {
            title = res.getString(R.string.door_open_notification_title_template);
            text = res.getString(R.string.door_open_notification_text_template, doorOpenTime);
        }else if (notificationType == DOOR_AUTO_CLOSED_CODE){
            title = res.getString(R.string.door_auto_closed_title_template);
            text = res.getString(R.string.door_auto_closed_text_template, doorOpenTime);
        }else if (notificationType == DOOR_OPEN_TOO_LONG_CODE){
            title = res.getString(R.string.door_open_too_long_title_template);
            text = res.getString(R.string.door_open_too_long_text_template, doorOpenTime, doorOpenDuration);
        }else {
            title = "Error";
            text = "Error";
        }


        final String channelID = "garageChannel";
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelID)

                // Set appropriate defaults for the notification light, sound,
                // and vibration.
                .setDefaults(Notification.DEFAULT_ALL)

                        // Set required fields, including the small icon, the
                        // notification title, and text.
                .setSmallIcon(R.drawable.ic_door_open_notification2)
                .setContentTitle(title)
                .setContentText(text)

                        // All fields below this line are optional.

                        // Use a default priority (recognized on devices running Android
                        // 4.1 or later)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                        // Provide a large icon, shown with the notification in the
                        // notification drawer on devices running Android 3.0 or later.
//                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_door_open_notification2))

                        // Set ticker text (preview) information for this notification.
                .setTicker(doorOpenTime)

                        // Show a number. This is useful when stacking notifications of
                        // a single type.
//                .setNumber(number)

                        // Set the pending intent to be initiated when the user touches
                        // the notification.
                .setContentIntent(
                        PendingIntent.getActivity(
                                context,
                                0,
                                new Intent(context, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT))

                        // Show expanded text content on devices running Android 4.1 or
                        // later.
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text)
                        .setBigContentTitle(title))
//                        .setSummaryText(res.getString(R.string.door_open_notification_summary)))

                        // Automatically dismiss the notification when it is touched.
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);


        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID,
                    "Garage Door Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }
        nm.notify(NOTIFICATION_TAG, notificationType,  builder.build());
//        notify(context, builder.build(), notificationType);
    }

//    @TargetApi(Build.VERSION_CODES.ECLAIR)
//    private static void notify(final Context context, final Notification notification, final Integer code) {
//        final NotificationManager nm = (NotificationManager) context
//                .getSystemService(Context.NOTIFICATION_SERVICE);
//
//        nm.notify(NOTIFICATION_TAG, code, notification);
//    }

    /**
     * Cancels any notifications of this type previously shown
     */
//    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public static void cancel(final Context context, final Integer code) {
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_TAG, code);
    }
}
