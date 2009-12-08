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

final class CPU extends WDC_65c816 
{	
	private Timings timings;
	
	void setUp()
	{
		super.setUp();
		timings = globals.timings;
	}
	
	void SoftResetCPU ()
	{
		PBPC(0);
		PB(0);
		PCw = GetWord( 0xFFFC );
		globals.OpenBus = PCh();
		D = 0;
		DB = 0;
		
		S = 0x0100 |( ( (S & 0xFF) + 3 ) & 0xFF);
		X = (X & 0x00FF);
		Y = (Y & 0x00FF);
		
		ShiftedPB = 0;
		ShiftedDB = 0;
		SetFlags( MemoryFlag | IndexFlag | IRQ, Emulation );
		
		PL = PL & (~Decimal & 0xFF);

		Flags = Flags & ( SnesSystem.DEBUG_MODE_FLAG | SnesSystem.TRACE_FLAG);
		BranchSkip = false;
		NMIActive = false;
		IRQActive = 0;
		WaitingForInterrupt = false;
		InDMA = false;
		InHDMA = false;
		InDMAorHDMA = false;
		InWRAMDMAorHDMA = false;
		HDMARanInDMA = 0;
		PCBase = null;
		PBPCAtOpcodeStart = 0xffffffff;
		WaitAddress = 0xffffffff;
		WaitCounter = 0;
		Cycles = 182; // Or 188. This is the cycle count just after the jump to the Reset Vector.
		PrevCycles = -1;
		V_Counter = 0;
		MemSpeed = SnesSystem.SLOW_ONE_CYCLE;
		MemSpeedx2 = SnesSystem.SLOW_ONE_CYCLE * 2;
		FastROMSpeed = SnesSystem.SLOW_ONE_CYCLE;
		AutoSaveTimer = 0;
		SRAMModified = false;
		BRKTriggered = false;
		IRQPending = 0;
		
		timings.InterlaceField = 0;
		timings.H_Max = timings.H_Max_Master;
		timings.V_Max = timings.V_Max_Master;
		timings.NMITriggerPos = 0xffff;
		
		globals.timings.WRAMRefreshPos = SnesSystem.SNES_WRAM_REFRESH_HC_v2;

		WhichEvent = SnesSystem.HC_RENDER_EVENT;
		NextEvent  = timings.RenderPos;
		
		SetPCBase( PBPC() );
		
		Opcodes = OpcodesE1;
		OpLengths = OpLengthsM1X1;
		CPUExecuting = 1;
		
		UnpackStatus();
		
	}

	void ResetCPU ()
	{
	    SoftResetCPU();
	    
	    S = (S & 0xFF00 ) | 0xFF;
	    PL = 0;
	    A = 0;
	    X = 0;
	    Y = 0;
		
		SetFlags( MemoryFlag | IndexFlag | IRQ, Emulation );
		
		PL = PL & (~Decimal & 0xFF);
	}

	void ClearIRQSource( int M )
	{
	    IRQActive = IRQActive & ~M;
	    
	    if ( ! ( IRQActive != 0 ) )
	    {
	    	Flags &= (  ~SnesSystem.IRQ_FLAG ) & 0xFF;
	    }
	}

	void SetIRQ( int source )
	{
		IRQActive |= source;
		Flags |= SnesSystem.IRQ_FLAG;

		if ( WaitingForInterrupt)
		{
			// Force IRQ to trigger immediately after WAI -
			// Final Fantasy Mystic Quest crashes without this.
			WaitingForInterrupt = false;
			PCw++;
		}
	}

	void ClearIRQ(int source)
	{
		ClearIRQSource(source);
	}
	
	void FixCycles()
	{
	    if ( CheckEmulation() )
	    {
	    	// Acumulator is in 8 Bit Mode With A B C Registers
	        // A = High Byte, B = Low Byte, C = High And Low Byte ( 16 bits )
	        // and X,Y Registers are in 8 Bit Mode
	    	Opcodes = OpcodesE1;
	        OpLengths = OpLengthsM1X1;
	    }
	    // Acumulator is in 16 bit mode ( M=1 )
	    else if ( CheckMemory() )
	    {
	    	// X,Y Registers are in 8 Bit Mode ( X=1 )
			if ( CheckIndex() )
			{
				Opcodes = OpcodesM1X1;
		        OpLengths = OpLengthsM1X1;
			}
			// X,Y Registers are in 16 Bit Mode ( X=0 )
			else
			{
			    Opcodes = OpcodesM1X0;
		        OpLengths = OpLengthsM1X0;
			}
		}
	    // Acumulator is in 8 Bit Mode ( M=0 )
	    else
	    {
	    	// Index Registers X,Y are in 8 Bit Mode ( X=1 )
			if ( CheckIndex() )
			{
			    Opcodes = OpcodesM0X1;
		        OpLengths = OpLengthsM0X1;
			}
			// Index Registers X,Y are in 16 Bit Mode ( X=0 )
			else
			{
			    Opcodes = OpcodesM0X0;
		        OpLengths = OpLengthsM0X0;
			}
	    }
	}
}
