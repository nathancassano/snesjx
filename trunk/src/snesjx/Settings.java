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

import java.util.HashMap;

class Settings {

	/* CPU options */
	boolean  APUEnabled;
	boolean  Shutdown;
	int  SoundSkipMethod;
	int  HDMATimingHack;

	boolean  Paused;
	boolean  ForcedPause;
	boolean  StopEmulation;
	boolean  FrameAdvance;
	
	/* Tracing options */
	boolean  TraceDMA;
	boolean  TraceHDMA;
	boolean  TraceVRAM;
	boolean  TraceDSP;
	
	/* Joystick options */
	boolean  JoystickEnabled;
	
	/* ROM timing options (see also H_Max above) */
	boolean  ForcePAL;
	boolean  ForceNTSC;
	boolean  PAL;
	int FrameTimePAL;
	int FrameTimeNTSC;
	int FrameTime;
	int SkipFrames;
	
	boolean  SRTC;
	
	boolean  ShutdownMaster;
	boolean  SuperScopeMaster;
	boolean  MouseMaster;

	static final boolean SuperFX = false;
	boolean  DSP1Master;
	boolean  SA1;
	
	//boolean  C4;
	//boolean  SDD1;
	//boolean  SPC7110;
	//boolean  SPC7110RTC;
	//boolean  OBC1;

	/* Sound options */
	int SoundPlaybackRate;
	boolean  TraceSoundDSP;
	boolean  Stereo;
	boolean  SixteenBitSound;
	int	SoundBufferSize;
	int	SoundMixInterval;
	boolean  SoundEnvelopeHeightReading;
	boolean  DisableSoundEcho;
	boolean  DisableSampleCaching;
	boolean  DisableMasterVolume;
	int SoundSync;
	boolean  FakeMuteFix;
	boolean  InterpolatedSound;
	boolean  ThreadSound;
	boolean  Mute;
	boolean  NextAPUEnabled;
	int  AltSampleDecode;
	boolean  FixFrequency;
	
	/* Multi ROMs */
	boolean  Multi;
	String CartAName;
	String CartBName;
	
	/* Others */
	boolean  NetPlay;
	boolean  NetPlayServer;
	String	ServerName;
	int Port;

	int AutoSaveDelay; /* Time in seconds before S-RAM auto-saved if modified. */
	boolean ApplyCheats;
	boolean TurboMode;

	int HighSpeedSeek;
	int TurboSkipFrames;
	int AutoMaxSkipFrames;
	
	/* Fixes for individual games */
	//boolean  BS;  /* Japanese Satellite System games. */
	//boolean  BSXItself;
	//boolean  BSXBootup;

	//boolean  SETA;
	
	static boolean GetBool( String value, boolean default_value )
	{
		boolean bol_value = default_value;
		
		if ( value == "true" || value == "TRUE" || value == "1" )
		{
			bol_value = true;
		}
		else if ( value == "false" || value == "FALSE" || value == "0" )
		{
			bol_value = false;
		}
			
		return bol_value;
	}
	
	static int GetInt(String value, int default_value )
	{
		int int_value;
		try 
		{			
			if ( value == null || value.length() == 0)
			{
				throw new NumberFormatException();
			}
			
			int_value =  Integer.parseInt( value );
		}
		catch ( NumberFormatException e )
		{
			int_value = default_value;
		}
		
		return int_value;
	}
	
