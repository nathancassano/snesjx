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


final class SnesSystem
{
	// NAC: Make these final eventually
	static boolean DEBUG_CPU = false;
	final static boolean DEBUG_APU = false;
	final static boolean DEBUG_PPU = false;
	final static boolean DEBUG_PPU_MODES = false;
	final static boolean DEBUG_SFX = false;
	final static boolean DEBUG_MEM = false;
	final static boolean DEBUG_DSP = false;
	final static boolean DEBUG_DMA = false;
	
    static final int _5C77 = 1;
    static final int _5C78 = 3;
    static final int _5A22 = 2;
	
	static final int SNES_WIDTH				= 256;
	static final int SNES_HEIGHT			= 224;
	static final int SNES_HEIGHT_EXTENDED	= 239;

	//static final BigDecimal NTSC_MASTER_CLOCK = new BigDecimal( "21477272.0");
	static final double NTSC_MASTER_CLOCK	= 21477272.0;
	static final double PAL_MASTER_CLOCK	= 21281370.0;

	static final int SNES_MAX_NTSC_VCOUNTER	= 262;
	static final int SNES_MAX_PAL_VCOUNTER	= 312;
	static final int SNES_HCOUNTER_MAX		= 341;

	static final int ONE_CYCLE		= 6;
	static final int SLOW_ONE_CYCLE	= 8;
	static final int TWO_CYCLES		= 12;
	static final int ONE_DOT_CYCLE	= 4;

	//static final BigDecimal SNES_CYCLES_PER_SCANLINE 	= new BigDecimal( SNES_HCOUNTER_MAX * ONE_DOT_CYCLE );
	//static final int SNES_SCANLINE_TIME				= SNES_CYCLES_PER_SCANLINE.divide( NTSC_MASTER_CLOCK ).intValue();
	static final int SNES_CYCLES_PER_SCANLINE	= (SNES_HCOUNTER_MAX * ONE_DOT_CYCLE);
	static final int SNES_SCANLINE_TIME			= (int) (SNES_CYCLES_PER_SCANLINE / NTSC_MASTER_CLOCK );
	

	static final int SNES_WRAM_REFRESH_HC_v1		= 530;
	static final int SNES_WRAM_REFRESH_HC_v2		= 538;
	static final int SNES_WRAM_REFRESH_CYCLES	= 40;

	static final int SNES_HBLANK_START_HC	= 1096;
	static final int SNES_HDMA_START_HC		= 1106;
	static final int SNES_HBLANK_END_HC		= 4;
	static final int SNES_HDMA_INIT_HC		= 20;
	static final int SNES_RENDER_START_HC	= (48 * ONE_DOT_CYCLE);

	static final int SNES_APU_CLOCK				= 1024000;
	static final int SNES_APU_ACCURACY			= 10;
	
	static final int SNES_APU_ONE_CYCLE_SCALED = 21477; // NAC: Just hard coded these values
	static final int SNES_APUTIMER2_CYCLE_SCALED = 343636; 

	//static final int SNES_APU_ONE_CYCLE_SCALED = ((int) (NTSC_MASTER_CLOCK / SNES_APU_CLOCK) * (1 << SNES_APU_ACCURACY));
	//static final int SNES_APUTIMER2_CYCLE_SCALED	= ((int) (NTSC_MASTER_CLOCK / 64000.0 * (1 << SNES_APU_ACCURACY)));
	
	static final int AUTO_FRAMERATE = 200;

	static final int SNES_TR_MASK		= (1 << 4);
	static final int SNES_TL_MASK		= (1 << 5);
	static final int SNES_X_MASK		= (1 << 6);
	static final int SNES_A_MASK		= (1 << 7);
	static final int SNES_RIGHT_MASK	= (1 << 8);
	static final int SNES_LEFT_MASK	    = (1 << 9);
	static final int SNES_DOWN_MASK	    = (1 << 10);
	static final int SNES_UP_MASK	    = (1 << 11);
	static final int SNES_START_MASK	= (1 << 12);
	static final int SNES_SELECT_MASK	= (1 << 13);
	static final int SNES_Y_MASK		= (1 << 14);
	static final int SNES_B_MASK		= (1 << 15);

