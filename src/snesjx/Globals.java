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

final class Globals
{
	static Globals globals = new Globals();
	
	EngineRunner Engine;

	CPU cpu = new CPU();
	
	CPUExecutor cpuexecutor = new CPUExecutor();

	Timings timings = new Timings();

	APU apu = new APU();

	Settings settings = new Settings();

	DSP1 dsp1 = new DSP1();

	SA1 sa1 = new SA1();

	SMulti Multi = new SMulti();

	SoundData sounddata = new SoundData();

	Memory memory = new Memory();
	
	//SSNESGameFixes SNESGameFixes = new SSNESGameFixes();
	
	int OpenBus = 0;
	
	SuperFX superfx = new SuperFX();
	
	PPU ppu = new PPU();

	DMA dma = new DMA();
	
	RTC rtc = new RTC(); 
	
	//DD1 dd1 = new DD1();
	
	//SPC7110 s7r = new SPC7110();
	
	Controls controls = new Controls();
	
	GLDisplay gamedisplay = new GLDisplay();
	
	short GetBank = 0;
	
	int intInstCount = 0;
	
	//struct SCheatData Cheat;

	static int[] SignExtend = {0x00, 0xff00};

	static long[] HeadMask = new long[4];
	
	static {
		if (System.getProperties().getProperty("sun.cpu.endian") == "little")
		{
			HeadMask[0] = 0xffffffff;
			HeadMask[1] = 0xffffff00;
			HeadMask[2] = 0xffff0000;
			HeadMask[3] = 0xff000000;
		}
		else
		{
			HeadMask[0] = 0xffffffff;
			HeadMask[1] = 0x00ffffff;
			HeadMask[2] = 0x0000ffff;
			HeadMask[3] = 0x000000ff;
		}
	};


	static long[] TailMask = new long[5];
	static
	{
		if (System.getProperties().getProperty("sun.cpu.endian") == "little")
		{
			TailMask[0] = 0x00000000;
			TailMask[1] = 0x000000ff;
			TailMask[2] = 0x0000ffff;
			TailMask[3] = 0x00ffffff;
			TailMask[4] = 0xffffffff;
		}
		else
		{
			TailMask[0] = 0x00000000;
			TailMask[1] = 0xff000000;
			TailMask[2] = 0xffff0000;
			TailMask[3] = 0xffffff00;
			TailMask[4] = 0xffffffff;
		}
	};

	//NetPlayClient netplay = new NetPlayClient();
	//NetPlayServer netplayserver = new NetPlayServer();
	
	static void setUp()
	{
		globals.ppu.setUp();
		globals.sa1.setUp();
		globals.memory.setUp();
		globals.apu.setUp();
		globals.sounddata.setUp();
		globals.cpu.setUp();
		globals.dma.setUp();
		globals.gamedisplay.setUp();
		globals.cpuexecutor.setUp();
		globals.rtc.setUp();
		globals.controls.setUp();
	}
	
}
