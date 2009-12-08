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

class CPUExecutor
{
	private Globals globals;
	private CPU cpu;
	private Timings timings;
	private APU apu;
	private SA1 sa1;
	private PPU ppu;
	private Memory memory;
	private DMA dma;

	void setUp()
	{
		globals = Globals.globals;
		cpu = globals.cpu;
		timings = globals.timings;
		apu = globals.apu;
		sa1 = globals.sa1;
		ppu = globals.ppu;
		memory = globals.memory;
		dma = globals.dma;
	}

	void MainLoop ()
	{
		boolean gotoOp;
		
		if( cpu.SavedAtOp )
		{
			cpu.SavedAtOp = false;
			cpu.PCw = cpu.PBPCAtOpcodeStart;
			
			if( cpu.PCBase != null )
			{
				cpu.Cycles -= cpu.MemSpeed;
			}
			
			gotoOp = true;
		}
		else
		{
			gotoOp = false;
		}

		while (true)
		{
			if ( ! gotoOp)
			{
				globals.intInstCount++;
				
				// Start Debug after
				
				if (globals.intInstCount >= 20000000 )
				{
					globals.intInstCount = globals.intInstCount;
					//SnesSystem.DEBUG_APU = true;
					//SnesSystem.DEBUG_CPU = true;
					//SnesSystem.DEBUG_DMA = true;
					//SnesSystem.DEBUG_PPU = true;
					//SSystem.DEBUG_MEM = true;
					//SSystem.DEBUG_PPU_MODES = true;

					//System.out.println();
					//System.exit(0);
				}
				
				
				if ( cpu.Flags != 0 )
				{
					if ( ( cpu.Flags & SnesSystem.NMI_FLAG ) != 0 )
					{
						if (timings.NMITriggerPos <= cpu.Cycles)
						{
							cpu.Flags &= (~SnesSystem.NMI_FLAG) & 0xff;
							timings.NMITriggerPos = 0xffff;
							
							if (cpu.WaitingForInterrupt)
							{
								cpu.WaitingForInterrupt = false;
								cpu.PCw++;
							}
	
							cpu.Opcode_NMI();
						}
					}
	
					//NAC: Not used
					//CHECK_SOUND();
	
					if ( ( cpu.Flags & SnesSystem.IRQ_FLAG ) != 0 )
					{
						if (cpu.IRQPending != 0)
						{
							// FIXME: In case of IRQ during WRAM refresh
							cpu.IRQPending = 0;
						}
						else
						{
							if (cpu.WaitingForInterrupt)
							{
								cpu.WaitingForInterrupt = false;
								cpu.PCw++;
							}
	
							if ( cpu.IRQActive != 0 )
							{
								// in IRQ handler $4211 is supposed to be read, so IRQ_FLAG should be cleared.
								if ( ! cpu.CheckFlag( WDC_65c816.IRQ ) )
								{
									cpu.Opcode_IRQ();
								}
							}
							else
							{
								cpu.Flags &= (~SnesSystem.IRQ_FLAG) & 0xFF;
							}
						}
					}
	
					if ( ( cpu.Flags & SnesSystem.SCAN_KEYS_FLAG ) != 0 )
					{
						break;
					}
				}
	
				cpu.PBPCAtOpcodeStart = cpu.PBPC();
			}
			
		    if ( SnesSystem.DEBUG_CPU )
		    {
		    	CPUDebug.Trace();
		    }

			int Op;
			
			cpu.PrevCycles = cpu.Cycles;

			if (cpu.PCBase != null)
			{
				Op = cpu.PCBase.get8Bit( cpu.PCw );
				cpu.Cycles += cpu.MemSpeed;
			}
			else
			{
				Op = cpu.GetByte( cpu.PBPC() );
				globals.OpenBus = Op;
				cpu.Opcodes = WDC_65c816.OpcodesSlow;
			}

			if ( (cpu.PCw & Memory.MEMMAP_MASK) + cpu.OpLengths[Op] >= Memory.MEMMAP_BLOCK_SIZE )
			{
				ByteArrayOffset oldPCBase = cpu.PCBase;

				cpu.PCBase = cpu.GetBasePointer( cpu.ShiftedPB + ( (cpu.PCw + 4) & 0xFFFF ) );
				
				if ( ! ( cpu.PCBase.buffer == oldPCBase.buffer &&
						cpu.PCBase.getOffset() == oldPCBase.getOffset() ) ||
					( cpu.PCw & ~Memory.MEMMAP_MASK) == (0xffff & ~Memory.MEMMAP_MASK ) )
				{
					cpu.Opcodes = WDC_65c816.OpcodesSlow;
				}
			}

			cpu.Incr8BitPC();
			
			cpu.ExecuteOp(Op);
			
			if( cpu.SavedAtOp )
			{
				cpu.SavedAtOp = false;
				continue;
			}

			apu.APUExecute();

			if (sa1.Executing)
				sa1.SA1MainLoop();

			while (cpu.Cycles >= cpu.NextEvent)
			{
				DoHEventProcessing();
			}
	    }

	    cpu.PackStatus();
	    
	    apu.APUPackStatus();

	    if ( ( cpu.Flags & SnesSystem.SCAN_KEYS_FLAG ) != 0 )
	    {
			SnesSystem.SyncSpeed();
			cpu.Flags &= (~SnesSystem.SCAN_KEYS_FLAG) & 0xFF;
	    }
	    
	}