	static final int DEBUG_MODE_FLAG	= (1 << 0);
	static final int TRACE_FLAG			= (1 << 1);
	static final int SINGLE_STEP_FLAG	= (1 << 2);
	static final int BREAK_FLAG			= (1 << 3);
	static final int SCAN_KEYS_FLAG		= (1 << 4);
	static final int SAVE_SNAPSHOT_FLAG	= (1 << 5);
	static final int DELAYED_NMI_FLAG	= (1 << 6);
	static final int NMI_FLAG			= (1 << 7);
	static final int PROCESS_SOUND_FLAG	= (1 << 8);
	static final int FRAME_ADVANCE_FLAG	= (1 << 9);
	static final int DELAYED_NMI_FLAG2	= (1 << 10);
	static final int IRQ_FLAG			= (1 << 11);
	static final int HALTED_FLAG		= (1 << 12);
	
	static final int HC_HBLANK_START_EVENT = 1;
	static final int HC_IRQ_1_3_EVENT      = 2;
	static final int HC_HDMA_START_EVENT   = 3;
	static final int HC_IRQ_3_5_EVENT      = 4;
	static final int HC_HCOUNTER_MAX_EVENT = 5;
	static final int HC_IRQ_5_7_EVENT      = 6;
	static final int HC_HDMA_INIT_EVENT    = 7;
	static final int HC_IRQ_7_9_EVENT      = 8;
	static final int HC_RENDER_EVENT       = 9;
	static final int HC_IRQ_9_A_EVENT      = 10;
	static final int HC_WRAM_REFRESH_EVENT = 11;
	static final int HC_IRQ_A_1_EVENT      = 12;

	static final int _TRACE = 0;
	static final int _DEBUG = 1;
	static final int _WARNING = 2;
	static final int _INFO = 3;
	static final int _ERROR = 4;
	static final int _FATAL_ERROR = 5;

	static final int _ROM_INFO = 0;
	static final int _HEADERS_INFO = 1; 
	static final int _CONFIG_INFO = 2;
	static final int _ROM_CONFUSING_FORMAT_INFO = 3;
	static final int _ROM_INTERLEAVED_INFO = 4;
	static final int _SOUND_DEVICE_OPEN_FAILED = 5;
	static final int _APU_STOPPED = 6;
	static final int _USAGE = 7;
	static final int _GAME_GENIE_CODE_ERROR = 8;
	static final int _ACTION_REPLY_CODE_ERROR = 9;
	static final int _GOLD_FINGER_CODE_ERROR = 10;
	static final int _DEBUG_OUTPUT = 11;
	static final int _DMA_TRACE = 12;
	static final int _HDMA_TRACE = 13;
	static final int _WRONG_FORMAT = 14;
	static final int _WRONG_VERSION = 15;
	static final int _ROM_NOT_FOUND = 16;
	static final int _FREEZE_FILE_NOT_FOUND = 17;
	static final int _PPU_TRACE = 18;
	static final int _TRACE_DSP1 = 19;
	static final int _FREEZE_ROM_NAME = 20;
	static final int _HEADER_WARNING = 21;
	static final int _NETPLAY_NOT_SERVER = 22;
	static final int _FREEZE_FILE_INFO = 23;
	static final int _TURBO_MODE = 24;
	static final int _SOUND_NOT_BUILT = 25;
	static final int _MOVIE_INFO = 26;
	static final int _WRONG_MOVIE_SNAPSHOT = 27;
	static final int _NOT_A_MOVIE_SNAPSHOT = 28;
	static final int _SNAPSHOT_INCONSISTENT = 29;
	static final int _AVI_INFO = 30;

	static final int DEFAULT_DIR = 0;
	static final int HOME_DIR = 1;
	static final int ROM_DIR = 2;
	static final int ROMFILENAME_DIR = 3;
	static final int SNAPSHOT_DIR = 4;
	static final int SRAM_DIR = 5;
	static final int SCREENSHOT_DIR = 6;
	static final int SPC_DIR = 7;
	static final int PATCH_DIR = 8;
	static final int CHEAT_DIR = 9;
	static final int PACK_DIR = 10;
	static final int BIOS_DIR = 11;
	static final int LOG_DIR = 12;
	
	static int Rates[] = { 0, 8000, 11025, 16000, 22050, 32000, 44100, 48000	};
	