	void LoadConfigFiles( HashMap<String, String> conf )
	{		
		NextAPUEnabled = GetBool( conf.get("Sound.APUEnabled"), APUEnabled );
		SoundSkipMethod = GetInt( conf.get("Sound.SoundSkip"), 0);
		int i = GetInt( conf.get("CPU.HDMATimingHack"), 100);
		if(i > 0 && i < 200)
			HDMATimingHack =  i;
		
		ShutdownMaster = GetBool( conf.get("Settings.SpeedHacks"), false);
		ForcePAL = GetBool( conf.get("ROM.PAL"), false);
		ForceNTSC = GetBool( conf.get("ROM.NTSC"), false);
		
		if( conf.get("Settings.FrameSkip") == "Auto" )
		{
			SkipFrames = SnesSystem.AUTO_FRAMERATE;
		} else {
			SkipFrames = GetInt( conf.get("Settings.FrameSkip"), 0 ) + 1;
		}
		
		TurboSkipFrames = GetInt( conf.get("Settings.TurboFrameSkip"), 15);
		TurboMode = GetBool( conf.get("Settings.TurboMode"), false);
		AutoSaveDelay = GetInt( conf.get("Settings.AutoSaveDelay"), 30);
		ApplyCheats = GetBool( conf.get("ROM.Cheat"), true);

		FrameTimePAL = GetInt( conf.get("Settings.PALFrameTime"), 20000);
		FrameTimeNTSC = GetInt( conf.get("Settings.NTSCFrameTime"), 16667);
		String FrameTime = conf.get("Settings.FrameTime");
		
		if( FrameTime != null)
		{
			double ft = Double.parseDouble( FrameTime );
			FrameTimePAL = (int) ft;
			FrameTimeNTSC = (int) ft;
		}
		DisableSoundEcho = ! GetBool( conf.get("Sound.Echo"), true);
		SoundPlaybackRate = GetInt( conf.get("Sound.Rate"), SoundPlaybackRate) & 7;
		SoundBufferSize = GetInt( conf.get("Sound.BufferSize"), SoundBufferSize);
		if( conf.get("Sound.Stereo") != null)
		{
			Stereo = GetBool( conf.get("Sound.Stereo"), false );
			APUEnabled  =  true;
			NextAPUEnabled  =  true;
		}
		if( conf.get("Sound.Mono") != null)
		{
			Stereo  = ! GetBool( conf.get("Sound.Mono"), false );
			NextAPUEnabled  =  true;
		}
		SoundEnvelopeHeightReading = GetBool( conf.get("Sound.EnvelopeHeightReading"), false);
		DisableSampleCaching = !GetBool( conf.get("Sound.SampleCaching"), false);
		DisableMasterVolume = !GetBool( conf.get("Sound.MasterVolume"), false);
		InterpolatedSound = GetBool( conf.get("Sound.Interpolate"), true);
		
		if( conf.get("Sound.Sync") != null)
		{
			SoundSync = GetInt( conf.get("Sound.Sync"), 1);
			if( SoundSync > 2 )
				SoundSync = 1;
			SoundEnvelopeHeightReading  =  true;
			InterpolatedSound  =  true;
		}

		ThreadSound = GetBool( conf.get("Sound.ThreadSound"), false);

		if( null != conf.get("Sound.AltDecode"))
		{
			AltSampleDecode = GetInt( conf.get("Sound.AltDecode"), 1);
		}
		if( null != conf.get("Sound.FixFrequency"))
		{
			FixFrequency = GetBool( conf.get("Sound.FixFrequency"), true);
		}

		MouseMaster = GetBool( conf.get("Controls.MouseMaster"), true);
		SuperScopeMaster = GetBool( conf.get("Controls.SuperscopeMaster"), true);
		
		Port  =  NetPlayClient.NP_DEFAULT_PORT;
		
		if( null != conf.get("Netplay.Port"))
		{
			NetPlay  =  true;
			Port  =  GetInt( conf.get("Netplay.Port"), NetPlayClient.NP_DEFAULT_PORT);
		}
		ServerName = "";
		if( null != conf.get("Netplay.Server"))
		{
			NetPlay  =  true;
			ServerName = conf.get("Netplay.Server").toString();
		}
		NetPlay = GetBool( conf.get("Netplay.Enable"), false);

		JoystickEnabled = GetBool( conf.get("Controls.Joystick"), JoystickEnabled);

	}
	
}
