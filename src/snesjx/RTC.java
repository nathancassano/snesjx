/*
 * SnesJx - Portable Super Nintendo Entertainment System (TM) emulator.
 * 
 * (C) Copyright 2009 Nathan Cassano
 *
 * Permission to use, copy, modify and distribute SnesJx in both binary and
 * source form, for non-commercial purposes, is hereby granted without fee,
 * providing that this license information and copyright notice appear with
 * all copies and any derived work.
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event shall the authors be held liable for any damages
 * arising from the use of this software.
 *
 * SnesJx is freeware for PERSONAL USE only. Commercial users should
 * seek permission of the copyright holders first. Commercial use includes
 * charging money for SnesJx or software derived from SnesJx.
 *
 * The copyright holders request that bug fixes and improvements to the code
 * should be forwarded to them so everyone can benefit from the modifications
 * in future versions.
 *
 * Super NES and Super Nintendo Entertainment System are trademarks of
 * Nintendo Co., Limited and its subsidiary companies.
 */

package snesjx;

import java.util.Date;

class RTC
{
	private static int MAX_RTC_INDEX = 0xC;
	
	static final int MODE_READ = 0;
	static final int MODE_LOAD_RTC = 1;
	static final int MODE_COMMAND = 2;
	static final int MODE_COMMAND_DONE = 3;
	static final int COMMAND_LOAD_RTC = 0;
	static final int COMMAND_CLEAR_RTC = 4;
	
	static final int SRTC_SRAM_PAD = (4 + 8 + 1 + MAX_RTC_INDEX);

	private static final int DAYTICKS = (60*60*24);
	private static final int HOURTICKS = (60*60);
	private static final int MINUTETICKS  = 60;
	
	private boolean needs_init;
	private boolean count_enable;
	private int data[] = new int[MAX_RTC_INDEX+1];
	int index;
	int mode;
	
	private long system_timestamp;
	private long pad;
	
	private int month_keys[] = { 1, 4, 4, 0, 2, 5, 0, 3, 6, 1, 4, 6 };
	
	Date date = new Date();
	
	Globals globals;
	
	void ResetSRTC ()
	{
		index = -1;
		mode = MODE_READ;
	}
	
	void setUp()
	{
		globals = Globals.globals;
	}

	void HardResetSRTC ()
	{
		index = -1;
		mode = MODE_READ;
		count_enable = false;
		needs_init = true;
		for ( int i = 0; i < data.length; i++) data[i] = 0;
		pad = 0;

		// Get system timestamp
		system_timestamp = date.getTime();
	}

	// Get 0-6 for Sunday-Saturday															 */
	private int SRTCComputeDayOfWeek ()
	{
		int day_of_week = 0;

		int year = data[10] * 10 + data[9];
		int month = data[8];
		int day = data[7] * 10 + data[6];

		year += (data[11] - 9) * 100;

		// Range check the month for valid array indicies
		if ( month > 12 )
			month = 1;

		day_of_week = year + (year / 4) + month_keys[month-1] + day - 1;

		if( ( year % 4 == 0 ) && ( month <= 2 ) )
			day_of_week--;

		day_of_week %= 7;

		return day_of_week;
	}


	
	// Get the number of days in a specific month for a certain year
	private int SRTCDaysInMmonth( int month, int year )
	{
		int	mdays;

		switch ( month )
		{
		case 2:
			if ( ( year % 4 == 0 ) )	// DKJM2 only uses 199x - 22xx
				mdays = 29;
			else
				mdays = 28;
			break;

		case 4:
		case 6:
		case 9:
		case 11:
			mdays = 30;
			break;

		default:	// months 1,3,5,7,8,10,12
			mdays = 31;
			break;
		}

		return mdays;
	}