	void DoHEventProcessing()
	{

		cpu.WaitCounter++;

		switch (cpu.WhichEvent)
	    {
			case SnesSystem.HC_HBLANK_START_EVENT:
				ppu.CheckMissingHTimerPosition(timings.HBlankStart);

				break;

			case SnesSystem.HC_HDMA_START_EVENT:
				if (ppu.HDMA != 0 && ( cpu.V_Counter <= ppu.ScreenHeight ) )
				{
					ppu.HDMA = dma.DoHDMA(ppu.HDMA);
				}

				ppu.CheckMissingHTimerPosition(timings.HDMAStart);

				break;

			case SnesSystem.HC_HCOUNTER_MAX_EVENT:
				// NAC: SFX Disabled
				/*
				if (settings.SuperFX)
				{
					if (!superfx.oneLineDone)
						ppu.SuperFXExec();
					superfx.oneLineDone = false;
				}
				*/

				/* NAC: STORM?
				#ifndef STORM
					if (Settings.SoundSync)
						GenerateSound();
				#endif
				*/

				cpu.Cycles -= timings.H_Max;
				apu.NextAPUTimerPos -= (timings.H_Max << SnesSystem.SNES_APU_ACCURACY);
				apu.ApuCycles -= (timings.H_Max << SnesSystem.SNES_APU_ACCURACY);

				if ((timings.NMITriggerPos != 0xffff) && (timings.NMITriggerPos >= timings.H_Max))
				{
					timings.NMITriggerPos -= timings.H_Max;
				}

				cpu.V_Counter++;

				if (cpu.V_Counter >= timings.V_Max)	// V ranges from 0 to Timings.V_Max - 1
				{
					cpu.V_Counter = 0;
					globals.timings.InterlaceField ^= 1;

					// From byuu:
					// [NTSC]
					// interlace mode has 525 scanlines: 263 on the even frame, and 262 on the odd.
					// non-interlace mode has 524 scanlines: 262 scanlines on both even and odd frames.
					// [PAL] <PAL info is unverified on hardware>
					// interlace mode has 625 scanlines: 313 on the even frame, and 312 on the odd.
					// non-interlace mode has 624 scanlines: 312 scanlines on both even and odd frames.
					if (ppu.Interlace && timings.InterlaceField == 0)
					{
						timings.V_Max = timings.V_Max_Master + 1;	// 263 (NTSC), 313?(PAL)
					}
					else
					{
						timings.V_Max = timings.V_Max_Master;		// 262 (NTSC), 312?(PAL)
					}

					memory.FillRAM.buffer[0x213F] ^= 0x80;
					ppu.RangeTimeOver = 0;

					// FIXME: reading $4210 will wait 2 cycles, then perform reading, then wait 4 more cycles.
					memory.FillRAM.put8Bit(0x4210, SnesSystem._5A22);
					cpu.Flags &= (~SnesSystem.NMI_FLAG) & 0xff;
					timings.NMITriggerPos = 0xffff;

					ppu.HVBeamCounterLatched = 0;
					cpu.Flags |= SnesSystem.SCAN_KEYS_FLAG;
				}

				// From byuu:
				// In non-interlace mode, there are 341 dots per scanline, and 262 scanlines per frame.
				// On odd frames, scanline 240 is one dot short.
				// In interlace mode, there are always 341 dots per scanline. Even frames have 263 scanlines,
				// and odd frames have 262 scanlines.
				// Interlace mode scanline 240 on odd frames is not missing a dot.
				if (cpu.V_Counter == 240 && !ppu.Interlace && timings.InterlaceField != 0)
				{
					// V=240
					timings.H_Max = timings.H_Max_Master - SnesSystem.ONE_DOT_CYCLE;	// HC=1360
				}
				else
				{
					timings.H_Max = timings.H_Max_Master;					// HC=1364
				}

				if (SnesSystem._5A22 == 2)
				{
					if (cpu.V_Counter != 240 || ppu.Interlace || timings.InterlaceField == 0)	// V=240
					{
						if (timings.WRAMRefreshPos == SnesSystem.SNES_WRAM_REFRESH_HC_v2 - SnesSystem.ONE_DOT_CYCLE)	// HC=534
							timings.WRAMRefreshPos = SnesSystem.SNES_WRAM_REFRESH_HC_v2;					// HC=538
						else
							timings.WRAMRefreshPos = SnesSystem.SNES_WRAM_REFRESH_HC_v2 - SnesSystem.ONE_DOT_CYCLE;	// HC=534
					}
				}
				else
				{
					timings.WRAMRefreshPos = SnesSystem.SNES_WRAM_REFRESH_HC_v1;
				}

				ppu.CheckMissingHTimerPosition(0);

				if (cpu.V_Counter == ppu.ScreenHeight + PPU.FIRST_VISIBLE_LINE)	// VBlank starts from V=225(240).
				{
					globals.Engine.EndScreenRefresh();
					
					ppu.HDMA = 0;
					// Bits 7 and 6 of $4212 are computed when read in GetPPU.

					ppu.ForcedBlanking = (memory.FillRAM.getByte(0x2100) & 0x80) != 0;

					if ( ! ppu.ForcedBlanking )
					{
						ppu.OAMAddr = ppu.SavedOAMAddr;

						int tmp = 0;

						if ( ppu.OAMPriorityRotation )
						{
							tmp = (ppu.OAMAddr & 0xFE) >>> 1;
						}
						
						if ( ( ppu.OAMFlip & 1) != 0 || ppu.FirstSprite != tmp )
						{
							ppu.FirstSprite = tmp;
							ppu.OBJChanged = true;
						}

						ppu.OAMFlip = 0;
					}

					// FIXME: writing to $4210 will wait 6 cycles.
					memory.FillRAM.put8Bit( 0x4210, 0x80 | SnesSystem._5A22 );
					
					if( ( memory.FillRAM.getByte(0x4200) & 0x80) != 0 )
					{
						// FIXME: triggered at HC=6, checked just before the final CPU cycle,
						// then, when to call Opcode_NMI()?
						cpu.Flags |= SnesSystem.NMI_FLAG;
						timings.NMITriggerPos = 6 + 6;
					}

				}

				if (cpu.V_Counter == ppu.ScreenHeight + 3)	// FIXME: not true
				{
					if( ( memory.FillRAM.getByte(0x4200) & 1 ) != 0 )
					{
						globals.controls.DoAutoJoypad();
					}
				}

				if (cpu.V_Counter == PPU.FIRST_VISIBLE_LINE)	// V=1
				{
					globals.Engine.StartScreenRefresh();
				}

				cpu.NextEvent = -1;

				break;

			case SnesSystem.HC_HDMA_INIT_EVENT:
				
				if (cpu.V_Counter == 0)
				{
					dma.StartHDMA();
				}

				ppu.CheckMissingHTimerPosition(timings.HDMAInit);

				break;

			case SnesSystem.HC_RENDER_EVENT:
				
				if (cpu.V_Counter >= PPU.FIRST_VISIBLE_LINE && cpu.V_Counter <= ppu.ScreenHeight)
				{
					// TODO: Line based scanner
					//globals.ppu.RenderLine( (cpu.V_Counter - PPU.FIRST_VISIBLE_LINE) & 0xFF);
				}

				ppu.CheckMissingHTimerPosition(timings.RenderPos);

				break;

			case SnesSystem.HC_WRAM_REFRESH_EVENT:
				
				if (SnesSystem.DEBUG_CPU)
				{
					System.out.format("*** WRAM Refresh  HC:%04d\n", cpu.Cycles );
				}
				ppu.CheckMissingHTimerHalt( timings.WRAMRefreshPos, SnesSystem.SNES_WRAM_REFRESH_CYCLES );
				cpu.Cycles += SnesSystem.SNES_WRAM_REFRESH_CYCLES;
				apu.APUExecute();

				ppu.CheckMissingHTimerPosition( timings.WRAMRefreshPos );

				break;

			case SnesSystem.HC_IRQ_1_3_EVENT:
			case SnesSystem.HC_IRQ_3_5_EVENT:
			case SnesSystem.HC_IRQ_5_7_EVENT:
			case SnesSystem.HC_IRQ_7_9_EVENT:
			case SnesSystem.HC_IRQ_9_A_EVENT:
			case SnesSystem.HC_IRQ_A_1_EVENT:
				
				if ( ppu.HTimerEnabled && ( ! ppu.VTimerEnabled || (cpu.V_Counter == ppu.VTimerPosition ) ) )
				{
					cpu.SetIRQ( PPU.PPU_H_BEAM_IRQ_SOURCE );
				}
				else if ( ppu.VTimerEnabled && ( cpu.V_Counter == ppu.VTimerPosition ) )
				{
					cpu.SetIRQ( PPU.PPU_V_BEAM_IRQ_SOURCE );
				}

				break;
	    }

	    Reschedule();
	}
	