	static Globals globals;
	
	static void Message (int type, int number, String message)
	{
			System.out.println(message);
	}
	
	static void Reset()
	{
		Globals globals = Globals.globals;
		Memory memory = globals.memory;
		Settings settings = globals.settings;
		
		//TODO: ResetLogger, ResetSaveTimer
	    //ResetLogger();
	    //ResetSaveTimer (false);

	    memory.FillRAM.fill(0, 0, 0x8000);
	    memory.VRAM.fill( 0, 0, 0x10000 );
	    memory.RAM.fill( 0x55, 0, 0x20000 );

	    /* TODO:  Reset calls
		if (Settings.BS)
			ResetBSX();
			*/
		
	    /*
		if(settings.SPC7110)
		{
			globals.s7r.Spc7110Reset();
		}
		*/
		
		globals.cpu.ResetCPU();
		globals.ppu.ResetPPU();
		globals.rtc.ResetSRTC();
	    
		/*
	    if (globals.settings.SDD1)
	    {
	    	//TODO: ResetSDD1 ();
	    }
	    */

	    globals.dma.ResetDMA();
	    globals.apu.ResetAPU();
	    globals.dsp1.ResetDSP1();
	    globals.sa1.SA1Init();
	    
	    /*
	    if (globals.settings.C4)
	    {
	        //TODO: InitC4 ();
	    }
	    */
	    
	    //TODO: InitCheatData ();
	    
	    /*
		if (settings.OBC1)
		{
			//TODO: ResetOBC1();
		}
		*/
		
	    if (settings.SuperFX)
	    {
	    	globals.superfx.ResetSuperFX();
	    }
	   
	}
	
	void SoftReset ()
	{
		// TODO:  Reset calls
		
		Globals globals = Globals.globals;
		Memory Memory = globals.memory;
		
		//TODO: ResetSaveTimer
		//ResetSaveTimer (false);

		//if (Settings.BS)
		//	ResetBSX();
		
	    if (globals.settings.SuperFX)
	    {
	    	globals.superfx.ResetSuperFX();
	    }

	    Memory.FillRAM.fill( 0, 0x8000, Memory.FillRAM.size() );
	    Memory.VRAM.fill( 0, 0x10000, Memory.VRAM.size() );

	    /*
		if(globals.settings.SPC7110)
			globals.s7r.Spc7110Reset();
		*/
	    
		globals.cpu.SoftResetCPU();
		globals.ppu.SoftResetPPU();
		globals.rtc.ResetSRTC();
	    
		//if (globals.Settings.SDD1)
	    //    ResetSDD1 ();

		globals.dma.ResetDMA();
		globals.apu.ResetAPU();
		globals.dsp1.ResetDSP1();
		//if(Settings.OBC1)
		//	ResetOBC1();
		globals.sa1.SA1Init();
	    
	    //if (Settings.C4)
	    //    InitC4 ();
	    //InitCheatData ();

	}

	static String GetFilename (String ex, int dirtype)
	{
		//TOOD: Finish GetFilename
	    return "";
	}
	
	static String GetFilenameInc(String file_extension, int dirtype)
	{
		//TODO: Finish GetFilenameInc()
		return "";
	}
	
	static boolean OpenSoundDevice (int mode, boolean stereo, int buffer_size)
	{
		Globals globals = Globals.globals;
		SoundData SoundData = globals.sounddata;
	    
	    SoundData.so.stereo = stereo;
	    
	    SoundData.so.playback_rate = Rates[mode & 0x07];
	    SoundData.so.sixteen_bit = true;

	    if ( ! globals.settings.Mute )
	    {
	    	/*
	    	System.out.format("Sound: Rate: %d, Buffer size: %d, 16-bit: %s, Stereo: %s, Encoded: %s\n",
	    	SoundData.so.playback_rate, SoundData.so.buffer_size, SoundData.so.sixteen_bit ? "yes" : "no",
	    	SoundData.so.stereo ? "yes" : "no", SoundData.so.encoded ? "yes" : "no");
	     	*/
	    }
	    
	    SoundData.SetPlaybackRate(SoundData.so.playback_rate);
	    
		return true;
	}
	
