package no.rehn.android.trafikanten;

import java.util.Calendar;
import java.util.Date;

public final class TimeUtils {
	static long mStaticTime = 0;
	public static long currentTimeMillis() {
		if (mStaticTime > 0) {
			return mStaticTime;
		}
		return System.currentTimeMillis();
	}
	
	static void setStaticTime(long staticTime) {
		mStaticTime = staticTime;
	}

	public static Date newDate() {
		return new Date(currentTimeMillis());
	}

	public static Calendar newCalendar() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(currentTimeMillis());
		return calendar;
	}
}
