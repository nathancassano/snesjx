/*
 * SnesJx - Portable Super Nintendo Entertainment System (TM) emulator.
 * 
 * (C) Copyright Nathan Cassano 2009
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

final class APU
{	
    int P;
    int YA;
    int X;
    int S;
    int PC;
	
	private int YA_W(){ return YA & 0xFFFF; }
	
	private void YA_W( int value ){ YA = value & 0xFFFF; }
	
	private int YA_A(){ return YA & 0x00FF; }
	
	private void YA_A( int value )
	{ 
		YA = (YA & 0xFF00) | (value & 0x00FF); 
	}
	
	private int YA_Y(){ return ( YA & 0xFF00 ) >>> 8; }
	
	private final void YA_Y( int value )
	{ 
		YA = (YA & 0x00FF) | ( ( value << 8 ) & 0xFF00 );
	}
	
	int iPC;
	
	ByteArray RAM;
	private ByteArrayOffset DirectPage;
	boolean APUExecuting;
	private int Bit;
	private int Address;
	private int WaitAddress1;
	private int WaitAddress2;
	int WaitCounter;

	private int _Carry;
	private int _Zero;
	private int _Overflow;
	int TimerErrorCounter;
	int NextAPUTimerPos;
	private int APUTimerCounter;

	int OneCycle;
	private int TwoCycles;
		
	private boolean ShowROM;
	private int Flags;
	int KeyedChannels;
	
	int[] OutPorts = new int[4];
	int[] DSP = new int[0x80];
	private ByteArray ExtraRAM = new ByteArray(64);
	int[] Timer = new int[3];
	int[] TimerTarget = new int[3];
	
	boolean[] TimerEnabled = new boolean[3];
	boolean[] TimerValueWritten = new boolean[3];
	
	int ApuCycles;
	
	private void zero()
	{
		for(int i = 0; i < OutPorts.length; i++)
		{
			OutPorts[i] = 0;
		}
	}
	
	static final int APU_VOL_LEFT = 0x00;
	static final int APU_VOL_RIGHT = 0x01;
	static final int APU_P_LOW = 0x02;
	static final int APU_P_HIGH = 0x03;
	static final int APU_SRCN = 0x04;
	static final int APU_ADSR1 = 0x05;
	static final int APU_ADSR2 = 0x06;
	static final int APU_GAIN = 0x07;
	static final int APU_ENVX = 0x08;
	static final int APU_OUTX = 0x09;
	
	static final int APU_MVOL_LEFT = 0x0c;
	static final int APU_MVOL_RIGHT = 0x1c;
	static final int APU_EVOL_LEFT = 0x2c;
	static final int APU_EVOL_RIGHT = 0x3c;
	static final int APU_KON = 0x4c;
	static final int APU_KOFF = 0x5c;
	static final int APU_FLG = 0x6c;
	static final int APU_ENDX = 0x7c;
	
	static final int APU_EFB = 0x0d;
	static final int APU_PMON = 0x2d;
	static final int APU_NON = 0x3d;
	static final int APU_EON = 0x4d;
	static final int APU_DIR = 0x5d;
	static final int APU_ESA = 0x6d;
	static final int APU_EDL = 0x7d;
	
	static final int APU_C0 = 0x0f;
	static final int APU_C1 = 0x1f;
	static final int APU_C2 = 0x2f;
	static final int APU_C3 = 0x3f;
	static final int APU_C4 = 0x4f;
	static final int APU_C5 = 0x5f;
	static final int APU_C6 = 0x6f;
	static final int APU_C7 = 0x7f;
	
	static final int APU_SOFT_RESET = 0x80;
	static final int APU_MUTE = 0x40;
	static final int APU_ECHO_DISABLED = 0x20;
	
	private static final int FREQUENCY_MASK = 0x3fff;
	
	private static final int HALTED_FLAG = (1 << 12);

	private static int KeyOn;
	private static int KeyOnPrev;
	
	private int Work8;
	private int Work16;
	private int Work32;
	private int Int8;
	private int Int16;
	private int Int32;
	private int W1;

	private static final int Carry = 1;
	
	private static final int Zero = 2;
	private static final int Interrupt = 4;
	private static final int HalfCarry = 8;
	private static final int BreakFlag = 16;
	private static final int DirectPageFlag = 32;
	private static final int Overflow = 64;
	private static final int Negative = 128;
	
	private static final byte[] apurom = {
		-0x33,-0x11,-0x43,-0x18, 0x00,-0x3a, 0x1d,-0x30,
		-0x04,-0x71,-0x56,-0x0c,-0x71,-0x45,-0x0b, 0x78,
		-0x34,-0x0c,-0x30,-0x05, 0x2f, 0x19,-0x15,-0x0c,
		-0x30,-0x04, 0x7e,-0x0c,-0x30, 0x0b,-0x1c,-0x0b,
		-0x35,-0x0c,-0x29, 0x00,-0x04,-0x30,-0x0d,-0x55,
		 0x01, 0x10,-0x11, 0x7e,-0x0c, 0x10,-0x15,-0x46,
		-0x0a,-0x26, 0x00,-0x46,-0x0c,-0x3c,-0x0c,-0x23,
		 0x5d,-0x30,-0x25, 0x1f, 0x00, 0x00,-0x40,-0x01	
	};
	
	private static final ByteArray APUROM = new ByteArray(apurom);

	// Variable cycle length
	private int[] APUCycles = new int[256];
	
	// Raw SPC700 instruction cycle lengths
	private static final byte[] APUCycleLengths =
	{
		2, 8, 4, 5, 3, 4, 3, 6, 2, 6, 5, 4, 5, 4, 6, 8,
		2, 8, 4, 5, 4, 5, 5, 6, 5, 5, 6, 5, 2, 2, 4, 6,
		2, 8, 4, 5, 3, 4, 3, 6, 2, 6, 5, 4, 5, 4, 5, 4,
		2, 8, 4, 5, 4, 5, 5, 6, 5, 5, 6, 5, 2, 2, 3, 8,
		2, 8, 4, 5, 3, 4, 3, 6, 2, 6, 4, 4, 5, 4, 6, 6,
		2, 8, 4, 5, 4, 5, 5, 6, 5, 5, 4, 5, 2, 2, 4, 3,
		2, 8, 4, 5, 3, 4, 3, 6, 2, 6, 4, 4, 5, 4, 5, 5,
		2, 8, 4, 5, 4, 5, 5, 6, 5, 5, 5, 5, 2, 2, 3, 6,
		2, 8, 4, 5, 3, 4, 3, 6, 2, 6, 5, 4, 5, 2, 4, 5,
		2, 8, 4, 5, 4, 5, 5, 6, 5, 5, 5, 5, 2, 2,12, 5,
		3, 8, 4, 5, 3, 4, 3, 6, 2, 6, 4, 4, 5, 2, 4, 4,
		2, 8, 4, 5, 4, 5, 5, 6, 5, 5, 5, 5, 2, 2, 3, 4,
		3, 8, 4, 5, 4, 5, 4, 7, 2, 5, 6, 4, 5, 2, 4, 9,
		2, 8, 4, 5, 5, 6, 6, 7, 4, 5, 5, 5, 2, 2, 6, 3,
		2, 8, 4, 5, 3, 4, 3, 6, 2, 4, 5, 3, 4, 3, 4, 3,
		2, 8, 4, 5, 4, 5, 5, 6, 3, 4, 5, 4, 2, 2, 4, 3
	};
	
	private Globals globals;
	private Settings settings;
	private CPU cpu;
	private SoundData sounddata;
	
	void InitAPU()
	{
		RAM = new ByteArray(0x10000);
	}
	
	void setUp()
	{
		globals = Globals.globals;
		settings = globals.settings;
		cpu = globals.cpu;
		sounddata = globals.sounddata;
	}

	void ResetAPU()
	{
		settings.APUEnabled = settings.NextAPUEnabled;

		if(settings.APUEnabled)
			Flags &= ~HALTED_FLAG;

		RAM.fill(0, 0, 0x100);
		
		RAM.fill(0xFF, 0x20, 0x20);
		RAM.fill(0xFF, 0x60, 0x20);
		RAM.fill(0xFF, 0xA0, 0x20);
		RAM.fill(0xFF, 0xE0, 0x20);

		for( int i = 1 ; i < 256; i++)
		{
			RAM.arraycopy( i << 8 , RAM, 0, 0x100);
		}

		zero();
		
		DirectPage = RAM.getOffsetBuffer(0);
		
		ExtraRAM.arraycopy(0, RAM, 0xffc0, APU.APUROM.size() );
		
		RAM.arraycopy(0xffc0, APU.APUROM, 0, APU.APUROM.size() );
		
		iPC = RAM.get16Bit(0xfffe);

		ApuCycles = 0;

		YA_W(0);
		X = 0;
		S = 0xef;
		P = 0x02;
		APUUnpackStatus();
		PC = 0;
		
		APUExecuting = settings.APUEnabled;
		WaitAddress1 = 0;
		WaitAddress2 = 0;
		WaitCounter = 0;
		NextAPUTimerPos = 0;
		APUTimerCounter = 0;
		ShowROM = true;
		RAM.put8Bit(0xf1, 0x80);

		for (int i = 0; i < 3; i++)
		{
			TimerEnabled[i] = false;
			TimerValueWritten[i] = false;
			TimerTarget[i] = 0;
			Timer[i] = 0;
		}
		
		for (int j = 0; j < 0x80; j++)
			DSP[j] = 0;

		TwoCycles = OneCycle * 2;

		for (int i = 0; i < 256; i++)
		{
			APUCycles[i] = (APU.APUCycleLengths[i] * OneCycle);
		}

		DSP[APU_ENDX] = 0;
		DSP[APU_KOFF] = 0;
		DSP[APU_KON] = 0;
		DSP[APU_FLG] = APU_MUTE | APU_ECHO_DISABLED;
		KeyedChannels = 0;

		sounddata.ResetSound(true);
		sounddata.SetEchoEnable(0);
	}
	
	private void SetAPUDSP (int Byte)
	{
		int reg = RAM.get8Bit(0xf2);

		switch (reg)
		{
		case APU_FLG:
			if ( ( Byte & APU_SOFT_RESET ) > 0 )
			{
				DSP[reg] = APU_MUTE | APU_ECHO_DISABLED | (Byte & 0x1f);
				DSP[APU_ENDX] = 0;
				DSP[APU_KOFF] = 0;
				DSP[APU_KON] = 0;
				sounddata.SetEchoWriteEnable(0);

				if ( SnesSystem.DEBUG_DSP )
				{
					System.out.format("[%d] DSP reset\n", cpu.Cycles);
				}

				// Kill sound
				sounddata.ResetSound(false);
			}
			else
			{
				sounddata.SetEchoWriteEnable( ( Byte & APU_ECHO_DISABLED ) );
				if ( ( Byte & APU_MUTE ) > 9)
				{
					if ( SnesSystem.DEBUG_DSP )
					{
						System.out.format("[%d] Mute sound\n", cpu.Cycles);
					}
					
					sounddata.SetSoundMute(true);
				}
				else
					sounddata.SetSoundMute(false);

				sounddata.noise_rate = SoundData.env_counter_table[Byte & 0x1f];
			}
			break;
		case APU_NON:
			if (Byte != DSP[APU_NON])
			{
				if ( SnesSystem.DEBUG_DSP )
				{
					System.out.format("[%d] Noise:", cpu.Cycles);
				}
				
				int mask = 1;
				for (int c = 0; c < 8; c++, mask <<= 1)
				{
					int type;
					if ( ( Byte & mask ) > 0 )
					{
						if ( SnesSystem.DEBUG_DSP )
						{
							if ( ( DSP[reg] & mask) > 0 )
								System.out.format("%d,\n", c);
							else
								System.out.format("%d(on),\n", c);
						}
						
						type = SoundData.SOUND_NOISE;
					}
					else
					{
						if ( SnesSystem.DEBUG_DSP )
						{
							if ( ( DSP[reg] & mask ) > 0 )
								System.out.format("%d(off),\n", c);
						}
						
						type = SoundData.SOUND_SAMPLE;
					}
					
					sounddata.SetSoundType(c, type);
				}
			}
			break;
		case APU_MVOL_LEFT:
			if (Byte != DSP[APU_MVOL_LEFT])
			{
				if ( SnesSystem.DEBUG_DSP )
					System.out.format("[%d] Master volume left:%d\n", cpu.Cycles, Byte);
				
				sounddata.SetMasterVolume( Byte, DSP[APU_MVOL_RIGHT]);
			}
			break;
		case APU_MVOL_RIGHT:
			if (Byte != DSP[APU_MVOL_RIGHT])
			{
				if ( SnesSystem.DEBUG_DSP )
					System.out.format("[%d] Master volume right:%d\n", cpu.Cycles, Byte);
				
				sounddata.SetMasterVolume( DSP[APU_MVOL_LEFT], Byte);
			}
			break;
		case APU_EVOL_LEFT:
			if (Byte != DSP[APU_EVOL_LEFT])
			{
				if ( SnesSystem.DEBUG_DSP )
					System.out.format("[%d] Echo volume left:%d\n", cpu.Cycles, Byte);
				sounddata.SetEchoVolume( Byte, DSP[APU_EVOL_RIGHT]);
			}
			break;
		case APU_EVOL_RIGHT:
			if (Byte != DSP[APU_EVOL_RIGHT])
			{
				if ( SnesSystem.DEBUG_DSP )
					System.out.format("[%d] Echo volume right:%d\n", cpu.Cycles, Byte);
				sounddata.SetEchoVolume( DSP[APU_EVOL_LEFT], Byte);
			}
			break;
		case APU_ENDX:
			if ( SnesSystem.DEBUG_DSP ) { System.out.format("[%d] Reset ENDX\n", cpu.Cycles); }
			
			Byte = 0;
			break;

		case APU_KOFF:
			
			//if (Byte > 0)
			{
				int mask = 1;
				
				if ( SnesSystem.DEBUG_DSP ) System.out.format("[%d] Key off:", cpu.Cycles);

				for (int c = 0; c < 8; c++, mask <<= 1)
				{
					if ((Byte & mask) != 0)
					{
						if ( SnesSystem.DEBUG_DSP ) System.out.format("%d,", c);
						
						if ( ( KeyedChannels & mask ) > 0 )
						{
							{
								KeyOnPrev &= ~mask;
								KeyedChannels &= ~mask;
								DSP[APU_KON] &= ~mask;
								//DSP[APU_KOFF] |= mask;
								sounddata.SetSoundKeyOff (c);
							}
						}
					}
					else if( ( KeyOnPrev & mask )!= 0 )
					{
						KeyOnPrev &= ~mask;
						KeyedChannels |= mask;
						//DSP[APU_KON] |= mask;
						DSP[APU_KOFF] &= ~mask;
						DSP[APU_ENDX] &= ~mask;
						sounddata.PlaySample(c);
					}
				}
				
				if ( SnesSystem.DEBUG_DSP ) System.out.format("\n");
			}
			//KeyOnPrev=0;
			DSP[APU_KOFF] = Byte;
			return;
		case APU_KON:
			
			if (Byte > 0)
			{
				int mask = 1;
				
				if ( SnesSystem.DEBUG_DSP ) System.out.format("[%d] Key on:", cpu.Cycles);
				
				for (int c = 0; c < 8; c++, mask <<= 1)
				{
					if ((Byte & mask) != 0)
					{
						if ( SnesSystem.DEBUG_DSP ) System.out.format("%d,", c);
						
						// Pac-In-Time requires that channels can be key-on
						// regardeless of their current state.
						if( (DSP[APU_KOFF] & mask) == 0 )
						{
							KeyOnPrev &= ~mask;
							KeyedChannels |= mask;
							//DSP[APU_KON] |= mask;
							//DSP[APU_KOFF] &= ~mask;
							DSP[APU_ENDX] &= ~mask;
							sounddata.PlaySample(c);
						}
						else
						{
							KeyOn|=mask;
						}
					}
				}
				
				if ( SnesSystem.DEBUG_DSP ) System.out.format("\n");
			}
			return;

		case APU_VOL_LEFT + 0x00:
		case APU_VOL_LEFT + 0x10:
		case APU_VOL_LEFT + 0x20:
		case APU_VOL_LEFT + 0x30:
		case APU_VOL_LEFT + 0x40:
		case APU_VOL_LEFT + 0x50:
		case APU_VOL_LEFT + 0x60:
		case APU_VOL_LEFT + 0x70:
			if ( SnesSystem.DEBUG_DSP )
				System.out.format("[%d] %d volume left: %d\n", cpu.Cycles, reg >>> 4, Byte);
			
			sounddata.SetSoundVolume(reg >>> 4, Byte, DSP[reg + 1]);
			break;
		case APU_VOL_RIGHT + 0x00:
		case APU_VOL_RIGHT + 0x10:
		case APU_VOL_RIGHT + 0x20:
		case APU_VOL_RIGHT + 0x30:
		case APU_VOL_RIGHT + 0x40:
		case APU_VOL_RIGHT + 0x50:
		case APU_VOL_RIGHT + 0x60:
		case APU_VOL_RIGHT + 0x70:
			if ( SnesSystem.DEBUG_DSP )
				System.out.format("[%d] %d volume right: %d\n", cpu.Cycles, reg >>> 4, Byte);
			
			sounddata.SetSoundVolume (reg >>> 4, DSP[reg - 1], Byte);
			break;

		case APU_P_LOW + 0x00:
		case APU_P_LOW + 0x10:
		case APU_P_LOW + 0x20:
		case APU_P_LOW + 0x30:
		case APU_P_LOW + 0x40:
		case APU_P_LOW + 0x50:
		case APU_P_LOW + 0x60:
		case APU_P_LOW + 0x70:
			if ( SnesSystem.DEBUG_DSP )
				System.out.format("[%d] %d freq low: %d\n", cpu.Cycles, reg >>> 4, Byte);
			
			sounddata.SetSoundHertz (reg >>> 4, ((Byte + (DSP[reg + 1] << 8)) & FREQUENCY_MASK) * 8);
			break;

		case APU_P_HIGH + 0x00:
		case APU_P_HIGH + 0x10:
		case APU_P_HIGH + 0x20:
		case APU_P_HIGH + 0x30:
		case APU_P_HIGH + 0x40:
		case APU_P_HIGH + 0x50:
		case APU_P_HIGH + 0x60:
		case APU_P_HIGH + 0x70:
			if ( SnesSystem.DEBUG_DSP )
				System.out.format("[%d] %d freq high: %d\n", cpu.Cycles, reg >>> 4, Byte);
			
			sounddata.SetSoundHertz (reg >>> 4, (((Byte << 8) + DSP[reg - 1]) & FREQUENCY_MASK) * 8);
			break;

		case APU_SRCN + 0x00:
		case APU_SRCN + 0x10:
		case APU_SRCN + 0x20:
		case APU_SRCN + 0x30:
		case APU_SRCN + 0x40:
		case APU_SRCN + 0x50:
		case APU_SRCN + 0x60:
		case APU_SRCN + 0x70:
			
			if ( SnesSystem.DEBUG_DSP )
				System.out.format("[%d] %d sample number: %d\n", cpu.Cycles, reg >>> 4, Byte);
			break;

		case APU_ADSR1 + 0x00:
		case APU_ADSR1 + 0x10:
		case APU_ADSR1 + 0x20:
		case APU_ADSR1 + 0x30:
		case APU_ADSR1 + 0x40:
		case APU_ADSR1 + 0x50:
		case APU_ADSR1 + 0x60:
		case APU_ADSR1 + 0x70:
			if (Byte != DSP[reg])
			{
				if ( SnesSystem.DEBUG_DSP )
					System.out.format("[%d] %d adsr1: %02x\n", cpu.Cycles, reg >>> 4, Byte);
				
				FixEnvelope (reg >>> 4, DSP[reg + 2], Byte, DSP[reg + 1]);
			}
			break;

		case APU_ADSR2 + 0x00:
		case APU_ADSR2 + 0x10:
		case APU_ADSR2 + 0x20:
		case APU_ADSR2 + 0x30:
		case APU_ADSR2 + 0x40:
		case APU_ADSR2 + 0x50:
		case APU_ADSR2 + 0x60:
		case APU_ADSR2 + 0x70:
			if (Byte != DSP[reg])
			{
				if ( SnesSystem.DEBUG_DSP )
					System.out.format("[%d] %d adsr2: %02x\n", cpu.Cycles, reg >>> 4, Byte);
				
					FixEnvelope (reg >>> 4, DSP[reg + 1], DSP[reg - 1], Byte);
			}
			break;

		case APU_GAIN + 0x00:
		case APU_GAIN + 0x10:
		case APU_GAIN + 0x20:
		case APU_GAIN + 0x30:
		case APU_GAIN + 0x40:
		case APU_GAIN + 0x50:
		case APU_GAIN + 0x60:
		case APU_GAIN + 0x70:
			if (Byte != DSP[reg])
			{
				if ( SnesSystem.DEBUG_DSP )
					System.out.format("[%d] %d gain: %02x\n", cpu.Cycles, reg >>> 4, Byte);
				
					FixEnvelope (reg >>> 4, Byte, DSP[reg - 2], DSP[reg - 1]);
			}
			break;

		case APU_ENVX + 0x00:
		case APU_ENVX + 0x10:
		case APU_ENVX + 0x20:
		case APU_ENVX + 0x30:
		case APU_ENVX + 0x40:
		case APU_ENVX + 0x50:
		case APU_ENVX + 0x60:
		case APU_ENVX + 0x70:
			break;

		case APU_OUTX + 0x00:
		case APU_OUTX + 0x10:
		case APU_OUTX + 0x20:
		case APU_OUTX + 0x30:
		case APU_OUTX + 0x40:
		case APU_OUTX + 0x50:
		case APU_OUTX + 0x60:
		case APU_OUTX + 0x70:
			break;

		case APU_DIR:
			if ( SnesSystem.DEBUG_DSP )
				System.out.format("[%d] Sample directory to: %02x\n", cpu.Cycles, Byte);
			break;

		case APU_PMON:
			if (Byte != DSP[APU_PMON])
			{
				if ( SnesSystem.DEBUG_DSP )
				{
					System.out.format("[%d] FreqMod:", cpu.Cycles);
					int mask = 1;
					for (int c = 0; c < 8; c++, mask <<= 1)
					{
						if ( ( Byte & mask ) > 0 )
						{
							if ( ( DSP [reg] & mask ) > 0 )
								System.out.format("%d", c);
							else
								System.out.format("%d(on),", c);
						}
						else
						{
							if ( ( DSP [reg] & mask ) > 0 )
								System.out.format("%d(off),", c);
						}
					}
					System.out.format("\n");
				}
				
				sounddata.SetFrequencyModulationEnable (Byte);
			}
			break;

		case APU_EON:
			if (Byte != DSP[APU_EON])
			{
				if ( SnesSystem.DEBUG_DSP )
				{
					System.out.format ("[%d] Echo:", cpu.Cycles);
					int mask = 1;
					for (int c = 0; c < 8; c++, mask <<= 1)
					{
						if ( ( Byte & mask ) > 0 )
						{
							if ( ( DSP[reg] & mask ) > 0 )
								System.out.format("%d", c);
							else
								System.out.format("%d(on),", c);
						}
						else
						{
							if ( ( DSP[reg] & mask ) > 0 )
								System.out.format("%d(off),", c);
						}
					}
					System.out.format("\n");
				}
				
				sounddata.SetEchoEnable(Byte);
			}
			break;

		case APU_EFB:
			sounddata.SetEchoFeedback( Byte);
			break;

		case APU_ESA:
			break;

		case APU_EDL:
			sounddata.SetEchoDelay(Byte & 0xf);
			break;

		case APU_C0:
		case APU_C1:
		case APU_C2:
		case APU_C3:
		case APU_C4:
		case APU_C5:
		case APU_C6:
		case APU_C7:
			sounddata.SetFilterCoefficient(reg >>> 4, Byte);
			break;
		default:
			break;
		}

		KeyOnPrev |= KeyOn;
		KeyOn = 0;

		if (reg < 0x80)
			DSP[reg] = Byte;
	}
	
	void FixEnvelope (int channel, int gain, int adsr1, int adsr2)
	{
		if ( ( adsr1 & 0x80 ) > 0)
		{
			if (sounddata.SetSoundMode (channel, SoundData.MODE_ADSR))
			{
				sounddata.SetSoundADSR (channel, adsr1 & 0xf, (adsr1 >>> 4) & 7, adsr2 & 0x1f, (adsr2 >>> 5) & 7);
			}
		}
		else
		{
			if ((gain & 0x80) == 0)
			{
				if (sounddata.SetSoundMode (channel, SoundData.MODE_GAIN))
					sounddata.SetEnvelopeHeight (channel, (gain & 0x7f) << SoundData.ENV_SHIFT);
			}
			else
			{
				if ( ( gain & 0x40) > 0 )
				{
					if (sounddata.SetSoundMode (channel, (gain & 0x20) > 0 ?
						SoundData.MODE_INCREASE_BENT_LINE : SoundData.MODE_INCREASE_LINEAR))
						sounddata.SetEnvelopeRate (channel, SoundData.env_counter_table[gain & 0x1f], SoundData.ENV_MAX);
				}
				else
				{
					if (sounddata.SetSoundMode (channel, (gain & 0x20) > 0 ?
						SoundData.MODE_DECREASE_EXPONENTIAL : SoundData.MODE_DECREASE_LINEAR))
						sounddata.SetEnvelopeRate (channel, SoundData.env_counter_table[gain & 0x1f], 0);
				}
			}
		}
	}

	private void SetAPUControl (int Byte)
	{
		if ((Byte & 1) != 0 && !TimerEnabled [0])
		{
			Timer[0] = 0;
			RAM.put8Bit(0xfd, 0);
			
			if ((TimerTarget [0] = RAM.get8Bit(0xfa) ) == 0)
				TimerTarget [0] = 0x100;
		}
		
		if ((Byte & 2) != 0 && !TimerEnabled [1])
		{
			Timer[1] = 0;
			RAM.put8Bit(0xfe, 0);
			if ((TimerTarget [1] = RAM.get8Bit(0xfb) ) == 0)
				TimerTarget [1] = 0x100;
		}
		if ((Byte & 4) != 0 && !TimerEnabled [2])
		{
			Timer[2] = 0;
			RAM.put8Bit(0xff,  0);
			if ((TimerTarget [2] = RAM.get8Bit(0xfc) ) == 0)
				TimerTarget [2] = 0x100;
		}
		TimerEnabled [0] = ( Byte & 1) > 0;
		TimerEnabled [1] = ( ( Byte & 2 ) >>> 1 ) > 0;
		TimerEnabled [2] = ( ( Byte & 4 ) >>> 2 ) > 0;

		if ( ( Byte & 0x10 ) > 0)
		{
			RAM.put8Bit(0xF4, 0);
			RAM.put8Bit(0xF5, 0);
		}

		if ( ( Byte & 0x20 ) > 0)
		{
			RAM.put8Bit(0xF6, 0);
			RAM.put8Bit(0xF7, 0);
		}

		if ( ( Byte & 0x80) > 0 )
		{
			if (!ShowROM)
			{
				RAM.arraycopy(0xffc0, APU.APUROM, 0, APU.APUROM.size() );	
				ShowROM = true;
			}
		}
		else
		{
			if (ShowROM)
			{
				ShowROM = false;
				RAM.arraycopy(0xffc0, ExtraRAM, 0, APU.APUROM.size() );
			}
		}
		
		RAM.put8Bit(0xf1, Byte);
	}

	void SetAPUTimer( int Address, int Byte )
	{
		RAM.put8Bit(Address, Byte);

		switch (Address)
		{
		case 0xfa:
			if ((TimerTarget[0] = RAM.get8Bit(0xfa) ) == 0)
				TimerTarget[0] = 0x100;
			TimerValueWritten[0] = true;
			break;
		case 0xfb:
			if ((TimerTarget[1] = RAM.get8Bit(0xfb) ) == 0)
				TimerTarget[1] = 0x100;
			TimerValueWritten[1] = true;
			break;
		case 0xfc:
			if ((TimerTarget[2] = RAM.get8Bit(0xfc) ) == 0)
				TimerTarget[2] = 0x100;
			TimerValueWritten[2] = true;
			break;
		}
	}

	void APUExecute()
	{
		while ( ( cpu.Cycles << SnesSystem.SNES_APU_ACCURACY ) >= NextAPUTimerPos)
		{
			// catch up the APU timers
			if (APUExecuting)
			{
				while (ApuCycles < NextAPUTimerPos)
					APU_EXECUTE1();
			}
			else
			{
				ApuCycles = NextAPUTimerPos;
			}

			NextAPUTimerPos += SnesSystem.SNES_APUTIMER2_CYCLE_SCALED;

			if( TimerEnabled[2] )
			{
				Timer[2] ++;
				if (Timer[2] >= TimerTarget[2] )
				{
					RAM.put8Bit(0xff, ( RAM.get8Bit(0xff) + 1) & 0xf );
					Timer[2] = 0;
					WaitCounter++;
					APUExecuting = true;
				}
			}

			if (++APUTimerCounter == 8)
			{
				APUTimerCounter = 0;

				if (TimerEnabled[0] )
				{
					Timer[0]++;
					
					if ( Timer[0] >= TimerTarget [0] )
					{
						RAM.put8Bit(0xfd, ( RAM.get8Bit(0xfd) + 1) & 0xf );
						Timer[0] = 0;
						WaitCounter++;
						APUExecuting = true;
					}
				}

				if( TimerEnabled[1] )
				{
					Timer[1]++;
					if ( Timer[1] >= TimerTarget [1] )
					{
						RAM.put8Bit(0xfe, ( RAM.get8Bit(0xfe) + 1) & 0xf );
						Timer[1] = 0;
						WaitCounter++;
						APUExecuting = true;
					}
				}
			}
		}

		// catch up the current cycles
		if (APUExecuting)
		{
			while (ApuCycles < (cpu.Cycles << SnesSystem.SNES_APU_ACCURACY))
			{
				APU_EXECUTE1();
			}
		}
		else
		{
			ApuCycles = (cpu.Cycles << SnesSystem.SNES_APU_ACCURACY);
		}
	}

	private int GetAPUDSP ()
	{
		int reg = RAM.getByte(0xf2) & 0x7f;
		int Byte = DSP[reg];

		switch (reg)
		{
			case APU_KON:
				break;
			case APU_KOFF:
				break;

			case APU_OUTX + 0x00:
			case APU_OUTX + 0x10:
			case APU_OUTX + 0x20:
			case APU_OUTX + 0x30:
			case APU_OUTX + 0x40:
			case APU_OUTX + 0x50:
			case APU_OUTX + 0x60:
			case APU_OUTX + 0x70:
				
			if(settings.FakeMuteFix)
			{
				// hack that is off by default: fixes Terranigma desync
				return 0;
			}
			else
			{
				if ( sounddata.channels[reg >>> 4].state == SoundData.SOUND_SILENT )
					return 0;

				return (byte)(sounddata.channels[reg >>> 4].out_sample >>> 8) & 0xFF;
			}

			case APU_ENVX + 0x00:
			case APU_ENVX + 0x10:
			case APU_ENVX + 0x20:
			case APU_ENVX + 0x30:
			case APU_ENVX + 0x40:
			case APU_ENVX + 0x50:
			case APU_ENVX + 0x60:
			case APU_ENVX + 0x70:
				return sounddata.GetEnvelopeHeight(reg >>> 4);

			case APU_ENDX:
				// To fix speech in Magical Drop 2 6/11/00
				//	DSP[APU_ENDX] = 0;
				break;

			default:
				break;
		}

		return (Byte);
	}
	
	
	private int apu_get_reg (int Address)
	{
		switch (Address)
		{
			case 0xf0:	// -w TEST
				return 0;

			case 0xf1:	// -w CONTROL
				return 0;

			case 0xf2:	// rw DSPADDR
				return RAM.get8Bit(Address);

			case 0xf3:	// rw DSPDATA
				return GetAPUDSP();

			case 0xf4:	// r- CPUI0
			case 0xf5:	// r- CPUI1
			case 0xf6:	// r- CPUI2
			case 0xf7:	// r- CPUI3
				WaitAddress2 = WaitAddress1;
				WaitAddress1 = iPC;
				return RAM.get8Bit(Address);

			case 0xf8:	// rw - Normal RAM
			case 0xf9:	// rw - Normal RAM
				return RAM.get8Bit(Address);

			case 0xfa:	// -w T0TARGET
			case 0xfb:	// -w T1TARGET
			case 0xfc:	// -w T2TARGET
				return 0;

			case 0xfd:	// r- T0OUT
			case 0xfe:	// r- T1OUT
			case 0xff:	// r- T2OUT

				WaitAddress2 = WaitAddress1;
				WaitAddress1 = iPC;
				int t = RAM.getByte(Address) & 0xF;
				RAM.put8Bit(Address, 0);
				return t;
		}

		return 0;
	}

	private void apu_set_reg (int Byte, int Address)
	{
		switch (Address)
		{
			case 0xf0:	// -w TEST
				return;

			case 0xf1:	// -w CONTROL
				SetAPUControl(Byte);
				return;

			case 0xf2:	// rw DSPADDR
				RAM.put8Bit(Address, Byte);
				return;

			case 0xf3:	// rw DSPDATA
				SetAPUDSP(Byte);
				return;

			case 0xf4:	// -w CPUO0
			case 0xf5:	// -w CPUO1
			case 0xf6:	// -w CPUO2
			case 0xf7:	// -w CPUO3
				OutPorts[Address - 0xf4] = Byte;
				return;

			case 0xf8:	// rw - Normal RAM
			case 0xf9:	// rw - Normal RAM
				RAM.put8Bit(Address, Byte);
				return;

			case 0xfa:	// -w T0TARGET
			case 0xfb:	// -w T1TARGET
			case 0xfc:	// -w T2TARGET
				RAM.put8Bit(Address, Byte);
				if (Byte == 0)
					TimerTarget[Address - 0xfa] = 0x100;
				else
					TimerTarget[Address - 0xfa] = Byte;
				return;

			case 0xfd:	// r- T0OUT
			case 0xfe:	// r- T1OUT
			case 0xff:	// r- T2OUT
				return;
		}
	}

	private int APUGetByteZ(int Address)
	{
		// NAC: Possible optimization for seeing if DirectPage is the same as RAM
		if (Address >= 0xf0 && DirectPage.getOffset() == 0 )
			return apu_get_reg(Address);
		else
			return DirectPage.get8Bit(Address);
	}

	private void APUSetByteZ(int Byte, int Address)
	{		
		if ( SnesSystem.DEBUG_APU )
			System.out.println( String.format("APU SetByteZ: %02X to %06X", Byte, Address ));			
		
		if (Address >= 0xf0 && DirectPage.getOffset() == 0 )
			apu_set_reg(Byte, Address);
		else
			DirectPage.put8Bit(Address, Byte);
	}

	private int APUGetByte (int Address)
	{
		Address &= 0xffff;
		if (Address <= 0xff && Address >= 0xf0)
		{
			return apu_get_reg(Address & 0xff);
		}
		else
		{
			return RAM.get8Bit(Address);
		}
	}

	private void APUSetByte (int Byte, int Address)
	{	
		if ( SnesSystem.DEBUG_APU )
			System.out.println( String.format("APU SetByte: %02X to %06X", (Byte & 0xFF), Address ));

		Address &= 0xffff;
		if (Address <= 0xff && Address >= 0xf0)
		{
			apu_set_reg(Byte, Address & 0xff);
		}
		else if (Address < 0xffc0)
		{
			RAM.put8Bit(Address, Byte);
		}
		else
		{
			ExtraRAM.put8Bit(Address - 0xffc0, Byte);
			if ( ! ShowROM )
			{
				RAM.put8Bit(Address, Byte);
			}
		}
	}

	ByteArrayOffset GetSampleAddress (int sample_number)
	{
		int addr = ((DSP[APU_DIR] << 8) + (sample_number << 2)) & 0xFFFF;		
		return RAM.getOffsetBuffer(addr);
	}
	
	private void APUUnpackStatus()
	{
		_Zero = ((P & Zero) == 0 ? 1 : 0 ) | (P & Negative);
		_Carry = (P & Carry);
		_Overflow = (P & Overflow) >>> 6;
	}

	void APUPackStatus()
	{
		P &= ~(Zero | Negative | Carry | Overflow);
		P |= _Carry | ((_Zero == 0 ? 0 : 1) << 1) | (_Zero & 0x80) | (_Overflow << 6);
	}	

	private void APUClearCarry()
	{
		_Carry = 0;
	}
	private void APUSetCarry()
	{
		_Carry = 1;
	}
	private void APUSetInterrupt()
	{
		P |= Interrupt;
	}
	private void APUClearInterrupt()
	{
		P &= ~Interrupt;
	}
	
	private void APUSetHalfCarry()
	{
		P |= HalfCarry;
	}
	
	private void APUClearHalfCarry()
	{
		P &= ~HalfCarry;
	}
	
	private void APUSetBreak()
	{
		P |= BreakFlag;
	}
	
	private void APUSetDirectPage()
	{
		P |= DirectPageFlag;
	}
	
	private void APUClearDirectPage()
	{
		P &= ~DirectPageFlag;
	}
	
	private void APUSetOverflow()
	{
		_Overflow = 1;
	}
	
	private void APUClearOverflow()
	{
		_Overflow = 0;
	}

	private boolean APUCheckZero()
	{
		return _Zero == 0;
	}
	
	private int APUCheckCarry()
	{
		return _Carry;
	}
	
	private boolean APUCheckInterrupt()
	{
		return (P & Interrupt) == Interrupt;
	}
	
	private boolean APUCheckHalfCarry()
	{
		return (P & HalfCarry) == HalfCarry;
	}

	private boolean APUCheckBreak()
	{
		return (P & BreakFlag) == BreakFlag;
	}
	
	private boolean APUCheckDirectPage()
	{
		return (P & DirectPageFlag) == DirectPageFlag;
	}
	
	private int APUCheckOverflow()
	{
		return _Overflow;
	}
	
	private boolean APUCheckNegative()
	{
		return (_Zero & 0x80) != 0;
	}
	
	private void APUShutdown()
	{
		if (settings.Shutdown && (iPC == WaitAddress1 || iPC == WaitAddress2))
		{
			if (WaitCounter == 0)
			{
				if (cpu.CPUExecuting != 0)
				{
					APUExecute();
				}
				else
				{
					APUExecuting = false;					
				}
			}
			else
			{
				if (WaitCounter >= 2)
				{
					WaitCounter = 1;
				}
				else
				{
					WaitCounter--;
				}
			}
		}
	}
	
	private int OP1()
	{
		return RAM.get8Bit(iPC + 1);
	}
	private int OP2()
	{
		return RAM.get8Bit(iPC + 2);
	}
	
	private void APUSetZN8( int b)
	{
		_Zero = b;
	}
	
	private void APUSetZN16(int w)
	{
		_Zero = ((w) != 0 ? 1 : 0) | ((w) >>> 8);
	}
	
	private void TCALL(int n)
	{
		PushW (iPC + 1);
		iPC = APUGetByte(0xffc0 + ((15 - n) << 1)) + (APUGetByte(0xffc1 + ((15 - n) << 1)) << 8);
	}

	private int SBC( int a, int b)
	{
		Int16 = (short) (a) - (short) (b) + (short) (APUCheckCarry()) - 1;
		_Carry = Int16 >= 0 ? 1: 0;
		
		if ( ( ( ( a ^  b ) & 0x80 ) & ( ( a ^ (Int16 & 0xFF) ) & 0x80 ) ) != 0 )
		{
			APUSetOverflow();
		}
		else
		{
			APUClearOverflow();
		}
		APUSetHalfCarry();
		
		if( ( ( a ^ b ^ ( Int16 & 0xFF) ) & 0x10 ) != 0 )
		{
			APUClearHalfCarry();
		}
		
		APUSetZN8(Int16 & 0xFF);
		
		return Int16 & 0xFF;
	}
	
	private int ADC( int a, int b)
	{
		Work16 = (a) + (b) + APUCheckCarry();
		_Carry = Work16 >= 0x100 ? 1 : 0; 
		if ( ( ~((a) ^ (b)) & ((b) ^ (Work16 & 0xFF)) & 0x80 ) == 0x80)
		{
			APUSetOverflow();
		}
		else
		{
			APUClearOverflow();
		}
		
		APUClearHalfCarry();
		
		if ( ( ( a ^ b ^ (Work16 & 0xFF) ) & 0x10) != 0 )
		{
			APUSetHalfCarry();
		}
		
		a = Work16 & 0xFF;
		
		APUSetZN8( Work16 & 0xFF );	
		return a;
	}

	private void CMP( int a, int b)
	{
		Int16 = (short) (a) - (short) (b);
		_Carry = Int16 >= 0 ? 1 : 0;
		APUSetZN8(Int16 & 0xFF);
	}

	private int ASL( int b)
	{
		// If the signed bit is set then set carry to 1
		_Carry = ( b & 0x80 ) >>> 7; 
		b = (b << 1) & 0xFF;
		APUSetZN8(b);
		return b;
	}
		
	private int LSR( int b )
	{
		_Carry = (b) & 1;
		(b) >>= 1;
		APUSetZN8(b);
		return b;
	}
	private int ROL( int b )
	{
		Work16 = ((b) << 1) | APUCheckCarry(); 
		_Carry = Work16 >= 0x100 ? 1 : 0; 
		(b) = Work16 & 0xFF; 
		APUSetZN8(b);
		return b;
	}
	
	private int ROR( int b)
	{
		Work16 = (b) | ((APUCheckCarry()) << 8) & 0xFFFF; 
		_Carry = Work16 & 1; 
		Work16 >>= 1; 
		(b) = Work16 & 0xFF; 
		APUSetZN8(b);
		return b;
	}

	private void Push( int b)
	{
		// When 0xFF masking S r
		RAM.put8Bit(0x100 + S, b);
		S = ( S - 1 ) & 0xff;
	}

	private int Pop( )
	{
		S = ( S + 1 ) & 0xff;
		return RAM.get8Bit( 0x100 + S);
	}

	private void PushW(int w)
	{
		if(S == 0)
		{ 
			RAM.put8Bit(0x1ff, w);
			RAM.put8Bit(0x100, w >>> 8);
		} else { 
			RAM.put16Bit(0xff + S, w);
		} 
		S -= 2;
	}
	
	private int PopW()
	{
		S += 2;
		if(S == 0)
		{ 
			return RAM.get8Bit(0x1ff) | (RAM.get8Bit(0x100) << 8);
		} else { 
			return RAM.get16Bit( 0xff + S );
		}
	}
	
	private void Relative()
	{
		Int8 = (byte) OP1();
		Int16 = (short) ( (iPC + 2) + Int8 );
	}

	private void Relative2()
	{
		Int8 = (byte) OP2();
		Int16 = 0xFFFF & ( (iPC + 3) + Int8);
	}
	
	private void IndexedXIndirect()
	{
		Address = (DirectPage.get16Bit(((OP1() + X) & 0xff)));
	}
	
	private void Absolute()
	{
		Address = RAM.get16Bit(iPC + 1);
	}
	
	private void AbsoluteX()
	{
		Address = RAM.get16Bit(iPC + 1) + X;
	}
	
	private void AbsoluteY()
	{
		Address = RAM.get16Bit(iPC + 1 ) + YA_Y();
	}
	
	private void MemBit()
	{
		Address = RAM.get16Bit(iPC + 1);
		Bit = (byte)(Address >>> 13);
		Address &= 0x1fff;
	}
	
	private void IndirectIndexedY()
	{
		Address = DirectPage.get16Bit( OP1() ) + YA_Y();
	}

	private void Apu00 ()
	{
		// NOP
		iPC++;
	}

	private void Apu01 () { TCALL(0); }
	private void Apu11 () { TCALL(1); }
	private void Apu21 () { TCALL(2); }
	private void Apu31 () { TCALL(3); }
	private void Apu41 () { TCALL(4); }
	private void Apu51 () { TCALL(5); }
	private void Apu61 () { TCALL(6); }
	private void Apu71 () { TCALL(7); }
	private void Apu81 () { TCALL(8); }
	private void Apu91 () { TCALL(9); }
	private void ApuA1 () { TCALL(10); }
	private void ApuB1 () { TCALL(11); }
	private void ApuC1 () { TCALL(12); }
	private void ApuD1 () { TCALL(13); }
	private void ApuE1 () { TCALL(14); }
	private void ApuF1 () { TCALL(15); }

	private void Apu3F () // CALL absolute
	{
		Absolute ();
		// 0xB6f for Star Fox 2
		PushW(iPC + 3);
		iPC = Address;
	}

	private void Apu4F () // PCALL $XX
	{
		Work8 = OP1();
		PushW(iPC + 2);
		iPC = 0xff00 + Work8;
	}

	private void SET( int b)
	{
		APUSetByteZ((APUGetByteZ(OP1() ) | (1 << (b))) & 0xFF, OP1());
		iPC += 2;
	}

	private void Apu02() { SET(0); }
	private void Apu22() { SET(1); }
	private void Apu42() { SET(2); }
	private void Apu62() { SET(3); }
	private void Apu82() { SET(4); }
	private void ApuA2()	{ SET(5); }
	private void ApuC2()	{ SET(6); }
	private void ApuE2() { SET(7); }

	private void CLR(int b)
	{
		APUSetByteZ((APUGetByteZ(OP1()) & ~(1 << (b))) & 0xFF, OP1());
		iPC += 2;
	}

	private void Apu12() { CLR(0); }
	private void Apu32()	{ CLR(1); }
	private void Apu52() { CLR(2); }
	private void Apu72() { CLR(3); }
	private void Apu92() { CLR(4); }
	private void ApuB2() { CLR(5); }
	private void ApuD2() { CLR(6); }
	private void ApuF2() { CLR(7); }

	private void BBS( int b)
	{
		Work8 = OP1();
		Relative2();
		if ( ( APUGetByteZ(Work8) & (1 << (b))) != 0 )
		{
			iPC = 0xFFFF & Int16;
			ApuCycles += TwoCycles;
		}
		else
			iPC += 3;
	}

	private void Apu03() { BBS(0); }
	private void Apu23() { BBS(1); }
	private void Apu43() { BBS(2); }
	private void Apu63() { BBS(3); }
	private void Apu83() { BBS(4); }
	private void ApuA3() { BBS(5); }
	private void ApuC3() { BBS(6); }
	private void ApuE3() { BBS(7); }

	private void BBC( int b )
	{
		Work8 = OP1();
		Relative2();
		if ( (APUGetByteZ(Work8) & (1 << (b))) == 0)
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
		}
		else
			iPC += 3;
	}

	private void Apu13() { BBC (0); }
	private void Apu33() { BBC (1); }
	private void Apu53()	{ BBC (2); }
	private void Apu73()	{ BBC (3); }
	private void Apu93()	{ BBC (4); }
	private void ApuB3() { BBC (5); }
	private void ApuD3() { BBC (6); }
	private void ApuF3()	{ BBC (7); }

	private void Apu04()
	{
		// OR A,dp
		YA_A( YA_A() | APUGetByteZ( OP1() ) );
		APUSetZN8( YA_A() );
		iPC += 2;
	}

	private void Apu05()
	{
		// OR A,abs
		Absolute();
		YA_A( YA_A() | APUGetByte(Address) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void Apu06()
	{
		// OR A,(X)
		YA_A( YA_A() | APUGetByteZ( X ) );
		APUSetZN8( YA_A());
		iPC++;
	}

	private void Apu07()
	{
		// OR A,(dp+X)
		IndexedXIndirect();
		YA_A( YA_A() | APUGetByte( Address ) );
		APUSetZN8( YA_A() );
		iPC += 2;
	}

	private void Apu08()
	{
		// OR A,#00
		YA_A( YA_A() | OP1() );
		APUSetZN8( YA_A() );
		iPC += 2;
	}

	private void Apu09()
	{
		// OR dp(dest),dp(src)
		Work8 = APUGetByteZ( OP1() );
		Work8 |= APUGetByteZ( OP2() );
		APUSetByteZ(Work8, OP2() );
		APUSetZN8(Work8);
		iPC += 3;
	}

	private void Apu14()
	{
		// OR A,dp+X
		YA_A( YA_A() | APUGetByteZ(OP1() + X) );
		APUSetZN8( YA_A() );
		iPC += 2;
	}

	private void Apu15()
	{
		// OR A,abs+X
		AbsoluteX();
		YA_A( YA_A() | APUGetByte( Address ) );
		APUSetZN8( YA_A() );
		iPC += 3;
	}

	private void Apu16()
	{
		// OR A,abs+Y
		AbsoluteY();
		YA_A( YA_A() | APUGetByte(Address) );
		APUSetZN8( YA_A() );
		iPC += 3;
	}

	private void Apu17()
	{
		// OR A,(dp)+Y
		IndirectIndexedY();
		YA_A( YA_A() | APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu18()
	{
		// OR dp,#00
		Work8 = OP1();
		Work8 |= APUGetByteZ(OP2());
		APUSetByteZ(Work8, OP2());
		APUSetZN8(Work8);
		iPC += 3;
	}

	private void Apu19()
	{
	// OR (X),(Y)
		Work8 = APUGetByteZ(X) | APUGetByteZ(YA_Y());
		APUSetZN8(Work8);
		APUSetByteZ(Work8, X);
		iPC++;
	}

	private void Apu0A()
	{
		// OR1 C,membit
		MemBit();
		if ( APUCheckCarry() == 0 )
		{
			if ( ( APUGetByte( Address ) & (1 << Bit) ) != 0)
				APUSetCarry();
		}
		iPC += 3;
	}

	private void Apu2A()
	{
		// OR1 C,not membit
		MemBit();
		if ( APUCheckCarry() == 0)
		{
			if ( (APUGetByte( Address ) & (1 << Bit)) == 0 )
				APUSetCarry();
		}
		iPC += 3;
	}

	private void Apu4A()
	{
		// AND1 C,membit
		MemBit();
		if ( APUCheckCarry() != 0 )
		{
			if ( (APUGetByte( Address ) & (1 << Bit)) == 0 )
				APUClearCarry();
		}
		iPC += 3;
	}

	private void Apu6A()
	{
		// AND1 C, not membit
		MemBit();
		if ( APUCheckCarry() != 0 )
		{
			if ( ( APUGetByte( Address ) & (1 << Bit) ) != 0 )
				APUClearCarry();
		}
		iPC += 3;
	}

	private void Apu8A()
	{
		// EOR1 C, membit
		MemBit();
		if ( APUCheckCarry() != 0 )
		{
			if ( ( APUGetByte( Address ) & (1 << Bit) ) != 0 )
				APUClearCarry();
		}
		else
		{
			if ( ( APUGetByte( Address ) & (1 << Bit) ) != 0 )
				APUSetCarry();
		}
		iPC += 3;
	}

	private void ApuAA()
	{
		// MOV1 C,membit
		MemBit();
		if ( ( APUGetByte( Address ) & (1 << Bit) ) != 0 )
			APUSetCarry();
		else
			APUClearCarry();
		iPC += 3;
	}

	private void ApuCA()
	{
	// MOV1 membit,C
		MemBit();
		if ( APUCheckCarry() != 0 )
		{
		APUSetByte (APUGetByte( Address ) | (1 << Bit), Address);
		}
		else
		{
		APUSetByte (APUGetByte( Address ) & ~(1 << Bit), Address);
		}
		iPC += 3;
	}

	private void ApuEA()
	{
		// NOT1 membit
		MemBit();
		APUSetByte (APUGetByte( Address ) ^ (1 << Bit), Address);
		iPC += 3;
	}

	private void Apu0B()
	{
		// ASL dp
		Work8 = APUGetByteZ(OP1());
		Work8 = ASL( Work8 );
		APUSetByteZ(Work8, OP1());
		iPC += 2;
	}

	private void Apu0C()
	{
		// ASL abs
		Absolute();
		Work8 = APUGetByte( Address );
		Work8 = ASL (Work8);
		APUSetByte (Work8, Address);
		iPC += 3;
	}

	private void Apu1B()
	{
		// ASL dp+X
		Work8 = APUGetByteZ(OP1() + X);
		Work8 = ASL (Work8);
		APUSetByteZ(Work8, OP1() + X);
		iPC += 2;
	}

	private void Apu1C()
	{
		// ASL A
		YA_A( ASL( YA_A() ) );
		iPC++;
	}

	private void Apu0D()
	{
		// PUSH PSW
		APUPackStatus();
		Push(P);
		iPC++;
	}

	private void Apu2D()
	{
		// PUSH A
		Push( YA_A() );
		iPC++;
	}

	private void Apu4D()
	{
		// PUSH X
		Push(X);
		iPC++;
	}

	private void Apu6D()
	{
		// PUSH Y
		Push( YA_Y() );
		iPC++;
	}

	private void Apu8E()
	{
		// POP PSW
		P = Pop();
		APUUnpackStatus();
		if (APUCheckDirectPage())
			DirectPage = RAM.getOffsetBuffer( 0x100 );
		else
			DirectPage = RAM.getOffsetBuffer(0);
		
		iPC++;
	}

	private void ApuAE()
	{
		// POP A
		YA_A( Pop() );
		iPC++;
	}

	private void ApuCE()
	{
		// POP X
		X = Pop();
		iPC++;
	}

	private void ApuEE()
	{
		// POP Y
		YA_Y( Pop() );
		iPC++;
	}

	private void Apu0E()
	{
		// TSET1 abs
		Absolute();
		Work8 = APUGetByte( Address );
		APUSetByte (Work8 | YA_A(), Address);
		Work8 = YA_A() - Work8;
		APUSetZN8(Work8);
		iPC += 3;
	}

	private void Apu4E()
	{
		// TCLR1 abs
		Absolute();
		Work8 = APUGetByte( Address );
		APUSetByte (Work8 & ~YA_A(), Address);
		Work8 = YA_A() - Work8;
		APUSetZN8(Work8);
		iPC += 3;
	}

	private void Apu0F()
	{
		// BRK

		PushW(iPC + 1);
		APUPackStatus();
		Push(P);
		APUSetBreak();
		APUClearInterrupt();
		iPC = APUGetByte(0xffde) + (APUGetByte(0xffdf)<<8);

	}

	private void ApuEF()
	{
		// SLEEP
		Flags |= HALTED_FLAG;
		TimerEnabled[0] = TimerEnabled[1] = TimerEnabled[2] = false;
		APUExecuting = false;
	}

	private void ApuFF()
	{
		// STOP
		Flags |= HALTED_FLAG;
		TimerEnabled[0] = TimerEnabled[1] = TimerEnabled[2] = false;
		APUExecuting = false;
		settings.APUEnabled = false; // re-enabled on next APU reset
	}

	private void Apu10()
	{
		// BPL
		Relative();
		if (!APUCheckNegative())
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
			APUShutdown();
		}
		else
			iPC += 2;
	}

	private void Apu30()
	{
		// BMI
		Relative();
		if (APUCheckNegative())
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
			APUShutdown();
		}
		else
			iPC += 2;
	}

	private void Apu90()
	{
		// BCC
		Relative();
		if ( APUCheckCarry() == 0 )
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
			APUShutdown();
		}
		else
			iPC += 2;
	}

	private void ApuB0()
	{
		// BCS
		Relative();
		if ( APUCheckCarry() != 0 )
		{
			iPC = Int16 & 0xFFFF;;
			ApuCycles += TwoCycles;
			APUShutdown();
		}
		else
			iPC += 2;
	}

	private void ApuD0()
	{
		// BNE
		Relative();
		if ( ! APUCheckZero() )
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
			APUShutdown();
		}
		else
			iPC += 2;
	}

	private void ApuF0()
	{
		// BEQ
		Relative();
		if (APUCheckZero())
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
			APUShutdown();
		}
		else
			iPC += 2;
	}

	private void Apu50()
	{
		// BVC
		Relative();
		if ( APUCheckOverflow() == 0 )
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
		}
		else
			iPC += 2;
	}

	private void Apu70()
	{
		// BVS
		Relative();
		if ( APUCheckOverflow() != 0 )
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
		}
		else
			iPC += 2;
	}

	private void Apu2F()
	{
		// BRA
		Relative();
		iPC = Int16 & 0xFFFF;
	}

	private void Apu80()
	{
		// SETC
		APUSetCarry();
		iPC++;
	}

	private void ApuED()
	{
		// NOTC
		_Carry ^= 1;
		iPC++;
	}

	private void Apu40()
	{
		// SETP
		APUSetDirectPage();
		DirectPage = RAM.getOffsetBuffer( 0x100 );
		iPC++;
	}

	private void Apu1A()
	{
		// DECW dp
		Work16 = APUGetByteZ(OP1()) + (APUGetByteZ(OP1() + 1) << 8);
		Work16--;
		APUSetByteZ(Work16 & 0xFF, OP1() );
		APUSetByteZ(Work16 >>> 8, OP1() + 1);
		APUSetZN16 (Work16);
		iPC += 2;
	}

	private void Apu5A()
	{
		// CMPW YA,dp
		Work16 = APUGetByteZ(OP1()) + (APUGetByteZ(OP1() + 1) << 8);
		Int32 = YA_W() - Work16;
		_Carry = Int32 >= 0 ? 1 : 0;
		APUSetZN16( Int32 & 0xFFFF );
		iPC += 2;
	}

	private void Apu3A()
	{
		// INCW dp
		Work16 = APUGetByteZ(OP1()) + (APUGetByteZ(OP1() + 1) << 8);
		Work16++;
		APUSetByteZ( Work16 & 0xFF, OP1());
		APUSetByteZ(Work16 >>> 8, OP1() + 1);
		APUSetZN16 (Work16);
		iPC += 2;
	}

	private void Apu7A()
	{
		// ADDW YA,dp
		Work16 = APUGetByteZ(OP1()) + (APUGetByteZ(OP1() + 1) << 8);
		Work32 = YA_W() + Work16;
		_Carry = Work32 >= 0x10000 ? 1 : 0;
		if ( ( ~( YA_W() ^ Work16 ) & ( Work16 ^ ( Work32 & 0xFFFF ) ) & 0x8000 ) == 0x8000 )
			APUSetOverflow();
		else
			APUClearOverflow();
		
		APUClearHalfCarry();
		if( ( ( YA_W() ^ Work16 ^ ( Work32 & 0xFFFF ) ) & 0x1000)  != 0 )
			APUSetHalfCarry();
		YA_W( Work32 & 0xFFFF);
		APUSetZN16( YA_W() );
		iPC += 2;
	}

	private void Apu9A()
	{
		// SUBW YA,dp
		Work16 = APUGetByteZ(OP1()) + (APUGetByteZ(OP1() + 1) << 8);
		Int32 = YA_W() - Work16;
		APUClearHalfCarry();
		_Carry = Int32 >= 0 ? 1 : 0;
		if (((YA_W() ^ Work16) & 0x8000) != 0 &&
			((YA_W() ^ ( Int32 & 0xFFFF ) ) & 0x8000) != 0 )
			APUSetOverflow();
		else
			APUClearOverflow();
		
		APUSetHalfCarry();
		if(((YA_W() ^ Work16 ^ (Int32 & 0xFFFF ) ) & 0x1000) != 0 )
			APUClearHalfCarry();
		YA_W( Int32 & 0xFFFF);
		APUSetZN16 (YA_W() );
		iPC += 2;
	}

	private void ApuBA()
	{
		// MOVW YA,dp
		YA_A( APUGetByteZ(OP1()));
		YA_Y( APUGetByteZ(OP1() + 1) );
		APUSetZN16 (YA_W() );
		iPC += 2;
	}

	private void ApuDA()
	{
		// MOVW dp,YA
		APUSetByteZ(YA_A(), OP1() );
		APUSetByteZ(YA_Y(), OP1() + 1 );
		iPC += 2;
	}

	private void Apu64()
	{
		// CMP A,dp
		Work8 = APUGetByteZ(OP1());
		CMP (YA_A(), Work8);
		iPC += 2;
	}

	private void Apu65()
	{
		// CMP A,abs
		Absolute();
		Work8 = APUGetByte( Address );
		CMP (YA_A(), Work8);
		iPC += 3;
	}

	private void Apu66()
	{
		// CMP A,(X)
		Work8 = APUGetByteZ(X);
		CMP (YA_A(), Work8);
		iPC++;
	}

	private void Apu67()
	{
		// CMP A,(dp+X)
		IndexedXIndirect();
		Work8 = APUGetByte( Address );
		CMP (YA_A(), Work8);
		iPC += 2;
	}

	private void Apu68()
	{
		// CMP A,#00
		Work8 = OP1();
		CMP (YA_A(), Work8);
		iPC += 2;
	}

	private void Apu69()
	{
		// CMP dp(dest), dp(src)
		W1 = APUGetByteZ(OP1());
		Work8 = APUGetByteZ(OP2());
		CMP (Work8, W1);
		iPC += 3;
	}

	private void Apu74()
	{
		// CMP A, dp+X
		Work8 = APUGetByteZ(OP1() + X);
		CMP (YA_A(), Work8);
		iPC += 2;
	}

	private void Apu75()
	{
		// CMP A,abs+X
		AbsoluteX();
		Work8 = APUGetByte( Address );
		CMP (YA_A(), Work8);
		iPC += 3;
	}

	private void Apu76()
	{
		// CMP A, abs+Y
		AbsoluteY();
		Work8 = APUGetByte( Address );
		CMP (YA_A(), Work8);
		iPC += 3;
	}

	private void Apu77()
	{
		// CMP A,(dp)+Y
		IndirectIndexedY();
		Work8 = APUGetByte( Address );
		CMP (YA_A(), Work8);
		iPC += 2;
	}

	private void Apu78()
	{
		// CMP dp,#00
		Work8 = OP1();
		W1 = APUGetByteZ(OP2());
		CMP (W1, Work8);
		iPC += 3;
	}

	private void Apu79()
	{
		// CMP (X),(Y)
		W1 = APUGetByteZ(X);
		Work8 = APUGetByteZ(YA_Y());
		CMP (W1, Work8);
		iPC++;
	}

	private void Apu1E()
	{
		// CMP X,abs
		Absolute();
		Work8 = APUGetByte( Address );
		CMP (X, Work8);
		iPC += 3;
	}

	private void Apu3E()
	{
		// CMP X,dp
		Work8 = APUGetByteZ(OP1());
		CMP (X, Work8);
		iPC += 2;
	}

	private void ApuC8()
	{
		// CMP X,#00
		CMP (X, OP1());
		iPC += 2;
	}

	private void Apu5E()
	{
		// CMP Y,abs
		Absolute();
		Work8 = APUGetByte( Address );
		CMP (YA_Y(), Work8);
		iPC += 3;
	}

	private void Apu7E()
	{
		// CMP Y,dp
		Work8 = APUGetByteZ(OP1());
		CMP (YA_Y(), Work8);
		iPC += 2;
	}

	private void ApuAD()
	{
		// CMP Y,#00
		Work8 = OP1();
		CMP (YA_Y(), Work8);
		iPC += 2;
	}

	private void Apu1F()
	{
		// JMP (abs+X)
		Absolute();
		iPC = APUGetByte(Address + X) + ( APUGetByte(Address + X + 1) << 8);
	}

	private void Apu5F()
	{
		// JMP abs
		Absolute();
		iPC = Address;
	}

	private void Apu20()
	{
		// CLRP
		APUClearDirectPage();
		DirectPage = RAM.getOffsetBuffer(0);
		iPC++;
	}

	private void Apu60()
	{
		// CLRC
		APUClearCarry();
		iPC++;
	}

	private void ApuE0()
	{
		// CLRV
		APUClearHalfCarry();
		APUClearOverflow();
		iPC++;
	}

	private void Apu24()
	{
		// AND A,dp
		YA_A( YA_A() & APUGetByteZ(OP1()) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu25()
	{
		// AND A,abs
		Absolute();
		YA_A( YA_A() & APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void Apu26()
	{
		// AND A,(X)
		YA_A( YA_A() & APUGetByteZ(X) );
		APUSetZN8(YA_A());
		iPC++;
	}

	private void Apu27()
	{
		// AND A,(dp+X)
		IndexedXIndirect();
		YA_A( YA_A() & APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu28()
	{
		// AND A,#00
		YA_A( YA_A() & OP1() );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu29()
	{
		// AND dp(dest),dp(src)
		Work8 = APUGetByteZ(OP1());
		Work8 &= APUGetByteZ(OP2());
		APUSetByteZ(Work8, OP2());
		APUSetZN8(Work8);
		iPC += 3;
	}

	private void Apu34()
	{
		// AND A,dp+X
		YA_A( YA_A() & APUGetByteZ(OP1() + X) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu35()
	{
		// AND A,abs+X
		AbsoluteX();
		YA_A( YA_A() & APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void Apu36()
	{
		// AND A,abs+Y
		AbsoluteY();
		YA_A( YA_A() & APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void Apu37()
	{
		// AND A,(dp)+Y
		IndirectIndexedY();
		YA_A( YA_A() & APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu38()
	{
		// AND dp,#00
		Work8 = OP1();
		Work8 &= APUGetByteZ(OP2());
		APUSetByteZ(Work8, OP2());
		APUSetZN8(Work8);
		iPC += 3;
	}

	private void Apu39()
	{
		// AND (X),(Y)
		Work8 = APUGetByteZ(X) & APUGetByteZ(YA_Y());
		APUSetZN8(Work8);
		APUSetByteZ(Work8, X);
		iPC++;
	}

	private void Apu2B()
	{
		// ROL dp
		Work8 = APUGetByteZ(OP1());
		Work8 = ROL( Work8 );
		APUSetByteZ(Work8, OP1());
		iPC += 2;
	}

	private void Apu2C()
	{
		// ROL abs
		Absolute();
		Work8 = APUGetByte( Address );
		Work8 = ROL( Work8 );
		APUSetByte (Work8, Address);
		iPC += 3;
	}

	private void Apu3B()
	{
		// ROL dp+X
		Work8 = APUGetByteZ(OP1() + X);
		Work8 = ROL( Work8 );
		APUSetByteZ(Work8, OP1() + X);
		iPC += 2;
	}

	private void Apu3C()
	{
		// ROL A
		YA_A( ROL( YA_A() ) );
		iPC++;
	}

	private void Apu2E()
	{
		// CBNE dp,rel
		Work8 = OP1();
		Relative2();

		if (APUGetByteZ(Work8) != YA_A())
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
			APUShutdown();
		}
		else
			iPC += 3;
	}

	private void ApuDE()
	{
		// CBNE dp+X,rel
		Work8 = OP1() + X;
		Relative2();

		if (APUGetByteZ(Work8) != YA_A())
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
			APUShutdown();
		}
		else
			iPC += 3;
	}

	private void Apu3D()
	{
		// INC X
		X = ( X + 1 ) & 0xff;
		APUSetZN8(X);
		WaitCounter++;

		iPC++;
	}

	private void ApuFC()
	{
		// INC Y
		YA_Y( YA_Y() + 1);
		APUSetZN8(YA_Y());
		WaitCounter++;

		iPC++;
	}

	private void Apu1D()
	{
		// DEC X
		X = ( X - 1 ) & 0xFF;
		APUSetZN8(X);
		WaitCounter++;
		iPC++;
	}

	private void ApuDC()
	{
		// DEC Y
		YA_Y( YA_Y() - 1);
		APUSetZN8(YA_Y());
		WaitCounter++;

		iPC++;
	}

	private void ApuAB()
	{
		// INC dp
		Work8 = (APUGetByteZ(OP1()) + 1) & 0xFF;
		APUSetByteZ(Work8, OP1());
		APUSetZN8(Work8);
		WaitCounter++;

		iPC += 2;
	}

	private void ApuAC()
	{
		// INC abs
		Absolute();
		Work8 = ( APUGetByte( Address ) + 1 ) & 0xFF;
		APUSetByte (Work8, Address);
		APUSetZN8(Work8);
		WaitCounter++;

		iPC += 3;
	}

	private void ApuBB()
	{
		// INC dp+X
		Work8 = ( APUGetByteZ(OP1() + X) + 1 ) & 0xFF;
		APUSetByteZ(Work8, OP1() + X);
		APUSetZN8(Work8);
		WaitCounter++;

		iPC += 2;
	}

	private void ApuBC()
	{
		// INC A
		YA_A( YA_A() + 1 );
		APUSetZN8(YA_A());
		WaitCounter++;

		iPC++;
	}

	private void Apu8B()
	{
		// DEC dp
		Work8 = (APUGetByteZ(OP1()) - 1) & 0xFF;
		APUSetByteZ(Work8, OP1());
		APUSetZN8(Work8);
		WaitCounter++;

		iPC += 2;
	}

	private void Apu8C()
	{
		// DEC abs
		Absolute();
		Work8 = (APUGetByte( Address ) - 1 ) & 0xFF;
		APUSetByte (Work8, Address);
		APUSetZN8(Work8);
		WaitCounter++;

		iPC += 3;
	}

	private void Apu9B()
	{
		// DEC dp+X
		Work8 = ( APUGetByteZ(OP1() + X) - 1 ) & 0xFF;
		APUSetByteZ(Work8, OP1() + X);
		APUSetZN8(Work8);
		WaitCounter++;

		iPC += 2;
	}

	private void Apu9C()
	{
		// DEC A
		YA_A( YA_A() - 1);
		APUSetZN8(YA_A());
		WaitCounter++;

		iPC++;
	}

	private void Apu44()
	{
		// EOR A,dp
		YA_A( YA_A() ^ APUGetByteZ(OP1()) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu45()
	{
		// EOR A,abs
		Absolute();
		YA_A( YA_A() ^ APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void Apu46()
	{
		// EOR A,(X)
		YA_A( YA_A() ^ APUGetByteZ(X) );
		APUSetZN8(YA_A());
		iPC++;
	}

	private void Apu47()
	{
		// EOR A,(dp+X)
		IndexedXIndirect();
		YA_A( YA_A() ^ APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu48()
	{
		// EOR A,#00
		YA_A( YA_A() ^ OP1() );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu49()
	{
		// EOR dp(dest),dp(src)
		Work8 = APUGetByteZ(OP1());
		Work8 ^= APUGetByteZ(OP2());
		APUSetByteZ(Work8, OP2());
		APUSetZN8(Work8);
		iPC += 3;
	}

	private void Apu54()
	{
		// EOR A,dp+X
		YA_A( YA_A() ^ APUGetByteZ(OP1() + X) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu55()
	{
		// EOR A,abs+X
		AbsoluteX();
		YA_A( YA_A() ^ APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void Apu56()
	{
		// EOR A,abs+Y
		AbsoluteY();
		YA_A( YA_A() ^ APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void Apu57()
	{
		// EOR A,(dp)+Y
		IndirectIndexedY();
		YA_A( YA_A() ^ APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void Apu58()
	{
		// EOR dp,#00
		Work8 = OP1();
		Work8 ^= APUGetByteZ(OP2());
		APUSetByteZ(Work8, OP2());
		APUSetZN8(Work8);
		iPC += 3;
	}

	private void Apu59()
	{
		// EOR (X),(Y)
		Work8 = APUGetByteZ(X) ^ APUGetByteZ(YA_Y());
		APUSetZN8(Work8);
		APUSetByteZ(Work8, X);
		iPC++;
	}

	private void Apu4B()
	{
		// LSR dp
		Work8 = APUGetByteZ(OP1());
		Work8 = LSR( Work8 );
		APUSetByteZ(Work8, OP1());
		iPC += 2;
	}

	private void Apu4C()
	{
		// LSR abs
		Absolute();
		Work8 = APUGetByte( Address );
		Work8 = LSR( Work8 );
		APUSetByte (Work8, Address);
		iPC += 3;
	}

	private void Apu5B()
	{
		// LSR dp+X
		Work8 = APUGetByteZ(OP1() + X);
		Work8 = LSR( Work8 );
		APUSetByteZ(Work8, OP1() + X);
		iPC += 2;
	}

	private void Apu5C()
	{
		// LSR A
		YA_A( LSR( YA_A() ) );
		iPC++;
	}

	private void Apu7D()
	{
		// MOV A,X
		YA_A( X );
		APUSetZN8(YA_A());
		iPC++;
	}

	private void ApuDD()
	{
		// MOV A,Y
		YA_A( YA_Y() );
		APUSetZN8(YA_A());
		iPC++;
	}

	private void Apu5D()
	{
		// MOV X,A
		X = YA_A();
		APUSetZN8(X);
		iPC++;
	}

	private void ApuFD()
	{
		// MOV Y,A
		YA_Y( YA_A() );
		APUSetZN8(YA_Y());
		iPC++;
	}

	private void Apu9D()
	{
		//MOV X,SP
		X = S;
		APUSetZN8(X);
		iPC++;
	}

	private void ApuBD()
	{
		// MOV SP,X
		S = X;
		iPC++;
	}

	private void Apu6B()
	{
		// ROR dp
		Work8 = APUGetByteZ(OP1());
		Work8 = ROR( Work8 );
		APUSetByteZ(Work8, OP1());
		iPC += 2;
	}

	private void Apu6C()
	{
		// ROR abs
		Absolute();
		Work8 = APUGetByte( Address );
		Work8 = ROR( Work8 );
		APUSetByte (Work8, Address);
		iPC += 3;
	}

	private void Apu7B()
	{
		// ROR dp+X
		Work8 = APUGetByteZ(OP1() + X);
		Work8 = ROR( Work8 );
		APUSetByteZ(Work8, OP1() + X);
		iPC += 2;
	}

	private void Apu7C()
	{
		// ROR A
		YA_A( ROR( YA_A() ) ); 
		iPC++;
	}

	private void Apu6E()
	{
		// DBNZ dp,rel
		Work8 = OP1();
		Relative2();
		W1 = APUGetByteZ(Work8) - 1;
		APUSetByteZ(W1, Work8);
		if (W1 != 0)
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
		}
		else
			iPC += 3;
	}

	private void ApuFE()
	{
		// DBNZ Y,rel
		Relative();
		YA_Y( YA_Y() - 1 );
		if (YA_Y() != 0)
		{
			iPC = Int16 & 0xFFFF;
			ApuCycles += TwoCycles;
		}
		else
			iPC += 2;
	}

	private void Apu6F()
	{
		// RET
		PC = PopW();
		iPC = PC;
	}

	private void Apu7F()
	{
		// RETI
		// STOP ("RETI");
		P = Pop();
		APUUnpackStatus();
		PC = PopW();
		iPC = PC;
	}

	private void Apu84()
	{
		// ADC A,dp
		Work8 = APUGetByteZ(OP1());
		YA_A( ADC( YA_A(), Work8) );
		iPC += 2;
	}

	private void Apu85()
	{
		// ADC A, abs
		Absolute();
		Work8 = APUGetByte( Address );
		YA_A( ADC( YA_A(), Work8) );
		iPC += 3;
	}

	private void Apu86()
	{
		// ADC A,(X)
		Work8 = APUGetByteZ(X);
		YA_A( ADC( YA_A(), Work8) );
		iPC++;
	}

	private void Apu87()
	{
		// ADC A,(dp+X)
		IndexedXIndirect();
		Work8 = APUGetByte( Address );
		YA_A( ADC( YA_A(), Work8) );
		iPC += 2;
	}

	private void Apu88()
	{
		// ADC A,#00
		Work8 = OP1();
		YA_A( ADC( YA_A(), Work8) );
		iPC += 2;
	}

	private void Apu89()
	{
		// ADC dp(dest),dp(src)
		Work8 = APUGetByteZ(OP1());
		W1 = APUGetByteZ(OP2());
		YA_A( ADC (W1, Work8) );
		APUSetByteZ(W1, OP2());
		iPC += 3;
	}

	private void Apu94()
	{
		// ADC A,dp+X
		Work8 = APUGetByteZ(OP1() + X);
		YA_A( ADC( YA_A(), Work8 ) );
		iPC += 2;
	}

	private void Apu95()
	{
		// ADC A, abs+X
		AbsoluteX();
		Work8 = APUGetByte( Address );
		YA_A( ADC( YA_A(), Work8 ) );
		iPC += 3;
	}

	private void Apu96()
	{
		// ADC A, abs+Y
		AbsoluteY();
		Work8 = APUGetByte( Address );
		YA_A( ADC( YA_A(), Work8 ) );
		iPC += 3;
	}

	private void Apu97()
	{
		// ADC A, (dp)+Y
		IndirectIndexedY();
		Work8 = APUGetByte( Address );
		YA_A( ADC( YA_A(), Work8 ) );
		iPC += 2;
	}

	private void Apu98()
	{
		// ADC dp,#00
		Work8 = OP1();
		W1 = APUGetByteZ( OP2() );
		YA_A( ADC( W1, Work8 ) );
		APUSetByteZ( W1, OP2() );
		iPC += 3;
	}

	private void Apu99()
	{
		// ADC (X),(Y)
		W1 = APUGetByteZ( X );
		Work8 = APUGetByteZ( YA_Y() );
		YA_A( ADC( W1, Work8 ) );
		APUSetByteZ(W1, X);
		iPC++;
	}

	private void Apu8D()
	{
		// MOV Y,#00
		YA_Y( OP1() );
		APUSetZN8( YA_Y() );
		iPC += 2;
	}

	private void Apu8F()
	{
		// MOV dp,#00
		Work8 = OP1();
		APUSetByteZ( Work8, OP2() );
		iPC += 3;
	}

	private void Apu9E()
	{
		// DIV YA,X
		if( ( X & 0xf ) <= ( YA_Y() & 0x0f ) )
		{
			APUSetHalfCarry();
		} else {
			APUClearHalfCarry();
		}
		
		int yva, x, i;
		yva = YA_W();
		
		x = X << 9;
		for( i = 0; i < 9; i++)
		{
			yva <<= 1;
			if( ( yva & 0x20000) != 0 )
				yva = (yva & 0x1ffff) | 1;
			if(yva >= x)
				yva ^= 1;
			if( ( yva & 1 ) == 1 )
				yva = (yva - x ) & 0x1ffff;
		}
		
		if( (yva & 0x100) != 0 )
		{
			APUSetOverflow();
		}
		else
		{
			APUClearOverflow();
		}
		YA_Y( (yva >>> 9 ) & 0xff );
		YA_A( yva & 0xff );
		APUSetZN8(YA_A());
		iPC++;
	}

	private void Apu9F()
	{
		// XCN A
		YA_A( (YA_A() >>> 4) | (YA_A() << 4) );
		APUSetZN8(YA_A());
		iPC++;
	}

	private void ApuA4()
	{
		// SBC A, dp
		Work8 = APUGetByteZ(OP1());
		YA_A( SBC (YA_A(), Work8) );
		iPC += 2;
	}

	private void ApuA5()
	{
		// SBC A, abs
		Absolute();
		Work8 = APUGetByte( Address );
		YA_A( SBC(YA_A(), Work8) );
		iPC += 3;
	}

	private void ApuA6()
	{
		// SBC A, (X)
		Work8 = APUGetByteZ(X);
		YA_A( SBC( YA_A(), Work8) );
		iPC++;
	}

	private void ApuA7()
	{
		// SBC A,(dp+X)
		IndexedXIndirect();
		Work8 = APUGetByte( Address );
		YA_A( SBC( YA_A(), Work8) );
		iPC += 2;
	}

	private void ApuA8()
	{
		// SBC A,#00
		Work8 = OP1();
		YA_A( SBC( YA_A(), Work8) ); 
		iPC += 2;
	}

	private void ApuA9()
	{
		// SBC dp(dest), dp(src)
		Work8 = APUGetByteZ(OP1());
		W1 = APUGetByteZ(OP2());
		W1 = SBC( W1, Work8 );
		APUSetByteZ(W1, OP2());
		iPC += 3;
	}

	private void ApuB4()
	{
		// SBC A, dp+X
		Work8 = APUGetByteZ(OP1() + X);
		YA_A( SBC( YA_A(), Work8 ) );
		iPC += 2;
	}

	private void ApuB5()
	{
		// SBC A,abs+X
		AbsoluteX();
		Work8 = APUGetByte( Address );
		YA_A( SBC( YA_A(), Work8) );
		iPC += 3;
	}

	private void ApuB6()
	{
		// SBC A,abs+Y
		AbsoluteY();
		Work8 = APUGetByte( Address );
		YA_A( SBC( YA_A(), Work8) );
		iPC += 3;
	}

	private void ApuB7()
	{
		// SBC A,(dp)+Y
		IndirectIndexedY();
		Work8 = APUGetByte( Address );
		YA_A( SBC( YA_A(), Work8) );
		iPC += 2;
	}

	private void ApuB8()
	{
		// SBC dp,#00
		Work8 = OP1();
		W1 = APUGetByteZ(OP2());
		W1 = SBC( W1, Work8);
		APUSetByteZ(W1, OP2());
		iPC += 3;
	}

	private void ApuB9()
	{
		// SBC (X),(Y)
		W1 = APUGetByteZ(X);
		Work8 = APUGetByteZ(YA_Y());
		W1 = SBC( W1, Work8);
		APUSetByteZ(W1, X);
		iPC++;
	}

	private void ApuAF()
	{
		// MOV (X)+, A
		APUSetByteZ(YA_A(), X );
		X = (X + 1) & 0xFF;
		iPC++;
	}

	private void ApuBE()
	{
		// DAS
		if (YA_A() > 0x99 || _Carry == 0 )
		{
			YA_A( YA_A() - 0x60);
			APUClearCarry();
		}
		else
		{ 
			APUSetCarry();
		}
		
		if ((YA_A() & 0x0f) > 9 || !APUCheckHalfCarry())
		{
			YA_A( YA_A() - 6);
		}
		APUSetZN8(YA_A());
		iPC++;
	}

	private void ApuBF()
	{
		// MOV A,(X)+
		YA_A( APUGetByteZ( X ) );
		X = ( X + 1 ) & 0xFF;
		APUSetZN8(YA_A());
		iPC++;
	}

	private void ApuC0()
	{
		// DI
		APUClearInterrupt();
		iPC++;
	}

	private void ApuA0()
	{
		// EI
		APUSetInterrupt();
		iPC++;
	}

	private void ApuC4()
	{
		// MOV dp,A
		APUSetByteZ(YA_A(), OP1());
		iPC += 2;
	}

	private void ApuC5()
	{
		// MOV abs,A
		Absolute();
		APUSetByte (YA_A(), Address);
		iPC += 3;
	}

	private void ApuC6()
	{
		// MOV (X), A
		APUSetByteZ(YA_A(), X);
		iPC++;
	}

	private void ApuC7()
	{
		// MOV (dp+X),A
		IndexedXIndirect();
		APUSetByte (YA_A(), Address);
		iPC += 2;
	}

	private void ApuC9()
	{
		// MOV abs,X
		Absolute();
		APUSetByte (X, Address);
		iPC += 3;
	}

	private void ApuCB()
	{
		// MOV dp,Y
		APUSetByteZ(YA_Y(), OP1());
		iPC += 2;
	}

	private void ApuCC()
	{
		// MOV abs,Y
		Absolute();
		APUSetByte (YA_Y(), Address);
		iPC += 3;
	}

	private void ApuCD()
	{
		// MOV X,#00
		X = OP1();
		APUSetZN8(X);
		iPC += 2;
	}

	private void ApuCF()
	{
		// MUL YA
		YA_W( ( YA_A() * YA_Y() ) & 0xFFFF);
		APUSetZN8(YA_Y());
		iPC++;
	}

	private void ApuD4()
	{
		// MOV dp+X, A
		APUSetByteZ(YA_A(), OP1() + X);
		iPC += 2;
	}

	private void ApuD5()
	{
		// MOV abs+X,A
		AbsoluteX();
		APUSetByte (YA_A(), Address);
		iPC += 3;
	}

	private void ApuD6()
	{
		// MOV abs+Y,A
		AbsoluteY();
		APUSetByte (YA_A(), Address);
		iPC += 3;
	}

	private void ApuD7()
	{
		// MOV (dp)+Y,A
		IndirectIndexedY();
		APUSetByte (YA_A(), Address);
		iPC += 2;
	}

	private void ApuD8()
	{
		// MOV dp,X
		APUSetByteZ(X, OP1());
		iPC += 2;
	}

	private void ApuD9()
	{
		// MOV dp+Y,X
		APUSetByteZ(X, OP1() + YA_Y());
		iPC += 2;
	}

	private void ApuDB()
	{
		// MOV dp+X,Y
		APUSetByteZ(YA_Y(), OP1() + X);
		iPC += 2;
	}

	private void ApuDF()
	{
		// DAA
		if (YA_A() > 0x99 || _Carry != 0)
		{
			YA_A( YA_A() + 0x60 );
			APUSetCarry();
		}
		else
		{
			APUClearCarry();
		}
		
		if ((YA_A() & 0x0f) > 9 || APUCheckHalfCarry())
		{
			YA_A( YA_A() + 6 );
			//APUSetHalfCarry(); Intel procs do this, but this is a Sony proc...
		}
			//else { APUClearHalfCarry(); } ditto as above
		APUSetZN8(YA_A());
		iPC++;
	}

	private void ApuE4()
	{
		// MOV A, dp
		YA_A( APUGetByteZ(OP1()) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void ApuE5()
	{
		// MOV A,abs
		Absolute();
		YA_A( APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void ApuE6()
	{
		// MOV A,(X)
		YA_A( APUGetByteZ(X) );
		APUSetZN8(YA_A());
		iPC++;
	}

	private void ApuE7()
	{
		// MOV A,(dp+X)
		IndexedXIndirect();
		YA_A( APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void ApuE8()
	{
		// MOV A,#00
		YA_A( OP1() );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void ApuE9()
	{
		// MOV X, abs
		Absolute();
		X = APUGetByte( Address );
		APUSetZN8(X);
		iPC += 3;
	}

	private void ApuEB()
	{
		// MOV Y,dp
		YA_Y( APUGetByteZ(OP1()) );
		APUSetZN8(YA_Y());
		iPC += 2;
	}

	private void ApuEC()
	{
		// MOV Y,abs
		Absolute();
		YA_Y( APUGetByte( Address ) );
		APUSetZN8(YA_Y());
		iPC += 3;
	}

	private void ApuF4()
	{
		// MOV A, dp+X
		YA_A( APUGetByteZ(OP1() + X) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void ApuF5()
	{
		// MOV A, abs+X
		AbsoluteX();
		YA_A( APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void ApuF6()
	{
		// MOV A, abs+Y
		AbsoluteY();
		YA_A( APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 3;
	}

	private void ApuF7()
	{
		// MOV A, (dp)+Y
		IndirectIndexedY();
		YA_A( APUGetByte( Address ) );
		APUSetZN8(YA_A());
		iPC += 2;
	}

	private void ApuF8()
	{
		// MOV X,dp
		X = APUGetByteZ(OP1());
		APUSetZN8(X);
		iPC += 2;
	}

	private void ApuF9()
	{
		// MOV X,dp+Y
		X = APUGetByteZ(OP1() + YA_Y());
		APUSetZN8(X);
		iPC += 2;
	}

	private void ApuFA()
	{
		// MOV dp(dest),dp(src)
		APUSetByteZ(APUGetByteZ(OP1()), OP2());
		iPC += 3;
	}

	private void ApuFB()
	{
		// MOV Y,dp+X
		YA_Y( APUGetByteZ(OP1() + X) );
		APUSetZN8(YA_Y());
		iPC += 2;
	}

	void APU_EXECUTE1()
	{
		int opCode = RAM.get8Bit( iPC );

		if ( SnesSystem.DEBUG_APU )
		{
			globals.apu.TraceAPU();
		}
		
		ApuCycles += APUCycles[opCode];
		
		switch( opCode )
		{
		case 0x00: Apu00(); break;
		case 0x01: Apu01(); break;
		case 0x02: Apu02(); break;
		case 0x03: Apu03(); break;
		case 0x04: Apu04(); break;
		case 0x05: Apu05(); break;
		case 0x06: Apu06(); break;
		case 0x07: Apu07(); break;
		case 0x08: Apu08(); break;
		case 0x09: Apu09(); break;
		case 0x0a: Apu0A(); break;
		case 0x0b: Apu0B(); break;
		case 0x0c: Apu0C(); break;
		case 0x0d: Apu0D(); break;
		case 0x0e: Apu0E(); break;
		case 0x0f: Apu0F(); break;
		case 0x10: Apu10(); break;
		case 0x11: Apu11(); break;
		case 0x12: Apu12(); break;
		case 0x13: Apu13(); break;
		case 0x14: Apu14(); break;
		case 0x15: Apu15(); break;
		case 0x16: Apu16(); break;
		case 0x17: Apu17(); break;
		case 0x18: Apu18(); break;
		case 0x19: Apu19(); break;
		case 0x1a: Apu1A(); break;
		case 0x1b: Apu1B(); break;
		case 0x1c: Apu1C(); break;
		case 0x1d: Apu1D(); break;
		case 0x1e: Apu1E(); break;
		case 0x1f: Apu1F(); break;
		case 0x20: Apu20(); break;
		case 0x21: Apu21(); break;
		case 0x22: Apu22(); break;
		case 0x23: Apu23(); break;
		case 0x24: Apu24(); break;
		case 0x25: Apu25(); break;
		case 0x26: Apu26(); break;
		case 0x27: Apu27(); break;
		case 0x28: Apu28(); break;
		case 0x29: Apu29(); break;
		case 0x2a: Apu2A(); break;
		case 0x2b: Apu2B(); break;
		case 0x2c: Apu2C(); break;
		case 0x2d: Apu2D(); break;
		case 0x2e: Apu2E(); break;
		case 0x2f: Apu2F(); break;
		case 0x30: Apu30(); break;
		case 0x31: Apu31(); break;
		case 0x32: Apu32(); break;
		case 0x33: Apu33(); break;
		case 0x34: Apu34(); break;
		case 0x35: Apu35(); break;
		case 0x36: Apu36(); break;
		case 0x37: Apu37(); break;
		case 0x38: Apu38(); break;
		case 0x39: Apu39(); break;
		case 0x3a: Apu3A(); break;
		case 0x3b: Apu3B(); break;
		case 0x3c: Apu3C(); break;
		case 0x3d: Apu3D(); break;
		case 0x3e: Apu3E(); break;
		case 0x3f: Apu3F(); break;
		case 0x40: Apu40(); break;
		case 0x41: Apu41(); break;
		case 0x42: Apu42(); break;
		case 0x43: Apu43(); break;
		case 0x44: Apu44(); break;
		case 0x45: Apu45(); break;
		case 0x46: Apu46(); break;
		case 0x47: Apu47(); break;
		case 0x48: Apu48(); break;
		case 0x49: Apu49(); break;
		case 0x4a: Apu4A(); break;
		case 0x4b: Apu4B(); break;
		case 0x4c: Apu4C(); break;
		case 0x4d: Apu4D(); break;
		case 0x4e: Apu4E(); break;
		case 0x4f: Apu4F(); break;
		case 0x50: Apu50(); break;
		case 0x51: Apu51(); break;
		case 0x52: Apu52(); break;
		case 0x53: Apu53(); break;
		case 0x54: Apu54(); break;
		case 0x55: Apu55(); break;
		case 0x56: Apu56(); break;
		case 0x57: Apu57(); break;
		case 0x58: Apu58(); break;
		case 0x59: Apu59(); break;
		case 0x5a: Apu5A(); break;
		case 0x5b: Apu5B(); break;
		case 0x5c: Apu5C(); break;
		case 0x5d: Apu5D(); break;
		case 0x5e: Apu5E(); break;
		case 0x5f: Apu5F(); break;
		case 0x60: Apu60(); break;
		case 0x61: Apu61(); break;
		case 0x62: Apu62(); break;
		case 0x63: Apu63(); break;
		case 0x64: Apu64(); break;
		case 0x65: Apu65(); break;
		case 0x66: Apu66(); break;
		case 0x67: Apu67(); break;
		case 0x68: Apu68(); break;
		case 0x69: Apu69(); break;
		case 0x6a: Apu6A(); break;
		case 0x6b: Apu6B(); break;
		case 0x6c: Apu6C(); break;
		case 0x6d: Apu6D(); break;
		case 0x6e: Apu6E(); break;
		case 0x6f: Apu6F(); break;
		case 0x70: Apu70(); break;
		case 0x71: Apu71(); break;
		case 0x72: Apu72(); break;
		case 0x73: Apu73(); break;
		case 0x74: Apu74(); break;
		case 0x75: Apu75(); break;
		case 0x76: Apu76(); break;
		case 0x77: Apu77(); break;
		case 0x78: Apu78(); break;
		case 0x79: Apu79(); break;
		case 0x7a: Apu7A(); break;
		case 0x7b: Apu7B(); break;
		case 0x7c: Apu7C(); break;
		case 0x7d: Apu7D(); break;
		case 0x7e: Apu7E(); break;
		case 0x7f: Apu7F(); break;
		case 0x80: Apu80(); break;
		case 0x81: Apu81(); break;
		case 0x82: Apu82(); break;
		case 0x83: Apu83(); break;
		case 0x84: Apu84(); break;
		case 0x85: Apu85(); break;
		case 0x86: Apu86(); break;
		case 0x87: Apu87(); break;
		case 0x88: Apu88(); break;
		case 0x89: Apu89(); break;
		case 0x8a: Apu8A(); break;
		case 0x8b: Apu8B(); break;
		case 0x8c: Apu8C(); break;
		case 0x8d: Apu8D(); break;
		case 0x8e: Apu8E(); break;
		case 0x8f: Apu8F(); break;
		case 0x90: Apu90(); break;
		case 0x91: Apu91(); break;
		case 0x92: Apu92(); break;
		case 0x93: Apu93(); break;
		case 0x94: Apu94(); break;
		case 0x95: Apu95(); break;
		case 0x96: Apu96(); break;
		case 0x97: Apu97(); break;
		case 0x98: Apu98(); break;
		case 0x99: Apu99(); break;
		case 0x9a: Apu9A(); break;
		case 0x9b: Apu9B(); break;
		case 0x9c: Apu9C(); break;
		case 0x9d: Apu9D(); break;
		case 0x9e: Apu9E(); break;
		case 0x9f: Apu9F(); break;
		case 0xa0: ApuA0(); break;
		case 0xa1: ApuA1(); break;
		case 0xa2: ApuA2(); break;
		case 0xa3: ApuA3(); break;
		case 0xa4: ApuA4(); break;
		case 0xa5: ApuA5(); break;
		case 0xa6: ApuA6(); break;
		case 0xa7: ApuA7(); break;
		case 0xa8: ApuA8(); break;
		case 0xa9: ApuA9(); break;
		case 0xaa: ApuAA(); break;
		case 0xab: ApuAB(); break;
		case 0xac: ApuAC(); break;
		case 0xad: ApuAD(); break;
		case 0xae: ApuAE(); break;
		case 0xaf: ApuAF(); break;
		case 0xb0: ApuB0(); break;
		case 0xb1: ApuB1(); break;
		case 0xb2: ApuB2(); break;
		case 0xb3: ApuB3(); break;
		case 0xb4: ApuB4(); break;
		case 0xb5: ApuB5(); break;
		case 0xb6: ApuB6(); break;
		case 0xb7: ApuB7(); break;
		case 0xb8: ApuB8(); break;
		case 0xb9: ApuB9(); break;
		case 0xba: ApuBA(); break;
		case 0xbb: ApuBB(); break;
		case 0xbc: ApuBC(); break;
		case 0xbd: ApuBD(); break;
		case 0xbe: ApuBE(); break;
		case 0xbf: ApuBF(); break;
		case 0xc0: ApuC0(); break;
		case 0xc1: ApuC1(); break;
		case 0xc2: ApuC2(); break;
		case 0xc3: ApuC3(); break;
		case 0xc4: ApuC4(); break;
		case 0xc5: ApuC5(); break;
		case 0xc6: ApuC6(); break;
		case 0xc7: ApuC7(); break;
		case 0xc8: ApuC8(); break;
		case 0xc9: ApuC9(); break;
		case 0xca: ApuCA(); break;
		case 0xcb: ApuCB(); break;
		case 0xcc: ApuCC(); break;
		case 0xcd: ApuCD(); break;
		case 0xce: ApuCE(); break;
		case 0xcf: ApuCF(); break;
		case 0xd0: ApuD0(); break;
		case 0xd1: ApuD1(); break;
		case 0xd2: ApuD2(); break;
		case 0xd3: ApuD3(); break;
		case 0xd4: ApuD4(); break;
		case 0xd5: ApuD5(); break;
		case 0xd6: ApuD6(); break;
		case 0xd7: ApuD7(); break;
		case 0xd8: ApuD8(); break;
		case 0xd9: ApuD9(); break;
		case 0xda: ApuDA(); break;
		case 0xdb: ApuDB(); break;
		case 0xdc: ApuDC(); break;
		case 0xdd: ApuDD(); break;
		case 0xde: ApuDE(); break;
		case 0xdf: ApuDF(); break;
		case 0xe0: ApuE0(); break;
		case 0xe1: ApuE1(); break;
		case 0xe2: ApuE2(); break;
		case 0xe3: ApuE3(); break;
		case 0xe4: ApuE4(); break;
		case 0xe5: ApuE5(); break;
		case 0xe6: ApuE6(); break;
		case 0xe7: ApuE7(); break;
		case 0xe8: ApuE8(); break;
		case 0xe9: ApuE9(); break;
		case 0xea: ApuEA(); break;
		case 0xeb: ApuEB(); break;
		case 0xec: ApuEC(); break;
		case 0xed: ApuED(); break;
		case 0xee: ApuEE(); break;
		case 0xef: ApuEF(); break;
		case 0xf0: ApuF0(); break;
		case 0xf1: ApuF1(); break;
		case 0xf2: ApuF2(); break;
		case 0xf3: ApuF3(); break;
		case 0xf4: ApuF4(); break;
		case 0xf5: ApuF5(); break;
		case 0xf6: ApuF6(); break;
		case 0xf7: ApuF7(); break;
		case 0xf8: ApuF8(); break;
		case 0xf9: ApuF9(); break;
		case 0xfa: ApuFA(); break;
		case 0xfb: ApuFB(); break;
		case 0xfc: ApuFC(); break;
		case 0xfd: ApuFD(); break;
		case 0xfe: ApuFE(); break;
		case 0xff: ApuFF(); break;
		}
	}
	
	// Debugger
	private static String Mnemonics[] = {
	    "NOP", "TCALL 0", "SET1 $%02X.0", "BBS $%02X.0,$%04X",
	    "OR A,$%02X", "OR A,!$%04X", "OR A,(X)", "OR A,[$%02X+X]",
	    "OR A,#$%02X", "OR $%02X,$%02X", "OR1 C,$%04X.%d", "ASL $%02X",
	    "MOV !$%04X,Y", "PUSH PSW", "TSET1 !$%04X", "BRK",
	    "BPL $%04X", "TCALL 1", "CLR1 $%02X.0", "BBC $%02X.0,$%04X",
	    "OR A,$%02X+X", "OR A,!$%04X+X", "OR A,!$%04X+Y", "OR A,[$%02X]+Y",
	    "OR $%02X,#$%02X", "OR (X),(Y)", "DECW $%02X", "ASL $%02X+X",
	    "ASL A", "DEC X", "CMP X,!$%04X", "JMP [!$%04X+X]",
	    "CLRP", "TCALL 2", "SET1 $%02X.1", "BBS $%02X.1,$%04X",
	    "AND A,$%02X", "AND A,!$%04X", "AND A,(X)", "AND A,[$%02X+X]",
	    "AND A,#$%02X", "AND $%02X,$%02X", "OR1 C,/$%04X.%d", "ROL $%02X",
	    "ROL !$%04X", "PUSH A", "CBNE $%02X,$%04X", "BRA $%04X",
	    "BMI $%04X", "TCALL 3", "CLR1 $%02X.1", "BBC $%02X.1,$%04X",
	    "AND A,$%02X+X", "AND A,!$%04X+X", "AND A,!$%04X+Y", "AND A,[$%02X]+Y",
	    "AND $%02X,#$%02X", "AND (X),(Y)", "INCW $%02X", "ROL $%02X+X",
	    "ROL A", "INC X", "CMP X,$%02X", "CALL !$%04X",
	    "SETP", "TCALL 4", "SET1 $%02X.2", "BBS $%02X.2,$%04X",
	    "EOR A,$%02X", "EOR A,!$%04X", "EOR A,(X)", "EOR A,[$%02X+X]",
	    "EOR A,#$%02X", "EOR $%02X,$%02X", "AND1 C,$%04X.%d", "LSR $%02X",
	    "LSR !$%04X", "PUSH X", "TCLR1 !$%04X", "PCALL $%02X",
	    "BVC $%04X", "TCALL 5", "CLR1 $%02X.2", "BBC $%02X.2,$%04X",
	    "EOR A,$%02X+X", "EOR A,!$%04X+X", "EOR A,!$%04X+Y", "EOR A,[$%02X]+Y",
	    "EOR $%02X,#$%02X", "EOR (X),(Y)", "CMPW YA,$%02X", "LSR $%02X+X",
	    "LSR A", "MOV X,A", "CMP Y,!$%04X", "JMP !$%04X",
	    "CLRC", "TCALL 6", "SET1 $%02X.3", "BBS $%02X.3,$%04X",
	    "CMP A,$%02X", "CMP A,!$%04X", "CMP A,(X)", "CMP A,[$%02X+X]",
	    "CMP A,#$%02X", "CMP $%02X,$%02X", "AND1 C,/$%04X.%d", "ROR $%02X",
	    "ROR !$%04X", "PUSH Y", "DBNZ $%02X,$%04X", "RET",
	    "BVS $%04X", "TCALL 7", "CLR1 $%02X.3", "BBC $%02X.3,$%04X",
	    "CMP A,$%02X+X", "CMP A,!$%04X+X", "CMP A,!$%04X+Y", "CMP A,[$%02X]+Y",
	    "CMP $%02X,#$%02X", "CMP (X),(Y)", "ADDW YA,$%02X", "ROR $%02X+X",
	    "ROR A", "MOV A,X", "CMP Y,$%02X", "RET1",
	    "SETC", "TCALL 8", "SET1 $%02X.4", "BBS $%02X.4,$%04X",
	    "ADC A,$%02X", "ADC A,!$%04X", "ADC A,(X)", "ADC A,[$%02X+X]",
	    "ADC A,#$%02X", "ADC $%02X,$%02X", "EOR1 C,$%04X.%d", "DEC $%02X",
	    "DEC !$%04X", "MOV Y,#$%02X", "POP PSW", "MOV $%02X,#$%02X",
	    "BCC $%04X", "TCALL 9", "CLR1 $%02X.4", "BBC $%02X.4,$%04X",
	    "ADC A,$%02X+X", "ADC A,!$%04X+X", "ADC A,!$%04X+Y", "ADC A,[$%02X]+Y",
	    "ADC $%02X,#$%02X", "ADC (X),(Y)", "SUBW YA,$%02X", "DEC $%02X+X",
	    "DEC A", "MOV X,SP", "DIV YA,X", "XCN A",
	    "EI", "TCALL 10", "SET1 $%02X.5", "BBS $%02X.5,$%04X",
	    "SBC A,$%02X", "SBC A,!$%04X", "SBC A,(X)", "SBC A,[$%02X+X]",
	    "SBC A,#$%02X", "SBC $%02X,$%02X", "MOV1 C,$%04X.%d", "INC $%02X",
	    "INC !$%04X", "CMP Y,#$%02X", "POP A", "MOV (X)+,A",
	    "BCS $%04X", "TCALL 11", "CLR1 $%02X.5", "BBC $%02X.5,$%04X",
	    "SBC A,$%02X+X", "SBC A,!$%04X+X", "SBC A,!$%04X+Y", "SBC A,[$%02X]+Y",
	    "SBC $%02X,#$%02X", "SBC (X),(Y)", "MOVW YA,$%02X", "INC $%02X+X",
	    "INC A", "MOV SP,X", "DAS A", "MOV A,(X)+",
	    "DI", "TCALL 12", "SET1 $%02X.6", "BBS $%02X.6,$%04X",
	    "MOV $%02X,A", "MOV !$%04X,A", "MOV (X),A", "MOV [$%02X+X],A",
	    "CMP X,#$%02X", "MOV !$%04X,X", "MOV1 $%04X.%d,C", "MOV $%02X,Y",
	    "ASL !$%04X", "MOV X,#$%02X", "POP X", "MUL YA",
	    "BNE $%04X", "TCALL 13", "CLR1 $%02X.6", "BBC $%02X.6,$%04X",
	    "MOV $%02X+X,A", "MOV !$%04X+X,A", "MOV !$%04X+Y,A", "MOV [$%02X]+Y,A",
	    "MOV $%02X,X", "MOV $%02X+Y,X", "MOVW $%02X,YA", "MOV $%02X+X,Y",
	    "DEC Y", "MOV A,Y", "CBNE $%02X+X,$%04X", "DAA A",
	    "CLRV", "TCALL 14", "SET1 $%02X.7", "BBS $%02X.7,$%04X",
	    "MOV A,$%02X", "MOV A,!$%04X", "MOV A,(X)", "MOV A,[$%02X+X]",
	    "MOV A,#$%02X", "MOV X,!$%04X", "NOT1 $%04X.%d", "MOV Y,$%02X",
	    "MOV Y,!$%04X", "NOTC", "POP Y", "SLEEP",
	    "BEQ $%04X", "TCALL 15", "CLR1 $%02X.7", "BBC $%02X.7,$%04X",
	    "MOV A,$%02X+X", "MOV A,!$%04X+X", "MOV A,!$%04X+Y", "MOV A,[$%02X]+Y",
	    "MOV X,$%02X", "MOV X,$%02X+Y", "MOV $%02X,$%02X", "MOV Y,$%02X+X",
	    "INC Y", "MOV Y,A", "DBNZ Y,$%04X", "STOP"
	};
	
	private final static byte DP = 0;
	private final static byte ABS = 1;
	private final static byte IM = 2;
	private final static byte DP2DP = 3;
	private final static byte DPIM = 4;
	private final static byte DPREL = 5;
	private final static byte ABSBIT = 6;
	private final static byte REL = 7;

	private final static byte Modes[] = {
	    IM, IM, DP, DPREL,
	    DP, ABS, IM, DP,
	    DP, DP2DP, ABSBIT, DP,
	    ABS, IM, ABS, IM,
	    REL, IM, DP, DPREL,
	    DP, ABS, ABS, DP,
	    DPIM, IM, DP, DP,
	    IM, IM, ABS, ABS,
	    IM, IM, DP, DPREL,
	    DP, ABS, IM, DP,
	    DP, DP2DP, ABSBIT, DP,
	    ABS, IM, DPREL, REL,
	    REL, IM, DP, DPREL,
	    DP, ABS, ABS, DP,
	    DPIM, IM, DP, DP,
	    IM, IM, DP, ABS,
	    IM, IM, DP, DPREL,
	    DP, ABS, IM, DP,
	    DP, DP2DP, ABSBIT, DP,
	    ABS, IM, ABS, DP,
	    REL, IM, DP, DPREL,
	    DP, ABS, ABS, DP,
	    DPIM, IM, DP, DP,
	    IM, IM, ABS, ABS,
	    IM, IM, DP, DPREL,
	    DP, ABS, IM, DP,
	    DP, DP2DP, ABSBIT, DP,
	    ABS, IM, DPREL, IM,
	    REL, IM, DP, DPREL,
	    DP, ABS, ABS, DP,
	    DPIM, IM, DP, DP,
	    IM, IM, DP, IM,
	    IM, IM, DP, DPREL,
	    DP, ABS, IM, DP,
	    DP, DP2DP, ABSBIT, DP,
	    ABS, DP, IM, DPIM,
	    REL, IM, DP, DPREL,
	    DP, ABS, ABS, DP,
	    DPIM, IM, DP, DP,
	    IM, IM, IM, IM,
	    IM, IM, DP, DPREL,
	    DP, ABS, IM, DP,
	    DP, DP2DP, ABSBIT, DP,
	    ABS, DP, IM, IM,
	    REL, IM, DP, DPREL,
	    DP, ABS, ABS, DP,
	    DPIM, IM, DP, DP,
	    IM, IM, IM, IM,
	    IM, IM, DP, DPREL,
	    DP, ABS, IM, DP,
	    DP, ABS, ABSBIT, DP,
	    ABS, DP, IM, IM,
	    REL, IM, DP, DPREL,
	    DP, ABS, ABS, DP,
	    DP, DP, DP, DP,
	    IM, IM, DPREL, IM,
	    IM, IM, DP, DPREL,
	    DP, ABS, IM, DP,
	    DP, ABS, ABSBIT, DP,
	    ABS, IM, IM, IM,
	    REL, IM, DP, DPREL,
	    DP, ABS, ABS, DP,
	    DP, DP, DP2DP, DP,
	    IM, IM, REL, IM
	};
	
	private final static byte ModesToBytes [] = {
	    2, 3, 1, 3, 3, 3, 3, 2
	}; 
	
	void TraceAPU()
	{
		
		String buffer = new String();
	    String mnem = new String();
	    
	    int Address = iPC; 
	    
	    ByteArrayOffset p = RAM.getOffsetBuffer( Address );
	    int mode = Modes[ p.get8Bit(0) ];
	    int bytes = ModesToBytes [mode];

	    switch (bytes)
	    {
	    case 1:
	    	buffer = String.format( "%04X %02X       ", Address, p.get8Bit(0) );
			break;
	    case 2:
	    	buffer = String.format( "%04X %02X %02X    ", Address, p.get8Bit(0), p.get8Bit(1));
			break;
	    case 3:
	    	buffer = String.format( "%04X %02X %02X %02X ", Address, p.get8Bit(0), p.get8Bit(1), p.get8Bit(2) );
			break;
	    }

	    switch (mode)
	    {
	    case DP:
			mnem = String.format(  Mnemonics[p.get8Bit(0)], p.get8Bit(1));
			break;
	    case ABS:
			mnem = String.format(  Mnemonics[p.get8Bit(0)], p.get8Bit(1) + (p.get8Bit(2) << 8));
			break;
	    case IM:
			mnem = String.format(  Mnemonics[p.get8Bit(0)]);
			break;
	    case DP2DP:
			mnem = String.format(  Mnemonics[p.get8Bit(0)], p.get8Bit(2), p.get8Bit(1));;
			break;
	    case DPIM:
			mnem = String.format(  Mnemonics[p.get8Bit(0)], p.get8Bit(2), p.get8Bit(1));;
			break;
	    case DPREL:
	    	mnem = String.format(  Mnemonics[p.get8Bit(0)], p.get8Bit(1),
			p.getOffset() + 3 + ( (byte) p.get8Bit(2) ) );
	    	break;
	    case ABSBIT:
	    	mnem = String.format(  Mnemonics[p.get8Bit(0)], (p.get8Bit(1) + (p.get8Bit(2) << 8)) & 0x1fff,
			p.get8Bit(2) >>> 5);
	    	break;
	    case REL:
	    	mnem = String.format(  Mnemonics [p.get8Bit(0)],
			(int) (p.getOffset() + 2 ) + (p.getByte(1)) & 0xFFFF );
	    	break;
	    }
	    
	    buffer = String.format( "APU %s %-20s A:%02X X:%02X Y:%02X S:%02X P:%c%c%c%c%c%c%c%c %03d %04d %08d - %08d",
	    	buffer, mnem,
			YA_A(), X, YA_Y(),
			S,
			APUCheckNegative() ? 'N' : 'n',
			APUCheckOverflow() > 0 ? 'V' : 'v',
			APUCheckDirectPage () ? 'P' : 'p',
			APUCheckBreak() ? 'B' : 'b',
			APUCheckHalfCarry() ? 'H' : 'h',
			APUCheckInterrupt() ? 'I' : 'i',
			APUCheckZero() ? 'Z' : 'z',
			APUCheckCarry() > 0 ? 'C' : 'c',
			globals.cpu.V_Counter,
			globals.cpu.Cycles,
			globals.apu.ApuCycles,
			globals.intInstCount);

	    System.out.println( buffer );
	}
}
