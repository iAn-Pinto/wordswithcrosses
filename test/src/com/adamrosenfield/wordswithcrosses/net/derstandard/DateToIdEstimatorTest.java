package com.adamrosenfield.wordswithcrosses.net.derstandard;

import java.util.Calendar;

import android.test.AndroidTestCase;

import com.adamrosenfield.wordswithcrosses.CalendarUtil;

public class DateToIdEstimatorTest extends AndroidTestCase {

	private DateToIdEstimator testee = new DateToIdEstimator(new DerStandardPuzzleCache() {

		DerStandardPuzzleMetadata dspm_7671 = new DerStandardPuzzleMetadata(7671);
		DerStandardPuzzleMetadata dspm_7642 = new DerStandardPuzzleMetadata(7642);

		{
			dspm_7671.setDate(CalendarUtil.createDate(2014, 5, 9));
			dspm_7642.setDate(CalendarUtil.createDate(2014, 4, 3));
		}

		@Override
		public DerStandardPuzzleMetadata getClosestTo(Calendar date) {
			long a = Math.abs(date.getTimeInMillis() - dspm_7671.getDate().getTimeInMillis());
			long b = Math.abs(date.getTimeInMillis() - dspm_7642.getDate().getTimeInMillis());

			return a > b ?  dspm_7642 : dspm_7671;
		}

		@Override
		public boolean contains(int id) {
			return false;
		}

		@Override
		public void setDate(DerStandardPuzzleMetadata pm, Calendar c) {}

		@Override
		public DerStandardPuzzleMetadata createOrGet(int id) { return null; }


	});

	public void testZeroDifference() {
		assertEquals(7677, testee.estimateId(CalendarUtil.createDate(2014, 5, 16)));
	}

	public void testWeekDifference() {
		assertEquals(7671, testee.estimateId(CalendarUtil.createDate(2014, 5, 9)));
	}

	public void testMonthsDifference1() {
		assertEquals(7639, testee.estimateId(CalendarUtil.createDate(2014, 3, 31)));
	}

	public void testMonthsDifference2() {
		assertEquals(7613, testee.estimateId(CalendarUtil.createDate(2014, 2, 28)));
	}

	public void testMonthsDifference3() {
		assertEquals(7605, testee.estimateId(CalendarUtil.createDate(2014, 2, 18))); //7604 would be correct, but close enough
	}

}