	static void Exit()
	{
		Globals globals = Globals.globals;
		
		globals.settings.StopEmulation = true;
		
	    //TODO: ResetSaveTimer (false);
	    
		/*
	    if( globals.settings.SPC7110 )
	    {
	        //TOOD: (*CleanUp7110)();
	    }
	    */

	    globals.sounddata.SetSoundMute( true );

	    globals.memory.SaveSRAM( SnesSystem.GetFilename (".srm", SRAM_DIR));
	    //TODO:  SaveCheatFile(GetFilename (".cht", PATCH_DIR));
	    //globals.Memory.Deinit();

	    if( globals.settings.NetPlay )
	    {
	    	//TODO: NPDisconnect();
	    }
	}

	static void SyncSpeed()
	{

		// NETPLAY_SUPPORT
		
		/*
		if (globals.Settings.NetPlay && globals.NetPlay.Connected)
		{
			
			// Send joypad position update to server
			NPSendJoypadUpdate (old_joypads [0]);
			
			// set input from network
			for (int J = 0; J < 8; J++)
			    globals joypads[J] = NPGetJoypad(J);
			
			if (!NPCheckForHeartBeat())
			{
			    // No heartbeats already arrived, have to wait for one.
			    NetPlay.PendingWait4Sync = !NPWaitForHeartBeatDelay(100);
			
			    IPPU.RenderThisFrame = TRUE;
			    IPPU.SkippedFrames = 0;
			}
			else
			{
			    NetPlay.PendingWait4Sync = !NPWaitForHeartBeatDelay(200);
			
			
			    if (IPPU.SkippedFrames < NetPlay.MaxFrameSkip)
			    {
					IPPU.SkippedFrames++;
					IPPU.RenderThisFrame = FALSE;
			    }
			    else
			    {
			  		IPPU.RenderThisFrame = TRUE;
			  		IPPU.SkippedFrames = 0;
			    }
			}
			
			if (!NetPlay.PendingWait4Sync)
			{
			    NetPlay.FrameCount++;
			    NPStepJoypadHistory ();
			}
			
			return;
		}
		*/
	  
		/*

		if (globals.Settings.HighSpeedSeek > 0)
			globals.Settings.HighSpeedSeek--;
		      
		if (globals.Settings.TurboMode)
		{
			if( (++globals.IPPU.FrameSkip >= globals.Settings.TurboSkipFrames || globals.Settings.OldTurbo) && globals.Settings.HighSpeedSeek != 0)
			{
				globals.IPPU.FrameSkip = 0;
				globals.IPPU.SkippedFrames = 0;
				globals.IPPU.RenderThisFrame = true;
			}
			else
			{
			    ++globals.IPPU.SkippedFrames;
			    globals.IPPU.RenderThisFrame = false;
			}
			return;
		}
		*/


	      /* Check events */
	      //TODO: Finish porting this
	      /*
	      //static struct timeval next1 = {0, 0};
	      //struct timeval now;

	      //CHECK_SOUND();
	      ProcessEvents(FALSE);

	      //while (gettimeofday (&now, NULL) < 0) ;

	      // If there is no known "next" frame, initialize it now
	      if (next1.tv_sec == 0) { next1 = now; ++next1.tv_usec; }

	      // If we're on AUTO_FRAMERATE, we'll display frames always
	      // only if there's excess time.
	      // Otherwise we'll display the defined amount of frames.
	      //
	      unsigned limit = Settings.SkipFrames == AUTO_FRAMERATE
	                       ? (timercmp(&next1, &now, <) ? 10 : 1)
	                       : Settings.SkipFrames;

	      IPPU.RenderThisFrame = ++IPPU.SkippedFrames >= limit;
	      if(IPPU.RenderThisFrame)
	      {
	          IPPU.SkippedFrames = 0;
	      }
	      else
	      {
	          // If we were behind the schedule, check how much it is
	          if(timercmp(&next1, &now, <))
	          {
	              unsigned lag =
	                  (now.tv_sec - next1.tv_sec) * 1000000
	                 + now.tv_usec - next1.tv_usec;
	              if(lag >= 500000)
	              {
	                  // More than a half-second behind means probably
	                  // pause. The next line prevents the magic
	                  // fast-forward effect.
	                  next1 = now;
	              }
	          }
	      }

	      // Delay until we're completed this frame 

	      // Can't use setitimer because the sound code already could
	      // be using it. We don't actually need it either.

	      while(timercmp(&next1, &now, >))
	      {
	          // If we're ahead of time, sleep a while 
	          unsigned timeleft =
	              (next1.tv_sec - now.tv_sec) * 1000000
	             + next1.tv_usec - now.tv_usec;
	          //fprintf(stderr, "<%u>", timeleft);
	          usleep(timeleft);

	          CHECK_SOUND(); ProcessEvents(FALSE);

	          while (gettimeofday (&now, NULL) < 0) ;
	          // Continue with a while-loop because usleep()
	          // could be interrupted by a signal

	      }

	      // Calculate the timestamp of the next frame.
	      next1.tv_usec += Settings.FrameTime;
	      if (next1.tv_usec >= 1000000)
	      {
	          next1.tv_sec += next1.tv_usec / 1000000;
	          next1.tv_usec %= 1000000;
	      }
		*/
	}
	