	private void Reschedule ()
	{
		int	next = 0;
		int	hpos = 0;

		switch (cpu.WhichEvent)
		{
			case SnesSystem.HC_HBLANK_START_EVENT:
			case SnesSystem.HC_IRQ_1_3_EVENT:
				next = SnesSystem.HC_HDMA_START_EVENT;
				hpos = timings.HDMAStart;
				break;

			case SnesSystem.HC_HDMA_START_EVENT:
			case SnesSystem.HC_IRQ_3_5_EVENT:
				next = SnesSystem.HC_HCOUNTER_MAX_EVENT;
				hpos = timings.H_Max;
				break;

			case SnesSystem.HC_HCOUNTER_MAX_EVENT:
			case SnesSystem.HC_IRQ_5_7_EVENT:
				next = SnesSystem.HC_HDMA_INIT_EVENT;
				hpos = timings.HDMAInit;
				break;

			case SnesSystem.HC_HDMA_INIT_EVENT:
			case SnesSystem.HC_IRQ_7_9_EVENT:
				next = SnesSystem.HC_RENDER_EVENT;
				hpos = timings.RenderPos;
				break;

			case SnesSystem.HC_RENDER_EVENT:
			case SnesSystem.HC_IRQ_9_A_EVENT:
				next = SnesSystem.HC_WRAM_REFRESH_EVENT;
				hpos = timings.WRAMRefreshPos;
				break;

			case SnesSystem.HC_WRAM_REFRESH_EVENT:
			case SnesSystem.HC_IRQ_A_1_EVENT:
				next = SnesSystem.HC_HBLANK_START_EVENT;
				hpos = timings.HBlankStart;
				break;
		}

		if ( ( ppu.HTimerPosition > cpu.NextEvent ) && ( ppu.HTimerPosition < hpos ) )
		{
			hpos = ppu.HTimerPosition;

			switch (next)
			{
				case SnesSystem.HC_HDMA_START_EVENT:
					next = SnesSystem.HC_IRQ_1_3_EVENT;
					break;

				case SnesSystem.HC_HCOUNTER_MAX_EVENT:
					next = SnesSystem.HC_IRQ_3_5_EVENT;
					break;

				case SnesSystem.HC_HDMA_INIT_EVENT:
					next = SnesSystem.HC_IRQ_5_7_EVENT;
					break;

				case SnesSystem.HC_RENDER_EVENT:
					next = SnesSystem.HC_IRQ_7_9_EVENT;
					break;

				case SnesSystem.HC_WRAM_REFRESH_EVENT:
					next = SnesSystem.HC_IRQ_9_A_EVENT;
					break;

				case SnesSystem.HC_HBLANK_START_EVENT:
					next = SnesSystem.HC_IRQ_A_1_EVENT;
					break;
			}
		}

		cpu.NextEvent  = hpos;
		cpu.WhichEvent = next;
	}
}