	// Advance the  S-RTC time if counting is enabled											 */
	private void UpdateSrtcTime()
	{		
		long cur_systime;
		long time_diff;

		// Keep track of game time by computing the number of seconds that pass on the system
		// clock and adding the same number of seconds to the S-RTC clock structure.
		// I originally tried using mktime and localtime library functions to keep track
		// of time but some of the GNU time functions fail when the year goes to 2099
		// (and maybe less) and this would have caused a bug with DKJM2 so I'm doing
		// it this way to get around that problem.

		// Note: Dai Kaijyu Monogatari II only allows dates in the range 1996-21xx.

		if (count_enable && !needs_init)
		{
			cur_systime = date.getTime();

			// This method assumes one time_t clock tick is one second
			//		which should work on PCs and GNU systems.
			//		If your tick interval is different adjust the
			// DAYTICK, HOURTICK, and MINUTETICK defines

			time_diff = (long) (cur_systime - system_timestamp);
			system_timestamp = cur_systime;

			if ( time_diff > 0 )
			{
				int		seconds;
				int		minutes;
				int		hours;
				int		days;
				int		month;
				int		year;
				int		temp_days;
	
				int		year_hundreds;
				int		year_tens;
				int		year_ones;
	
				if ( time_diff > DAYTICKS )
				{
					days = (int) (time_diff / DAYTICKS);
					time_diff = time_diff - days * DAYTICKS;
				}
				else
				{
					days = 0;
				}
	
				if ( time_diff > HOURTICKS )
				{
					hours = (int) (time_diff / HOURTICKS);
					time_diff = time_diff - hours * HOURTICKS;
				}
				else
				{
					hours = 0;
				}
	
				if ( time_diff > MINUTETICKS )
				{
					minutes = (int) (time_diff / MINUTETICKS);
					time_diff = time_diff - minutes * MINUTETICKS;
				}
				else
				{
					minutes = 0;
				}
	
				if ( time_diff > 0 )
				{
					seconds = (int) time_diff;
				}
				else
				{
					seconds = 0;
				}

				seconds += (data[1]*10 + data[0]);
				
				if ( seconds >= 60 )
				{
					seconds -= 60;
					minutes += 1;
				}
	
				minutes += (data[3]*10 + data[2]);
				if ( minutes >= 60 )
				{
					minutes -= 60;
					hours += 1;
				}
	
				hours += (data[5]*10 + data[4]);
				if ( hours >= 24 )
				{
					hours -= 24;
					days += 1;
				}
	
				if ( days > 0 )
				{
					year =  data[10]*10 + data[9];
					year += ( 1000 + data[11] * 100 );
	
					month = data[8];
					days += (data[7]*10 + data[6]);
					while ( days > (temp_days = SRTCDaysInMmonth( month, year )) )
					{
						days -= temp_days;
						month += 1;
						
						if ( month > 12 )
						{
							year += 1;
							month = 1;
						}
					}
	
					year_tens = year % 100;
					year_ones = year_tens % 10;
					year_tens /= 10;
					year_hundreds = (year - 1000) / 100;
	
					data[6] = days % 10;
					data[7] = days / 10;
					data[8] = month;
					data[9] = year_ones;
					data[10] = year_tens;
					data[11] = year_hundreds;
					data[12] = SRTCComputeDayOfWeek ();
				}
	
				data[0] = seconds % 10;
				data[1] = seconds / 10;
				data[2] = minutes % 10;
				data[3] = minutes / 10;
				data[4] = hours % 10;
				data[5] = hours / 10;
			}
		}
	}