	static boolean InitUpdate()
	{
		return true;
	}
	
	static void AutoSaveSRAM()
	{
		
	}
	
	static void Usage()
	{
		// TODO: Usage ?
	}
	
	static String ParseArgs (String argv[])
	{
		Globals globals = Globals.globals;
		Settings settings = globals.settings;
		
		String rom_filename = null;

		for (int i = 1; i < argv.length; i++)
		{
			String arg = argv[i];
			
			if( arg.substring(0, 1) == "-")
			{
				if ( arg == "-so" ||  arg == "-sound")
				{
					settings.NextAPUEnabled = true;
				}
				else if ( arg == "-ns" || arg == "-nosound")
				{
					settings.NextAPUEnabled = false;
				}
				else if ( arg == "-soundskip" || arg == "-sk")
				{
					if (i + 1 < argv.length  )
					{
						i++;
						settings.SoundSkipMethod = Integer.parseInt( argv[i] );
					}
					else
					{
						Usage();
					}
				}
				else if ( arg == "-ra" || arg == "-ratio")
				{
					if ((i + 1) < argv.length)
					{
						
					}
					else
					{
						Usage();
					}
				}
				else if ( arg == "-h" || arg == "-hdmahacks")
				{
					if (i + 1 < argv.length)
					{
						i++;
						int p = Integer.parseInt( argv[i]);
						if (p > 0 && p < 200)
							settings.HDMATimingHack = p;
					}
					else
					{
						Usage();
					}
				}
				else if ( arg == "-nsh" || arg == "-nospeedhacks")
				{
					settings.ShutdownMaster = false;
				}
				else if ( arg == "-sh" || arg == "-speedhacks")
				{
					settings.ShutdownMaster = true;
				}
				else if ( arg == "-p" || arg == "-pal")
				{
					settings.ForcePAL = true;
				}
				else if ( arg == "-n" || arg == "-ntsc")
				{
					settings.ForceNTSC = true;
				}
				else if ( arg == "-f" || arg == "-frameskip")
				{
					if (i + 1 < argv.length)
					{
						settings.SkipFrames = Integer.parseInt (argv [++i]) + 1;
					}
					else
					{
						Usage ();
					}
				}
				else if ( arg == "-16" ||
					  arg == "-sixteen")
				{
				}
				else if ( arg == "-nocheat")
				{
					settings.ApplyCheats=false;
				}
				else if ( arg == "-cheat")
				{
					settings.ApplyCheats=true;
				}
				else if ( arg == "-gg" || arg == "-gamegenie")
				{
					if (i + 1 < argv.length)
					{
						// NAC: TODO gamegenie?
						/*
						int address;
						uint8 byte;
						const char *error;
						if ((error = GameGenieToRaw (argv [++i], address, byte)) == NULL)
						AddCheat (true, false, address, byte);
						else
						Message (_ERROR, _GAME_GENIE_CODE_ERROR,
								error);
						*/
					}
					else
					{
						Usage ();
					}
				}
				else if ( arg == "-ar" || arg == "-actionreplay")
				{
					// NAC: TODO actionreplay?
					/*
					if (i + 1 < argv.length)
					{
						uint32 address;
						uint8 byte;
						const char *error;
						if ((error = ProActionReplayToRaw (argv [++i], address, byte)) == NULL)
						AddCheat (true, false, address, byte);
						else
						Message (_ERROR, _ACTION_REPLY_CODE_ERROR,
								error);
					
					}
					else
					{
						Usage ();
					}
					*/
				}
				else if ( arg == "-gf" || arg == "-goldfinger")
				{
					// NAC: TODO goldfinger?
					/*
					if (i + 1 < argv.length)
					{
						uint32 address;
						uint8 bytes [3];
						bool8 sram;
						uint8 num_bytes;
						const char *error;
						if ((error = GoldFingerToRaw (argv [++i], address, sram,
										 num_bytes, bytes)) == NULL)
						{
						for (int c = 0; c < num_bytes; c++)
							AddCheat (true, false, address + c, bytes [c]);
						}
						else
						Message (_ERROR, _GOLD_FINGER_CODE_ERROR,
								error);
					}
					else
					{
						Usage ();
					}
						*/
				}
				else if ( arg == "-ft" || arg == "-frametime")
				{
					if (i + 1 < argv.length)
					{
						i++;
						double ft = Double.parseDouble(argv[i]);
						settings.FrameTimePAL = (int) ft;
						settings.FrameTimeNTSC = (int) ft;
	
					}
					else
					{
						Usage ();
					}
				}
				else if ( arg == "-e" || arg == "-echo")
				{
					settings.DisableSoundEcho = false;
				}
				else if ( arg == "-ne" || arg == "-noecho")
				{
					settings.DisableSoundEcho = true;
				}
				else if ( arg == "-r" || arg == "-soundquality" || arg == "-sq")
				{
					if (i + 1 < argv.length)
					{
						settings.SoundPlaybackRate = Integer.parseInt (argv [++i]) & 7;
					}
					else
					{
						Usage ();
					}
				}
				else if ( arg == "-stereo" || arg == "-st")
				{
					settings.Stereo = true;
					settings.APUEnabled = true;
					settings.NextAPUEnabled = true;
				}
				else if ( arg == "-mono")
				{
					settings.Stereo = false;
					settings.NextAPUEnabled = true;
				}
				else if ( arg == "-envx" ||  arg == "-ex")
				{
					settings.SoundEnvelopeHeightReading = true;
				}
				else if ( arg == "-nosamplecaching" || arg == "-nsc" || arg == "-nc")
				{
					settings.DisableSampleCaching = true;
				}
				else if ( arg == "-nomastervolume" || arg == "-nmv")
				{
					settings.DisableMasterVolume = true;
				}
				else if ( arg == "-soundsync" ||  arg == "-sy")
				{
					settings.SoundSync = 1;
					settings.SoundEnvelopeHeightReading = true;
					settings.InterpolatedSound = true;
				}
				else if ( arg == "-soundsync2" ||  arg == "-sy2")
				{
					settings.SoundSync = 1;
					settings.SoundEnvelopeHeightReading = true;
					settings.InterpolatedSound = true;
				}
				else if ( arg == "-nois")
				{
					settings.InterpolatedSound = false;
				}
				else if ( arg == "-threadsound" || arg == "-ts")
				{
					settings.ThreadSound = true;
				}
				else if ( arg == "-alt" || arg == "-altsampledecode")
				{
					settings.AltSampleDecode = 1;
				}
				else if ( arg == "-fix")
				{
					settings.FixFrequency = true;
				}
				else if ( arg == "-nomouse")
				{
					settings.MouseMaster = false;
				}
				else if ( arg == "-nosuperscope")
				{
					settings.SuperScopeMaster = false;
				}
			    else if ( arg == "-port" || arg == "-po" )
				{
					if (i + 1 < argv.length )
					{
					    settings.NetPlay = true;
					    settings.Port = Integer.parseInt( argv[++i] );
					}
					else
					{
					    Usage ();
					}
				}
				else if ( arg == "-server" || arg == "-srv")
				{
					if (i + 1 < argv.length)
					{
						settings.NetPlay = true;
						settings.ServerName = argv[++i];
					}
					else
					{
						Usage ();
					}
				}
				else if ( arg == "-net")
				{
					settings.NetPlay = true;
				}
			}
			else
			{
				rom_filename = arg;
			}
		}

		globals.controls.VerifyControllers();
		
		return rom_filename;
	}
}
