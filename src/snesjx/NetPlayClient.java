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

class NetPlayClient
{
	public static final int NP_VERSION = 10;
	public static final int NP_JOYPAD_HIST_SIZE = 120;
	public static final int NP_DEFAULT_PORT = 6096;
	
	public static final int NP_MAX_CLIENTS = 8;
	
	public static final int NP_SERV_MAGIC = 'S';
	public static final int NP_CLNT_MAGIC = 'C';
	
	public static final int NP_CLNT_HELLO = 0;
	public static final int NP_CLNT_JOYPAD = 1;
	public static final int NP_CLNT_RESET = 2;
	public static final int NP_CLNT_PAUSE = 3;
	public static final int NP_CLNT_LOAD_ROM = 4;
	public static final int NP_CLNT_ROM_IMAGE = 5;
	public static final int NP_CLNT_FREEZE_FILE = 6;
	public static final int NP_CLNT_SRAM_DATA = 7; 
	public static final int NP_CLNT_READY = 8;
	public static final int NP_CLNT_LOADED_ROM = 9;
	public static final int NP_CLNT_RECEIVED_ROM_IMAGE = 10;
	public static final int NP_CLNT_WAITING_FOR_ROM_IMAGE = 11;
	
	public int MySequenceNum;
	public int ServerSequenceNum;
	public boolean Connected;
	public boolean Abort;
	public int Player;
	public boolean ClientsReady[] = new boolean [NP_MAX_CLIENTS];
	public boolean ClientsPaused[] = new boolean [NP_MAX_CLIENTS];
	public boolean Paused;
	public boolean PendingWait4Sync;
	public int PercentageComplete;
	public boolean Waiting4EmulationThread;
	public boolean Answer;
	
	int Socket;
	String ServerHostName;
	String ROMName;
	int Port;
	public int JoypadWriteInd;
	public int JoypadReadInd;
	public int Joypads[][]= new int[NP_JOYPAD_HIST_SIZE][NP_MAX_CLIENTS];
	public int Frame[] = new int[NP_JOYPAD_HIST_SIZE];
	public int FrameCount;
	public int MaxFrameSkip;
	public int MaxBehindFrameCount;
	public boolean JoypadsReady[][] = new boolean[NP_JOYPAD_HIST_SIZE][NP_MAX_CLIENTS];
	String ActionMsg;
	String ErrorMsg;
	String WarningMsg;
	
	private Globals globals;
	private Settings settings;
	
	public void setUp()
	{
		globals = Globals.globals;
		settings = globals.settings;
	} 
	
	
}