	// This function sends data to the S-RTC used in Dai Kaijyu Monogatari II
	void SetSRTC (int value, int Address)
	{

		value &= 0x0F;	// Data is only 4-bits, mask out unused bits.

		if( value >= 0xD )
		{
			// It's an RTC command

			switch ( value )
			{
				case 0xD:
					mode = MODE_READ;
					index = -1;
					break;

				case 0xE:
					mode = MODE_COMMAND;
					break;

				default:
					// Ignore the write if it's an 0xF ???
					// Probably should switch back to read mode -- but this
					//  sequence never occurs in DKJM2
					break;
			}

			return;
		}

		if ( mode == MODE_LOAD_RTC )
		{
			if ( (index >= 0) && (index < MAX_RTC_INDEX) )
			{
				data[index++] = value;

				if ( index == MAX_RTC_INDEX )
				{
					// We have all the data for the RTC load
					system_timestamp = date.getTime();	// Get local system time

					// Get the day of the week
					data[index++] = SRTCComputeDayOfWeek();

					// Start RTC counting again
					count_enable = true;
					needs_init = false;
				}

				return;
			}
			else
			{
				// Attempting to write too much data
				// error(); // ignore??
			}
		}
		else if ( mode == MODE_COMMAND )
		{
			switch( value )
			{
				case COMMAND_CLEAR_RTC:
					// Disable RTC counter
					count_enable = false;

					for ( int i = 0; i < data.length; i++) data[i] = 0;
					index = -1;
					mode = MODE_COMMAND_DONE;
					break;

				case COMMAND_LOAD_RTC:
					// Disable RTC counter
					count_enable = false;

					index = 0;  // Setup for writing
					mode = MODE_LOAD_RTC;
					break;

				default:
					mode = MODE_COMMAND_DONE;
					// unrecognized command - need to implement.
			}

			return;
		}
		else
		{
			if ( mode == MODE_READ )
			{
				// Attempting to write while in read mode. Ignore.
			}

			if ( mode == MODE_COMMAND_DONE )
			{
				// Maybe this isn't an error.  Maybe we should kick off
				// a new E command.  But is this valid?
			}
		}
	}

	// Retrieves data from the S-RTC												*/
	int GetSRTC (int Address)
	{
		if ( mode == MODE_READ )
		{
			if ( index < 0 )
			{
				UpdateSrtcTime ();	// Only update it if the game reads it
				index++;
				return ( 0x0f );		// Send start marker.
			}
			else if (index > MAX_RTC_INDEX)
			{
				index = -1;		 // Setup for next set of reads
				return ( 0x0f );		// Data done marker.
			}
			else
			{
				// Feed out the data
				return data[index++];
			}
		 }
		 else
		 {
			 return 0x0;
		 }
	}

	void SRTCPreSaveState ()
	{
		Memory Memory = globals.memory;
		
		if (globals.settings.SRTC)
		{
			UpdateSrtcTime ();
	
			int s = Memory.SRAMSize > 0 ?
				(1 << (Memory.SRAMSize + 3)) * 128 : 0;
			if (s > 0x20000)
				s = 0x20000;
	
			Memory.SRAM.put8Bit(s,  needs_init ? 1 : 0);
			Memory.SRAM.put8Bit(s + 1, count_enable ? 1 : 0);
			
			for (int i = 0; i < data.length; i++)
			{
				Memory.SRAM.put8Bit(s + 2 + i, data[i]);
			}
			
			Memory.SRAM.put8Bit(s + 3 + MAX_RTC_INDEX, index);
			Memory.SRAM.put8Bit(s + 4 + MAX_RTC_INDEX, mode);

			for (int i = 0; i < 8; i++)
			{
				Memory.SRAM.put8Bit(s + 5 + MAX_RTC_INDEX + i, (int) ((system_timestamp >> (i * 8) ) & 0xFF) );
			}
		}
	}

	void SRTCPostLoadState ()
	{
		Memory Memory = globals.memory;
		
		if (globals.settings.SRTC)
		{
			int s = Memory.SRAMSize > 0 ? (1 << (Memory.SRAMSize + 3)) * 128 : 0;
			
			if (s > 0x20000)
			{
				s = 0x20000;
			}
	
			needs_init = Memory.SRAM.get8Bit(s) == 1 ? true : false;
			count_enable = Memory.SRAM.get8Bit(s + 1) == 1 ? true : false;
			
			for (int i = 0; i < data.length; i++)
			{
				data[i] = Memory.SRAM.get8Bit(s + 2 + i);
			}

			index = Memory.SRAM.get8Bit(s + 3 + MAX_RTC_INDEX);
			mode = Memory.SRAM.get8Bit(s + 4 + MAX_RTC_INDEX);
			
			system_timestamp = 0;
			
			for (int i = 0; i < 8; i++)
			{
				system_timestamp |= Memory.SRAM.get8Bit(s + 5 + MAX_RTC_INDEX + i) << (i * 8);
			}
	
			UpdateSrtcTime();
		}
	}
}
