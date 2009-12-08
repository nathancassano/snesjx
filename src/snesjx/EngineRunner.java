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

public abstract class EngineRunner
{	
	private Globals globals;
	protected GLDisplay gamedisplay;
	
	public EngineRunner()
	{
		Globals.setUp();
		globals = Globals.globals;
		gamedisplay = globals.gamedisplay;
		Globals.globals.Engine = this;
	}
	
	public void Initialize(String[] args)
	{
		Settings settings = globals.settings;
		
		settings.SoundPlaybackRate = 5;
	    settings.Stereo = false;
	    settings.SoundBufferSize = 0;
	    settings.APUEnabled = settings.NextAPUEnabled = false;
		
		settings.LoadConfigFiles(new java.util.HashMap<String, String>());

		SnesSystem.ParseArgs(args);
		
		globals.settings.InterpolatedSound = false;
	
		//globals.memory.Init();
				
		globals.apu.InitAPU();
		
		globals.sounddata.InitSound( settings.SoundPlaybackRate, settings.Stereo, settings.SoundBufferSize);
		
		InitDisplay();

		globals.sounddata.SetSoundMute(true);
		
		try
		{
			globals.memory.LoadROM( GetRomFilePath() );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
		try {
			globals.memory.InitROM();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		globals.sounddata.SetSoundMute(false);

    	// TODO: InitCheatData
    	//InitCheatData();
    	//ApplyCheats();

		SnesSystem.Reset();
	}
	
	public void Execute()
	{
		globals.cpuexecutor.MainLoop();
	}
	
	public abstract void StartScreenRefresh();

	public abstract void InitDisplay();
	
	public abstract void EndScreenRefresh();
	
	public abstract String GetRomFilePath();
	
}
