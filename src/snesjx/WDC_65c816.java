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

abstract class WDC_65c816
{
	//	 Access Modes
	private static final int NONE 	= 0;
	private static final int READ 	= 1;
	private static final int WRITE 	= 2;
	private static final int MODIFY = 3;
	private static final int JUMP 	= 5;
	private static final int JSR 	= 8;
	
	//	 CPU Flags
	static final int Carry		= 1;
	static final int Zero		= 2;
	static final int IRQ		= 4;
	static final int Decimal	= 8;
	static final int IndexFlag  = 16;
	static final int MemoryFlag = 32;
	static final int Overflow   = 64;
	static final int Negative   = 128;
	static final int Emulation  = 1;

	// CPU OpCodes
	static final int OpcodesM1X1	= 0x000;
	static final int OpcodesE1		= 0x100;
	static final int OpcodesM1X0	= 0x200;
	static final int OpcodesM0X0	= 0x300;
	static final int OpcodesM0X1	= 0x400;
	static final int OpcodesSlow	= 0x500;
	static final int OpcodesNull	= 0x600;
	
	
	static final byte OpLengthsM0X0[] = {
	//  0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 0
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 1
		3, 2, 4, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 2
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 3
		1, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 4
		2, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 4, 3, 3, 4, // 5
		1, 2, 3, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 6
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 7
		2, 2, 3, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 8
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 9
		3, 2, 3, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // A
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // B
		3, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // C
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // D
		3, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // E
		2, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4  // F
	};
	
	static final byte OpLengthsM0X1[] = {
	//  0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 0
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 1
		3, 2, 4, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 2
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 3
		1, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 4
		2, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 4, 3, 3, 4, // 5
		1, 2, 3, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 6
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 7
		2, 2, 3, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 8
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 9
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // A
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // B
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // C
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // D
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // E
		2, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4  // F
	};
	
	static final byte OpLengthsM1X0[] = {
	//  0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 0
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 1
		3, 2, 4, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 2
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 3
		1, 2, 2, 2, 3, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 4
		2, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 4, 3, 3, 4, // 5
		1, 2, 3, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 6
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 7
		2, 2, 3, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 8
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 9
		3, 2, 3, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // A
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // B
		3, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // C
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // D
		3, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // E
		2, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4  // F
	};
	
	static final byte OpLengthsM1X1[] = {
	//  0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 0
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 1
		3, 2, 4, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 2
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 3
		1, 2, 2, 2, 3, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 4
		2, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 4, 3, 3, 4, // 5
		1, 2, 3, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 6
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 7
		2, 2, 3, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // 8
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // 9
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // A
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // B
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // C
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4, // D
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 4, // E
		2, 2, 2, 2, 3, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 4  // F
	};
	
	Globals globals;
	SA1 sa1;
	DSP1 dsp1;
	APU apu;
	Memory memory;
	SMulti Multi;
	PPU ppu;
	
	int Opcodes = OpcodesNull;
	byte OpLengths[];
	
	int _Carry;
	
	int _Negative;
	
	int _Overflow;
	
	int _Zero;
	
	// A register
	int A = 0;
	// D register
	int D = 0;
	
	// P register
	int PL = 0;
	int PH = 0;
	
	// S register
	int S = 0;
	// X register
	int X = 0;
	// Y register
	int Y = 0;
	
	int AutoSaveTimer;
	
	boolean BranchSkip;
	boolean BRKTriggered;
	
	// From SSA1
	ByteArrayOffset BWRAM;
	
	int CPUExecuting; // boolean

	int Cycles;

	int DB;
	int FastROMSpeed;
	int Flags;

	int HDMARanInDMA;

	boolean InDMA;
	boolean InDMAorHDMA;
	
	boolean InHDMA;
	boolean InWRAMDMAorHDMA;
	int IRQActive;
	int IRQPending;
	
	int MemSpeed;
	int MemSpeedx2;
	int NextEvent;
	boolean NMIActive;
		
	int PBPCAtOpcodeStart;
	int PCb;
	ByteArrayOffset PCBase = null;
	int PCw;

	int PrevCycles;
	
	boolean SavedAtOp;

	int ShiftedDB;
	int ShiftedPB;
	boolean SRAMModified;

	int V_Counter;
	int WaitAddress;
	int WaitCounter;
	boolean WaitingForInterrupt;
	int WhichEvent;

	
	WDC_65c816()
	{}
	
	// Registers
	
	private int A_H() {  return ( A & 0xFF00 ) >>> 8; }
	private void A_H( int value ) { A = (A & 0x00FF) | ( value << 8 ) & 0xFF00; }

	private int A_L() { return A & 0x00FF; }
	private void A_L( int value ) {  A = (A & 0xFF00 ) | (value & 0x00FF); }

	private void A_W( int value) { A = value & 0xFFFF; }
	
	
	private int D_H() {  return ( D & 0xFF00 ) >>> 8; }

	private int D_L() { return D & 0x00FF; }

	private void D_W( int value) { D = value & 0xFFFF; }	
	
	private void S_H( int value ) { S = (S & 0x00FF) | ( value << 8 ) & 0xFF00; }

	private int S_L() { return S & 0x00FF; }

	private void S_L( int value ) {  S = (S & 0xFF00 ) | (value & 0x00FF); }

	private void S_W( int value) { S = value & 0xFFFF; }
	

	private int X_H() {  return ( X & 0xFF00 ) >>> 8; } 

	private void X_H( int value ) { X = (X & 0x00FF) | ( value << 8 ) & 0xFF00; }

	private int X_L() { return X & 0x00FF; }

	private void X_L( int value ) {  X = (X & 0xFF00 ) | (value & 0x00FF); }

	private void X_W( int value) { X = value & 0xFFFF; }

	
	private int Y_H() {  return ( Y & 0xFF00 ) >>> 8; }
		
	private void Y_H( int value ) { Y = (Y & 0x00FF) | ( value << 8 ) & 0xFF00; }

	private int Y_L() { return Y & 0x00FF; }

	private void Y_L( int value ) {  Y = (Y & 0xFF00 ) | (value & 0x00FF); }
		
	private void Y_W( int value) { Y = value & 0xFFFF; }
	
	final ByteArrayOffset GetBasePointer( int Address )
	{
		int block = ( ( Address & 0xffffff ) >>> Memory.MEMMAP_SHIFT );
		int GetAddress = memory.Map[ block ];

		if ( GetAddress <= Memory.MAP_LAST )
		{
			return memory.RAM.getOffsetBuffer(GetAddress);
		}

		switch ( GetAddress )
		{
		case Memory.MAP_SA1RAM:
			return memory.SRAM;

		case Memory.MAP_LOROM_SRAM:
			if ( ( memory.SRAMMask & Memory.MEMMAP_MASK ) != Memory.MEMMAP_MASK )
				return null;

			return memory.SRAM.getOffsetBuffer( ( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & memory.SRAMMask ) - ( Address & 0xffff ) );

		case Memory.MAP_LOROM_SRAM_B:
			if ( ( Multi.sramMaskB & Memory.MEMMAP_MASK ) != Memory.MEMMAP_MASK )
				return null;

			return Multi.sramB.getOffsetBuffer( ( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & Multi.sramMaskB ) - ( Address & 0xffff ) );

		case Memory.MAP_BWRAM:
			return BWRAM.getOffsetBuffer( -0x6000 - ( Address & 0x8000 ) );

		case Memory.MAP_HIROM_SRAM:
			if ( ( memory.SRAMMask & Memory.MEMMAP_MASK ) != Memory.MEMMAP_MASK )
				return null;

			return memory.SRAM.getOffsetBuffer( ( ( ( Address & 0x7fff ) - 0x6000 + ( ( Address & 0xf0000 ) >>> 3 ) ) & memory.SRAMMask ) - ( Address & 0xffff ) );
		/*
		case Memory.MAP_C4:
			//TODO: return GetBasePointerC4(Address);

		case Memory.MAP_OBC_RAM:
			//TODO: return GetBasePointerOBC1(Address);
		
		case Memory.MAP_SPC7110_ROM:
			return globals.s7r.Get7110BasePtr(Address);			
		case Memory.MAP_DEBUG:
		*/
		default:
		case Memory.MAP_NONE:
			return null;
		}
	}

	int GetByte( int Address )
	{
		int block = ( ( Address & 0xffffff ) >>> Memory.MEMMAP_SHIFT );
		int GetAddress = memory.Map[block];

		if ( ! InDMAorHDMA )
			AddCycles( memory.MemorySpeed[block] );

		if ( GetAddress <= Memory.MAP_LAST )
		{
			if ( memory.BlockIsRAM[block] )
				WaitAddress = PBPCAtOpcodeStart;

			return memory.RAM.get8Bit( GetAddress + ( Address & 0xffff ) );
		}

		switch ( GetAddress )
		{
		case Memory.MAP_PPU:
			if ( InDMAorHDMA && ( Address & 0xff00 ) == 0x2100 )
			{
				return globals.OpenBus;
			}
			return ppu.GetPPU( Address & 0xffff );
		case Memory.MAP_CPU:
			return ppu.GetCPU ( Address & 0xffff );
		case Memory.MAP_DSP:
			return dsp1.GetDSP ( Address & 0xffff );
		case Memory.MAP_SA1RAM:
		case Memory.MAP_LOROM_SRAM:
			return memory.SRAM.get8Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & memory.SRAMMask );
		case Memory.MAP_LOROM_SRAM_B:
			return Multi.sramB.get8Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & Multi.sramMaskB );
		case Memory.MAP_RONLY_SRAM:
		case Memory.MAP_HIROM_SRAM:
			return memory.SRAM.get8Bit( ( ( Address & 0x7fff ) - 0x6000 + ( ( Address & 0xf0000 ) >>> 3 ) ) & memory.SRAMMask );
		case Memory.MAP_BWRAM:
			return memory.BWRAM.get8Bit( ( Address & 0x7fff ) - 0x6000 );
		/*
		case Memory.MAP_C4:
			//TODO: return (GetC4 (Address & 0xffff));

		case Memory.MAP_SPC7110_ROM:
			//TODO: return GetSPC7110Byte(Address);

		case Memory.MAP_SPC7110_DRAM:
			//TODO: return GetSPC7110(0x4800);

		case Memory.MAP_OBC_RAM:
			//TODO: return GetOBC1(Address & 0xffff);

		case Memory.MAP_SETA_DSP:
			//TODO: return GetSetaDSP(Address);

		case Memory.MAP_SETA_RISC:
			//TODO: return GetST018(Address);

		case Memory.MAP_BSX:
			//TODO: return GetBSX(Address);
		
		*/

		case Memory.MAP_DEBUG:
		default:
		case Memory.MAP_NONE:
			return globals.OpenBus;
		}
	}

	int GetWord( int Address )
	{
		return GetWord( Address, Memory.WRAP_NONE );
	}

	int GetWord( int Address, int w )
	{
		switch ( w )
		{
		case Memory.WRAP_PAGE:
			if ( ( Address & 0xFF ) == 0xFF )
			{
				globals.OpenBus = GetByte( Address );
				Address = (Address & 0xFFFF00 ) | ( ( Address + 1 ) & 0xFF);
				return ( globals.OpenBus | ( GetByte( Address ) << 8 ) );
			}
			break;
		case Memory.WRAP_BANK:
			if ( ( Address & Memory.MEMMAP_MASK ) == Memory.MEMMAP_MASK )
			{
				globals.OpenBus = GetByte( Address );
				Address = (Address & 0xFF0000 ) | ( ( Address + 1 ) & 0xFFFF);
				return ( globals.OpenBus | ( GetByte( Address ) << 8 ) );
			}
			break;
		case Memory.WRAP_NONE:
		default:
			
			if ( ( Address & Memory.MEMMAP_MASK ) == Memory.MEMMAP_MASK )
			{
				globals.OpenBus = GetByte( Address );
				return ( globals.OpenBus | ( GetByte( Address + 1 ) << 8 ) );
			}
			break;
		}
		
		int block = ( Address & 0xffffff ) >>> Memory.MEMMAP_SHIFT;
		int GetAddress = memory.Map[ block ];

		if ( ! InDMAorHDMA )
			AddCycles( memory.MemorySpeed [block] << 1 );

		if ( GetAddress <= Memory.MAP_LAST )
		{
			if ( memory.BlockIsRAM[block] )
				WaitAddress = PBPCAtOpcodeStart;

			return memory.RAM.get16Bit( GetAddress + ( Address & 0xffff ) );
		}

		switch ( GetAddress )
		{
		case Memory.MAP_PPU:
			if ( InDMAorHDMA )
			{
				globals.OpenBus = GetByte(Address);
				return (globals.OpenBus | ( GetByte(Address + 1) << 8));
			}
			return ppu.GetPPU( Address & 0xffff) | (ppu.GetPPU ((Address + 1) & 0xffff) << 8);
			
		case Memory.MAP_CPU:
			return ppu.GetCPU( Address & 0xffff ) | ( ppu.GetCPU ( ( Address + 1 ) & 0xffff ) << 8 );
			
		case Memory.MAP_DSP:
			return dsp1.GetDSP( Address & 0xffff ) | ( dsp1.GetDSP ( ( Address + 1 ) & 0xffff ) << 8 );
			
		case Memory.MAP_SA1RAM:
		case Memory.MAP_LOROM_SRAM:
			//Address &0x7FFF -offset into bank
			//Address&0xFF0000 -bank
			//bank>>1 | offset = s-ram address, unbound
			//unbound & SRAMMask = Sram offset
			if ( memory.SRAMMask >= Memory.MEMMAP_MASK )
			{
				return memory.SRAM.get16Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & memory.SRAMMask );
			}
			else
			{
				// no READ_WORD here, since if SRAMMask=0x7ff
				// then the high byte doesn't follow the low byte.
				return
					( memory.SRAM.get8Bit ( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & memory.SRAMMask ) ) |
					( memory.SRAM.get8Bit ( ( ( ( ( Address + 1 ) & 0xFF0000 ) >>> 1 ) | ( ( Address + 1 ) & 0x7FFF ) ) & memory.SRAMMask ) << 8 );
			}

		case Memory.MAP_LOROM_SRAM_B:
			if ( Multi.sramMaskB >= Memory.MEMMAP_MASK )
			{
				return Multi.sramB.get16Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & Multi.sramMaskB );
			}
			else
			{
				return
					( Multi.sramB.get8Bit ( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & Multi.sramMaskB ) ) |
					( Multi.sramB.get8Bit ( ( ( ( ( Address + 1 ) & 0xFF0000 ) >>> 1 ) | ( ( Address + 1 ) & 0x7FFF ) ) & Multi.sramMaskB ) << 8 );

			}

		case Memory.MAP_RONLY_SRAM:
		case Memory.MAP_HIROM_SRAM:
			if ( memory.SRAMMask >= Memory.MEMMAP_MASK )
			{
				return memory.SRAM.get16Bit( ( ( Address & 0x7fff ) - 0x6000 + ( ( Address & 0xf0000 ) >>> 3 ) ) & memory.SRAMMask );
			}
			else
			{
				// no READ_WORD here, since if SRAMMask=0x7ff
				//then the high byte doesn't follow the low byte.
				return
					memory.SRAM.get8Bit( ( ( ( Address & 0x7fff ) - 0x6000 + ( ( Address & 0xf0000 ) >>> 3 ) ) & memory.SRAMMask ) |
										 memory.SRAM.get8Bit( ( ( ( ( Address + 1 ) & 0x7fff ) - 0x6000 + ( ( ( Address + 1 ) & 0xf0000 ) >>> 3 ) ) & memory.SRAMMask ) ) << 8 );
			}

		case Memory.MAP_BWRAM:
			return memory.BWRAM.get16Bit( ( Address & 0x7fff ) - 0x6000 );
		/*
		case Memory.MAP_C4:
			//TODO: return (GetC4 (Address & 0xffff) | (GetC4 ((Address + 1) & 0xffff) << 8));

		case Memory.MAP_SPC7110_ROM:
			return (globals.s7r.GetSPC7110Byte(Address) | (globals.s7r.GetSPC7110Byte (Address+1))<<8);
		case Memory.MAP_SPC7110_DRAM:
			return (globals.s7r.GetSPC7110(0x4800) | (globals.s7r.GetSPC7110 (0x4800) << 8));
		case Memory.MAP_OBC_RAM:
			//TODO: return GetOBC1(Address&0xFFFF) | (GetOBC1((Address+1)&0xFFFF)<<8);

		case Memory.MAP_SETA_DSP:
			//TODO: return GetSetaDSP(Address) | (GetSetaDSP((Address+1))<<8);

		case Memory.MAP_SETA_RISC:
			//TODO: return GetST018(Address) | (GetST018((Address+1))<<8);

		case Memory.MAP_BSX:
			//TODO: return GetBSX(Address) | (GetBSX((Address+1))<<8);
		*/
		case Memory.MAP_DEBUG:
		default:
		case Memory.MAP_NONE:
			return ( globals.OpenBus | ( globals.OpenBus << 8 ) );
		}
	}
	

	void SetPCBase( int Address )
	{
		PBPC( Address & 0xffffff );
		ShiftedPB = Address & 0xff0000;

		int block;
		int GetAddress = memory.Map [ block = ( ( Address & 0xffffff ) >>> Memory.MEMMAP_SHIFT )];

		MemSpeed = memory.MemorySpeed [block];
		MemSpeedx2 = MemSpeed << 1;

		if ( GetAddress <= Memory.MAP_LAST )
		{
			PCBase = memory.RAM.getOffsetBuffer( GetAddress );
			return;
		}

		switch ( GetAddress )
		{
		case Memory.MAP_SA1RAM:
			PCBase = memory.SRAM.getOffsetBuffer( 0 );
			return;

		case Memory.MAP_LOROM_SRAM:
			if ( ( memory.SRAMMask & Memory.MEMMAP_MASK ) != Memory.MEMMAP_MASK )
			{
				PCBase = null;
			}
			else
			{
				PCBase = memory.SRAM.getOffsetBuffer( ( ( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & memory.SRAMMask ) ) - ( Address & 0xffff ) );
			}
			return;

		case Memory.MAP_LOROM_SRAM_B:
			if ( ( Multi.sramMaskB & Memory.MEMMAP_MASK ) != Memory.MEMMAP_MASK )
			{
				PCBase = null;
			}
			else
			{
				PCBase = Multi.sramB.getOffsetBuffer( ( ( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & Multi.sramMaskB ) ) - ( Address & 0xffff ) );
			}
			return;

		case Memory.MAP_BWRAM:
			PCBase = BWRAM.getOffsetBuffer( 0x6000 - ( Address & 0x8000 ) );
			return;

		case Memory.MAP_HIROM_SRAM:
			if ( (memory.SRAMMask & Memory.MEMMAP_MASK ) != Memory.MEMMAP_MASK )
			{
				PCBase = null;
			}
			else
			{
				PCBase = memory.SRAM.getOffsetBuffer( ( ( ( ( Address & 0x7fff ) - 0x6000 + ( ( Address & 0xf0000 ) >>> 3 ) ) & memory.SRAMMask ) ) - ( Address & 0xffff ) );
			}
			return;
		/*
		case Memory.MAP_SPC7110_ROM:
			//TODO: PCBase = Get7110BasePtr(Address);
			return;
		case Memory.MAP_C4:
			//TODO: PCBase = GetBasePointerC4(Address);
			return;

		case Memory.MAP_OBC_RAM:
			//TODO: PU.PCBase = GetBasePointerOBC1(Address);
			return;

		case Memory.MAP_BSX:
			//TODO: PCBase = GetBasePointerBSX(Address);
			return;
		case Memory.MAP_DEBUG:
		*/
		default:
		case Memory.MAP_NONE:
			PCBase = null;
			return;
		}
	}

	void setUp()
	{
		globals = Globals.globals;
		memory = globals.memory;
		Multi = globals.Multi;
		ppu = globals.ppu;
		sa1 = globals.sa1;
		dsp1 = globals.dsp1;
		apu = globals.apu;
	}
	
	void SetByte( int Byte, int Address )
	{
		WaitAddress = 0xffffffff;
		
		if ( SnesSystem.DEBUG_CPU )
			System.out.println( String.format("MEM SetByte: %02X to %06X", (Byte & 0xFF), Address ));
		
		int block = ( ( Address & 0xffffff ) >>> Memory.MEMMAP_SHIFT );
		
		int SetAddress = memory.WriteMap[block];

		if ( ! InDMAorHDMA )
			AddCycles( memory.MemorySpeed[block] );

		if ( SetAddress <= Memory.MAP_LAST )
		{
			SetAddress += Address & 0xffff;

			if ( SetAddress == sa1.WaitByteAddress1 || SetAddress == sa1.WaitByteAddress2 )
			{
				sa1.Executing = sa1.Opcodes != OpcodesNull;
				sa1.WaitCounter = 0;
			}
			
			memory.RAM.put8Bit( SetAddress, Byte );
			return;
		}

		switch ( SetAddress )
		{
		case Memory.MAP_PPU:
			if ( InDMAorHDMA && ( Address & 0xff00 ) == 0x2100 ) return;
			ppu.SetPPU( Byte, Address & 0xffff );
			return;

		case Memory.MAP_CPU:
			ppu.SetCPU( Byte, Address & 0xffff );
			return;

		case Memory.MAP_DSP:
			dsp1.SetDSP( Byte, Address & 0xffff );
			return;

		case Memory.MAP_LOROM_SRAM:
			if ( memory.SRAMMask > 0 )
			{
				memory.SRAM.put8Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & memory.SRAMMask, Byte );
				SRAMModified = true;
			}
			return;

		case Memory.MAP_LOROM_SRAM_B:
			if ( Multi.sramMaskB > 0 )
			{
				Multi.sramB.put8Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & Multi.sramMaskB, Byte );
				SRAMModified = true;
			}
			return;

		case Memory.MAP_HIROM_SRAM:
			if ( memory.SRAMMask > 0 )
			{
				memory.SRAM.put8Bit( ( ( Address & 0x7fff ) - 0x6000 +	( ( Address & 0xf0000 ) >>> 3 ) ) & memory.SRAMMask, Byte );
				SRAMModified = true;
			}
			return;

		case Memory.MAP_BWRAM:
			BWRAM.put8Bit( ( Address & 0x7fff ) - 0x6000, Byte );
			SRAMModified = true;
			return;

		case Memory.MAP_SA1RAM:
			memory.SRAM.put8Bit( Address & 0xffff, Byte );
			sa1.Executing = !sa1.Waiting;
			break;
		/*
		case Memory.MAP_C4:
			//TODO: SetC4 (Byte, Address & 0xffff);
			return;

		case Memory.MAP_SPC7110_DRAM:
			globals.s7r.bank50.put8Bit( Address & 0xffff, Byte );
			break;

		case Memory.MAP_OBC_RAM:
			//TODO: SetOBC1(Byte, Address &0xFFFF);
			return;

		case Memory.MAP_SETA_DSP:
			//TODO: SetSetaDSP(Byte,Address);
			return;

		case Memory.MAP_SETA_RISC:
			//TODO: SetST018(Byte,Address);
			return;

		case Memory.MAP_BSX:
			//TODO: SetBSX(Byte,Address);
			return;
		case Memory.MAP_DEBUG:
		*/
		default:
		case Memory.MAP_NONE:
			return;
		}
	}

	void SetWord( int Word, int Address, int w, int o )
	{
		if ( SnesSystem.DEBUG_CPU )
		{
			// NAC: It's okay if Word overflows because everything in this routine masks the overflow off
			System.out.println( String.format("MEM SetWord: %04X to %06X", Word & 0xFFFF, Address ));
		}

		switch ( w )
		{
		case Memory.WRAP_PAGE:
			if ( ( Address & 0xff ) == 0xff )
			{
				if ( o == 0 ) SetByte( Word & 0x00FF, Address );
				int xPBPC = (Address & 0xFFFF00) | (Address + 1) & 0xFF;
				SetByte( Word >>> 8, xPBPC );
				if ( o != 0 ) SetByte( Word & 0x00FF, Address );
				return;
			}
		case Memory.WRAP_BANK:
			if ( ( Address & Memory.MEMMAP_MASK ) == Memory.MEMMAP_MASK )
			{
				if ( o == 0 ) SetByte( Word & 0x00FF, Address );
				int xPBPC = (Address & 0xFF0000) | (Address + 1) & 0xFFFF; 
				SetByte( Word >>> 8, xPBPC );
				if ( o != 0 ) SetByte( Word & 0x00FF, Address );
				return;
			}
		case Memory.WRAP_NONE:
		default:
			if ( ( Address & Memory.MEMMAP_MASK ) == Memory.MEMMAP_MASK )
			{
				if ( o == 0 ) SetByte( Word & 0x00FF, Address );
				SetByte( Word >>> 8, Address + 1 );
				if ( o != 0 ) SetByte( Word & 0x00FF, Address );
				return;
			}
		}

		WaitAddress = 0xffffffff;
		int block = ( ( Address & 0xffffff ) >>> Memory.MEMMAP_SHIFT );
		int SetAddress = memory.WriteMap[block];

		if ( !InDMAorHDMA )
			AddCycles( memory.MemorySpeed [block] << 1 );

		if ( SetAddress <= Memory.MAP_LAST )
		{
			SetAddress += Address & 0xffff;
			
			if ( SetAddress == sa1.WaitByteAddress1 ||
					SetAddress == sa1.WaitByteAddress2 )
			{
				sa1.Executing = sa1.Opcodes != WDC_65c816.OpcodesNull;
				sa1.WaitCounter = 0;
			}
			
			memory.RAM.put16Bit( SetAddress, Word );
			return;
		}

		switch ( SetAddress )
		{
		case Memory.MAP_PPU:
			if ( InDMAorHDMA )
			{
				if ( ( Address & 0xff00 ) != 0x2100 )
					ppu.SetPPU( Word & 0xff, Address & 0xffff );
				if ( ( ( Address + 1 ) & 0xff00 ) != 0x2100 )
					ppu.SetPPU( Word >>> 8, ( Address + 1 ) & 0xffff );
				return;
			}
			if ( o > 0 )
			{
				ppu.SetPPU( Word >>> 8, ( Address & 0xffff ) + 1 );
				ppu.SetPPU( Word & 0xff, Address & 0xffff );
			}
			else
			{
				ppu.SetPPU( Word & 0xff, Address & 0xffff );
				ppu.SetPPU( Word >>> 8, ( Address & 0xffff ) + 1 );
			}
			return;

		case Memory.MAP_CPU:
			if ( o > 0 )
			{
				ppu.SetCPU( Word >>> 8, ( Address & 0xffff ) + 1 );
				ppu.SetCPU( Word & 0xff, Address & 0xffff );
			}
			else
			{
				ppu.SetCPU( Word & 0xff, Address & 0xffff );
				ppu.SetCPU( Word >>> 8, ( Address & 0xffff ) + 1 );
			}
			return;

		case Memory.MAP_DSP:
			if ( o > 0 )
			{
				dsp1.SetDSP ( Word >>> 8, ( Address & 0xffff ) + 1 );
				dsp1.SetDSP ( Word & 0xFF, ( Address & 0xffff ) ); //TODO: & 0xff
			}
			else
			{
				dsp1.SetDSP ( Word & 0xFF, ( Address & 0xffff ) );
				dsp1.SetDSP ( Word >>> 8, ( Address & 0xffff ) + 1 );
			}
			return;

		case Memory.MAP_LOROM_SRAM:
			if ( memory.SRAMMask > 0 )
			{
				if ( memory.SRAMMask >= Memory.MEMMAP_MASK )
				{
					memory.SRAM.put16Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & memory.SRAMMask, Word );
				}
				else
				{
					// no WRITE_WORD here, since if SRAMMask=0x7ff
					// then the high byte doesn't follow the low byte.
					memory.SRAM.put8Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & memory.SRAMMask, Word );
					memory.SRAM.put8Bit( ( ( ( ( Address + 1 ) & 0xFF0000 ) >>> 1 ) | ( ( Address + 1 ) & 0x7FFF ) ) & memory.SRAMMask, Word >>> 8 );
				}

				SRAMModified = true;
			}
			return;

		case Memory.MAP_LOROM_SRAM_B:
			if ( Multi.sramMaskB > 0 )
			{
				if ( Multi.sramMaskB >= Memory.MEMMAP_MASK )
				{
					Multi.sramB.put16Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & Multi.sramMaskB, Word );
				}
				else
				{
					Multi.sramB.put8Bit( ( ( ( Address & 0xFF0000 ) >>> 1 ) | ( Address & 0x7FFF ) ) & Multi.sramMaskB, Word );
					Multi.sramB.put8Bit( ( ( ( ( Address + 1 ) & 0xFF0000 ) >>> 1 ) | ( ( Address + 1 ) & 0x7FFF ) ) & Multi.sramMaskB, Word >>> 8 );
				}

				SRAMModified = true;
			}
			return;

		case Memory.MAP_HIROM_SRAM:
			if ( memory.SRAMMask != 0 )
			{
				if ( memory.SRAMMask >= Memory.MEMMAP_MASK )
				{
					memory.SRAM.put16Bit( ( ( ( Address & 0x7fff ) - 0x6000 + ( ( Address & 0xf0000 ) >>> 3 ) & memory.SRAMMask ) ), Word );
				}
				else
				{
					// no WRITE_WORD here, since if SRAMMask=0x7ff
					// then the high byte doesn't follow the low byte.
					memory.SRAM.put8Bit( ( Address & 0x7fff ) - 0x6000 + ( ( Address & 0xf0000 ) >>> 3 ) & memory.SRAMMask, Word );
					memory.SRAM.put8Bit( ( ( Address + 1 ) & 0x7fff ) - 0x6000 + ( ( ( Address + 1 ) & 0xf0000 ) >>> 3 ) & memory.SRAMMask, Word >>> 8 );
				}
				SRAMModified = true;
			}
			return;
			
		case Memory.MAP_SA1RAM:
			memory.SRAM.put16Bit( ( Address & 0xffff ), Word );
			sa1.Executing = ! sa1.Waiting;
			break;

		case Memory.MAP_BWRAM:
			BWRAM.put16Bit( ( Address & 0x7fff ) - 0x6000, Word );
			SRAMModified = true;
			return;
		/*
		case Memory.MAP_SPC7110_DRAM:
			//TODO: WRITE_WORD(s7r.bank50+(Address & 0xffff), Word);
			break;

		case Memory.MAP_C4:
			if ( o > 0 )
			{
				//TODO: SetC4 ((int) (Word >>> 8), (Address + 1) & 0xffff);
				//TODO: SetC4 (Word & 0xff, Address & 0xffff);
			}
			else
			{
				//TODO: SetC4 (Word & 0xff, Address & 0xffff);
				//TODO: SetC4 ((int) (Word >>> 8), (Address + 1) & 0xffff);
			}
			return;

		case Memory.MAP_OBC_RAM:
			if ( o > 0 )
			{
				//TODO: SetOBC1((int) (Word >>> 8), (Address + 1) & 0xffff);
				//TODO: SetOBC1(Word & 0xff, Address &0xFFFF);
			}
			else
			{
				//TODO: SetOBC1(Word & 0xff, Address &0xFFFF);
				//TODO: SetOBC1 ((int) (Word >>> 8), (Address + 1) & 0xffff);
			}
			return;

		case Memory.MAP_SETA_DSP:
			if ( o > 0 )
			{
				//TODO: SetSetaDSP ((int) (Word >>> 8),(Address + 1));
				//TODO: SetSetaDSP (Word & 0xff, Address);
			}
			else
			{
				//TODO: SetSetaDSP (Word & 0xff, Address);
				//TODO: SetSetaDSP ((int) (Word >>> 8),(Address + 1));
			}
			return;

		case Memory.MAP_SETA_RISC:
			if ( o > 0 )
			{
				//TODO: SetST018 ((int) (Word >>> 8),(Address + 1));
				//TODO: SetST018 (Word & 0xff, Address);
			}
			else
			{
				//TODO: SetST018 (Word & 0xff, Address);
				//TODO: SetST018 ((int) (Word >>> 8),(Address + 1));
			}
			return;

		case Memory.MAP_BSX:
			if ( o > 0 )
			{
				//TODO: SetBSX ((int) (Word >>> 8),(Address + 1));
				//TODO: SetBSX (Word & 0xff, Address);
			}
			else
			{
				//TODO: SetBSX (Word & 0xff, Address);
				//TODO: SetBSX ((int) (Word >>> 8),(Address + 1));
			}
			return;
		case Memory.MAP_DEBUG:
		*/
		default:
		case Memory.MAP_NONE:
			return;
		}
	}
	
	
	private int Absolute()
	{
		return ShiftedDB | Immediate16();
	}
	
	private int AbsoluteRead()
	{
		return ShiftedDB | Immediate16Read();
	}

	private int AbsoluteIndexedIndirect() // (a,X)
	{
		// Get the offset specified by the read
		int work16 = Immediate16SlowRead();

		// Add in the X Register
		work16 += X;
		
		// Address load wraps within the bank
		int work16_2 = GetWord( ShiftedPB | work16, Memory.WRAP_BANK);
		globals.OpenBus = work16_2 >>> 8;
		return work16_2;
	}
	
	private int AbsoluteIndexedIndirectSlowJump() // (a,X)
	{
		int work16 = Immediate16SlowRead();

		AddCycles( SnesSystem.ONE_CYCLE );
		
		// Add in the X Register to get the new address
		work16 += X;
		// Address load wraps within the bank
		int work16_2 = GetWord( ShiftedPB | work16, Memory.WRAP_BANK);
		globals.OpenBus = work16_2 >>> 8;
		
		return work16_2;
	}
	
	private int AbsoluteIndexedIndirectSlowJsr() // (a,X)
	{
		int work16;

		// JSR (a,X) pushes the old address in the middle of loading the new.
		// OpenBus needs to be set to account for this.
		work16 = Immediate8SlowRead();
		globals.OpenBus = PCl();
		
		work16 = work16 | ( Immediate8SlowRead() << 8 );

		AddCycles( SnesSystem.ONE_CYCLE );
		
		// Add in the X Register to get the new address
		work16 += X;
		// Address load wraps within the bank
		int work16_2 = GetWord( ShiftedPB | work16, Memory.WRAP_BANK);
		globals.OpenBus = work16_2 >>> 8;
		return work16_2;
	}
	
	private int AbsoluteIndexedXSlow()  // a,X
	{
		int addr = AbsoluteSlowRead();
		if ( ! CheckIndex() || ( addr & 0xff ) + X_L() >= 0x100 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + X );
	}
	
	private int AbsoluteIndexedXSlowWrite()  // a,X
	{
		int addr = AbsoluteSlow();
		AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + X );
	}
	
	private int AbsoluteIndexedXSlowModify()  // a,X
	{
		int addr = AbsoluteSlowRead();
		AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + X );
	}
	
	private int AbsoluteIndexedXX0() // a,X
	{
		int addr = AbsoluteRead();
		AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + X);
	}
	
	private int AbsoluteIndexedXX1() // a,X
	{
		int addr = AbsoluteRead();
		if ( ( addr & 0xff ) + X_L() >= 0x100 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + X);
	}
	
	private int AbsoluteIndexedXX1Write() // a,X
	{
		int addr = Absolute();
		AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + X);
	}
	
	private int AbsoluteIndexedXX1Modify() // a,X
	{
		int addr = AbsoluteRead();
		AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + X);
	}

	private int AbsoluteIndexedYSlow() // a,Y
	{
		int addr = AbsoluteSlowRead();
		if ( ! CheckIndex() || ( addr & 0xff ) + Y_L() >= 0x100 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + Y);
	}
	
	private int AbsoluteIndexedYSlowWrite() // a,Y
	{
		int addr = AbsoluteSlow();
		AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + Y);
	}
	
	private int AbsoluteIndexedYX0() // a,Y
	{
		int addr = Absolute();
		AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + Y);
	}

	private int AbsoluteIndexedYX0Read() // a,Y
	{
		int addr = AbsoluteRead();
		AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + Y);
	}
	
	private int AbsoluteIndexedYX1Read()// a,Y
	{
		int addr = AbsoluteRead();
		if ( ( addr & 0xff ) + Y_L() >= 0x100 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + Y);
	}
	
	private int AbsoluteIndexedYX1Write()// a,Y
	{
		int addr = Absolute();
		AddCycles( SnesSystem.ONE_CYCLE );
		return (addr + Y);
	}
	
	private int AbsoluteIndirect()
	{
		// No info on wrapping, but it doesn't matter anyway due to mirroring
		int uint16addr2 = GetWord( Immediate16Read() );
		globals.OpenBus = uint16addr2 >>> 8;
		return uint16addr2;
	}
	
	private int AbsoluteIndirectLong()  // [a]
	{
		int uint16addr = Immediate16Read();

		// No info on wrapping, but it doesn't matter anyway due to mirroring
		int addr2 = GetWord( uint16addr );
		globals.OpenBus = addr2 >>> 8;
		addr2 |= ( globals.OpenBus = GetByte( uint16addr + 2 ) ) << 16;
		return addr2;
	}
	
	private int AbsoluteIndirectLongSlow () // [a]
	{
		int uint16addr = Immediate16SlowRead();

		// No info on wrapping, but it doesn't matter anyway due to mirroring
		int addr2 = GetWord( uint16addr );
		globals.OpenBus = addr2 >>> 8;
		addr2 |= ( globals.OpenBus = GetByte( uint16addr + 2 ) ) << 16;
		return addr2;
	}

	private int AbsoluteIndirectSlow()
	{
		// No info on wrapping, but it doesn't matter anyway due to mirroring
		int uint16addr2 = GetWord( Immediate16SlowRead() );
		globals.OpenBus = uint16addr2 >>> 8;
		return uint16addr2;
	}

	private int AbsoluteLong() // l
	{
		int addr = PCBase.get32Bit( PCw ) & 0x00ffffff;
		
		AddCycles( MemSpeedx2 + MemSpeed );

		IncrPC(3);
		return addr;
	}

	private int AbsoluteLongIndexedX () // l,X
	{
		return AbsoluteLong() + X;
	}

	private int AbsoluteLongIndexedXRead() // l,X
	{
		return AbsoluteLongRead() + X;
	}

	private int AbsoluteLongIndexedXSlow() // l,X
	{
		return (AbsoluteLongSlow() + X);
	}

	private int AbsoluteLongIndexedXSlowRead() // l,X
	{
		return (AbsoluteLongSlowRead() + X);
	}

	private int AbsoluteLongRead() // l
	{
		int addr = PCBase.get32Bit( PCw ) & 0x00ffffff;
		
		AddCycles( MemSpeedx2 + MemSpeed );
		globals.OpenBus = addr >>> 16;
		IncrPC(3);
		return addr;
	}

	private int AbsoluteLongSlow() // l
	{
		int addr = Immediate16Slow();
		addr |= Immediate8Slow() << 16;
		return addr;
	}

	private int AbsoluteLongSlowRead() // l
	{
		int addr = Immediate16SlowRead();

		addr |= Immediate8SlowRead() << 16;
		return addr;
	}

	private int AbsoluteSlow ()
	{
		return ShiftedDB | Immediate16Slow();
	}

	private int AbsoluteSlowRead()
	{
		return ShiftedDB | Immediate16SlowRead();
	}

	// ADC ( Add With Carry ) 16Bit Version
	private void ADC16 ( int Work16 )
	{
		// If Decimal FLAG is on
		if ( CheckDecimal () )
		{
			int A1 = A & 0x000F;
			int A2 = A & 0x00F0;
			int A3 = A & 0x0F00;
			int A4 = A & 0xF000;
			int W1 = Work16 & 0x000F;
			int W2 = Work16 & 0x00F0;
			int W3 = Work16 & 0x0F00;
			int W4 = Work16 & 0xF000;

			A1 += W1 + _Carry;
			if ( A1 > 0x0009 )
			{
				A1 -= 0x000A;
				A1 &= 0x000F;
				A2 += 0x0010;
			}

			A2 += W2;
			if ( A2 > 0x0090 )
			{
				A2 -= 0x00A0;
				A2 &= 0x00F0;
				A3 += 0x0100;
			}

			A3 += W3;
			if ( A3 > 0x0900 )
			{
				A3 -= 0x0A00;
				A3 &= 0x0F00;
				A4 += 0x1000;
			}

			A4 += W4;
			if ( A4 > 0x9000 )
			{
				A4 -= 0xA000;
				A4 &= 0xF000;
				SetCarry();
			}
			else
			{
				ClearCarry();
			}

			int Ans16 = A4 | A3 | A2 | A1;

			// if the result leave the 16th bit on?
			if ( ( ( ~( A ^ Work16 ) & ( Work16 ^ Ans16 ) ) & 0x8000 ) != 0 )
			{
				SetOverflow();
			}
			else
			{
				ClearOverflow();
			}
			A_W( Ans16 );
			SetZN16( A );

		}
		else
		{
			int Ans32 = A + Work16 + _Carry;

			// If our result gives us a value greater than the 17th bit on Set the Carry flag
			_Carry = Ans32 >= 0x10000 ? 1 : 0;

			if ( ( ~( A ^ Work16 ) & ( Work16 ^ ( Ans32 & 0xFFFF ) ) & 0x8000 ) != 0 )
			{
				SetOverflow();
			}
			else
			{
				ClearOverflow();
			}

			A_W( Ans32 );
			SetZN16( A );
		}
	}

	private void ADC8 ( int Work8 )
	{
		// If Decimal flag is on
		if ( CheckDecimal() )
		{

			int A1 = A & 0x0F;
			int A2 = A & 0xF0;
			int W1 = Work8 & 0x0F;
			int W2 = Work8 & 0xF0;

			A1 += W1 + _Carry;
			if ( A1 > 0x09 )
			{
				A1 -= 0x0A;
				A1 &= 0x0F;
				A2 += 0x10;
			}

			A2 += W2;
			if ( A2 > 0x90 )
			{
				A2 -= 0xA0;
				A2 &= 0xF0;
				SetCarry();
			}
			else
			{
				ClearCarry();
			}

			int Ans8 = A2 | A1;

			if ( ( ~( A_L() ^ Work8 ) & ( Work8 ^ Ans8 ) & 0x80 ) != 0 )
			{
				SetOverflow();
			}
			else
			{
				ClearOverflow();
			}

			A_L( Ans8 );
			SetZN8 ( A_L() );

		}
		else
		{
			int AL = A_L();
			int Ans16 = AL + Work8 + _Carry;

			_Carry = Ans16 >= 0x100 ? 1 : 0;
			
			// If result of add pushes byte past 127, set overflow
			if ( ( (~( AL ^ Work8 )) & ( Work8 ^ ( Ans16 & 0xFF ) ) & 0x80 ) != 0 )
			{
				SetOverflow();
			}
			else
			{
				ClearOverflow();
			}

			A_L( Ans16 );
			SetZN8( A_L() );
		}
	}

	private void AddCycles( int value )
	{
		Cycles += value;
	}

	private void AND16( int Work16 )
	{
		A &= Work16;
		SetZN16( A );
	}

	private void AND8( int Work8 )
	{
		A_L( A & Work8); 
		SetZN8( A_L() );
	}

	private void ASL16( int OpAddress, int w )
	{
		int Work16 = GetWord( OpAddress, w );
		_Carry = ( Work16 & 0x8000 ) >>> 15;
		Work16 = (Work16 << 1) & 0xFFFF;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( Work16, OpAddress, w, Memory.WRITE_10 );
		globals.OpenBus = ( Work16 & 0xff );
		SetZN16( Work16 );
	}
	
	private void ASL8( int OpAddress )
	{
		int Work8 = GetByte( OpAddress );
		_Carry = ( Work8 & 0x80 ) >>> 7;
		Work8 = (Work8 << 1) & 0xFF;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Work8, OpAddress );
		globals.OpenBus = Work8;
		SetZN8( Work8 );
	}
	
	private void BIT16( int Work16 )
	{
		_Overflow = ( Work16 & 0x4000 ) >>> 14;
		_Negative = ( Work16 >>> 8 ) & 0xFF;
		_Zero = ( Work16 & A ) != 0 ? 1 : 0;
	}
	
	private void BIT8( int Work8 )
	{
		_Overflow = ( Work8 & 0x40 ) >>> 6;
		_Negative = Work8;
		_Zero = Work8 & A_L();
	}
	
	final boolean CheckCarry()
	{
		return _Carry != 0;
	}
	
	final boolean CheckDecimal()
	{
		return ( PL & Decimal ) != 0;
	}
	
	final boolean CheckEmulation()
	{
		return PH != 0;
	}

	final boolean CheckFlag( int f )
	{
		return ( PL & f ) != 0;
	}

	final boolean CheckIndex()
	{
		return ( PL & IndexFlag ) != 0;
	}

	final boolean CheckIRQ()
	{
		return ( PL & IRQ ) != 0;
	}

	final boolean CheckMemory()
	{
		return ( PL & MemoryFlag ) != 0;
	}

	final boolean CheckNegative()
	{
		return ( _Negative & 0x80 ) != 0;
	}

	final boolean CheckOverflow()
	{
		return _Overflow != 0;
	}

	// Flag management
	final boolean CheckZero()
	{
		return _Zero == 0;
	}

	final void ClearCarry()
	{
		_Carry = 0;
	}
	
	final void ClearOverflow()
	{
		_Overflow = 0;
	}

	private void CMP16( int val )
	{
		int Int32 = A - val;
		_Carry = Int32 >= 0 ? 1 : 0;
		SetZN16( Int32 & 0xFFFF );
	}

	private void CMP8( int val )
	{
		int Int16 = A_L() - val;
		_Carry = Int16 >= 0 ? 1 : 0;
		SetZN8( Int16 & 0xFF );
	}

	void CPUShutdown()
	{
		if ( globals.settings.Shutdown && PBPC() == WaitAddress )
		{
			if ( WaitCounter == 0 && ( Flags & ( ( 1 << 11 ) | ( 1 << 7 ) ) ) == 0 )
			{
				WaitAddress = 0xffffffff;
				
				Cycles = NextEvent;
				CPUExecuting = 0;
				globals.apu.APUExecute();
				CPUExecuting = 1;
			}
			else if ( WaitCounter >= 2 )
			{
				WaitCounter = 1;
			}
			else
			{
				WaitCounter--;
			}
		}
	}

	private void CPX16( int val )
	{
		int Int32 = X - val;
		_Carry = Int32 >= 0 ? 1 : 0;
		SetZN16( Int32 & 0xFFFF );
	}

	private void CPX8 ( int val )
	{
		int Int16 = X_L() - val;
		_Carry = Int16 >= 0 ? 1 : 0;
		SetZN8( Int16 & 0xFF );
	}

	private void CPY16( int val )
	{
		int Int32 = Y - val;
		_Carry = Int32 >= 0 ? 1 : 0;
		SetZN16( Int32 & 0xFFFF );
	}

	private void CPY8( int val )
	{
		int Int16 = Y_L() - val;
		_Carry = Int16 >= 0 ? 1 : 0;
		SetZN8( Int16 & 0xFF );
	}

	private void DEC16( int OpAddress, int w )
	{
		WaitAddress = 0xffffffff;

		int Work16 = GetWord( OpAddress, w ) - 1;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( Work16, OpAddress, w, Memory.WRITE_10 );
		globals.OpenBus = Work16 & 0xff;
		SetZN16( Work16 );
	}

	private void DEC8( int OpAddress )
	{
		WaitAddress = 0xffffffff;

		int Work8 = GetByte( OpAddress ) - 1;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Work8, OpAddress );
		globals.OpenBus = Work8;
		SetZN8( Work8 );
	}

	private int Direct()  // d
	{
		int uint16 = Immediate8() + D;
		if ( D_L() != 0 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return uint16;
	}
	
	private int DirectRead()  // d
	{
		int uint16 = Immediate8Read() + D;
		if ( D_L() != 0 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return uint16;
	}

	private int DirectIndexedIndirectE0 ()  // (d,X)
	{
		int addr = GetWord( DirectIndexedXE0() );
		return ShiftedDB | addr;
	}
	
	private int DirectIndexedIndirectE0Read( )  // (d,X)
	{
		int addr = GetWord( DirectIndexedXE0Read() );
		globals.OpenBus = ( addr >>> 8 ) & 0xFF;
		return ShiftedDB | addr;
	}

	private int DirectIndexedIndirectE1()  // (d,X)
	{
		int addr = GetWord( DirectIndexedXE1Read(), D_L() != 0 ? Memory.WRAP_BANK : Memory.WRAP_PAGE );

		return ShiftedDB | addr;
	}
	
	private int DirectIndexedIndirectE1Read()  // (d,X)
	{
		int addr = GetWord( DirectIndexedXE1(), D_L() != 0 ? Memory.WRAP_BANK : Memory.WRAP_PAGE );
		globals.OpenBus = ( addr >>> 8 ) & 0xFF;

		return ShiftedDB | addr;
	}

	private int DirectIndexedIndirectSlow ()  // (d,X)
	{
		int addr = GetWord( DirectIndexedXSlow(),
			( CheckEmulation() == false || D_L() != 0 ) ? Memory.WRAP_BANK : Memory.WRAP_PAGE );

		return ShiftedDB | addr;
	}
	
	private int DirectIndexedIndirectSlowRead()  // (d,X)
	{
		int addr = GetWord( DirectIndexedXSlowRead(),
			( CheckEmulation() == false || D_L() != 0 ) ? Memory.WRAP_BANK : Memory.WRAP_PAGE );
		globals.OpenBus = ( addr >>> 8 ) & 0xFF;
		
		return ShiftedDB | addr;
	}

	private int DirectIndexedXE0()  // d,X
	{
		int addr16 = Direct() + X;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr16;
	}

	private int DirectIndexedXE0Read()  // d,X
	{
		int addr16 = DirectRead() + X;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr16;
	}

	private int DirectIndexedXE1 ()  // d,X
	{
		if ( D_L() != 0)
		{
			return DirectIndexedXE0();
		}
		else
		{
			int addr = Direct();
			addr = addr & 0xFF00 | ( ( X + addr ) & 0xFF );
			AddCycles( SnesSystem.ONE_CYCLE );
			return addr;
		}
	}

	private int DirectIndexedXE1Read()  // d,X
	{
		if ( D_L() != 0)
		{
			return DirectIndexedXE0Read();
		}
		else
		{
			int addr = DirectRead();
			addr = addr & 0xFF00 | ( ( X + addr ) & 0xFF );
			AddCycles( SnesSystem.ONE_CYCLE );
			return addr;
		}
	}

	private int DirectIndexedXSlow()  // d,X
	{
		int addr = DirectSlow();
		
		if ( CheckEmulation() == false || D_L() != 0 )
		{
			addr += X;
		}
		else
		{
			addr = addr & 0xFF00 | ( ( X + addr ) & 0xFF ); 
		}
		AddCycles( SnesSystem.ONE_CYCLE );
		
		return addr;
	}

	private int DirectIndexedXSlowRead()  // d,X
	{
		int addr = DirectSlowRead();
		
		if ( CheckEmulation() == false || D_L() != 0 )
		{
			addr += X;
		}
		else
		{
			addr = addr & 0xFF00 | ( ( X + addr ) & 0xFF ); 
		}
		AddCycles( SnesSystem.ONE_CYCLE );
		
		return addr;
	}

	private int DirectIndexedYE0()  // d,Y
	{
		int addr = ( Direct() + Y ) & 0xFFFF;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}

	private int DirectIndexedYE0Read()  // d,Y
	{
		int addr = ( DirectRead() + Y ) & 0xFFFF;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}

	private int DirectIndexedYE1 ()  // d,Y
	{
		if ( D_L() != 0 )
		{
			return DirectIndexedYE0();
		}
		else
		{
			int addr = Direct();
			addr = addr & 0xFF00 | ( ( Y + addr ) & 0xFF );
			AddCycles( SnesSystem.ONE_CYCLE );
			return addr;
		}
	}

	private int DirectIndexedYE1Read()  // d,Y
	{
		if ( D_L() != 0 )
		{
			return DirectIndexedYE0Read();
		}
		else
		{
			int addr = DirectRead();
			addr = addr & 0xFF00 | ( ( Y + addr ) & 0xFF );
			AddCycles( SnesSystem.ONE_CYCLE );
			return addr;
		}
	}

	private int DirectIndexedYSlow()  // d,Y
	{
		int addr = DirectSlow();
		
		if ( CheckEmulation() == false || D_L() != 0 )
		{
			addr += Y;
		}
		else
		{
			addr = addr & 0xFF00 | ( ( Y + addr ) & 0xFF );
		}
		AddCycles( SnesSystem.ONE_CYCLE );
		
		return addr;
	}

	private int DirectIndexedYSlowRead()  // d,Y
	{
		int addr = DirectSlowRead();
		
		if ( CheckEmulation() == false || D_L() != 0 )
		{
			addr += Y;
		}
		else
		{
			addr = addr & 0xFF00 | ( ( Y + addr ) & 0xFF );
		}
		AddCycles( SnesSystem.ONE_CYCLE );
		
		return addr;
	}

	private int DirectIndirectE0()  // (d)
	{
		int addr = GetWord( Direct() );
		addr |= ShiftedDB;
		return addr;
	}
	
	private int DirectIndirectE0Read()  // (d)
	{
		int addr = GetWord( DirectRead() );
		addr |= ShiftedDB;
		return addr;
	}

	private int DirectIndirectE1()  // (d)
	{
		int addr = GetWord( DirectSlow(), D_L() != 0 ? Memory.WRAP_BANK : Memory.WRAP_PAGE );
		addr |= ShiftedDB;
		return addr;
	}

	private int DirectIndirectE1Read()  // (d)
	{
		int addr = GetWord( DirectSlowRead(),
							   D_L() != 0 ? Memory.WRAP_BANK : Memory.WRAP_PAGE );
		globals.OpenBus = ( addr >>> 8 ) & 0xFF;
		addr |= ShiftedDB;
		return addr;
	}

	private int DirectIndirectIndexedE0X0() // (d),Y
	{
		int addr = DirectIndirectE0();
		AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + Y );
	}
	
	private int DirectIndirectIndexedE0X0Read() // (d),Y
	{
		int addr = DirectIndirectE0Read();
		AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + Y );
	}

	private int DirectIndirectIndexedE0X1() // (d),Y
	{
		int addr = DirectIndirectE0();
		AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + Y );
	}
	
	private int DirectIndirectIndexedE0X1Read() // (d),Y
	{
		int addr = DirectIndirectE0Read();
		if ( ( addr & 0xff ) + Y_L() >= 0x100 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + Y );
	}

	private int DirectIndirectIndexedE1Read() // (d),Y
	{
		int addr = DirectIndirectE1Read();
		if ( ( addr & 0xff ) + Y_L() >= 0x100 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + Y );
	}
	
	private int DirectIndirectIndexedE1Write() // (d),Y
	{
		int addr = DirectIndirectE1();
		AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + Y );
	}

	private int DirectIndirectIndexedLong() // [d],Y
	{
		return DirectIndirectLong() + Y;
	}
	
	private int DirectIndirectIndexedLongRead() // [d],Y
	{
		return DirectIndirectLongRead() + Y;
	}

	private int DirectIndirectIndexedLongSlow() // [d],Y
	{
		return DirectIndirectLongSlow() + Y;
	}
	
	private int DirectIndirectIndexedLongSlowRead() // [d],Y
	{
		return DirectIndirectLongSlowRead() + Y;
	}

	private int DirectIndirectIndexedSlowRead() // (d),Y
	{
		int addr = DirectIndirectSlowRead();
		if ( ! CheckIndex() || ( addr & 0xff ) + Y_L() >= 0x100 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + Y );
	}
	
	private int DirectIndirectIndexedSlowWrite() // (d),Y
	{
		int addr = DirectIndirectSlow();
		AddCycles( SnesSystem.ONE_CYCLE );
		return ( addr + Y );
	}

	private int DirectIndirectLong()  // [d]
	{
		int addr = Direct();
		int addr2 = GetWord( addr );
		globals.OpenBus = addr2 >>> 8;
		addr2 |= ( globals.OpenBus = GetByte( addr + 2 ) ) << 16;
		return addr2;
	}
	
	private int DirectIndirectLongRead()  // [d]
	{
		int addr = DirectRead();
		int addr2 = GetWord( addr );
		globals.OpenBus = addr2 >>> 8;
		addr2 |= ( globals.OpenBus = GetByte( addr + 2 ) ) << 16;
		return addr2;
	}

	private int DirectIndirectLongSlow()  // [d]
	{
		int addr = DirectSlow();
		int addr2 = GetWord( addr );
		globals.OpenBus = addr2 >>> 8;
		addr2 |= ( globals.OpenBus = GetByte( addr + 2 ) ) << 16;
		return addr2;
	}
	
	private int DirectIndirectLongSlowRead()  // [d]
	{
		int addr = DirectSlowRead();
		int addr2 = GetWord( addr );
		globals.OpenBus = addr2 >>> 8;
		addr2 |= ( globals.OpenBus = GetByte( addr + 2 ) ) << 16;
		return addr2;
	}

	private int DirectIndirectSlow()  // (d)
	{
		int addr = GetWord( DirectSlow(),
			( CheckEmulation() == false || D_L() != 0 ) ? Memory.WRAP_BANK : Memory.WRAP_PAGE );
		addr |= ShiftedDB;
		return addr;
	}

	private int DirectIndirectSlowRead()  // (d)
	{
		int addr = GetWord( DirectSlowRead(),
							   ( CheckEmulation() == false || D_L() != 0 ) ? Memory.WRAP_BANK : Memory.WRAP_PAGE );
		globals.OpenBus = ( addr >>> 8 ) & 0xFF;
		addr |= ShiftedDB;
		return addr;
	}

	private int DirectSlow()   // d
	{
		int uint16  = Immediate8Slow() + D;
		if ( D_L() != 0 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return uint16;
	}

	private int DirectSlowRead()   // d
	{
		int uint16  = Immediate8SlowRead() + D;
		if ( D_L() != 0 )
			AddCycles( SnesSystem.ONE_CYCLE );
		return uint16;
	}

	private void EOR16( int val )
	{
		A ^= val;
		SetZN16( A );
	}

	private void EOR8 ( int val )
	{
		A_L( A ^ val );
		SetZN8( A_L() );
	}

	void FixCycles() {}
	
	private int Immediate16()
 	{
 		
 		int uint16 = PCBase.get16Bit( PCw );
 		
 		AddCycles( MemSpeedx2 );
 		
 		// Increment the Program Counter
 		Incr16BitPC();
 		
 		return uint16;
 	}

	private int Immediate16Read()
 	{
 		int uint16 = PCBase.get16Bit( PCw );
 		
 		globals.OpenBus = uint16 >>> 8;
 		
 		AddCycles( MemSpeedx2 );
 		
 		// Increment the Program Counter
 		Incr16BitPC();
 		
 		return uint16;
 	}
	
	int Immediate16Slow ()
	{
		int uint16 = GetWord( PBPC() , Memory.WRAP_BANK );
		Incr16BitPC();
		return uint16;
	}

	int Immediate16SlowRead()
	{
		int uint16 = GetWord( PBPC() , Memory.WRAP_BANK );
		globals.OpenBus = uint16 >>> 8;
		Incr16BitPC();
		return uint16;
	}

	private int Immediate8()
 	{
 		int uint8 = PCBase.get8Bit( PCw );
 
 		AddCycles( MemSpeed );
 		
 		// Increment the Program Counter
 		Incr8BitPC();
 		
 		return uint8;
 	}

	private int Immediate8Read ()
 	{
 		int uint8 = PCBase.get8Bit( PCw );
 		
 		globals.OpenBus = uint8;
 		
 		AddCycles( MemSpeed );
 		
 		// Increment the Program Counter
 		Incr8BitPC();
 		
 		return uint8;
 	}

	private int Immediate8Slow()
	{
		int uint8 = GetByte( PBPC() );
		Incr8BitPC();
		return uint8;
	}

	private int Immediate8SlowRead()
	{
		int uint8 = GetByte( PBPC() );
		globals.OpenBus = uint8;
		Incr8BitPC();
		return uint8;
	}
	
	private void INC16( int OpAddress, int w )
	{
		WaitAddress = 0xffffffff;

		int Work16 = ( GetWord( OpAddress, w ) + 1 ) & 0xFFFF;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( Work16, OpAddress, w, Memory.WRITE_10 );
		globals.OpenBus = Work16 & 0xff;
		SetZN16( Work16 );
	}

	private void INC8( int OpAddress )
	{
		WaitAddress = 0xffffffff;

		int Work8 = ( GetByte( OpAddress ) + 1 ) & 0xFF;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Work8, OpAddress );
		globals.OpenBus = Work8;
		SetZN8( Work8 );
	}
	
	//	 Increment the Program Counter by 16bits
	private void Incr16BitPC() { PCw += 2; }

	// Increment the Program Counter by 8bits
	final void Incr8BitPC() { PCw++; }
	
	final private void IncrPC(int inc) { PCw += inc; }

	private void LDA16( int val )
	{
		A_W( val );
		SetZN16( A );
	}
	
	private void LDA8 ( int val )
	{
		A_L( val );
		SetZN8( A_L() );
	}

	private void LDX16( int val )
	{
		X_W( val );
		SetZN16( X );
	}
	
	private void LDX8 ( int val )
	{
		X_L( val );
		SetZN8( X_L() );
	}

	private void LDY16( int val )
	{
		Y = val;
		SetZN16( Y );
	}
	
	private void LDY8 ( int val )
	{
		Y_L( val );
		SetZN8( Y_L() );
	}

	private void LSR16 ( int OpAddress, int w )
	{
		int Work16 = GetWord( OpAddress, w );
		_Carry = Work16 & 1;
		Work16 >>= 1;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( Work16, OpAddress, w, Memory.WRITE_10 );
		globals.OpenBus = ( Work16 & 0xff );
		SetZN16( Work16 );
	}
	
	private void LSR8 ( int OpAddress )
	{
		int Work8 = GetByte( OpAddress );
		_Carry = Work8 & 1;
		Work8 >>= 1;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Work8, OpAddress );
		globals.OpenBus = Work8;
		SetZN8( Work8 );
	}

	private void Op00()
	{
		BRKTriggered = true;
		AddCycles( MemSpeed );

		int addr;
		
		if ( ! CheckEmulation() )
		{
			SetByte( PB(), S-- );
			SetWord( PCw + 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
			S -= 2;
			PackStatus();
			SetByte( PL, S-- );
			globals.OpenBus = PL;
			PL = ( PL & 0xF7 ) | 4;

			addr = GetWord( 0xFFE6 );
		}
		else
		{
			S_L( S - 1);
			SetWord( PCw + 1, S, Memory.WRAP_PAGE, Memory.WRITE_10 );
			S_L( S - 1);
			PackStatus();
			SetByte( PL, S );
			S_L( S - 1);
			globals.OpenBus = PL;
			PL = ( PL & 0xF7 ) | 4;

			addr = GetWord( 0xFFFE );
		}
		
		SetPCBase( addr );
		globals.OpenBus = addr >>> 8;
	}
	
	private void Op01E0M0()
	{
		int val = GetWord( DirectIndexedIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}

	private void Op01E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE0Read() );
		ORA8( val );
	}
	
	private void Op01E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE1Read() );
		ORA8( val );
	}

	private void Op01Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedIndirectSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}

	private void Op02()
	{
		AddCycles( MemSpeed );

		int addr;
		
		if ( ! CheckEmulation() )
		{
			SetByte( PB(), S-- );
			SetWord( PCw + 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
			S -= 2;
			PackStatus();
			SetByte( PL, S-- );
			globals.OpenBus = PL;
			PL = ( PL & 0xF7 ) | 4;
			
			addr = GetWord( 0xFFE4 );
		}
		else
		{			
			S_L( S - 1);
			SetWord( PCw + 1, S, Memory.WRAP_PAGE, Memory.WRITE_10 );
			S_L( S - 1);
			PackStatus();
			SetByte( PL, S );
			S_L( S - 1);
			globals.OpenBus = PL;

			PL = ( PL & 0xF7 ) | 4;
			
			addr = GetWord( 0xFFF4 );
		}
		SetPCBase( addr );
		globals.OpenBus = addr >>> 8;
	}

	private void Op03M0()
	{
		int val = GetWord( StackRelativeRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}

	private void Op03M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeRead() );
		ORA8( val );
	}

	private void Op03Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( StackRelativeSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}

	private void Op04M0()
	{
		TSB16( DirectRead(), Memory.WRAP_BANK );
	}

	private void Op04M1()
	{
		TSB8( DirectRead() );
	}

	private void Op04Slow()
	{
		if ( CheckMemory() )
		{
			TSB8( DirectSlowRead() );
		}
		else
		{
			TSB16( DirectSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void Op05M0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}
	
	private void Op05M1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		ORA8( val );
	}
	
	private void Op05Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}
	
	private void Op06M0()
	{
		ASL16 ( DirectRead(), Memory.WRAP_BANK );
	}

	private void Op06M1()
	{
		ASL8 ( DirectRead() );
	}
	
	private void Op06Slow()
	{
		if ( CheckMemory() )
		{
			ASL8 ( DirectSlowRead() );
		}
		else
		{
			ASL16 ( DirectSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void Op07M0()
	{
		int val = GetWord( DirectIndirectLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}
	
	private void Op07M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectLongRead() );
		ORA8( val );
	}

	private void Op07Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectLongSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}
	
	private void Op08E0()
	{
		PackStatus();
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( PL, S-- );
		globals.OpenBus = PL;
	}

	private void Op08E1()
	{
		PackStatus();
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( PL, S );
		S_L( S - 1);
		globals.OpenBus = PL;
	}
	
	private void Op08Slow()
	{
		PackStatus();
		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( CheckEmulation() )
		{
			SetByte( PL, S );
			S_L( S - 1);
		}
		else
		{
			SetByte( PL, S-- );
		}
		globals.OpenBus = PL;
	}

	private void Op09M0()
	{
		A |= Immediate16Read();
		SetZN16( A );
	}

	private void Op09M1()
	{
		A_L( A | Immediate8Read() );
		SetZN8( A_L() );
	}

	private void Op09Slow()
	{
		if ( CheckMemory() )
		{
			A_L( A | Immediate8SlowRead() );
			SetZN8( A_L() );
		}
		else
		{
			A |= Immediate16SlowRead();
			SetZN16( A );
		}
	}

	private void Op0AM0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		_Carry = ( A & 0x8000 ) != 0 ? 1 : 0;
		A_W(A << 1);
		SetZN16( A );
	}
	
	private void Op0AM1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		_Carry = ( A & 0x80 ) >>> 7;
		A_L( A << 1);
		SetZN8( A_L() );
	}
	
	private void Op0ASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckMemory() )
		{
			_Carry = ( A & 0x80 ) >>> 7;
			A_L( A << 1 );
			SetZN8( A_L() );
		}
		else
		{
			_Carry = ( A & 0x80 ) >>> 7;
			A_W( A << 1);
			SetZN16( A );
		}
	}
	
	private void Op0BE0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( D, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = D_L();
	}
	
	private void Op0BE1()
	{

		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( D, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = D_L();
		S_H(1);
	}

	private void Op0BSlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( D, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = D_L();
		if ( CheckEmulation() )
			S_H(1);
	}
	
	private void Op0CM0()
	{
		TSB16( AbsoluteRead(), Memory.WRAP_BANK );
	}
	
	private void Op0CM1()
	{
		TSB8( AbsoluteRead() );
	}

	private void Op0CSlow()
	{
		if ( CheckMemory() )
		{
			TSB8( AbsoluteSlowRead() );
		}
		else
		{
			TSB16( AbsoluteSlowRead(), Memory.WRAP_BANK );
		}
	}
	
	private void Op0DM0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}
	
	private void Op0DM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		ORA8( val );
	}

	private void Op0DSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}
	
	private void Op0EM0()
	{
		ASL16 ( AbsoluteRead(), Memory.WRAP_NONE );
	}

	private void Op0EM1()
	{
		ASL8 ( AbsoluteRead() );
	}
	
	private void Op0ESlow()
	{
		if ( CheckMemory() )
		{
			ASL8 ( AbsoluteSlowRead() );
		}
		else
		{
			ASL16 ( AbsoluteSlowRead(), Memory.WRAP_NONE );
		}
	}

	private void Op0FM0()
	{
		int val = GetWord( AbsoluteLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}
	
	private void Op0FM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongRead() );
		ORA8( val );
	}

	private void Op0FSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}
	
	private void Op10E0()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 1 ) return;
			if ( globals.settings.SoundSkipMethod == 3 ) if ( PCw > newPC ) return;
				else PCw = newPC;
		}

		if ( ! CheckNegative() )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op10E1()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 1 )
				return;
			
			if ( globals.settings.SoundSkipMethod == 3 )if ( PCw > newPC ) return;
				else PCw =  newPC;
		}

		if ( ! CheckNegative() )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			if ( PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}
	
	private void Op10Slow()
	{
		int newPC = RelativeSlow();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;

			else if ( globals.settings.SoundSkipMethod == 1 ) return;
			if ( globals.settings.SoundSkipMethod == 3 ) if ( PCw > newPC ) return;
				else PCw = newPC;
		}

		if ( ! CheckNegative() )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			if ( CheckEmulation() && PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op11E0M0X0()
	{
		int val = GetWord( DirectIndirectIndexedE0X0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}
	
	private void Op11E0M0X1()
	{
		int val = GetWord( DirectIndirectIndexedE0X1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}

	private void Op11E0M1X0()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X0Read() );
		ORA8( val );
	}
	
	private void Op11E0M1X1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X1Read() );
		ORA8( val );
	}

	private void Op11E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE1Read() );
		ORA8( val );
	}

	private void Op11Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}

	private void Op12E0M0()
	{
		int val = GetWord( DirectIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}
	
	private void Op12E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE0Read() );
		ORA8( val );
	}
	
	private void Op12E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE1Read() );
		ORA8( val );
	}

	private void Op12Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}

	private void Op13M0()
	{
		int val = GetWord( StackRelativeIndirectIndexedRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}

	private void Op13M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedRead() );
		ORA8( val );
	}

	private void Op13Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( StackRelativeIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}

	private void Op14M0()
	{
		TRB16( DirectRead(), Memory.WRAP_BANK );
	}

	private void Op14M1()
	{
		TRB8( DirectRead() );
	}
	
	private void Op14Slow()
	{
		if ( CheckMemory() )
		{
			TRB8( DirectSlowRead() );
		}
		else
		{
			TRB16( DirectSlowRead(), Memory.WRAP_BANK );
		}
	}
	
	private void Op15E0M0()
	{
		int val = GetWord( DirectIndexedXE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}

	private void Op15E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE0Read() );
		ORA8( val );
	}

	private void Op15E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE1Read() );
		ORA8( val );
	}
	
	private void Op15Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedXSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}

	private void Op16E0M0()
	{
		ASL16 ( DirectIndexedXE0Read(), Memory.WRAP_BANK );
	}

	private void Op16E0M1()
	{
		ASL8 ( DirectIndexedXE0Read() );
	}

	private void Op16E1()
	{
		ASL8 ( DirectIndexedXE1Read() );
	}

	private void Op16Slow()
	{
		if ( CheckMemory() )
		{
			ASL8 ( DirectIndexedXSlowRead() );
		}
		else
		{
			ASL16 ( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void Op17M0()
	{
		int val = GetWord( DirectIndirectIndexedLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}
	
	private void Op17M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongRead() );
		ORA8( val );
	}

	private void Op17Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}

	private void Op18()
	{
		_Carry = 0;
		AddCycles( SnesSystem.ONE_CYCLE );
	}

	private void Op19M0X0()
	{
		int val = GetWord( AbsoluteIndexedYX0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}
	
	private void Op19M0X1()
	{
		int val = GetWord( AbsoluteIndexedYX1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}

	private void Op19M1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX0Read() );
		ORA8( val );
	}
	
	private void Op19M1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX1Read() );
		ORA8( val );
	}

	private void Op19Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedYSlow() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedYSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}

	private void Op1AM0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		A_W( A + 1 );
		SetZN16( A );
	}
	
	private void Op1AM1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		A_L( A + 1 );
		SetZN8( A_L() );
	}

	private void Op1ASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		if ( CheckMemory() )
		{
			A_L( A + 1 );
			SetZN8( A_L() );
		}
		else
		{
			A_W( A + 1 );
			SetZN16( A );
		}
	}

	private void Op1B()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		S = A;
		if ( CheckEmulation() )
			S_H(1);
	}

	private void Op1CM0()
	{
		TRB16( AbsoluteRead(), Memory.WRAP_BANK );
	}

	private void Op1CM1()
	{
		TRB8( AbsoluteRead() );
	}
	
	private void Op1CSlow()
	{
		if ( CheckMemory() )
		{
			TRB8( AbsoluteSlowRead() );
		}
		else
		{
			TRB16( AbsoluteSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void Op1DM0X0()
	{
		int val = GetWord( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}

	private void Op1DM0X1()
	{
		int val = GetWord( AbsoluteIndexedXX1(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}

	private void Op1DM1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX0() );
		ORA8( val );
	}

	private void Op1DM1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX1() );
		ORA8( val );
	}
	
	private void Op1DSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedXSlow() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedXSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}

	private void Op1EM0X0()
	{
		ASL16 ( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
	}

	private void Op1EM0X1()
	{
		ASL16 ( AbsoluteIndexedXX1Modify(), Memory.WRAP_NONE );
	}

	private void Op1EM1X0()
	{
		ASL8 ( AbsoluteIndexedXX0() );
	}

	private void Op1EM1X1()
	{
		ASL8 ( AbsoluteIndexedXX1Modify() );
	}

	private void Op1ESlow()
	{
		if ( CheckMemory() )
		{
			ASL8 ( AbsoluteIndexedXSlowModify() );
		}
		else
		{
			ASL16 ( AbsoluteIndexedXSlowModify(), Memory.WRAP_NONE );
		}
	}

	private void Op1FM0()
	{
		int val = GetWord( AbsoluteLongIndexedXRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ORA16( val );
	}

	private void Op1FM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXRead() );
		ORA8( val );
	}
	
	private void Op1FSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXSlowRead() );
			ORA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongIndexedXSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ORA16( val );
		}
	}
	
	private void Op20E0()
	{
		int addr = Absolute() & 0xFFFF;
		AddCycles( SnesSystem.ONE_CYCLE + 0);
		SetWord( PCw - 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		SetPCBase( ShiftedPB + addr );
	}

	private void Op20E1()
	{
		int addr = Absolute() & 0xFFFF;
		AddCycles( SnesSystem.ONE_CYCLE );
		S_L( S - 1);
		SetWord( PCw - 1, S, Memory.WRAP_PAGE, Memory.WRITE_10 );
		S_L( S - 1);
		SetPCBase( ShiftedPB + addr );
	}
	
	private void Op20Slow()
	{
		int addr = AbsoluteSlow() & 0xFFFF;
		
		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( CheckEmulation() )
		{
			S_L( S - 1);
			SetWord( PCw - 1, S, Memory.WRAP_PAGE, Memory.WRITE_10 );
			S_L( S - 1);
		}
		else
		{
			SetWord( PCw - 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
			S -= 2;
		}
		SetPCBase( ShiftedPB + addr );
	}

	private void Op21E0M0()
	{
		int val = GetWord( DirectIndexedIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op21E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE0Read() );
		AND8( val );
	}

	private void Op21E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE1Read() );
		AND8( val );
	}

	private void Op21Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedIndirectSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}

	private void Op22E0()
	{
		int addr = AbsoluteLong();
		SetByte( PB(), S-- );
		SetWord( PCw - 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		SetPCBase( addr );
	}
	
	private void Op22E1()
	{
		int addr = AbsoluteLong();
		SetByte( PB(), S-- );
		SetWord( PCw - 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		S_H( 1 );
		SetPCBase( addr );
	}
	
	private void Op22Slow()
	{
		int addr = AbsoluteLongSlow();
		
		// JSR l pushes the old bank in the middle of loading the new.
		// globals.OpenBus needs to be set to account for this.
		globals.OpenBus = PB();
		
		SetByte( PB(), S-- );
		SetWord( PCw - 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		if ( CheckEmulation() )
			S_H(1);
		SetPCBase( addr );
	}

	private void Op23M0()
	{
		int val = GetWord( StackRelativeRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}
	
	private void Op23M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeRead() );
		AND8( val );
	}
	
	private void Op23Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( StackRelativeSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}
	
	private void Op24M0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		BIT16( val );
	}

	private void Op24M1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		BIT8( val );
	}
	
	private void Op24Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			BIT8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			BIT16( val );
		}
	}
	
	private void Op25M0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op25M1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		AND8( val );
	}

	private void Op25Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}
	
	private void Op26M0()
	{
		ROL16( DirectRead(), Memory.WRAP_BANK );
	}

	private void Op26M1()
	{
		ROL8( DirectRead() );
	}

	private void Op26Slow()
	{
		if ( CheckMemory() )
		{
			ROL8( DirectSlowRead() );
		}
		else
		{
			ROL16( DirectSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void Op27M0()
	{
		int val = GetWord( DirectIndirectLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}
	
	private void Op27M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectLongRead() );
		AND8( val );
	}
	
	private void Op27Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectLongSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}
	
	private void Op28E0()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		PL = GetByte( ++S );
		globals.OpenBus = PL;
		UnpackStatus();
		if ( CheckIndex() )
		{
			X_H(0);
			Y_H(0);
		}
		FixCycles();

	}
	
	private void Op28E1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		S_L( S + 1 );
		PL = GetByte( S );
		globals.OpenBus = PL;
		PL |= ( IndexFlag | MemoryFlag );
		UnpackStatus();
		FixCycles();

	}
	
	private void Op28Slow()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		if ( CheckEmulation() )
		{
			S_L( S + 1 );
			PL = GetByte( S );
			globals.OpenBus = PL;
			PL |= ( IndexFlag | MemoryFlag );
		}
		else
		{
			PL = GetByte( ++S );
			globals.OpenBus = PL;
		}
		UnpackStatus();
		if ( CheckIndex() )
		{
			X_H(0);
			Y_H(0);
		}
		FixCycles();

	}

	private void Op29M0()
	{
		A &= Immediate16Read();
		SetZN16( A );
	}
	
	private void Op29M1()
	{
		A_L( A & Immediate8Read() );
		SetZN8( A_L() );
	}
	
	private void Op29Slow()
	{
		if ( CheckMemory() )
		{
			A_L( A & Immediate8SlowRead() );
			SetZN8( A_L() );
		}
		else
		{
			A &= Immediate16SlowRead();
			SetZN16( A );
		}
	}

	private void Op2AM0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		int w = ( A << 1 ) | _Carry;
		_Carry = w >= 0x10000 ? 1 : 0;
		A_W( w );
		SetZN16( A );
	}
	
	private void Op2AM1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		int w = ( A_L() << 1 ) | _Carry;
		_Carry = w >= 0x100 ? 1 : 0;
		A_L( w );
		SetZN8( A_L() );
	}
	
	private void Op2ASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( CheckMemory() )
		{
			int w = ( ( A_L() ) << 1 ) | _Carry;
			_Carry = w >= 0x100 ? 1 : 0;
			A_L( w );
			SetZN8( A_L() );
		}
		else
		{
			int w = ( A << 1 ) | _Carry;
			_Carry = w >= 0x10000 ? 1 : 0;
			A_W( w );
			SetZN16( A );
		}
	}

	private void Op2BE0()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		D = GetWord( S + 1, Memory.WRAP_BANK );
		S_W( S + 2 );
		SetZN16( D );
		globals.OpenBus = D_H();
	}
	
	private void Op2BE1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		D = GetWord( S + 1, Memory.WRAP_BANK );
		S_W( S + 2 );
		SetZN16( D );
		globals.OpenBus = D_H();
		S_H(1);
	}
	
	private void Op2BSlow()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		D = GetWord( S + 1, Memory.WRAP_BANK );
		S_W( S + 2 );
		SetZN16( D );
		globals.OpenBus = D_H();
		if ( CheckEmulation() ) S_H(1);
	}

	private void Op2CM0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		BIT16( val );
	}
	
	private void Op2CM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		BIT8( val );
	}
	
	private void Op2CSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			BIT8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			BIT16( val );
		}
	}
	
	private void Op2DM0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}
	
	private void Op2DM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		AND8( val );
	}

	private void Op2DSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}
	
	private void Op2EM0()
	{
		ROL16( AbsoluteRead(), Memory.WRAP_NONE );
	}
	
	private void Op2EM1()
	{
		ROL8( AbsoluteRead() );
	}
	
	private void Op2ESlow()
	{
		if ( CheckMemory() )
		{
			ROL8( AbsoluteSlowRead() );
		}
		else
		{
			ROL16( AbsoluteSlowRead(), Memory.WRAP_NONE );
		}
	}
	
	private void Op2FM0()
	{
		int val = GetWord( AbsoluteLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op2FM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongRead() );
		AND8( val );
	}
	
	private void Op2FSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}

	private void Op30E0()
	{
		int newPC = Relative();
		if ( BranchSkip )
		{
			BranchSkip = false;
			
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
			{
				return;
			}
			else if ( globals.settings.SoundSkipMethod == 1 )
			{
				return;
			}
			else if ( globals.settings.SoundSkipMethod == 3 && PCw > newPC )
			{
				return;
			}
			else
			{
				PCw = newPC;
			}
		}

		if ( CheckNegative() )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op30E1()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 1 )
			{
				return;
			}
			else if ( globals.settings.SoundSkipMethod == 3 && PCw > newPC )
			{
				return;
			}
			else
			{
				PCw = newPC;
			}
		}

		if ( CheckNegative() )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}
	
	private void Op30Slow()
	{
		int newPC = RelativeSlow();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 1 ) 
			{
				return;
			}
			else if ( globals.settings.SoundSkipMethod == 3 )
			{
				if ( PCw > newPC ) return;
			}
			else
			{
				PCw = newPC;
			}
		}

		if ( CheckNegative() )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( CheckEmulation() && PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}
	
	private void Op31E0M0X0()
	{
		int val = GetWord( DirectIndirectIndexedE0X0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op31E0M0X1()
	{
		int val = GetWord( DirectIndirectIndexedE0X1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op31E0M1X0()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X0Read() );
		AND8( val );
	}
	
	private void Op31E0M1X1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X1Read() );
		AND8( val );
	}

	private void Op31E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE1Read() );
		AND8( val );
	}

	private void Op31Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}

	private void Op32E0M0()
	{
		int val = GetWord( DirectIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op32E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE0Read() );
		AND8( val );
	}

	private void Op32E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE1Read() );
		AND8( val );
	}

	private void Op32Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}

	private void Op33M0()
	{
		int val = GetWord( StackRelativeIndirectIndexedRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op33M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedRead() );
		AND8( val );
	}

	private void Op33Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( StackRelativeIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}

	private void Op34E0M0()
	{
		int val = GetWord( DirectIndexedXE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		BIT16( val );
	}

	private void Op34E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE0Read() );
		BIT8( val );
	}

	private void Op34E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE1Read() );
		BIT8( val );
	}

	private void Op34Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedXSlowRead() );
			BIT8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			BIT16( val );
		}
	}

	private void Op35E0M0()
	{
		int val = GetWord( DirectIndexedXE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op35E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE0Read() );
		AND8( val );
	}

	private void Op35E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE1Read() );
		AND8( val );
	}

	private void Op35Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedXSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}
	
	private void Op36E0M0()
	{
		ROL16( DirectIndexedXE0Read(), Memory.WRAP_BANK );
	}
	
	private void Op36E0M1()
	{
		ROL8( DirectIndexedXE0Read() );
	}
	
	private void Op36E1()
	{
		ROL8( DirectIndexedXE1Read() );
	}
	
	private void Op36Slow()
	{
		if ( CheckMemory() )
		{
			ROL8( DirectIndexedXSlowRead() );
		}
		else
		{
			ROL16( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void Op37M0()
	{
		int val = GetWord( DirectIndirectIndexedLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op37M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongRead() );
		AND8( val );
	}

	private void Op37Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}

	private void Op38()
	{
		_Carry = 1;
		AddCycles( SnesSystem.ONE_CYCLE );
	}

	private void Op39M0X0()
	{
		int val = GetWord( AbsoluteIndexedYX0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op39M0X1()
	{
		int val = GetWord( AbsoluteIndexedYX1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op39M1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX0Read() );
		AND8( val );
	}

	private void Op39M1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX1Read() );
		AND8( val );
	}

	private void Op39Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedYSlow() );
			AND8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedYSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}

	private void Op3AM0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		A--;
		SetZN16( A );
	}

	private void Op3AM1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		A_L( A - 1);
		SetZN8( A_L() );
	}

	private void Op3ASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		if ( CheckMemory() )
		{
			A_L( A_L() - 1 );
			SetZN8( A_L() );
		}
		else
		{
			A--;
			SetZN16( A );
		}
	}

	private void Op3B()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		A = S;
		SetZN16( A );
	}

	private void Op3CM0X0()
	{
		int val = GetWord( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		BIT16( val );
	}

	private void Op3CM0X1()
	{
		int val = GetWord( AbsoluteIndexedXX1(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		BIT16( val );
	}

	private void Op3CM1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX0() );
		BIT8( val );
	}

	private void Op3CM1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX1() );
		BIT8( val );
	}

	private void Op3CSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedXSlow() );
			BIT8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedXSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			BIT16( val );
		}
	}

	private void Op3DM0X0()
	{
		int val = GetWord( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op3DM0X1()
	{
		int val = GetWord( AbsoluteIndexedXX1(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}

	private void Op3DM1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX0() );
		AND8( val );
	}

	private void Op3DM1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX1() );
		AND8( val );
	}
	
	private void Op3DSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedXSlow() );
			AND8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedXSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}

	private void Op3EM0X0()
	{
		ROL16( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
	}

	private void Op3EM0X1()
	{
		ROL16( AbsoluteIndexedXX1Modify(), Memory.WRAP_NONE );
	}
	
	private void Op3EM1X0()
	{
		ROL8( AbsoluteIndexedXX0() );
	}
	
	private void Op3EM1X1()
	{
		ROL8( AbsoluteIndexedXX1Modify() );
	}
	
	private void Op3ESlow()
	{
		if ( CheckMemory() )
		{
			ROL8( AbsoluteIndexedXSlowModify() );
		}
		else
		{
			ROL16( AbsoluteIndexedXSlowModify(), Memory.WRAP_NONE );
		}
	}

	private void Op3FM0()
	{
		int val = GetWord( AbsoluteLongIndexedXRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		AND16( val );
	}
	
	private void Op3FM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXRead() );
		AND8( val );
	}
	
	private void Op3FSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXSlowRead() );
			AND8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongIndexedXSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			AND16( val );
		}
	}
	
	private void Op40Slow()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		if ( ! CheckEmulation() )
		{
			PL = GetByte( ++S );
			UnpackStatus();
			PCw = GetWord( S + 1, Memory.WRAP_BANK );
			S += 2;
			PB( GetByte( ++S ) );
			globals.OpenBus = PB();
			ShiftedPB = PB() << 16;
		}
		else
		{
			S_L( S + 1 );
			PL = GetByte( S );
			UnpackStatus();
			S_L( S + 1 );
			PCw = GetWord( S, Memory.WRAP_PAGE );
			S_L( S + 1 );
			globals.OpenBus = PCh();
			PL |= ( IndexFlag | MemoryFlag );
		}
		
		SetPCBase( PBPC() );

		if ( CheckIndex() )
		{
			X_H(0);
			Y_H(0);
		}
		FixCycles();

	}

	private void Op41E0M0()
	{
		int val = GetWord( DirectIndexedIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}
	
	private void Op41E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE0Read() );
		EOR8( val );
	}
	
	private void Op41E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE1Read() );
		EOR8( val );
	}
	
	private void Op41Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedIndirectSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op42()
	{
		GetWord( PBPC() );
		PCw++;
	}
	
	private void Op43M0()
	{
		int val = GetWord( StackRelativeRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}
	
	private void Op43M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeRead() );
		EOR8( val );
	}

	private void Op43Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( StackRelativeSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}
	
	private void Op44Slow()
	{
		int SrcBank;

		DB = Immediate8Slow();
		ShiftedDB = DB << 16;
		globals.OpenBus = SrcBank = Immediate8Slow();

		SetByte( globals.OpenBus = GetByte( ( SrcBank << 16 ) + X ), ShiftedDB + Y );

		if ( CheckIndex() )
		{
			X_L( X - 1 );
			Y_L( Y - 1 );
		}
		else
		{
			X--;
			Y--;
		}
		A--;
		if ( A != 0xffff )
			PCw = PCw - 3;

		AddCycles( SnesSystem.TWO_CYCLES );
	}
	
	private void Op44X0()
	{
		int SrcBank;

		DB = Immediate8();
		ShiftedDB = DB << 16;
		globals.OpenBus = SrcBank = Immediate8();

		SetByte( globals.OpenBus = GetByte( ( SrcBank << 16 ) + X ), ShiftedDB + Y );

		X--;
		Y--;
		A--;
		if ( A != 0xffff )
			PCw = PCw - 3;

		AddCycles( SnesSystem.TWO_CYCLES );
	}

	private void Op44X1()
	{
		int SrcBank;

		DB = Immediate8();
		ShiftedDB = DB << 16;
		globals.OpenBus = SrcBank = Immediate8();

		SetByte( globals.OpenBus = GetByte( ( SrcBank << 16 ) + X ), ShiftedDB + Y );

		X_L( X - 1 );
		Y_L( Y - 1 );
		A--;
		if ( A != 0xffff )
			PCw = PCw - 3;

		AddCycles( SnesSystem.TWO_CYCLES );
	}
	
	private void Op45M0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}
	
	private void Op45M1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		EOR8( val );
	}

	private void Op45Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}
	
	private void Op46M0()
	{
		LSR16 ( DirectRead(), Memory.WRAP_BANK );
	}

	private void Op46M1()
	{
		LSR8 ( DirectRead() );
	}

	private void Op46Slow()
	{
		if ( CheckMemory() )
		{
			LSR8 ( DirectSlowRead() );
		}
		else
		{
			LSR16 ( DirectSlowRead(), Memory.WRAP_BANK );
		}
	}
	
	private void Op47M0()
	{
		int val = GetWord( DirectIndirectLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op47M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectLongRead() );
		EOR8( val );
	}

	private void Op47Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectLongSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}
	
	private void Op48E0M0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( A, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = A_L();
	}
	
	private void Op48E0M1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		int AL = A_L();
		SetByte( AL, S-- );
		globals.OpenBus = AL;
	}

	private void Op48E1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		int AL = A_L();
		SetByte( AL, S );
		S_L( S - 1 );
		globals.OpenBus = AL;
	}

	private void Op48Slow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		
		int AL = A_L();
		if ( CheckEmulation() )
		{
			SetByte( AL, S );
			S_L( S - 1);
		}
		else if ( CheckMemory() )
		{
			SetByte( AL, S-- );
		}
		else
		{
			SetWord( A, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
			S -= 2;
		}
		globals.OpenBus = AL;
	}

	private void Op49M0()
	{
		A ^= Immediate16Read();
		SetZN16( A );
	}

	private void Op49M1()
	{
		A_L( A ^ Immediate8Read() );
		SetZN8( A_L() );
	}

	private void Op49Slow()
	{
		if ( CheckMemory() )
		{
			A_L( A ^ Immediate8SlowRead() );
			SetZN8( A_L() );
		}
		else
		{
			A ^= Immediate16SlowRead();
			SetZN16( A );
		}
	}

	private void Op4AM0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		_Carry = A & 1;
		A = A >> 1;
		SetZN16( A );
	}

	private void Op4AM1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		_Carry = A & 1;
		A_L( A_L() >>> 1);
		SetZN8( A_L() );
	}

	private void Op4ASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckMemory() )
		{
			_Carry = A & 1;
			A_L( A_L() >>> 1);
			SetZN8( A_L() );
		}
		else
		{
			_Carry = A & 1;
			A >>= 1;
			SetZN16( A );
		}
	}

	private void Op4BE0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( PB(), S-- );
		globals.OpenBus = PB();
	}

	private void Op4BE1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( PB(), S );
		S_L( S - 1);
		globals.OpenBus = PB();
	}

	private void Op4BSlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckEmulation() )
		{
			SetByte( PB(), S );
			S_L( S - 1);
		}
		else
		{
			SetByte( PB(), S-- );
		}
		globals.OpenBus = PB();
	}

	private void Op4C()
	{
		SetPCBase( ShiftedPB + ( AbsoluteRead() & 0xFFFF ) );
	}

	private void Op4CSlow()
	{
		SetPCBase( ShiftedPB + ( AbsoluteSlow() & 0xFFFF ) );
	}

	private void Op4DM0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op4DM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		EOR8( val );
	}

	private void Op4DSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op4EM0()
	{
		LSR16 ( AbsoluteRead(), Memory.WRAP_NONE );
	}

	private void Op4EM1()
	{
		LSR8 ( AbsoluteRead() );
	}

	private void Op4ESlow()
	{
		if ( CheckMemory() )
		{
			LSR8 ( AbsoluteSlowRead() );
		}
		else
		{
			LSR16 ( AbsoluteSlowRead(), Memory.WRAP_NONE );
		}
	}

	private void Op4FM0()
	{
		int val = GetWord( AbsoluteLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op4FM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongRead() );
		EOR8( val );
	}

	private void Op4FSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op50E0()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Overflow == 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );

			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op50E1()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Overflow == 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			if ( PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op50Slow()
	{
		int newPC = RelativeSlow();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Overflow == 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			if ( CheckEmulation() && PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op51E0M0X0()
	{
		int val = GetWord( DirectIndirectIndexedE0X0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op51E0M0X1()
	{
		int val = GetWord( DirectIndirectIndexedE0X1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op51E0M1X0()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X0Read() );
		EOR8( val );
	}
	
	private void Op51E0M1X1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X1Read() );
		EOR8( val );
	}

	private void Op51E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE1Read() );
		EOR8( val );
	}

	private void Op51Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op52E0M0()
	{
		int val = GetWord( DirectIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op52E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE0Read() );
		EOR8( val );
	}

	private void Op52E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE1Read() );
		EOR8( val );
	}

	private void Op52Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op53M0()
	{
		int val = GetWord( StackRelativeIndirectIndexedRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op53M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedRead() );
		EOR8( val );
	}

	private void Op53Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( StackRelativeIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op54Slow()
	{
		int SrcBank;

		globals.OpenBus = DB = Immediate8Slow();
		ShiftedDB = DB << 16;
		globals.OpenBus = SrcBank = Immediate8Slow();

		SetByte( globals.OpenBus = GetByte( ( SrcBank << 16 ) + X ),
					ShiftedDB + Y );

		if ( CheckIndex() )
		{
			X_L( X + 1 );
			Y_L( Y + 1 );
		}
		else
		{
			X_W( X + 1 );
			Y_W( Y + 1 );
		}
		A_W( A - 1 );

		if ( A != 0xffff )
			PCw = PCw - 3;

		AddCycles( SnesSystem.TWO_CYCLES );
	}

	private void Op54X0()
	{
		int SrcBank;

		DB = Immediate8();
		ShiftedDB = DB << 16;
		globals.OpenBus = SrcBank = Immediate8();

		SetByte( globals.OpenBus = GetByte( ( SrcBank << 16 ) + X ), ShiftedDB + Y );

		X_W( X + 1 );
		Y_W( Y + 1 );
		A_W( A - 1);
		if ( A != 0xffff )
			PCw = PCw - 3;

		AddCycles( SnesSystem.TWO_CYCLES );
	}

	private void Op54X1()
	{
		int SrcBank;

		DB = Immediate8();
		ShiftedDB = DB << 16;
		globals.OpenBus = SrcBank = Immediate8();

		SetByte( globals.OpenBus = GetByte( ( SrcBank << 16 ) + X ),
					ShiftedDB + Y );

		X_L( X + 1 );
		Y_L( Y + 1 );
		A--;
		if ( A != 0xffff )
			PCw = PCw - 3;

		AddCycles( SnesSystem.TWO_CYCLES );
	}

	private void Op55E0M0()
	{
		int val = GetWord( DirectIndexedXE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op55E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE0Read() );
		EOR8( val );
	}

	private void Op55E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE1Read() );
		EOR8( val );
	}

	private void Op55Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedXSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op56E0M0()
	{
		LSR16 ( DirectIndexedXE0Read(), Memory.WRAP_BANK );
	}

	private void Op56E0M1()
	{
		LSR8 ( DirectIndexedXE0Read() );
	}

	private void Op56E1()
	{
		LSR8 ( DirectIndexedXE1Read() );
	}

	private void Op56Slow()
	{
		if ( CheckMemory() )
		{
			LSR8 ( DirectIndexedXSlowRead() );
		}
		else
		{
			LSR16 ( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void Op57M0()
	{
		int val = GetWord( DirectIndirectIndexedLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op57M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongRead() );
		EOR8( val );
	}

	private void Op57Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op58()
	{
		PL = PL & 0xFB ;
		AddCycles( SnesSystem.ONE_CYCLE );
	}

	private void Op59M0X0()
	{
		int val = GetWord( AbsoluteIndexedYX0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op59M0X1()
	{
		int val = GetWord( AbsoluteIndexedYX1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op59M1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX0Read() );
		EOR8( val );
	}

	private void Op59M1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX1Read() );
		EOR8( val );
	}

	private void Op59Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedYSlow() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedYSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op5AE0X0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( Y, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = Y_L();
	}
	
	private void Op5AE0X1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Y_L(), S-- );
		globals.OpenBus = Y_L();
	}
	
	private void Op5AE1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Y_L(), S );
		S_L( S - 1);
		globals.OpenBus = Y_L();
	}

	private void Op5ASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckEmulation() )
		{
			SetByte( Y_L(), S );
			S_L( S - 1);
		}
		else if ( CheckIndex() )
		{
			SetByte( Y_L(), S-- );
		}
		else
		{
			SetWord( Y, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
			S -= 2;
		}
		globals.OpenBus = Y_L();
	}

	private void Op5B()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		D = A;
		SetZN16( D );
	}

	private void Op5C()
	{
		SetPCBase( AbsoluteLong() );
	}

	private void Op5CSlow()
	{
		SetPCBase( AbsoluteLongSlow() );
	}

	private void Op5DM0X0()
	{
		int val = GetWord( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op5DM0X1()
	{
		int val = GetWord( AbsoluteIndexedXX1(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op5DM1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX0() );
		EOR8( val );
	}

	private void Op5DM1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX1() );
		EOR8( val );
	}

	private void Op5DSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedXSlow() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedXSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op5EM0X0()
	{
		LSR16 ( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
	}

	private void Op5EM0X1()
	{
		LSR16 ( AbsoluteIndexedXX1Modify(), Memory.WRAP_NONE );
	}

	private void Op5EM1X0()
	{
		LSR8 ( AbsoluteIndexedXX0() );
	}

	private void Op5EM1X1()
	{
		LSR8 ( AbsoluteIndexedXX1Modify() );
	}

	private void Op5ESlow()
	{
		if ( CheckMemory() )
		{
			LSR8 ( AbsoluteIndexedXSlowModify() );
		}
		else
		{
			LSR16 ( AbsoluteIndexedXSlowModify(), Memory.WRAP_NONE );
		}
	}

	private void Op5FM0()
	{
		int val = GetWord( AbsoluteLongIndexedXRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		EOR16( val );
	}

	private void Op5FM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXRead() );
		EOR8( val );
	}

	private void Op5FSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXSlowRead() );
			EOR8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongIndexedXSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			EOR16( val );
		}
	}

	private void Op60E0()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		PCw = GetWord( S + 1, Memory.WRAP_BANK );
		S += 2;
		AddCycles( SnesSystem.ONE_CYCLE );
		PCw++;
		SetPCBase( PBPC() );
	}

	private void Op60E1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		S_L( S + 1 );
		PCw = GetWord( S, Memory.WRAP_PAGE );
		S_L( S + 1 );
		AddCycles( SnesSystem.ONE_CYCLE );
		PCw++;
		SetPCBase( PBPC() );
	}

	private void Op60Slow()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		
		if ( CheckEmulation() )
		{
			S_L( S + 1 );
			PCw = GetWord( S, Memory.WRAP_PAGE );
			S_L( S + 1 );
		}
		else
		{
			PCw = GetWord( S + 1, Memory.WRAP_BANK );
			S += 2;
		}
		AddCycles( SnesSystem.ONE_CYCLE );
		PCw++;
		
		SetPCBase( PBPC() );
	}

	private void Op61E0M0()
	{
		int value16 = GetWord( DirectIndexedIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8;
		ADC16( value16 );
	}

	private void Op61E0M1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndexedIndirectE0Read() );
		ADC8( value8 );
	}

	// 0x61
	private void Op61E1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndexedIndirectE1Read() );
		ADC8( value8 );
	}

	private void Op61Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedIndirectSlowRead() );
			ADC8(val);
		}
		else
		{
			int val = GetWord( DirectIndexedIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ADC16(val);
		}
	}

	private void Op62E0()
	{
		int val = RelativeLong();
		SetWord( val, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = val & 0xff;
	}

	private void Op62E1()
	{
		int val = RelativeLong();
		SetWord( val, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = val & 0xff;
		S_H(1);
	}

	private void Op62Slow()
	{
		int val = RelativeLongSlow();
		SetWord( val, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = val & 0xff;
		if ( CheckEmulation() )
			S_H(1);
	}

	private void Op63M0()
	{
		int val = GetWord( StackRelativeRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ADC16( val );
	}

	private void Op63M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeRead() );
		ADC8( val );
	}

	private void Op63Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeSlowRead() );
			ADC8( val );
		}
		else
		{
			int val = GetWord( StackRelativeSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ADC16( val );
		}
	}

	private void Op64M0()
	{
		STZ16( Direct(), Memory.WRAP_BANK );
	}

	private void Op64M1()
	{
		STZ8( Direct() );
	}

	private void Op64Slow()
	{
		if ( CheckMemory() )
		{
			STZ8( DirectSlow() );
		}
		else
		{
			STZ16( DirectSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op65M0()
	{
		int value16 = GetWord( DirectRead() , Memory.WRAP_BANK );
		globals.OpenBus = ( value16 >>> 8 );
		ADC16( value16 );
	}

	// 0x65
	private void Op65M1()
	{
		int value8 = globals.OpenBus = GetByte( DirectRead() );
		ADC8( value8 );
	}

	private void Op65Slow()
	{
		if ( CheckMemory())
		{
			int value8 = globals.OpenBus = GetByte( DirectSlowRead() );
			ADC8( value8 );
		}
		else
		{
			int value16 = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = value16 >>> 8;
			ADC16( value16 );		
		}
	}

	private void Op66M0()
	{
		ROR16 ( DirectRead(), Memory.WRAP_BANK );
	}

	private void Op66M1()
	{
		ROR8 ( DirectRead() );
	}

	private void Op66Slow()
	{
		if ( CheckMemory() )
		{
			ROR8 ( DirectSlowRead() );
		}
		else
		{
			ROR16 ( DirectSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void Op67M0()
	{
		int value16 = GetWord( DirectIndirectLongRead(), Memory.WRAP_NONE );
		globals.OpenBus =  value16 >>> 8;
		ADC16( value16 );
	}

	// 0x67
	private void Op67M1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndirectLongRead() );
		ADC8( value8 );
	}

	private void Op67Slow()
	{
		if ( CheckMemory() )
		{
			int value8 = globals.OpenBus = GetByte( DirectIndirectLongSlowRead() );
			ADC8( value8 );
		}
		else
		{
			int value16 = GetWord( DirectIndirectLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = value16 >>> 8;
			ADC16( value16 );
		}
	}

	private void Op68E0M0()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		A = GetWord( S + 1, Memory.WRAP_BANK );
		S += 2;
		SetZN16( A );
		globals.OpenBus = A_H();
	}

	private void Op68E0M1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		A_L( GetByte( ++S ) );
		SetZN8( A_L() );
		globals.OpenBus = A_L();
	}

	private void Op68E1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		S_L( S + 1 );
		A_L( GetByte( S ) );
		SetZN8( A_L() );
		globals.OpenBus = A_L();
	}

	private void Op68Slow()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		if ( CheckEmulation() )
		{
			S_L( S + 1 );
			A_L( GetByte( S ) );
			SetZN8( A_L() );
			globals.OpenBus = A_L();
		}
		else if ( CheckMemory() )
		{
			A_L( GetByte( ++S ) );
			SetZN8( A_L() );
			globals.OpenBus = A_L();
		}
		else
		{
			A = GetWord( S + 1, Memory.WRAP_BANK );
			S_W( S + 2 );
			SetZN16( A );
			globals.OpenBus = A_H();
		}
	}

	private void Op69M0()
	{
		ADC16( Immediate16Read() );
	}

	private void Op69M1()
	{
		ADC8( Immediate8Read() );
	}
	
	private void Op69Slow()
	{
		if ( CheckMemory() )
		{
			ADC8( Immediate8SlowRead() );
		}
		else
		{
			ADC16( Immediate16SlowRead() );
		}
	}

	private void Op6AM0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		int w = A | ( _Carry << 16 );
		_Carry = w & 1;
		w = w >>> 1;
		A_W( w );
		SetZN16( A );
	}

	private void Op6AM1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		int w =  A | ( _Carry << 8 );
		_Carry = w & 1;
		w = w >>> 1;
		A_L(w);
		SetZN8( A_L() );
	}

	private void Op6ASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( CheckMemory() )
		{
			int w = A | ( _Carry << 8 );
			_Carry = w & 1;
			w = w >>> 1;
			A_L(w);
			SetZN8( A_L() );
		}
		else
		{
			int w = A | ( _Carry << 16 );
			_Carry = w & 1;
			w = w >>> 1;
			A = w;
			SetZN16( A );
		}
	}

	private void Op6BE0()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		PCw = GetWord( S + 1, Memory.WRAP_BANK );
		S += 2;
		PB( GetByte( ++S ) );
		PCw++;
		SetPCBase( PBPC() );
	}

	private void Op6BE1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		PCw = GetWord( S + 1, Memory.WRAP_BANK );
		S += 2;
		PB( GetByte( ++S ) );
		S_H(1);
		PCw++;
		SetPCBase( PBPC() );
	}

	private void Op6BSlow()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		PCw = GetWord( S + 1, Memory.WRAP_BANK );
		S += 2;
		PB( GetByte( ++S ) );
		if ( CheckEmulation() )
			S_H(1);
		PCw = PCw + 1;
		SetPCBase( PBPC() );
	}

	private void Op6C()
	{
		SetPCBase( ShiftedPB + ( AbsoluteIndirect() & 0xFFFF ) );
	}

	private void Op6CSlow()
	{
		SetPCBase( ShiftedPB + ( AbsoluteIndirectSlow() & 0xFFFF ) );
	}

	private void Op6DM0()
	{
		int value16 = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8 ;
		ADC16( value16 );
	}

	private void Op6DM1()
	{
		int value8 = globals.OpenBus = GetByte( AbsoluteRead() );
		ADC8( value8 );
	}

	private void Op6DSlow()
	{
		if ( CheckMemory() )
		{
			int value8 = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			ADC8( value8 );
		}
		else
		{
			int value16 = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = value16 >>> 8;
			ADC16( value16 );
		}
	}

	private void Op6EM0()
	{
		ROR16 ( AbsoluteRead(), Memory.WRAP_NONE );
	}

	private void Op6EM1()
	{
		ROR8 ( AbsoluteRead() );
	}

	private void Op6ESlow()
	{
		if ( CheckMemory() )
		{
			ROR8 ( AbsoluteSlowRead() );
		}
		else
		{
			ROR16 ( AbsoluteSlowRead(), Memory.WRAP_NONE );
		}
	}

	private void Op6FM0()
	{
		int val = GetWord( AbsoluteLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ADC16( val );
	}

	private void Op6FM1()
	{
		int val8 = globals.OpenBus = GetByte( AbsoluteLongRead() );
		ADC8( val8 );
	}

	private void Op6FSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongSlowRead() );
			ADC8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ADC16( val );
		}
	}

	private void Op70E0()
	{
		int newPC = Relative();

		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Overflow != 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );

			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op70E1()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Overflow != 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op70Slow()
	{
		int newPC;
		newPC = RelativeSlow();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Overflow != 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( CheckEmulation() && PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op71E0M0X0()
	{
		int value16 = GetWord( DirectIndirectIndexedE0X0Read(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8;
		ADC16( value16 );
	}

	private void Op71E0M0X1()
	{
		int value16 = GetWord( DirectIndirectIndexedE0X1Read(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8;
		ADC16( value16 );
	}

	private void Op71E0M1X0()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndirectIndexedE0X0Read() );
		ADC8( value8 );
	}

	private void Op71E0M1X1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndirectIndexedE0X1Read() );
		ADC8( value8 );
	}

	// 0x71
	private void Op71E1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndirectIndexedE1Read() );
		ADC8( value8 );
	}

	private void Op71Slow()
	{
		if ( CheckMemory() )
		{
			int value8 = globals.OpenBus = GetByte( DirectIndirectIndexedSlowRead() );
			ADC8( value8 );
		}
		else
		{
			int value16 = GetWord( DirectIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = value16 >>> 8;
			ADC16( value16 );
		}
	}

	private void Op72E0M0()
	{
		int value16 = GetWord( DirectIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8 ;
		ADC16( value16 );
	}

	private void Op72E0M1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndirectE0Read() );
		ADC8( value8 );
	}

	// 0x72
	private void Op72E1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndirectE1Read() );
		ADC8( value8 );
	}

	private void Op72Slow()
	{
		if ( CheckMemory() )
		{
			int value8 = globals.OpenBus = GetByte( DirectIndirectSlowRead() );
			ADC8( value8 );
		}
		else
		{
			int value16 = GetWord( DirectIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = value16 >>> 8;
			ADC16( value16 );
		}
	}

	private void Op73M0()
	{
		int val = GetWord( StackRelativeIndirectIndexedRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ADC16( val );
	}

	private void Op73M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedRead() );
		ADC8( val );
	}

	private void Op73Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedSlowRead() );
			ADC8( val );
		}
		else
		{
			int val = GetWord( StackRelativeIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ADC16( val );
		}
	}

	private void Op74E0M0()
	{
		STZ16( DirectIndexedXE0(), Memory.WRAP_BANK );
	}

	private void Op74E0M1()
	{
		STZ8( DirectIndexedXE0() );
	}

	private void Op74E1()
	{
		STZ8( DirectIndexedXE1() );
	}

	private void Op74Slow()
	{
		if ( CheckMemory() )
		{
			STZ8( DirectIndexedXSlow() );
		}
		else
		{
			STZ16( DirectIndexedXSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op75E0M0()
	{
		int value16 = GetWord( DirectIndexedXE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = value16 >>> 8;
		ADC16( value16 );
	}

	private void Op75E0M1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndexedXE0Read() );
		ADC8( value8 );
	}

	// 0x75
	private void Op75E1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndexedXE1Read() );
		ADC8( value8 );
	}

	private void Op75Slow()
	{
		if ( CheckMemory() )
		{
			int value8 = globals.OpenBus = GetByte( DirectIndexedXSlowRead() );
			ADC8( value8 );
		}
		else
		{
			int value16 = GetWord( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = value16 >>> 8;
			ADC16( value16 );
		}
	}

	private void Op76E0M0()
	{
		ROR16 ( DirectIndexedXE0Read(), Memory.WRAP_BANK );
	}

	private void Op76E0M1()
	{
		ROR8 ( DirectIndexedXE0Read() );
	}

	private void Op76E1()
	{
		ROR8 ( DirectIndexedXE1Read() );
	}

	private void Op76Slow()
	{
		if ( CheckMemory() )
		{
			ROR8 ( DirectIndexedXSlowRead() );
		}
		else
		{
			ROR16 ( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void Op77M0()
	{
		int value16 = GetWord( DirectIndirectIndexedLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8;
		ADC16( value16 );
	}

	private void Op77M1()
	{
		int value8 = globals.OpenBus = GetByte( DirectIndirectIndexedLongRead() );
		ADC8( value8 );
	}

	private void Op77Slow()
	{
		if ( CheckMemory() )
		{
			int value8 = globals.OpenBus = GetByte( DirectIndirectIndexedLongSlowRead() );
			ADC8( value8 );
		}
		else
		{
			int value16 = GetWord( DirectIndirectIndexedLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = value16 >>> 8;
			ADC16( value16 );
		}
	}

	private void Op78()
	{
		PL = PL | 4;
		AddCycles( SnesSystem.ONE_CYCLE );
	}

	private void Op79M0X0()
	{
		int value16 = GetWord( AbsoluteIndexedYX0Read(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8 ;
		ADC16( value16 );
	}

	private void Op79M0X1()
	{
		int value16 = GetWord( AbsoluteIndexedYX1Read(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8;
		ADC16( value16 );
	}

	private void Op79M1X0()
	{
		int value8 = globals.OpenBus = GetByte( AbsoluteIndexedYX0Read() );
		ADC8( value8 );
	}

	private void Op79M1X1()
	{
		int value8 = globals.OpenBus = GetByte( AbsoluteIndexedYX1Read() );
		ADC8( value8 );
	}

	private void Op79Slow()
	{
		if ( CheckMemory() )
		{
			int value8 = globals.OpenBus = GetByte( AbsoluteIndexedYSlow() );
			ADC8( value8 );
		}
		else
		{
			int value16 = GetWord( AbsoluteIndexedYSlow(), Memory.WRAP_NONE );
			globals.OpenBus = value16 >>> 8;
			ADC16( value16 );
		}
	}

	private void Op7AE0X0()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		Y = GetWord( S + 1, Memory.WRAP_BANK );
		S_W( S + 2 );
		SetZN16( Y );
		globals.OpenBus = Y_H();
	}

	private void Op7AE0X1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		Y_L( GetByte( ++S ) );
		SetZN8( Y_L() );
		globals.OpenBus = Y_L();
	}

	private void Op7AE1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		S_L( S + 1 );
		Y_L( GetByte( S ) );
		SetZN8( Y_L() );
		globals.OpenBus = Y_L();
	}

	private void Op7ASlow()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		if ( CheckEmulation() )
		{
			S_L( S + 1 );
			Y_L( GetByte( S ) );
			SetZN8( Y_L() );
			globals.OpenBus = Y_L();
		}
		else if ( CheckIndex() )
		{
			Y_L( GetByte( ++S ) );
			SetZN8( Y_L() );
			globals.OpenBus = Y_L();
		}
		else
		{
			Y = GetWord( S + 1, Memory.WRAP_BANK );
			S += 2;
			SetZN16( Y );
			globals.OpenBus = Y_H();
		}
	}

	private void Op7B()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		A = D;
		SetZN16( A );
	}

	private void Op7C()
	{
		SetPCBase( ShiftedPB + ( AbsoluteIndexedIndirect() & 0xFFFF ) );
	}

	private void Op7CSlow()
	{
		SetPCBase( ShiftedPB + ( AbsoluteIndexedIndirectSlowJump() & 0xFFFF ) );
	}

	private void Op7DM0X0()
	{
		int value16 = GetWord( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8;
		ADC16( value16 );
	}

	private void Op7DM0X1()
	{
		int value16 = GetWord( AbsoluteIndexedXX1(), Memory.WRAP_NONE );
		globals.OpenBus = value16 >>> 8 ;
		ADC16( value16 );
	}

	private void Op7DM1X0()
	{
		int value8 = globals.OpenBus = GetByte( AbsoluteIndexedXX0() );
		ADC8( value8 );
	}

	private void Op7DM1X1()
	{
		int value8 = globals.OpenBus = GetByte( AbsoluteIndexedXX1() );
		ADC8( value8 );
	}

	private void Op7DSlow()
	{
		if ( CheckMemory() )
		{
			int value8 = globals.OpenBus = GetByte( AbsoluteIndexedXSlow() );
			ADC8( value8 );
		}
		else
		{
			int value16 = GetWord( AbsoluteIndexedXSlow(), Memory.WRAP_NONE );
			globals.OpenBus = value16 >>> 8;
			ADC16( value16 );
		}
	}

	private void Op7EM0X0()
	{
		ROR16 ( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
	}

	private void Op7EM0X1()
	{
		ROR16 ( AbsoluteIndexedXX1Modify(), Memory.WRAP_NONE );
	}

	private void Op7EM1X0()
	{
		ROR8 ( AbsoluteIndexedXX0() );
	}

	private void Op7EM1X1()
	{
		ROR8 ( AbsoluteIndexedXX1Modify() );
	}

	private void Op7ESlow()
	{
		if ( CheckMemory() )
		{
			ROR8 ( AbsoluteIndexedXSlowModify() );
		}
		else
		{
			ROR16 ( AbsoluteIndexedXSlowModify(), Memory.WRAP_NONE );
		}
	}

	private void Op7FM0()
	{
		int val = GetWord( AbsoluteLongIndexedXRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		ADC16( val );
	}

	private void Op7FM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXRead() );
		ADC8( val );
	}

	private void Op7FSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXSlowRead() );
			ADC8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongIndexedXSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			ADC16( val );
		}
	}

	private void Op80E0()
	{
		int newPC = Relative(); ;

		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
		{
			SetPCBase( ShiftedPB + newPC );
		}
		else
		{
			PCw = newPC;
		}
	}

	private void Op80E1()
	{
		int newPC = Relative(); ;

		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
		}
		if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
		{
			SetPCBase( ShiftedPB + newPC );
		}
		else
		{
			PCw = newPC;
		}
	}

	private void Op80Slow()
	{
		int newPC;
		newPC = RelativeSlow(); ;
		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( CheckEmulation() && PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
		}
		if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
		{
			SetPCBase( ShiftedPB + newPC );
		}
		else
		{
			PCw = newPC;
		}
	}

	private void Op81E0M0()
	{
		STA16( DirectIndexedIndirectE0(), Memory.WRAP_NONE );
	}

	private void Op81E0M1()
	{
		STA8( DirectIndexedIndirectE0() );
	}

	private void Op81E1()
	{
		STA8( DirectIndexedIndirectE1() );
	}
	
	private void Op81Slow()
	{
		if ( CheckMemory() )
		{
			STA8( DirectIndexedIndirectSlow() );
		}
		else
		{
			STA16( DirectIndexedIndirectSlow(), Memory.WRAP_NONE );
		}
	}

	private void Op82()
	{
		SetPCBase( ShiftedPB + RelativeLong() );
	}
	
	private void Op82Slow()
	{
		SetPCBase( ShiftedPB + RelativeLongSlow() );
	}

	private void Op83M0()
	{
		STA16( StackRelative(), Memory.WRAP_NONE );
	}
	
	private void Op83M1()
	{
		STA8( StackRelative() );
	}
	
	private void Op83Slow()
	{
		if ( CheckMemory() )
		{
			STA8( StackRelativeSlow() );
		}
		else
		{
			STA16( StackRelativeSlow(), Memory.WRAP_NONE );
		}
	}

	private void Op84Slow()
	{
		if ( CheckIndex() )
		{
			STY8( DirectSlow() );
		}
		else
		{
			STY16( DirectSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op84X0()
	{
		STY16( Direct(), Memory.WRAP_BANK );
	}
	
	private void Op84X1()
	{
		STY8( Direct() );
	}

	private void Op85M0()
	{
		STA16( Direct(), Memory.WRAP_BANK );
	}

	private void Op85M1()
	{
		STA8( Direct() );
	}

	private void Op85Slow()
	{
		if ( CheckMemory() )
		{
			STA8( DirectSlow() );
		}
		else
		{
			STA16( DirectSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op86Slow()
	{
		if ( CheckIndex() )
		{
			STX8( DirectSlow() );
		}
		else
		{
			STX16( DirectSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op86X0()
	{
		STX16( Direct(), Memory.WRAP_BANK );
	}

	private void Op86X1()
	{
		STX8( Direct() );
	}

	private void Op87M0()
	{
		STA16( DirectIndirectLong(), Memory.WRAP_NONE );
	}
	
	private void Op87M1()
	{
		STA8( DirectIndirectLong() );
	}

	private void Op87Slow()
	{
		if ( CheckMemory() )
		{
			STA8( DirectIndirectLongSlow() );
		}
		else
		{
			STA16( DirectIndirectLongSlow(), Memory.WRAP_NONE );
		}
	}

	private void Op88Slow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		if ( CheckIndex() )
		{
			Y_L( Y - 1 );
			SetZN8( Y_L() );
		}
		else
		{
			Y_W( Y - 1 );
			SetZN16( Y );
		}
	}

	private void Op88X0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		Y_W( Y - 1 );
		SetZN16( Y );
	}

	private void Op88X1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		Y_L( Y - 1 );
		SetZN8( Y_L() );
	}

	private void Op89M0()
	{
		_Zero = ( A & Immediate16Read() ) != 0 ? 1 : 0;
	}

	private void Op89M1()
	{
		_Zero = A & Immediate8Read();
	}

	private void Op89Slow()
	{
		if ( CheckMemory() )
		{
			_Zero = A_L() & Immediate8SlowRead();
		}
		else
		{
			_Zero = ( A & Immediate16SlowRead() ) != 0 ? 1 : 0;
		}
	}

	private void Op8AM0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		A = X;
		SetZN16( A );
	}

	private void Op8AM1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		A_L( X );
		SetZN8( A_L() );
	}

	private void Op8ASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckMemory() )
		{
			A_L( X );
			SetZN8( A_L() );
		}
		else
		{
			A = X;
			SetZN16( A );
		}
	}

	private void Op8BE0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( DB, S-- );
		globals.OpenBus = DB;
	}

	private void Op8BE1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( DB, S );
		S_L( S - 1);
		globals.OpenBus = DB;
	}

	private void Op8BSlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckEmulation() )
		{
			SetByte( DB, S );
			S_L( S - 1);
		}
		else
		{
			SetByte( DB, S-- );
		}
		globals.OpenBus = DB;
	}

	private void Op8CSlow()
	{
		if ( CheckIndex() )
		{
			STY8( AbsoluteSlow() );
		}
		else
		{
			STY16( AbsoluteSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op8CX0()
	{
		STY16( Absolute(), Memory.WRAP_BANK );
	}

	private void Op8CX1()
	{
		STY8( Absolute() );
	}

	private void Op8DM0()
	{
		STA16( Absolute(), Memory.WRAP_NONE );
	}

	private void Op8DM1()
	{
		STA8( Absolute() );
	}

	private void Op8DSlow()
	{
		if ( CheckMemory() )
		{
			STA8( AbsoluteSlow() );
		}
		else
		{
			STA16( AbsoluteSlow(), Memory.WRAP_NONE );
		}
	}

	private void Op8ESlow()
	{
		if ( CheckIndex() )
		{
			STX8( AbsoluteSlow() );
		}
		else
		{
			STX16( AbsoluteSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op8EX0()
	{
		STX16( Absolute(), Memory.WRAP_BANK );
	}

	private void Op8EX1()
	{
		STX8( Absolute() );
	}

	private void Op8FM0()
	{
		STA16( AbsoluteLong(), Memory.WRAP_NONE );
	}

	private void Op8FM1()
	{
		STA8( AbsoluteLong() );
	}

	private void Op8FSlow()
	{
		if ( CheckMemory() )
		{
			STA8( AbsoluteLongSlow() );
		}
		else
		{
			STA16( AbsoluteLongSlow(), Memory.WRAP_NONE );
		}
	}

	private void Op90E0()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Carry == 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );

			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op90E1()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Carry == 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			if ( PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op90Slow()
	{
		int newPC = RelativeSlow();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC ) 
				return;
		}
		
		if ( _Carry == 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			if ( CheckEmulation() && PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void Op91E0M0X0()
	{
		STA16( DirectIndirectIndexedE0X0(), Memory.WRAP_NONE );
	}

	private void Op91E0M0X1()
	{
		STA16( DirectIndirectIndexedE0X1(), Memory.WRAP_NONE );
	}
	
	private void Op91E0M1X0()
	{
		STA8( DirectIndirectIndexedE0X0() );
	}
	
	private void Op91E0M1X1()
	{
		STA8( DirectIndirectIndexedE0X1() );
	}

	private void Op91E1()
	{
		STA8( DirectIndirectIndexedE1Write() );
	}

	private void Op91Slow()
	{
		if ( CheckMemory() )
		{
			STA8( DirectIndirectIndexedSlowWrite() );
		}
		else
		{
			STA16( DirectIndirectIndexedSlowWrite(), Memory.WRAP_NONE );
		}
	}

	private void Op92E0M0()
	{
		STA16( DirectIndirectE0(), Memory.WRAP_NONE );
	}

	private void Op92E0M1()
	{
		STA8( DirectIndirectE0() );
	}

	private void Op92E1()
	{
		STA8( DirectIndirectE1() );
	}

	private void Op92Slow()
	{
		if ( CheckMemory() )
		{
			STA8( DirectIndirectSlow() );
		}
		else
		{
			STA16( DirectIndirectSlow(), Memory.WRAP_NONE );
		}
	}

	private void Op93M0()
	{
		STA16( StackRelativeIndirectIndexed(), Memory.WRAP_NONE );
	}

	private void Op93M1()
	{
		STA8( StackRelativeIndirectIndexed() );
	}

	private void Op93Slow()
	{
		if ( CheckMemory() )
		{
			STA8( StackRelativeIndirectIndexedSlow() );
		}
		else
		{
			STA16( StackRelativeIndirectIndexedSlow(), Memory.WRAP_NONE );
		}
	}

	private void Op94E0X0()
	{
		STY16( DirectIndexedXE0(), Memory.WRAP_BANK );
	}

	private void Op94E0X1()
	{
		STY8( DirectIndexedXE0() );
	}

	private void Op94E1()
	{
		STY8( DirectIndexedXE1() );
	}

	private void Op94Slow()
	{
		if ( CheckIndex() )
		{
			STY8( DirectIndexedXSlow() );
		}
		else
		{
			STY16( DirectIndexedXSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op95E0M0()
	{
		STA16( DirectIndexedXE0(), Memory.WRAP_BANK );
	}

	private void Op95E0M1()
	{
		STA8( DirectIndexedXE0() );
	}

	private void Op95E1()
	{
		STA8( DirectIndexedXE1() );
	}

	private void Op95Slow()
	{
		if ( CheckMemory() )
		{
			STA8( DirectIndexedXSlow() );
		}
		else
		{
			STA16( DirectIndexedXSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op96E0X0()
	{
		STX16( DirectIndexedYE0(), Memory.WRAP_BANK );
	}

	private void Op96E0X1()
	{
		STX8( DirectIndexedYE0() );
	}

	private void Op96E1()
	{
		STX8( DirectIndexedYE1() );
	}

	private void Op96Slow()
	{
		if ( CheckIndex() )
		{
			STX8( DirectIndexedYSlow() );
		}
		else
		{
			STX16( DirectIndexedYSlow(), Memory.WRAP_BANK );
		}
	}

	private void Op97M0()
	{
		STA16( DirectIndirectIndexedLong(), Memory.WRAP_NONE );
	}

	private void Op97M1()
	{
		STA8( DirectIndirectIndexedLong() );
	}

	private void Op97Slow()
	{
		if ( CheckMemory() )
		{
			STA8( DirectIndirectIndexedLongSlow() );
		}
		else
		{
			STA16( DirectIndirectIndexedLongSlow(), Memory.WRAP_NONE );
		}
	}

	private void Op98M0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		A = Y;
		SetZN16( A );
	}

	private void Op98M1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		A_L( Y );
		SetZN8( A_L() );
	}

	private void Op98Slow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckMemory() )
		{
			A_L( Y );
			SetZN8( A_L() );
		}
		else
		{
			A = Y;
			SetZN16( A );
		}
	}

	private void Op99M0X0()
	{
		STA16( AbsoluteIndexedYX0(), Memory.WRAP_NONE );
	}

	private void Op99M0X1()
	{
		STA16( AbsoluteIndexedYX1Write(), Memory.WRAP_NONE );
	}

	private void Op99M1X0()
	{
		STA8( AbsoluteIndexedYX0() );
	}

	private void Op99M1X1()
	{
		STA8( AbsoluteIndexedYX1Write() );
	}

	private void Op99Slow()
	{
		if ( CheckMemory() )
		{
			STA8( AbsoluteIndexedYSlowWrite() );
		}
		else
		{
			STA16( AbsoluteIndexedYSlowWrite(), Memory.WRAP_NONE );
		}
	}

	private void Op9A()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		S = X;
		if ( CheckEmulation() ) S_H(1);
	}

	private void Op9BSlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckIndex() )
		{
			Y_L( X );
			SetZN8( Y_L() );
		}
		else
		{
			Y = X;
			SetZN16( Y );
		}
	}

	private void Op9BX0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		Y = X;
		SetZN16( Y );
	}

	private void Op9BX1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		Y_L( X );
		SetZN8( Y_L() );
	}

	private void Op9CM0()
	{
		STZ16( Absolute(), Memory.WRAP_NONE );
	}

	private void Op9CM1()
	{
		STZ8( Absolute() );
	}

	private void Op9CSlow()
	{
		if ( CheckMemory() )
		{
			STZ8( AbsoluteSlow() );
		}
		else
		{
			STZ16( AbsoluteSlow(), Memory.WRAP_NONE );
		}
	}

	private void Op9DM0X0()
	{
		STA16( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
	}

	private void Op9DM0X1()
	{
		STA16( AbsoluteIndexedXX1Write(), Memory.WRAP_NONE );
	}

	private void Op9DM1X0()
	{
		STA8( AbsoluteIndexedXX0() );
	}

	private void Op9DM1X1()
	{
		STA8( AbsoluteIndexedXX1Write() );
	}

	private void Op9DSlow()
	{
		if ( CheckMemory() )
		{
			STA8( AbsoluteIndexedXSlowWrite() );
		}
		else
		{
			STA16( AbsoluteIndexedXSlowWrite(), Memory.WRAP_NONE );
		}
	}

	private void Op9EM0X0()
	{
		STZ16( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
	}

	private void Op9EM0X1()
	{
		STZ16( AbsoluteIndexedXX1Write(), Memory.WRAP_NONE );
	}

	private void Op9EM1X0()
	{
		STZ8( AbsoluteIndexedXX0() );
	}

	private void Op9EM1X1()
	{
		STZ8( AbsoluteIndexedXX1Write() );
	}

	private void Op9ESlow()
	{
		if ( CheckMemory() )
		{
			STZ8( AbsoluteIndexedXSlowWrite() );
		}
		else
		{
			STZ16( AbsoluteIndexedXSlowWrite(), Memory.WRAP_NONE );
		}
	}

	private void Op9FM0()
	{
		STA16( AbsoluteLongIndexedX(), Memory.WRAP_NONE );
	}

	private void Op9FM1()
	{
		STA8( AbsoluteLongIndexedX() );
	}

	private void Op9FSlow()
	{
		if ( CheckMemory() )
		{
			STA8( AbsoluteLongIndexedXSlow() );
		}
		else
		{
			STA16( AbsoluteLongIndexedXSlow(), Memory.WRAP_NONE );
		}
	}

	private void OpA0Slow()
	{
		if ( CheckIndex() )
		{
			Y_L( Immediate8SlowRead() );
			SetZN8( Y_L() );
		}
		else
		{
			Y = Immediate16SlowRead();
			SetZN16( Y );
		}
	}

	private void OpA0X0()
	{
		Y = Immediate16Read();
		SetZN16( Y );
	}

	private void OpA0X1()
	{
		Y_L( Immediate8Read() );
		SetZN8( Y_L() );
	}

	private void OpA1E0M0()
	{
		int val = GetWord( DirectIndexedIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpA1E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE0Read() );
		LDA8( val );
	}

	private void OpA1E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE1Read() );
		LDA8( val );
	}

	private void OpA1Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedIndirectSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpA2Slow()
	{
		if ( CheckIndex() )
		{
			X_L( Immediate8SlowRead() );
			SetZN8( X_L() );
		}
		else
		{
			X = Immediate16SlowRead();
			SetZN16( X );
		}
	}

	private void OpA2X0()
	{
		X = Immediate16Read();
		SetZN16( X );
	}

	private void OpA2X1()
	{
		X_L( Immediate8Read() );
		SetZN8( X_L() );
	}

	private void OpA3M0()
	{
		int val = GetWord( StackRelativeRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpA3M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeRead() );
		LDA8( val );
	}

	private void OpA3Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( StackRelativeSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpA4Slow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			LDY8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDY16( val );
		}
	}

	private void OpA4X0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDY16( val );
	}

	private void OpA4X1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		LDY8( val );
	}

	private void OpA5M0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpA5M1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		LDA8( val );
	}

	private void OpA5Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpA6Slow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			LDX8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDX16( val );
		}
	}

	private void OpA6X0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDX16( val );
	}

	private void OpA6X1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		LDX8( val );
	}

	private void OpA7M0()
	{
		int val = GetWord( DirectIndirectLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpA7M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectLongRead() );
		LDA8( val );
	}

	private void OpA7Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectLongSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpA8Slow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckIndex() )
		{
			Y_L( A );
			SetZN8( Y_L() );
		}
		else
		{
			Y = A;
			SetZN16( Y );
		}
	}

	private void OpA8X0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		Y = A;
		SetZN16( Y );
	}

	private void OpA8X1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		Y_L( A );
		SetZN8( Y_L() );
	}

	private void OpA9M0()
	{
		A = Immediate16Read();
		SetZN16( A );
	}

	private void OpA9M1()
	{
		A_L( Immediate8Read() );
		SetZN8( A_L() );
	}

	private void OpA9Slow()
	{
		if ( CheckMemory() )
		{
			A_L( Immediate8SlowRead() );
			SetZN8( A_L() );
		}
		else
		{
			A = Immediate16SlowRead();
			SetZN16( A );
		}
	}

	private void OpAASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckIndex() )
		{
			X_L( A );
			SetZN8( X_L() );
		}
		else
		{
			X = A;
			SetZN16( X );
		}
	}

	private void OpAAX0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		X = A;
		SetZN16( X );
	}

	private void OpAAX1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		X_L( A );
		SetZN8( X_L() );
	}

	private void OpABE0()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		DB = GetByte( ++S );
		SetZN8( DB );
		ShiftedDB = DB << 16;
		globals.OpenBus = DB;
	}

	private void OpABE1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		S_L( S + 1 );
		DB = GetByte( S );
		SetZN8( DB );
		ShiftedDB = DB << 16;
		globals.OpenBus = DB;
	}

	private void OpABSlow()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		if ( CheckEmulation() )
		{
			S_L( S + 1 );
			DB = GetByte( S );
		}
		else
		{
			DB = GetByte( ++S );
		}
		SetZN8( DB );
		ShiftedDB = DB << 16;
		globals.OpenBus = DB;
	}

	private void OpACSlow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			LDY8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDY16( val );
		}
	}

	private void OpACX0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDY16( val );
	}

	private void OpACX1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		LDY8( val );
	}

	private void OpADM0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpADM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		LDA8( val );
	}

	private void OpADSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpAESlow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			LDX8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDX16( val );
		}
	}

	private void OpAEX0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDX16( val );
	}

	private void OpAEX1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		LDX8( val );
	}

	private void OpAFM0()
	{
		int val = GetWord( AbsoluteLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpAFM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongRead() );
		LDA8( val );
	}

	private void OpAFSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpB0E0()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Carry != 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void OpB0E1()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Carry != 0  )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			if ( PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void OpB0Slow()
	{
		int newPC = RelativeSlow();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 && PCw > newPC )
				return;
		}

		if ( _Carry != 0  )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( CheckEmulation() && PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void OpB1E0M0X0()
	{
		int val = GetWord( DirectIndirectIndexedE0X0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpB1E0M0X1()
	{
		int val = GetWord( DirectIndirectIndexedE0X1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpB1E0M1X0()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X0Read() );
		LDA8( val );
	}

	private void OpB1E0M1X1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X1Read() );
		LDA8( val );
	}

	private void OpB1E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE1Read() );
		LDA8( val );
	}

	private void OpB1Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpB2E0M0()
	{
		int val = GetWord( DirectIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpB2E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE0Read() );
		LDA8( val );
	}

	private void OpB2E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE1Read() );
		LDA8( val );
	}

	private void OpB2Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpB3M0()
	{
		int val = GetWord( StackRelativeIndirectIndexedRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpB3M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedRead() );
		LDA8( val );
	}

	private void OpB3Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( StackRelativeIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpB4E0X0()
	{
		int val = GetWord( DirectIndexedXE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDY16( val );
	}

	private void OpB4E0X1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE0Read() );
		LDY8( val );
	}

	private void OpB4E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE1Read() );
		LDY8( val );
	}

	private void OpB4Slow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedXSlowRead() );
			LDY8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDY16( val );
		}
	}

	private void OpB5E0M0()
	{
		int val = GetWord( DirectIndexedXE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpB5E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE0Read() );
		LDA8( val );
	}

	private void OpB5E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE1Read() );
		LDA8( val );
	}

	private void OpB5Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedXSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpB6E0X0()
	{
		int val = GetWord( DirectIndexedYE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDX16( val );
	}

	private void OpB6E0X1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedYE0Read() );
		LDX8( val );
	}

	private void OpB6E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedYE1Read() );
		LDX8( val );
	}

	private void OpB6Slow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedYSlowRead() );
			LDX8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedYSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDX16( val );
		}
	}

	private void OpB7M0()
	{
		int val = GetWord( DirectIndirectIndexedLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpB7M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongRead() );
		LDA8( val );
	}

	private void OpB7Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpB8()
	{
		_Overflow = 0;
		AddCycles( SnesSystem.ONE_CYCLE );
	}

	private void OpB9M0X0()
	{
		int val = GetWord( AbsoluteIndexedYX0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpB9M0X1()
	{
		int val = GetWord( AbsoluteIndexedYX1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpB9M1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX0Read() );
		LDA8( val );
	}

	private void OpB9M1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX1Read() );
		LDA8( val );
	}

	private void OpB9Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedYSlow() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedYSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpBASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckIndex() )
		{
			X_L( S );
			SetZN8( X_L() );
		}
		else
		{
			X = S;
			SetZN16( X );
		}
	}

	private void OpBAX0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		X = S;
		SetZN16( X );
	}

	private void OpBAX1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		X_L( S );
		SetZN8( X_L() );
	}

	private void OpBBSlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckIndex() )
		{
			X_L( Y );
			SetZN8( X_L() );
		}
		else
		{
			X = Y;
			SetZN16( X );
		}
	}

	private void OpBBX0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		X = Y;
		SetZN16( X );
	}

	private void OpBBX1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		X_L( Y );
		SetZN8( X_L() );
	}

	private void OpBCSlow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedXSlow() );
			LDY8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedXSlow(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDY16( val );
		}
	}

	private void OpBCX0()
	{
		int val = GetWord( AbsoluteIndexedXX0(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDY16( val );
	}

	private void OpBCX1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX1() );
		LDY8( val );
	}

	private void OpBDM0X0()
	{
		int val = GetWord( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpBDM0X1()
	{
		int val = GetWord( AbsoluteIndexedXX1(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpBDM1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX0() );
		LDA8( val );
	}

	private void OpBDM1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX1() );
		LDA8( val );
	}

	private void OpBDSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedXSlow() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedXSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpBESlow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedYSlow() );
			LDX8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedYSlow(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			LDX16( val );
		}
	}

	private void OpBEX0()
	{
		int val = GetWord( AbsoluteIndexedYX0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		LDX16( val );
	}

	private void OpBEX1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX1Read() );
		LDX8( val );
	}

	private void OpBFM0()
	{
		int val = GetWord( AbsoluteLongIndexedXRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		LDA16( val );
	}

	private void OpBFM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXRead() );
		LDA8( val );
	}

	private void OpBFSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXSlowRead() );
			LDA8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongIndexedXSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			LDA16( val );
		}
	}

	private void OpC0Slow()
	{
		if ( CheckIndex() )
		{
			int Int16 = Y_L() - Immediate8SlowRead();
			_Carry = Int16 >= 0 ? 1 : 0;
			SetZN8( Int16 & 0xFF );
		}
		else
		{
			int Int32 = Y - Immediate16SlowRead();
			_Carry = Int32 >= 0 ? 1 : 0;
			SetZN16( Int32 & 0xFFFF );
		}
	}

	private void OpC0X0()
	{
		int Int32 = Y - Immediate16Read();
		_Carry = Int32 >= 0 ? 1 : 0;
		SetZN16( Int32 & 0xFFFF );
	}

	private void OpC0X1()
	{
		int Int16 = Y_L() - Immediate8Read();
		_Carry = Int16 >= 0 ? 1 : 0;
		SetZN8( Int16 & 0xFF );
	}

	private void OpC1E0M0()
	{
		int val = GetWord( DirectIndexedIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpC1E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE0Read() );
		CMP8( val );
	}

	private void OpC1E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE1Read() );
		CMP8( val );
	}

	private void OpC1Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedIndirectSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpC2()
	{
		int Work8 = ~Immediate8Read();
		PL = PL & Work8;
		_Carry &= Work8;
		_Overflow &= ( Work8 >>> 6 );
		_Negative &= Work8;
		_Zero |= ~Work8 & 2;

		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( CheckEmulation() )
		{
			PL |= ( IndexFlag | MemoryFlag );
		}
		if ( CheckIndex() )
		{
			X_H(0);
			Y_H(0);
		}
		FixCycles();

	}

	private void OpC2Slow()
	{
		int Work8 = ~Immediate8SlowRead();
		PL = PL & Work8;
		_Carry &= Work8;
		_Overflow &= ( Work8 >>> 6 );
		_Negative &= Work8;
		_Zero |= ~Work8 & 2;

		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( CheckEmulation() )
		{
			PL |= ( IndexFlag | MemoryFlag );
		}
		if ( CheckIndex() )
		{
			X_H(0);
			Y_H(0);
		}
		FixCycles();

	}

	private void OpC3M0()
	{
		int val = GetWord( StackRelativeRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpC3M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeRead() );
		CMP8( val );
	}

	private void OpC3Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( StackRelativeSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpC4Slow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			CPY8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			CPY16( val );
		}
	}

	private void OpC4X0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		CPY16( val );
	}

	private void OpC4X1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		CPY8( val );
	}

	private void OpC5M0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpC5M1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		CMP8( val );
	}

	private void OpC5Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpC6M0()
	{
		DEC16 ( DirectRead(), Memory.WRAP_BANK );
	}

	private void OpC6M1()
	{
		DEC8 ( DirectRead() );
	}

	private void OpC6Slow()
	{
		if ( CheckMemory() )
		{
			DEC8 ( DirectSlowRead() );
		}
		else
		{
			DEC16 ( DirectSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void OpC7M0()
	{
		int val = GetWord( DirectIndirectLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpC7M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectLongRead() );
		CMP8( val );
	}

	private void OpC7Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectLongSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpC8Slow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		if ( CheckIndex() )
		{
			Y_L( Y - 1 );
			SetZN8( Y_L() );
		}
		else
		{
			Y_W( Y - 1 );
			SetZN16( Y );
		}
	}

	private void OpC8X0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		Y_W( Y + 1 );
		SetZN16( Y );
	}

	private void OpC8X1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		Y_L( Y + 1 );
		SetZN8( Y_L() );
	}

	private void OpC9M0()
	{
		int Int32 = A - Immediate16Read();
		_Carry = Int32 >= 0 ? 1 : 0;
		SetZN16( Int32 & 0xFFFF );
	}

	private void OpC9M1()
	{
		int Int16 = A_L() - Immediate8Read();
		_Carry = Int16 >= 0 ? 1 : 0;
		SetZN8( Int16 & 0xFF );
	}

	private void OpC9Slow()
	{
		if ( CheckMemory() )
		{
			int Int16 = A_L() - Immediate8SlowRead();
			_Carry = Int16 >= 0 ? 1 : 0;
			SetZN8( Int16 & 0xFF );
		}
		else
		{
			int Int32 = A - Immediate16SlowRead();
			_Carry = Int32 >= 0 ? 1 : 0;
			SetZN16( Int32 & 0xFFFF );
		}
	}

	private void OpCASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		if ( CheckIndex() )
		{
			X_L( X - 1 );
			SetZN8( X_L() );
		}
		else
		{
			X_W( X - 1 );
			SetZN16( X );
		}
	}

	private void OpCAX0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		X_W( X - 1 );
		SetZN16( X );
	}

	private void OpCAX1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		X_L( X - 1 );
		SetZN8( X_L() );
	}

	private void OpCB()
	{
		WaitingForInterrupt = true;
		PCw--;

		if ( globals.settings.Shutdown )
		{
			Cycles = NextEvent;
			CPUExecuting = 0;
			apu.APUExecute();
			CPUExecuting = 1;
		}
		else
		{
			AddCycles( SnesSystem.TWO_CYCLES );
		}

	}

	private void OpCCSlow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			CPY8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CPY16( val );
		}
	}

	private void OpCCX0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CPY16( val );
	}

	private void OpCCX1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		CPY8( val );
	}

	private void OpCDM0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpCDM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		CMP8( val );
	}

	private void OpCDSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpCEM0()
	{
		DEC16 ( AbsoluteRead(), Memory.WRAP_NONE );
	}

	private void OpCEM1()
	{
		DEC8 ( AbsoluteRead() );
	}

	private void OpCESlow()
	{
		if ( CheckMemory() )
		{
			DEC8 ( AbsoluteSlowRead() );
		}
		else
		{
			DEC16 ( AbsoluteSlowRead(), Memory.WRAP_NONE );
		}
	}

	private void OpCFM0()
	{
		int val = GetWord( AbsoluteLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpCFM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongRead() );
		CMP8( val );
	}

	private void OpCFSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	final void Opcode_IRQ()
	{
		if (SnesSystem.DEBUG_CPU)
			System.out.println("*** IRQ");
		
		AddCycles( MemSpeed + SnesSystem.ONE_CYCLE );

		if ( ! CheckEmulation())
		{
			SetByte( PB(), S-- );
			SetWord( PCw, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
			S -= 2;
			PackStatus();
			SetByte( PL, S-- );
			globals.OpenBus = PL;
			PL = ( PL & 0xF7 ) | 4;

			if ( globals.settings.SA1 && ( memory.FillRAM.getByte(0x2209) & 0x40 ) != 0 )
			{
				globals.OpenBus = memory.FillRAM.get8Bit(0x220f);
				AddCycles( SnesSystem.TWO_CYCLES ); 
				SetPCBase( memory.FillRAM.get8Bit(0x220e) | ( memory.FillRAM.get8Bit(0x220f) << 8 ) );
			}
			else
			{
				int addr = GetWord( 0xFFEE );
				globals.OpenBus = addr >>> 8;
				SetPCBase( addr );
			}

		}
		else
		{
			S_L( S - 1);
			SetWord( PCw, S, Memory.WRAP_PAGE, Memory.WRITE_10 );
			S_L( S - 1);
			PackStatus();
			SetByte( PL, S );
			S_L( S - 1);
			globals.OpenBus = PL;
			PL = ( PL & 0xF7 ) | 4;

			if ( globals.settings.SA1 && ( memory.FillRAM.getByte(0x2209) & 0x40 ) != 0 )
			{
				globals.OpenBus = memory.FillRAM.get8Bit(0x220f);
				AddCycles( SnesSystem.TWO_CYCLES );
				SetPCBase( memory.FillRAM.get8Bit(0x220e) | ( memory.FillRAM.get8Bit(0x220f) << 8 ) );
			}
			else
			{
				int addr = GetWord( 0xFFFE );
				globals.OpenBus = addr >>> 8;
				SetPCBase( addr );
			}

		}
	}

	final void Opcode_NMI()
	{
		if (SnesSystem.DEBUG_CPU)
		{
			System.out.println("*** NMI");
		}

		AddCycles( MemSpeed + SnesSystem.ONE_CYCLE );

		if ( ! CheckEmulation() )
		{
			SetByte( PB(), S-- );
			SetWord( PCw, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
			S -= 2;
			PackStatus();
			SetByte( PL, S-- );
			globals.OpenBus = PL;
			PL = ( PL & 0xF7 ) | 4;
			
			if ( globals.settings.SA1 && ( memory.FillRAM.get8Bit( 0x2209 ) & 0x20 ) != 0 )
			{
				globals.OpenBus = memory.FillRAM.get8Bit( 0x220d );
				AddCycles( SnesSystem.TWO_CYCLES );
				SetPCBase( memory.FillRAM.get16Bit(0x220c) );
			}
			else
			{
				int addr = GetWord( 0xFFEA );
				globals.OpenBus = addr >>> 8;
				SetPCBase( addr );
			}
		}
		else
		{
			S_L( S - 1);
			SetWord( PCw, S, Memory.WRAP_PAGE, Memory.WRITE_10 );
			S_L( S - 1);
			PackStatus();
			SetByte( PL, S );
			S_L( S - 1);
			globals.OpenBus = PL;

			PL = ( PL & 0xF7 ) | 4;

			if ( globals.settings.SA1 && ( memory.FillRAM.get8Bit( 0x2209 ) & 0x20 ) != 0 )
			{
				globals.OpenBus = memory.FillRAM.get8Bit( 0x220d );
				AddCycles( SnesSystem.TWO_CYCLES );
				SetPCBase( memory.FillRAM.get16Bit( 0x220c ) );
			}
			else
			{
				int addr = GetWord( 0xFFFA );
				globals.OpenBus = addr >>> 8;
				SetPCBase( addr );
			}
		}
	}

	private void OpD0E0()
	{
		int newPC;
		newPC = Relative(); 
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 1 )
			{
				return;
			}
			else if ( globals.settings.SoundSkipMethod == 3 )
			{
				if ( PCw > newPC ) return;
			}
			else
			{
				PCw = newPC;
			}
		}

		if ( _Zero != 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void OpD0E1()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 1 )
			{
				return;
			}
			else if ( globals.settings.SoundSkipMethod == 3 )
			{
				if ( PCw > newPC ) return;
			}
			else 
			{
				PCw = newPC;
			}
		}

		if ( _Zero != 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void OpD0Slow()
	{
		int newPC = RelativeSlow();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 1 ) return;
			if ( globals.settings.SoundSkipMethod == 3 ) if ( PCw > newPC ) return;
				else PCw = newPC;
		}

		if ( !( _Zero == 0 ) )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			if ( CheckEmulation() && PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void OpD1E0M0X0()
	{
		int val = GetWord( DirectIndirectIndexedE0X0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpD1E0M0X1()
	{
		int val = GetWord( DirectIndirectIndexedE0X1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpD1E0M1X0()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X0Read() );
		CMP8( val );
	}

	private void OpD1E0M1X1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X1Read() );
		CMP8( val );
	}

	private void OpD1E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE1Read() );
		CMP8( val );
	}

	private void OpD1Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpD2E0M0()
	{
		int val = GetWord( DirectIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpD2E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE0Read() );
		CMP8( val );
	}

	private void OpD2E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE1Read() );
		CMP8( val );
	}

	private void OpD2Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpD3M0()
	{
		int val = GetWord( StackRelativeIndirectIndexedRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpD3M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedRead() );
		CMP8( val );
	}

	private void OpD3Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( StackRelativeIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpD4E0()
	{
		int val = DirectIndirectE0();
		SetWord( val, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = val & 0xff;
	}

	private void OpD4E1()
	{
		int val = DirectIndirectE1();
		SetWord( val, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = val & 0xff;
		S_H(1);
	}

	private void OpD4Slow()
	{
		int val = DirectIndirectSlow();
		SetWord( val, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = val & 0xff;
		if ( CheckEmulation() )
			S_H(1);
	}

	private void OpD5E0M0()
	{
		int val = GetWord( DirectIndexedXE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpD5E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE0Read() );
		CMP8( val );
	}

	private void OpD5E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE1Read() );
		CMP8( val );
	}

	private void OpD5Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedXSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpD6E0M0()
	{
		DEC16 ( DirectIndexedXE0Read(), Memory.WRAP_BANK );
	}

	private void OpD6E0M1()
	{
		DEC8 ( DirectIndexedXE0Read() );
	}

	private void OpD6E1()
	{
		DEC8 ( DirectIndexedXE1Read() );
	}

	private void OpD6Slow()
	{
		if ( CheckMemory() )
		{
			DEC8 ( DirectIndexedXSlowRead() );
		}
		else
		{
			DEC16 ( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void OpD7M0()
	{
		int val = GetWord( DirectIndirectIndexedLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpD7M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongRead() );
		CMP8( val );
	}

	private void OpD7Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpD8()
	{
		PL = PL & 0xF7;
		AddCycles( SnesSystem.ONE_CYCLE );
	}

	private void OpD9M0X0()
	{
		int val = GetWord( AbsoluteIndexedYX0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpD9M0X1()
	{
		int val = GetWord( AbsoluteIndexedYX1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpD9M1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX0Read() );
		CMP8( val );
	}

	private void OpD9M1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX1Read() );
		CMP8( val );
	}
	
	private void OpD9Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedYSlow() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedYSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}
	
	private void OpDAE0X0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( X, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = X_L();
	}

	private void OpDAE0X1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( X_L(), S-- );
		globals.OpenBus = X_L();
	}
	
	private void OpDAE1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( X_L(), S );
		S_L( S - 1);
		globals.OpenBus = X_L();
	}
	
	private void OpDASlow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
		if ( CheckEmulation() )
		{
			SetByte( X_L(), S );
			S_L( S - 1);
		}
		else if ( CheckIndex() )
		{
			SetByte( X_L(), S-- );
		}
		else
		{
			SetWord( X, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
			S -= 2;
		}
		globals.OpenBus = X_L();
	}

	private void OpDB()
	{
		PCw--;
		Flags |= 1 | ( 1 << 12 );
	}

	private void OpDC()
	{
		SetPCBase( AbsoluteIndirectLong() );
	}
	
	private void OpDCSlow()
	{
		SetPCBase( AbsoluteIndirectLongSlow() );
	}

	private void OpDDM0X0()
	{
		int val = GetWord( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpDDM0X1()
	{
		int val = GetWord( AbsoluteIndexedXX1(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}

	private void OpDDM1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX0() );
		CMP8( val );
	}

	private void OpDDM1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX1() );
		CMP8( val );
	}

	private void OpDDSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedXSlow() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedXSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpDEM0X0()
	{
		DEC16 ( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
	}

	private void OpDEM0X1()
	{
		DEC16 ( AbsoluteIndexedXX1Modify(), Memory.WRAP_NONE );
	}

	private void OpDEM1X0()
	{
		DEC8 ( AbsoluteIndexedXX0() );
	}

	private void OpDEM1X1()
	{
		DEC8 ( AbsoluteIndexedXX1Modify() );
	}

	private void OpDESlow()
	{
		if ( CheckMemory() )
		{
			DEC8 ( AbsoluteIndexedXSlow() );
		}
		else
		{
			DEC16 ( AbsoluteIndexedXSlow(), Memory.WRAP_NONE );
		}
	}

	private void OpDFM0()
	{
		int val = GetWord( AbsoluteLongIndexedXRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CMP16( val );
	}
	
	private void OpDFM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXRead() );
		CMP8( val );
	}

	private void OpDFSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXSlowRead() );
			CMP8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongIndexedXSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CMP16( val );
		}
	}

	private void OpE0Slow()
	{
		if ( CheckIndex() )
		{
			int Int16 = X_L() - Immediate8SlowRead();
			_Carry = Int16 >= 0 ? 1 : 0;
			SetZN8( Int16 & 0xFF );
		}
		else
		{
			int Int32 = ( int )X - ( int )Immediate16SlowRead();
			_Carry = Int32 >= 0 ? 1 : 0;
			SetZN16( Int32 & 0xFFFF );
		}
	}

	private void OpE0X0()
	{
		int Int32 = X - Immediate16Read();
		_Carry = Int32 >= 0 ? 1 : 0;
		SetZN16( Int32 & 0xFFFF );
	}

	private void OpE0X1()
	{
		int Int16 = X_L() - Immediate8Read();
		_Carry = Int16 >= 0 ? 1 : 0;
		SetZN8( Int16 & 0xFF );
	}

	private void OpE1E0M0()
	{
		int val = GetWord( DirectIndexedIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpE1E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE0Read() );
		SBC8( val );
	}

	private void OpE1E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedIndirectE1Read() );
		SBC8( val );
	}

	private void OpE1Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedIndirectSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpE2()
	{
		int Work8 = Immediate8Read();
		PL = PL | Work8;
		_Carry |= Work8 & 1;
		_Overflow |= ( Work8 >>> 6 ) & 1;
		_Negative |= Work8;
		
		if ( ( Work8 & 2 ) != 0 )
		{
			_Zero = 0;
		}

		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( CheckEmulation() )
		{
			PL |= ( IndexFlag | MemoryFlag );
		}
		
		if ( CheckIndex() )
		{
			X_H(0);
			Y_H(0);
		}
		FixCycles();
	}

	private void OpE2Slow()
	{
		int Work8 = Immediate8SlowRead();
		
		PL = PL | Work8;
		_Carry |= Work8 & 1;
		_Overflow |= ( Work8 >>> 6 ) & 1;
		_Negative |= Work8;
		
		if ( ( Work8 & 2 ) != 0 )
			_Zero = 0;

		AddCycles( SnesSystem.ONE_CYCLE );
		
		if ( CheckEmulation() )
		{
			PL |= ( IndexFlag | MemoryFlag );
		}
		
		if ( CheckIndex() )
		{
			X_H(0);
			Y_H(0);
		}
		FixCycles();
	}

	private void OpE3M0()
	{
		int val = GetWord( StackRelativeRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpE3M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeRead() );
		SBC8( val );
	}

	private void OpE3Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( StackRelativeSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpE4Slow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			CPX8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			CPX16( val );
		}
	}

	private void OpE4X0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		CPX16( val );
	}

	private void OpE4X1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		CPX8( val );
	}

	private void OpE5M0()
	{
		int val = GetWord( DirectRead(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpE5M1()
	{
		int val = globals.OpenBus = GetByte( DirectRead() );
		SBC8( val );
	}

	private void OpE5Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( DirectSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpE6M0()
	{
		INC16( DirectRead(), Memory.WRAP_BANK );
	}

	private void OpE6M1()
	{
		INC8( DirectRead() );
	}

	private void OpE6Slow()
	{
		if ( CheckMemory() )
		{
			INC8( DirectSlowRead() );
		}
		else
		{
			INC16( DirectSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void OpE7M0()
	{
		int val = GetWord( DirectIndirectLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpE7M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectLongRead() );
		SBC8( val );
	}

	private void OpE7Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectLongSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpE8Slow()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		if ( CheckIndex() )
		{
			X_L( X + 1 );
			SetZN8( X_L() );
		}
		else
		{
			X_W( X + 1 );
			SetZN16( X );
		}
	}

	private void OpE8X0()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		X_W( X + 1 );
		SetZN16( X );
	}

	private void OpE8X1()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		WaitAddress = 0xffffffff;

		X_L( X + 1 );
		SetZN8( X_L() );
	}

	private void OpE9M0()
	{
		SBC16( Immediate16Read() );
	}

	private void OpE9M1()
	{
		SBC8( Immediate8Read() );
	}

	private void OpE9Slow()
	{
		if ( CheckMemory() )
		{
			SBC8( Immediate8SlowRead() );
		}
		else
		{
			SBC16( Immediate16SlowRead() );
		}
	}

	private void OpEA()
	{
		AddCycles( SnesSystem.ONE_CYCLE );
	}

	private void OpEB()
	{
		int Work8 = A_L();
		A_L( A_H() );
		A_H( Work8 );
		SetZN8( A_L() );
		AddCycles( SnesSystem.TWO_CYCLES );
	}

	private void OpECSlow()
	{
		if ( CheckIndex() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			CPX8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			CPX16( val );
		}
	}

	private void OpECX0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		CPX16( val );
	}

	private void OpECX1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		CPX8( val );
	}

	private void OpEDM0()
	{
		int val = GetWord( AbsoluteRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpEDM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteRead() );
		SBC8( val );
	}

	private void OpEDSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( AbsoluteSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpEEM0()
	{
		INC16( AbsoluteRead(), Memory.WRAP_NONE );
	}

	private void OpEEM1()
	{
		INC8( AbsoluteRead() );
	}

	private void OpEESlow()
	{
		if ( CheckMemory() )
		{
			INC8( AbsoluteSlowRead() );
		}
		else
		{
			INC16( AbsoluteSlowRead(), Memory.WRAP_NONE );
		}
	}

	private void OpEFM0()
	{
		int val = GetWord( AbsoluteLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpEFM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongRead() );
		SBC8( val );
	}

	private void OpEFSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpF0E0()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 1 )
			{
				PCw = newPC;
			}
			else if ( globals.settings.SoundSkipMethod == 3 )
			{
				if ( PCw > newPC ) return;
			}
			else
			{
				PCw = newPC;
			}
		}

		if ( _Zero == 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void OpF0E1()
	{
		int newPC = Relative();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 3 && PCw > newPC )
			{
				return;
			}
			else
			{
				PCw = newPC;
			}
		}

		if ( _Zero == 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void OpF0Slow()
	{
		int newPC = RelativeSlow();
		
		if ( BranchSkip )
		{
			BranchSkip = false;
			if ( globals.settings.SoundSkipMethod == 0 )
			{
				if ( PCw > newPC ) return;
			}
			else if ( globals.settings.SoundSkipMethod == 3 && PCw > newPC )
			{
				return;
			}
			else
			{
				PCw = newPC;
			}
		}

		if ( _Zero == 0 )
		{
			AddCycles( SnesSystem.ONE_CYCLE );
			
			if ( CheckEmulation() && PCh() != ( ( newPC & 0xFF00 ) >>> 8 ) )
			{
				AddCycles( SnesSystem.ONE_CYCLE );
			}
			if ( ( PCw & ~( ( 0x1000 ) - 1 ) ) != ( newPC & ~( ( 0x1000 ) - 1 ) ) )
			{
				SetPCBase( ShiftedPB + newPC );
			}
			else
			{
				PCw = newPC;
			}
		}
	}

	private void OpF1E0M0X0()
	{
		int val = GetWord( DirectIndirectIndexedE0X0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpF1E0M0X1()
	{
		int val = GetWord( DirectIndirectIndexedE0X1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpF1E0M1X0()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X0Read() );
		SBC8( val );
	}

	private void OpF1E0M1X1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE0X1Read() );
		SBC8( val );
	}

	private void OpF1E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedE1Read() );
		SBC8( val );
	}

	private void OpF1Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpF2E0M0()
	{
		int val = GetWord( DirectIndirectE0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpF2E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE0Read() );
		SBC8( val );
	}

	private void OpF2E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectE1Read() );
		SBC8( val );
	}

	private void OpF2Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpF3M0()
	{
		int val = GetWord( StackRelativeIndirectIndexedRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpF3M1()
	{
		int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedRead() );
		SBC8( val );
	}

	private void OpF3Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( StackRelativeIndirectIndexedSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( StackRelativeIndirectIndexedSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpF4E0()
	{
		int val = Absolute();
		SetWord( val, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = val & 0xff;
	}

	private void OpF4E1()
	{
		int val = Absolute();
		SetWord( val, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = val & 0xff;
		S_H(1);
	}

	private void OpF4Slow()
	{
		int val = AbsoluteSlow();
		SetWord( val, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		globals.OpenBus = val & 0xff;
		if ( CheckEmulation() )
			S_H(1);
	}

	private void OpF5E0M0()
	{
		int val = GetWord( DirectIndexedXE0Read(), Memory.WRAP_BANK );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpF5E0M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE0Read() );
		SBC8( val );
	}

	private void OpF5E1()
	{
		int val = globals.OpenBus = GetByte( DirectIndexedXE1Read() );
		SBC8( val );
	}

	private void OpF5Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndexedXSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpF6E0M0()
	{
		INC16( DirectIndexedXE0Read(), Memory.WRAP_BANK );
	}

	private void OpF6E0M1()
	{
		INC8( DirectIndexedXE0Read() );
	}

	private void OpF6E1()
	{
		INC8( DirectIndexedXE1Read() );
	}

	private void OpF6Slow()
	{
		if ( CheckMemory() )
		{
			INC8( DirectIndexedXSlowRead() );
		}
		else
		{
			INC16( DirectIndexedXSlowRead(), Memory.WRAP_BANK );
		}
	}

	private void OpF7M0()
	{
		int val = GetWord( DirectIndirectIndexedLongRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpF7M1()
	{
		int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongRead() );
		SBC8( val );
	}

	private void OpF7Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( DirectIndirectIndexedLongSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( DirectIndirectIndexedLongSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpF8()
	{
		PL = PL | 8;
		AddCycles( SnesSystem.ONE_CYCLE );
	}

	private void OpF9M0X0()
	{
		int val = GetWord( AbsoluteIndexedYX0Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpF9M0X1()
	{
		int val = GetWord( AbsoluteIndexedYX1Read(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpF9M1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX0Read() );
		SBC8( val );
	}

	private void OpF9M1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedYX1Read() );
		SBC8( val );
	}

	private void OpF9Slow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedYSlow() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedYSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpFAE0X0()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		X = GetWord( S + 1, Memory.WRAP_BANK );
		S_W( S + 2 );
		SetZN16( X );
		globals.OpenBus = X_H();
	}

	private void OpFAE0X1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		X_L( GetByte( ++S ) );
		SetZN8( X_L() );
		globals.OpenBus = X_L();
	}

	private void OpFAE1()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		S_L( S + 1 );
		X_L( GetByte( S ) );
		SetZN8( X_L() );
		globals.OpenBus = X_L();
	}

	private void OpFASlow()
	{
		AddCycles( SnesSystem.TWO_CYCLES );
		if ( CheckEmulation() )
		{
			S_L( S + 1 );
			X_L( GetByte( S ) );
			SetZN8( X_L() );
			globals.OpenBus = X_L();
		}
		else if ( CheckIndex() )
		{
			X_L( GetByte( ++S ) );
			SetZN8( X_L() );
			globals.OpenBus = X_L();
		}
		else
		{
			X = GetWord( S + 1, Memory.WRAP_BANK );
			S_W( S + 2 );
			SetZN16( X );
			globals.OpenBus = X_H();
		}
	}

	private void OpFB()
	{
		AddCycles( SnesSystem.ONE_CYCLE );

		int A1 = _Carry;
		int A2 = PH;
		_Carry = A2 & 1;
		PH = A1;

		if ( CheckEmulation() )
		{
			PL |= ( IndexFlag | MemoryFlag ) ;
			S_H(1);
		}
		
		if ( CheckIndex() )
		{
			X_H(0);
			Y_H(0);
		}
		
		FixCycles();
	}

	private void OpFCE0()
	{
		int addr = AbsoluteIndexedIndirect() & 0xFFFF;
		SetWord( PCw - 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		SetPCBase( ShiftedPB + addr );
	}

	private void OpFCE1()
	{

		int addr = AbsoluteIndexedIndirect() & 0xFFFF;
		SetWord( PCw - 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		S_H(1);
		SetPCBase( ShiftedPB + addr );
	}

	private void OpFCSlow()
	{
		int addr = AbsoluteIndexedIndirectSlowJsr() & 0xFFFF;
		SetWord( PCw - 1, S - 1, Memory.WRAP_BANK, Memory.WRITE_10 );
		S -= 2;
		if ( CheckEmulation() )
			S_H(1);
		
		SetPCBase( ShiftedPB + addr );
	}

	private void OpFDM0X0()
	{
		int val = GetWord( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpFDM0X1()
	{
		int val = GetWord( AbsoluteIndexedXX1(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpFDM1X0()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX0() );
		SBC8( val );
	}

	private void OpFDM1X1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteIndexedXX1() );
		SBC8( val );
	}

	private void OpFDSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteIndexedXSlow() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( AbsoluteIndexedXSlow(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void OpFEM0X0()
	{
		INC16( AbsoluteIndexedXX0(), Memory.WRAP_NONE );
	}

	private void OpFEM0X1()
	{
		INC16( AbsoluteIndexedXX1Modify(), Memory.WRAP_NONE );
	}

	private void OpFEM1X0()
	{
		INC8( AbsoluteIndexedXX0() );
	}

	private void OpFEM1X1()
	{
		INC8( AbsoluteIndexedXX1Modify() );
	}

	private void OpFESlow()
	{
		if ( CheckMemory() )
		{
			INC8( AbsoluteIndexedXSlowModify() );
		}
		else
		{
			INC16( AbsoluteIndexedXSlowModify(), Memory.WRAP_NONE );
		}
	}

	private void OpFFM0()
	{
		int val = GetWord( AbsoluteLongIndexedXRead(), Memory.WRAP_NONE );
		globals.OpenBus = ( val >>> 8 );
		SBC16( val );
	}

	private void OpFFM1()
	{
		int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXRead() );
		SBC8( val );
	}

	private void OpFFSlow()
	{
		if ( CheckMemory() )
		{
			int val = globals.OpenBus = GetByte( AbsoluteLongIndexedXSlowRead() );
			SBC8( val );
		}
		else
		{
			int val = GetWord( AbsoluteLongIndexedXSlowRead(), Memory.WRAP_NONE );
			globals.OpenBus = ( val >>> 8 );
			SBC16( val );
		}
	}

	private void ORA16( int val )
	{
		A |= val;
		SetZN16( A );
	}

	private void ORA8 ( int val )
	{
		A_L( A | val );
		SetZN8( A_L() );
	}

 	void PackStatus()
	{
	    PL = PL & ~(Zero | Negative | Carry | Overflow);
	    PL = PL | _Carry | ( ( _Zero == 0 ) ? 1 << 1 : 0 ) | ( _Negative & 0x80 ) | ( _Overflow << 6 );
	}
 	
 	final int PB()
	{
		return PCb;
	}
 	
 	final void PB( int value )
	{ 
		PCb = value; 
	}
 	
 	final int PBPC()
	{
		return (PCb << 16) | PCw;
	}
 	
	final void PBPC( int value )
	{
		PCw = value & 0xFFFF;
		PCb = (value >>> 16);
	}
	
	final int PCh()
	{
		return ( PCw & 0xFF00 ) >>> 8;
	}

	final void PCh( int value )
	{ 
		PCw = (PCw & 0x00FF) | ( ( value << 8 ) & 0x0000FF00 );
	}

	final int PCl()
	{
		return PCw & 0x000000FF;
	}
	
	private int Relative()  // branch $xx
	{
		byte int8 = (byte)Immediate8();
		return ( PCw + int8 ) & 0xffff;
	}
   
	private int RelativeLong () // BRL $xxxx
	{
		short offset = (short)Immediate16();
		return ( PCw + offset ) & 0xffff;
	}
	
	private int RelativeLongSlow() // BRL $xxxx
	{
		short offset = (short)Immediate16Slow();
		return ( PCw + offset ) & 0xffff;
	}
	
	private int RelativeSlow ()
	{
		byte int8 = (byte) Immediate8Slow();
		return ( PCw + int8 ) & 0xffff;
	}

	private void ROL16( int OpAddress, int w )
	{
		int Work32 = ( ( GetWord( OpAddress, w ) ) << 1 ) | _Carry;
		_Carry = Work32 >= 0x10000 ? 1 : 0;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( Work32 & 0xFFFF, OpAddress, w, Memory.WRITE_10 );
		globals.OpenBus = ( Work32 & 0xff );
		SetZN16( Work32 & 0xFFFF );
	}

	private void ROL8( int OpAddress )
	{
		int Work16 = ( ( GetByte( OpAddress ) ) << 1 ) | _Carry;
		_Carry = Work16 >= 0x100 ? 1 : 0;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Work16 & 0xFF, OpAddress );
		globals.OpenBus = Work16 & 0xff;
		SetZN8( Work16 & 0xFF );
	}

	private void ROR16 ( int OpAddress, int w )
	{
		int Work32 = ( GetWord( OpAddress, w ) ) | ( _Carry << 16 );
		_Carry = Work32 & 1;
		Work32 = Work32 >> 1;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( Work32 & 0xFFFF, OpAddress, w, Memory.WRITE_10 );
		globals.OpenBus = ( Work32 & 0xff );
		SetZN16( Work32 & 0xFFFF );
	}

	private void ROR8 ( int OpAddress )
	{
		int Work16 = ( GetByte( OpAddress ) ) | ( _Carry << 8 );
		_Carry = Work16 & 1;
		Work16 = Work16 >> 1;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Work16 & 0xFF, OpAddress );
		globals.OpenBus = Work16 & 0xff;
		SetZN8( Work16 & 0xFFFF );
	}

	private void SBC16 ( int Work16 )
	{
		if ( CheckDecimal() )
		{
			int A1 = A & 0x000F;
			int A2 = A & 0x00F0;
			int A3 = A & 0x0F00;
			int A4 = A & 0xF000;
			int W1 = Work16 & 0x000F;
			int W2 = Work16 & 0x00F0;
			int W3 = Work16 & 0x0F00;
			int W4 = Work16 & 0xF000;

			A1 -= W1 + (_Carry ^ 1);
			A2 -= W2;
			A3 -= W3;
			A4 -= W4;
			if ( A1 > 0x000F )
			{
				A1 += 0x000A;
				A1 &= 0x000F;
				A2 -= 0x0010;
			}
			if ( A2 > 0x00F0 )
			{
				A2 += 0x00A0;
				A2 &= 0x00F0;
				A3 -= 0x0100;
			}
			if ( A3 > 0x0F00 )
			{
				A3 += 0x0A00;
				A3 &= 0x0F00;
				A4 -= 0x1000;
			}
			if ( A4 > 0xF000 )
			{
				A4 += 0xA000;
				A4 &= 0xF000;
				ClearCarry();
			}
			else
			{
				SetCarry();
			}

			int Ans16 = A4 | A3 | A2 | A1;
			
			if ( ( ( A ^ Work16 ) & ( A ^ Ans16 ) & 0x8000 ) != 0 )
			{
				SetOverflow();
			}
			else
			{
				ClearOverflow();
			}
			A_W( Ans16 );
			SetZN16( A );
		}
		else
		{
			int Int32 = A - Work16 + _Carry - 1;

			_Carry = Int32 >= 0 ? 1 : 0;

			if ( ( ( A ^ Work16 ) & ( A ^ ( Int32 & 0xFFFF ) ) & 0x8000 ) != 0 )
			{
				SetOverflow();
			}
			else
			{
				ClearOverflow();
			}
			
			A_W( Int32 );
			SetZN16( A );
		}
	}

	private void SBC8 ( int Work8 )
	{
		if ( CheckDecimal () )
		{
			int A1 = A & 0x0F;
			int A2 = A & 0xF0;
			int W1 = Work8 & 0x0F;
			int W2 = Work8 & 0xF0;

			A1 -= W1 + (_Carry ^ 1);
			A2 -= W2;
			
			if ( A1 > 0x0F )
			{
				A1 += 0x0A;
				A1 &= 0x0F;
				A2 -= 0x10;
			}
			if ( A2 > 0xF0 )
			{
				A2 += 0xA0;
				A2 &= 0xF0;
				ClearCarry();
			}
			else
			{
				SetCarry();
			}

			int Ans8 = ( A2 & 0xFF ) | A1;
			int AL = A_L();
			
			if ( ( ( AL ^ Work8 ) & ( AL ^ Ans8 ) & 0x80 ) != 0)
			{
				SetOverflow();
			}
			else
			{
				ClearOverflow();
			}
			A_L( Ans8 );
			SetZN8( A_L() );
		}
		else
		{
			int AL = A_L();
			int Int16 = AL - Work8 + _Carry - 1;

			_Carry = Int16 >= 0 ? 1 : 0;
			
			if ( ( ( AL ^ Work8 ) & ( AL ^ ( Int16 & 0xFF ) ) & 0x80 ) != 0 )
			{
				SetOverflow();
			}
			else
			{
				ClearOverflow();
			}
			
			A_L( Int16 );
			SetZN8( A_L() );
		}
	}

	final private void SetCarry()
	{
		_Carry = 1;
	}

	final void SetDecimal()
	{
		PL |= Decimal;
	}

	final void SetFlags ( int pl, int ph )
	{
		PL |= pl;
		PL |= ph;
	}

	final void SetOverflow()
	{
		_Overflow = 1;
	}

	// Assumes 16 bit argument
	private void SetZN16 ( int Work )
	{
		_Zero = Work != 0 ? 1 : 0;
		_Negative = ( Work >>> 8 );
	}

	// Assumes 8 bit argument
	private void SetZN8 ( int Work )
	{
		_Zero = Work != 0 ? 1 : 0;
		_Negative = Work;
	}

	private void STA16( int OpAddress, int w )
	{
		SetWord( A, OpAddress, w, Memory.WRITE_01 );
		globals.OpenBus = A_H();
	}

	private void STA8( int OpAddress )
	{
		SetByte( A_L(), OpAddress );
		globals.OpenBus = A_L();
	}

	private int StackRelative() // d,S
	{
		int addr = Immediate8() + S;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}

	private int StackRelativeIndirectIndexed() // (d,S),Y
	{
		int addr = GetWord( StackRelative() );
		addr = ( addr + Y + ShiftedDB ) & 0xffffff;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}
	
	private int StackRelativeIndirectIndexedRead() // (d,S),Y
	{
		int addr = GetWord( StackRelativeRead() );
		globals.OpenBus =  ( addr >>> 8 ) & 0xFF;
		addr = ( addr + Y + ShiftedDB ) & 0xffffff;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}

	private int StackRelativeIndirectIndexedSlow() // (d,S),Y
	{
		int addr = GetWord( StackRelativeSlow() );
		addr = ( addr + Y + ShiftedDB ) & 0xffffff;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}

	private int StackRelativeIndirectIndexedSlowRead() // (d,S),Y
	{
		int addr = GetWord( StackRelativeSlowRead() );
		globals.OpenBus = ( addr >>> 8 ) & 0xFF;
		addr = ( addr + Y + ShiftedDB ) & 0xffffff;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}

	private int StackRelativeRead() // d,S
	{
		int addr = Immediate8Read() + S;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}

	private int StackRelativeSlow() // d,S
	{
		int addr = Immediate8Slow() + S;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}

	private int StackRelativeSlowRead() // d,S
	{
		int addr = Immediate8SlowRead() + S;
		AddCycles( SnesSystem.ONE_CYCLE );
		return addr;
	}

	private void STX16( int OpAddress, int w )
	{
		SetWord( X, OpAddress, w, Memory.WRITE_01);
		globals.OpenBus = X_H();
	}

	private void STX8( int OpAddress )
	{
		SetByte( X_L(), OpAddress );
		globals.OpenBus = X_L();
	}

	private void STY16( int OpAddress, int w )
	{
		SetWord( Y, OpAddress, w, Memory.WRITE_01);
		globals.OpenBus = Y_H();
	}

	private void STY8( int OpAddress )
	{
		SetByte( Y_L(), OpAddress );
		globals.OpenBus = Y_L();
	}

	private void STZ16( int OpAddress, int w )
	{
		SetWord( 0, OpAddress, w, Memory.WRITE_01 );
		globals.OpenBus = 0;
	}

	private void STZ8( int OpAddress )
	{
		SetByte( 0, OpAddress );
		globals.OpenBus = 0;
	}

	private void TRB16( int OpAddress, int w )
	{
		int Work16 = GetWord( OpAddress, w );
		_Zero = ( Work16 & A ) != 0 ? 1 : 0;
		Work16 &= (~A & 0xFFFF) ;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( Work16, OpAddress, w, Memory.WRITE_10 );
		globals.OpenBus = Work16 & 0xff;
	}

	private void TRB8( int OpAddress )
	{
		int Work8 = GetByte( OpAddress );
		_Zero = Work8 & A_L();
		Work8 &= (~A & 0xFF);
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Work8, OpAddress );
		globals.OpenBus = Work8;
	}

	private void TSB16( int OpAddress, int w )
	{
		int Work16 = GetWord( OpAddress, w );
		_Zero = ( Work16 & A ) != 0 ? 1 : 0;
		Work16 |= A;
		AddCycles( SnesSystem.ONE_CYCLE );
		SetWord( Work16, OpAddress, w, Memory.WRITE_10 );
		globals.OpenBus = Work16 & 0xff;
	}

	private void TSB8( int OpAddress )
	{
		int Work8 = GetByte( OpAddress );
		_Zero = Work8 & A_L();
		Work8 |= A_L();
		AddCycles( SnesSystem.ONE_CYCLE );
		SetByte( Work8, OpAddress );
		globals.OpenBus = Work8;
	}
	
	void UnpackStatus()
	{
	    _Zero = (PL & Zero) == 0 ? 1 : 0;
	    _Negative = PL & Negative;
	    _Carry = PL & Carry;
	    _Overflow = (PL & Overflow) >> 6;
	}

	final void ExecuteOp( int opCode )
	{		
		switch ( Opcodes | opCode )
		{
		// M1X1 Opcodes
		case 0x000: Op00(); break;
		case 0x001: Op01E0M1(); break;
		case 0x002: Op02(); break;
		case 0x003: Op03M1(); break;
		case 0x004: Op04M1(); break;
		case 0x005: Op05M1(); break;
		case 0x006: Op06M1(); break;
		case 0x007: Op07M1(); break;
		case 0x008: Op08E0(); break;
		case 0x009: Op09M1(); break;
		case 0x00A: Op0AM1(); break;
		case 0x00B: Op0BE0(); break;
		case 0x00C: Op0CM1(); break;
		case 0x00D: Op0DM1(); break;
		case 0x00E: Op0EM1(); break;
		case 0x00F: Op0FM1(); break;
		case 0x010: Op10E0(); break;
		case 0x011: Op11E0M1X1(); break;
		case 0x012: Op12E0M1(); break;
		case 0x013: Op13M1(); break;
		case 0x014: Op14M1(); break;
		case 0x015: Op15E0M1(); break;
		case 0x016: Op16E0M1(); break;
		case 0x017: Op17M1(); break;
		case 0x018: Op18(); break;
		case 0x019: Op19M1X1(); break;
		case 0x01A: Op1AM1(); break;
		case 0x01B: Op1B(); break;
		case 0x01C: Op1CM1(); break;
		case 0x01D: Op1DM1X1(); break;
		case 0x01E: Op1EM1X1(); break;
		case 0x01F: Op1FM1(); break;
		case 0x020: Op20E0(); break;
		case 0x021: Op21E0M1(); break;
		case 0x022: Op22E0(); break;
		case 0x023: Op23M1(); break;
		case 0x024: Op24M1(); break;
		case 0x025: Op25M1(); break;
		case 0x026: Op26M1(); break;
		case 0x027: Op27M1(); break;
		case 0x028: Op28E0(); break;
		case 0x029: Op29M1(); break;
		case 0x02A: Op2AM1(); break;
		case 0x02B: Op2BE0(); break;
		case 0x02C: Op2CM1(); break;
		case 0x02D: Op2DM1(); break;
		case 0x02E: Op2EM1(); break;
		case 0x02F: Op2FM1(); break;
		case 0x030: Op30E0(); break;
		case 0x031: Op31E0M1X1(); break;
		case 0x032: Op32E0M1(); break;
		case 0x033: Op33M1(); break;
		case 0x034: Op34E0M1(); break;
		case 0x035: Op35E0M1(); break;
		case 0x036: Op36E0M1(); break;
		case 0x037: Op37M1(); break;
		case 0x038: Op38(); break;
		case 0x039: Op39M1X1(); break;
		case 0x03A: Op3AM1(); break;
		case 0x03B: Op3B(); break;
		case 0x03C: Op3CM1X1(); break;
		case 0x03D: Op3DM1X1(); break;
		case 0x03E: Op3EM1X1(); break;
		case 0x03F: Op3FM1(); break;
		case 0x040: Op40Slow(); break;
		case 0x041: Op41E0M1(); break;
		case 0x042: Op42(); break;
		case 0x043: Op43M1(); break;
		case 0x044: Op44X1(); break;
		case 0x045: Op45M1(); break;
		case 0x046: Op46M1(); break;
		case 0x047: Op47M1(); break;
		case 0x048: Op48E0M1(); break;
		case 0x049: Op49M1(); break;
		case 0x04A: Op4AM1(); break;
		case 0x04B: Op4BE0(); break;
		case 0x04C: Op4C(); break;
		case 0x04D: Op4DM1(); break;
		case 0x04E: Op4EM1(); break;
		case 0x04F: Op4FM1(); break;
		case 0x050: Op50E0(); break;
		case 0x051: Op51E0M1X1(); break;
		case 0x052: Op52E0M1(); break;
		case 0x053: Op53M1(); break;
		case 0x054: Op54X1(); break;
		case 0x055: Op55E0M1(); break;
		case 0x056: Op56E0M1(); break;
		case 0x057: Op57M1(); break;
		case 0x058: Op58(); break;
		case 0x059: Op59M1X1(); break;
		case 0x05A: Op5AE0X1(); break;
		case 0x05B: Op5B(); break;
		case 0x05C: Op5C(); break;
		case 0x05D: Op5DM1X1(); break;
		case 0x05E: Op5EM1X1(); break;
		case 0x05F: Op5FM1(); break;
		case 0x060: Op60E0(); break;
		case 0x061: Op61E0M1(); break;
		case 0x062: Op62E0(); break;
		case 0x063: Op63M1(); break;
		case 0x064: Op64M1(); break;
		case 0x065: Op65M1(); break;
		case 0x066: Op66M1(); break;
		case 0x067: Op67M1(); break;
		case 0x068: Op68E0M1(); break;
		case 0x069: Op69M1(); break;
		case 0x06A: Op6AM1(); break;
		case 0x06B: Op6BE0(); break;
		case 0x06C: Op6C(); break;
		case 0x06D: Op6DM1(); break;
		case 0x06E: Op6EM1(); break;
		case 0x06F: Op6FM1(); break;
		case 0x070: Op70E0(); break;
		case 0x071: Op71E0M1X1(); break;
		case 0x072: Op72E0M1(); break;
		case 0x073: Op73M1(); break;
		case 0x074: Op74E0M1(); break;
		case 0x075: Op75E0M1(); break;
		case 0x076: Op76E0M1(); break;
		case 0x077: Op77M1(); break;
		case 0x078: Op78(); break;
		case 0x079: Op79M1X1(); break;
		case 0x07A: Op7AE0X1(); break;
		case 0x07B: Op7B(); break;
		case 0x07C: Op7C(); break;
		case 0x07D: Op7DM1X1(); break;
		case 0x07E: Op7EM1X1(); break;
		case 0x07F: Op7FM1(); break;
		case 0x080: Op80E0(); break;
		case 0x081: Op81E0M1(); break;
		case 0x082: Op82(); break;
		case 0x083: Op83M1(); break;
		case 0x084: Op84X1(); break;
		case 0x085: Op85M1(); break;
		case 0x086: Op86X1(); break;
		case 0x087: Op87M1(); break;
		case 0x088: Op88X1(); break;
		case 0x089: Op89M1(); break;
		case 0x08A: Op8AM1(); break;
		case 0x08B: Op8BE0(); break;
		case 0x08C: Op8CX1(); break;
		case 0x08D: Op8DM1(); break;
		case 0x08E: Op8EX1(); break;
		case 0x08F: Op8FM1(); break;
		case 0x090: Op90E0(); break;
		case 0x091: Op91E0M1X1(); break;
		case 0x092: Op92E0M1(); break;
		case 0x093: Op93M1(); break;
		case 0x094: Op94E0X1(); break;
		case 0x095: Op95E0M1(); break;
		case 0x096: Op96E0X1(); break;
		case 0x097: Op97M1(); break;
		case 0x098: Op98M1(); break;
		case 0x099: Op99M1X1(); break;
		case 0x09A: Op9A(); break;
		case 0x09B: Op9BX1(); break;
		case 0x09C: Op9CM1(); break;
		case 0x09D: Op9DM1X1(); break;
		case 0x09E: Op9EM1X1(); break;
		case 0x09F: Op9FM1(); break;
		case 0x0A0: OpA0X1(); break;
		case 0x0A1: OpA1E0M1(); break;
		case 0x0A2: OpA2X1(); break;
		case 0x0A3: OpA3M1(); break;
		case 0x0A4: OpA4X1(); break;
		case 0x0A5: OpA5M1(); break;
		case 0x0A6: OpA6X1(); break;
		case 0x0A7: OpA7M1(); break;
		case 0x0A8: OpA8X1(); break;
		case 0x0A9: OpA9M1(); break;
		case 0x0AA: OpAAX1(); break;
		case 0x0AB: OpABE0(); break;
		case 0x0AC: OpACX1(); break;
		case 0x0AD: OpADM1(); break;
		case 0x0AE: OpAEX1(); break;
		case 0x0AF: OpAFM1(); break;
		case 0x0B0: OpB0E0(); break;
		case 0x0B1: OpB1E0M1X1(); break;
		case 0x0B2: OpB2E0M1(); break;
		case 0x0B3: OpB3M1(); break;
		case 0x0B4: OpB4E0X1(); break;
		case 0x0B5: OpB5E0M1(); break;
		case 0x0B6: OpB6E0X1(); break;
		case 0x0B7: OpB7M1(); break;
		case 0x0B8: OpB8(); break;
		case 0x0B9: OpB9M1X1(); break;
		case 0x0BA: OpBAX1(); break;
		case 0x0BB: OpBBX1(); break;
		case 0x0BC: OpBCX1(); break;
		case 0x0BD: OpBDM1X1(); break;
		case 0x0BE: OpBEX1(); break;
		case 0x0BF: OpBFM1(); break;
		case 0x0C0: OpC0X1(); break;
		case 0x0C1: OpC1E0M1(); break;
		case 0x0C2: OpC2(); break;
		case 0x0C3: OpC3M1(); break;
		case 0x0C4: OpC4X1(); break;
		case 0x0C5: OpC5M1(); break;
		case 0x0C6: OpC6M1(); break;
		case 0x0C7: OpC7M1(); break;
		case 0x0C8: OpC8X1(); break;
		case 0x0C9: OpC9M1(); break;
		case 0x0CA: OpCAX1(); break;
		case 0x0CB: OpCB(); break;
		case 0x0CC: OpCCX1(); break;
		case 0x0CD: OpCDM1(); break;
		case 0x0CE: OpCEM1(); break;
		case 0x0CF: OpCFM1(); break;
		case 0x0D0: OpD0E0(); break;
		case 0x0D1: OpD1E0M1X1(); break;
		case 0x0D2: OpD2E0M1(); break;
		case 0x0D3: OpD3M1(); break;
		case 0x0D4: OpD4E0(); break;
		case 0x0D5: OpD5E0M1(); break;
		case 0x0D6: OpD6E0M1(); break;
		case 0x0D7: OpD7M1(); break;
		case 0x0D8: OpD8(); break;
		case 0x0D9: OpD9M1X1(); break;
		case 0x0DA: OpDAE0X1(); break;
		case 0x0DB: OpDB(); break;
		case 0x0DC: OpDC(); break;
		case 0x0DD: OpDDM1X1(); break;
		case 0x0DE: OpDEM1X1(); break;
		case 0x0DF: OpDFM1(); break;
		case 0x0E0: OpE0X1(); break;
		case 0x0E1: OpE1E0M1(); break;
		case 0x0E2: OpE2(); break;
		case 0x0E3: OpE3M1(); break;
		case 0x0E4: OpE4X1(); break;
		case 0x0E5: OpE5M1(); break;
		case 0x0E6: OpE6M1(); break;
		case 0x0E7: OpE7M1(); break;
		case 0x0E8: OpE8X1(); break;
		case 0x0E9: OpE9M1(); break;
		case 0x0EA: OpEA(); break;
		case 0x0EB: OpEB(); break;
		case 0x0EC: OpECX1(); break;
		case 0x0ED: OpEDM1(); break;
		case 0x0EE: OpEEM1(); break;
		case 0x0EF: OpEFM1(); break;
		case 0x0F0: OpF0E0(); break;
		case 0x0F1: OpF1E0M1X1(); break;
		case 0x0F2: OpF2E0M1(); break;
		case 0x0F3: OpF3M1(); break;
		case 0x0F4: OpF4E0(); break;
		case 0x0F5: OpF5E0M1(); break;
		case 0x0F6: OpF6E0M1(); break;
		case 0x0F7: OpF7M1(); break;
		case 0x0F8: OpF8(); break;
		case 0x0F9: OpF9M1X1(); break;
		case 0x0FA: OpFAE0X1(); break;
		case 0x0FB: OpFB(); break;
		case 0x0FC: OpFCE0(); break;
		case 0x0FD: OpFDM1X1(); break;
		case 0x0FE: OpFEM1X1(); break;
		case 0x0FF: OpFFM1(); break;

		// OpcodesE1
		case 0x100: Op00(); break;
		case 0x101: Op01E1(); break;
		case 0x102: Op02(); break;
		case 0x103: Op03M1(); break;
		case 0x104: Op04M1(); break;
		case 0x105: Op05M1(); break;
		case 0x106: Op06M1(); break;
		case 0x107: Op07M1(); break;
		case 0x108: Op08E1(); break;
		case 0x109: Op09M1(); break;
		case 0x10A: Op0AM1(); break;
		case 0x10B: Op0BE1(); break;
		case 0x10C: Op0CM1(); break;
		case 0x10D: Op0DM1(); break;
		case 0x10E: Op0EM1(); break;
		case 0x10F: Op0FM1(); break;
		case 0x110: Op10E1(); break;
		case 0x111: Op11E1(); break;
		case 0x112: Op12E1(); break;
		case 0x113: Op13M1(); break;
		case 0x114: Op14M1(); break;
		case 0x115: Op15E1(); break;
		case 0x116: Op16E1(); break;
		case 0x117: Op17M1(); break;
		case 0x118: Op18(); break;
		case 0x119: Op19M1X1(); break;
		case 0x11A: Op1AM1(); break;
		case 0x11B: Op1B(); break;
		case 0x11C: Op1CM1(); break;
		case 0x11D: Op1DM1X1(); break;
		case 0x11E: Op1EM1X1(); break;
		case 0x11F: Op1FM1(); break;
		case 0x120: Op20E1(); break;
		case 0x121: Op21E1(); break;
		case 0x122: Op22E1(); break;
		case 0x123: Op23M1(); break;
		case 0x124: Op24M1(); break;
		case 0x125: Op25M1(); break;
		case 0x126: Op26M1(); break;
		case 0x127: Op27M1(); break;
		case 0x128: Op28E1(); break;
		case 0x129: Op29M1(); break;
		case 0x12A: Op2AM1(); break;
		case 0x12B: Op2BE1(); break;
		case 0x12C: Op2CM1(); break;
		case 0x12D: Op2DM1(); break;
		case 0x12E: Op2EM1(); break;
		case 0x12F: Op2FM1(); break;
		case 0x130: Op30E1(); break;
		case 0x131: Op31E1(); break;
		case 0x132: Op32E1(); break;
		case 0x133: Op33M1(); break;
		case 0x134: Op34E1(); break;
		case 0x135: Op35E1(); break;
		case 0x136: Op36E1(); break;
		case 0x137: Op37M1(); break;
		case 0x138: Op38(); break;
		case 0x139: Op39M1X1(); break;
		case 0x13A: Op3AM1(); break;
		case 0x13B: Op3B(); break;
		case 0x13C: Op3CM1X1(); break;
		case 0x13D: Op3DM1X1(); break;
		case 0x13E: Op3EM1X1(); break;
		case 0x13F: Op3FM1(); break;
		case 0x140: Op40Slow(); break;
		case 0x141: Op41E1(); break;
		case 0x142: Op42(); break;
		case 0x143: Op43M1(); break;
		case 0x144: Op44X1(); break;
		case 0x145: Op45M1(); break;
		case 0x146: Op46M1(); break;
		case 0x147: Op47M1(); break;
		case 0x148: Op48E1(); break;
		case 0x149: Op49M1(); break;
		case 0x14A: Op4AM1(); break;
		case 0x14B: Op4BE1(); break;
		case 0x14C: Op4C(); break;
		case 0x14D: Op4DM1(); break;
		case 0x14E: Op4EM1(); break;
		case 0x14F: Op4FM1(); break;
		case 0x150: Op50E1(); break;
		case 0x151: Op51E1(); break;
		case 0x152: Op52E1(); break;
		case 0x153: Op53M1(); break;
		case 0x154: Op54X1(); break;
		case 0x155: Op55E1(); break;
		case 0x156: Op56E1(); break;
		case 0x157: Op57M1(); break;
		case 0x158: Op58(); break;
		case 0x159: Op59M1X1(); break;
		case 0x15A: Op5AE1(); break;
		case 0x15B: Op5B(); break;
		case 0x15C: Op5C(); break;
		case 0x15D: Op5DM1X1(); break;
		case 0x15E: Op5EM1X1(); break;
		case 0x15F: Op5FM1(); break;
		case 0x160: Op60E1(); break;
		case 0x161: Op61E1(); break;
		case 0x162: Op62E1(); break;
		case 0x163: Op63M1(); break;
		case 0x164: Op64M1(); break;
		case 0x165: Op65M1(); break;
		case 0x166: Op66M1(); break;
		case 0x167: Op67M1(); break;
		case 0x168: Op68E1(); break;
		case 0x169: Op69M1(); break;
		case 0x16A: Op6AM1(); break;
		case 0x16B: Op6BE1(); break;
		case 0x16C: Op6C(); break;
		case 0x16D: Op6DM1(); break;
		case 0x16E: Op6EM1(); break;
		case 0x16F: Op6FM1(); break;
		case 0x170: Op70E1(); break;
		case 0x171: Op71E1(); break;
		case 0x172: Op72E1(); break;
		case 0x173: Op73M1(); break;
		case 0x174: Op74E1(); break;
		case 0x175: Op75E1(); break;
		case 0x176: Op76E1(); break;
		case 0x177: Op77M1(); break;
		case 0x178: Op78(); break;
		case 0x179: Op79M1X1(); break;
		case 0x17A: Op7AE1(); break;
		case 0x17B: Op7B(); break;
		case 0x17C: Op7C(); break;
		case 0x17D: Op7DM1X1(); break;
		case 0x17E: Op7EM1X1(); break;
		case 0x17F: Op7FM1(); break;
		case 0x180: Op80E1(); break;
		case 0x181: Op81E1(); break;
		case 0x182: Op82(); break;
		case 0x183: Op83M1(); break;
		case 0x184: Op84X1(); break;
		case 0x185: Op85M1(); break;
		case 0x186: Op86X1(); break;
		case 0x187: Op87M1(); break;
		case 0x188: Op88X1(); break;
		case 0x189: Op89M1(); break;
		case 0x18A: Op8AM1(); break;
		case 0x18B: Op8BE1(); break;
		case 0x18C: Op8CX1(); break;
		case 0x18D: Op8DM1(); break;
		case 0x18E: Op8EX1(); break;
		case 0x18F: Op8FM1(); break;
		case 0x190: Op90E1(); break;
		case 0x191: Op91E1(); break;
		case 0x192: Op92E1(); break;
		case 0x193: Op93M1(); break;
		case 0x194: Op94E1(); break;
		case 0x195: Op95E1(); break;
		case 0x196: Op96E1(); break;
		case 0x197: Op97M1(); break;
		case 0x198: Op98M1(); break;
		case 0x199: Op99M1X1(); break;
		case 0x19A: Op9A(); break;
		case 0x19B: Op9BX1(); break;
		case 0x19C: Op9CM1(); break;
		case 0x19D: Op9DM1X1(); break;
		case 0x19E: Op9EM1X1(); break;
		case 0x19F: Op9FM1(); break;
		case 0x1A0: OpA0X1(); break;
		case 0x1A1: OpA1E1(); break;
		case 0x1A2: OpA2X1(); break;
		case 0x1A3: OpA3M1(); break;
		case 0x1A4: OpA4X1(); break;
		case 0x1A5: OpA5M1(); break;
		case 0x1A6: OpA6X1(); break;
		case 0x1A7: OpA7M1(); break;
		case 0x1A8: OpA8X1(); break;
		case 0x1A9: OpA9M1(); break;
		case 0x1AA: OpAAX1(); break;
		case 0x1AB: OpABE1(); break;
		case 0x1AC: OpACX1(); break;
		case 0x1AD: OpADM1(); break;
		case 0x1AE: OpAEX1(); break;
		case 0x1AF: OpAFM1(); break;
		case 0x1B0: OpB0E1(); break;
		case 0x1B1: OpB1E1(); break;
		case 0x1B2: OpB2E1(); break;
		case 0x1B3: OpB3M1(); break;
		case 0x1B4: OpB4E1(); break;
		case 0x1B5: OpB5E1(); break;
		case 0x1B6: OpB6E1(); break;
		case 0x1B7: OpB7M1(); break;
		case 0x1B8: OpB8(); break;
		case 0x1B9: OpB9M1X1(); break;
		case 0x1BA: OpBAX1(); break;
		case 0x1BB: OpBBX1(); break;
		case 0x1BC: OpBCX1(); break;
		case 0x1BD: OpBDM1X1(); break;
		case 0x1BE: OpBEX1(); break;
		case 0x1BF: OpBFM1(); break;
		case 0x1C0: OpC0X1(); break;
		case 0x1C1: OpC1E1(); break;
		case 0x1C2: OpC2(); break;
		case 0x1C3: OpC3M1(); break;
		case 0x1C4: OpC4X1(); break;
		case 0x1C5: OpC5M1(); break;
		case 0x1C6: OpC6M1(); break;
		case 0x1C7: OpC7M1(); break;
		case 0x1C8: OpC8X1(); break;
		case 0x1C9: OpC9M1(); break;
		case 0x1CA: OpCAX1(); break;
		case 0x1CB: OpCB(); break;
		case 0x1CC: OpCCX1(); break;
		case 0x1CD: OpCDM1(); break;
		case 0x1CE: OpCEM1(); break;
		case 0x1CF: OpCFM1(); break;
		case 0x1D0: OpD0E1(); break;
		case 0x1D1: OpD1E1(); break;
		case 0x1D2: OpD2E1(); break;
		case 0x1D3: OpD3M1(); break;
		case 0x1D4: OpD4E1(); break;
		case 0x1D5: OpD5E1(); break;
		case 0x1D6: OpD6E1(); break;
		case 0x1D7: OpD7M1(); break;
		case 0x1D8: OpD8(); break;
		case 0x1D9: OpD9M1X1(); break;
		case 0x1DA: OpDAE1(); break;
		case 0x1DB: OpDB(); break;
		case 0x1DC: OpDC(); break;
		case 0x1DD: OpDDM1X1(); break;
		case 0x1DE: OpDEM1X1(); break;
		case 0x1DF: OpDFM1(); break;
		case 0x1E0: OpE0X1(); break;
		case 0x1E1: OpE1E1(); break;
		case 0x1E2: OpE2(); break;
		case 0x1E3: OpE3M1(); break;
		case 0x1E4: OpE4X1(); break;
		case 0x1E5: OpE5M1(); break;
		case 0x1E6: OpE6M1(); break;
		case 0x1E7: OpE7M1(); break;
		case 0x1E8: OpE8X1(); break;
		case 0x1E9: OpE9M1(); break;
		case 0x1EA: OpEA(); break;
		case 0x1EB: OpEB(); break;
		case 0x1EC: OpECX1(); break;
		case 0x1ED: OpEDM1(); break;
		case 0x1EE: OpEEM1(); break;
		case 0x1EF: OpEFM1(); break;
		case 0x1F0: OpF0E1(); break;
		case 0x1F1: OpF1E1(); break;
		case 0x1F2: OpF2E1(); break;
		case 0x1F3: OpF3M1(); break;
		case 0x1F4: OpF4E1(); break;
		case 0x1F5: OpF5E1(); break;
		case 0x1F6: OpF6E1(); break;
		case 0x1F7: OpF7M1(); break;
		case 0x1F8: OpF8(); break;
		case 0x1F9: OpF9M1X1(); break;
		case 0x1FA: OpFAE1(); break;
		case 0x1FB: OpFB(); break;
		case 0x1FC: OpFCE1(); break;
		case 0x1FD: OpFDM1X1(); break;
		case 0x1FE: OpFEM1X1(); break;
		case 0x1FF: OpFFM1(); break;

		// OpcodesM1X0
		
		case 0x200: Op00(); break;
		case 0x201: Op01E0M1(); break;
		case 0x202: Op02(); break;
		case 0x203: Op03M1(); break;
		case 0x204: Op04M1(); break;
		case 0x205: Op05M1(); break;
		case 0x206: Op06M1(); break;
		case 0x207: Op07M1(); break;
		case 0x208: Op08E0(); break;
		case 0x209: Op09M1(); break;
		case 0x20A: Op0AM1(); break;
		case 0x20B: Op0BE0(); break;
		case 0x20C: Op0CM1(); break;
		case 0x20D: Op0DM1(); break;
		case 0x20E: Op0EM1(); break;
		case 0x20F: Op0FM1(); break;
		case 0x210: Op10E0(); break;
		case 0x211: Op11E0M1X0(); break;
		case 0x212: Op12E0M1(); break;
		case 0x213: Op13M1(); break;
		case 0x214: Op14M1(); break;
		case 0x215: Op15E0M1(); break;
		case 0x216: Op16E0M1(); break;
		case 0x217: Op17M1(); break;
		case 0x218: Op18(); break;
		case 0x219: Op19M1X0(); break;
		case 0x21A: Op1AM1(); break;
		case 0x21B: Op1B(); break;
		case 0x21C: Op1CM1(); break;
		case 0x21D: Op1DM1X0(); break;
		case 0x21E: Op1EM1X0(); break;
		case 0x21F: Op1FM1(); break;
		case 0x220: Op20E0(); break;
		case 0x221: Op21E0M1(); break;
		case 0x222: Op22E0(); break;
		case 0x223: Op23M1(); break;
		case 0x224: Op24M1(); break;
		case 0x225: Op25M1(); break;
		case 0x226: Op26M1(); break;
		case 0x227: Op27M1(); break;
		case 0x228: Op28E0(); break;
		case 0x229: Op29M1(); break;
		case 0x22A: Op2AM1(); break;
		case 0x22B: Op2BE0(); break;
		case 0x22C: Op2CM1(); break;
		case 0x22D: Op2DM1(); break;
		case 0x22E: Op2EM1(); break;
		case 0x22F: Op2FM1(); break;
		case 0x230: Op30E0(); break;
		case 0x231: Op31E0M1X0(); break;
		case 0x232: Op32E0M1(); break;
		case 0x233: Op33M1(); break;
		case 0x234: Op34E0M1(); break;
		case 0x235: Op35E0M1(); break;
		case 0x236: Op36E0M1(); break;
		case 0x237: Op37M1(); break;
		case 0x238: Op38(); break;
		case 0x239: Op39M1X0(); break;
		case 0x23A: Op3AM1(); break;
		case 0x23B: Op3B(); break;
		case 0x23C: Op3CM1X0(); break;
		case 0x23D: Op3DM1X0(); break;
		case 0x23E: Op3EM1X0(); break;
		case 0x23F: Op3FM1(); break;
		case 0x240: Op40Slow(); break;
		case 0x241: Op41E0M1(); break;
		case 0x242: Op42(); break;
		case 0x243: Op43M1(); break;
		case 0x244: Op44X0(); break;
		case 0x245: Op45M1(); break;
		case 0x246: Op46M1(); break;
		case 0x247: Op47M1(); break;
		case 0x248: Op48E0M1(); break;
		case 0x249: Op49M1(); break;
		case 0x24A: Op4AM1(); break;
		case 0x24B: Op4BE0(); break;
		case 0x24C: Op4C(); break;
		case 0x24D: Op4DM1(); break;
		case 0x24E: Op4EM1(); break;
		case 0x24F: Op4FM1(); break;
		case 0x250: Op50E0(); break;
		case 0x251: Op51E0M1X0(); break;
		case 0x252: Op52E0M1(); break;
		case 0x253: Op53M1(); break;
		case 0x254: Op54X0(); break;
		case 0x255: Op55E0M1(); break;
		case 0x256: Op56E0M1(); break;
		case 0x257: Op57M1(); break;
		case 0x258: Op58(); break;
		case 0x259: Op59M1X0(); break;
		case 0x25A: Op5AE0X0(); break;
		case 0x25B: Op5B(); break;
		case 0x25C: Op5C(); break;
		case 0x25D: Op5DM1X0(); break;
		case 0x25E: Op5EM1X0(); break;
		case 0x25F: Op5FM1(); break;
		case 0x260: Op60E0(); break;
		case 0x261: Op61E0M1(); break;
		case 0x262: Op62E0(); break;
		case 0x263: Op63M1(); break;
		case 0x264: Op64M1(); break;
		case 0x265: Op65M1(); break;
		case 0x266: Op66M1(); break;
		case 0x267: Op67M1(); break;
		case 0x268: Op68E0M1(); break;
		case 0x269: Op69M1(); break;
		case 0x26A: Op6AM1(); break;
		case 0x26B: Op6BE0(); break;
		case 0x26C: Op6C(); break;
		case 0x26D: Op6DM1(); break;
		case 0x26E: Op6EM1(); break;
		case 0x26F: Op6FM1(); break;
		case 0x270: Op70E0(); break;
		case 0x271: Op71E0M1X0(); break;
		case 0x272: Op72E0M1(); break;
		case 0x273: Op73M1(); break;
		case 0x274: Op74E0M1(); break;
		case 0x275: Op75E0M1(); break;
		case 0x276: Op76E0M1(); break;
		case 0x277: Op77M1(); break;
		case 0x278: Op78(); break;
		case 0x279: Op79M1X0(); break;
		case 0x27A: Op7AE0X0(); break;
		case 0x27B: Op7B(); break;
		case 0x27C: Op7C(); break;
		case 0x27D: Op7DM1X0(); break;
		case 0x27E: Op7EM1X0(); break;
		case 0x27F: Op7FM1(); break;
		case 0x280: Op80E0(); break;
		case 0x281: Op81E0M1(); break;
		case 0x282: Op82(); break;
		case 0x283: Op83M1(); break;
		case 0x284: Op84X0(); break;
		case 0x285: Op85M1(); break;
		case 0x286: Op86X0(); break;
		case 0x287: Op87M1(); break;
		case 0x288: Op88X0(); break;
		case 0x289: Op89M1(); break;
		case 0x28A: Op8AM1(); break;
		case 0x28B: Op8BE0(); break;
		case 0x28C: Op8CX0(); break;
		case 0x28D: Op8DM1(); break;
		case 0x28E: Op8EX0(); break;
		case 0x28F: Op8FM1(); break;
		case 0x290: Op90E0(); break;
		case 0x291: Op91E0M1X0(); break;
		case 0x292: Op92E0M1(); break;
		case 0x293: Op93M1(); break;
		case 0x294: Op94E0X0(); break;
		case 0x295: Op95E0M1(); break;
		case 0x296: Op96E0X0(); break;
		case 0x297: Op97M1(); break;
		case 0x298: Op98M1(); break;
		case 0x299: Op99M1X0(); break;
		case 0x29A: Op9A(); break;
		case 0x29B: Op9BX0(); break;
		case 0x29C: Op9CM1(); break;
		case 0x29D: Op9DM1X0(); break;
		case 0x29E: Op9EM1X0(); break;
		case 0x29F: Op9FM1(); break;
		case 0x2A0: OpA0X0(); break;
		case 0x2A1: OpA1E0M1(); break;
		case 0x2A2: OpA2X0(); break;
		case 0x2A3: OpA3M1(); break;
		case 0x2A4: OpA4X0(); break;
		case 0x2A5: OpA5M1(); break;
		case 0x2A6: OpA6X0(); break;
		case 0x2A7: OpA7M1(); break;
		case 0x2A8: OpA8X0(); break;
		case 0x2A9: OpA9M1(); break;
		case 0x2AA: OpAAX0(); break;
		case 0x2AB: OpABE0(); break;
		case 0x2AC: OpACX0(); break;
		case 0x2AD: OpADM1(); break;
		case 0x2AE: OpAEX0(); break;
		case 0x2AF: OpAFM1(); break;
		case 0x2B0: OpB0E0(); break;
		case 0x2B1: OpB1E0M1X0(); break;
		case 0x2B2: OpB2E0M1(); break;
		case 0x2B3: OpB3M1(); break;
		case 0x2B4: OpB4E0X0(); break;
		case 0x2B5: OpB5E0M1(); break;
		case 0x2B6: OpB6E0X0(); break;
		case 0x2B7: OpB7M1(); break;
		case 0x2B8: OpB8(); break;
		case 0x2B9: OpB9M1X0(); break;
		case 0x2BA: OpBAX0(); break;
		case 0x2BB: OpBBX0(); break;
		case 0x2BC: OpBCX0(); break;
		case 0x2BD: OpBDM1X0(); break;
		case 0x2BE: OpBEX0(); break;
		case 0x2BF: OpBFM1(); break;
		case 0x2C0: OpC0X0(); break;
		case 0x2C1: OpC1E0M1(); break;
		case 0x2C2: OpC2(); break;
		case 0x2C3: OpC3M1(); break;
		case 0x2C4: OpC4X0(); break;
		case 0x2C5: OpC5M1(); break;
		case 0x2C6: OpC6M1(); break;
		case 0x2C7: OpC7M1(); break;
		case 0x2C8: OpC8X0(); break;
		case 0x2C9: OpC9M1(); break;
		case 0x2CA: OpCAX0(); break;
		case 0x2CB: OpCB(); break;
		case 0x2CC: OpCCX0(); break;
		case 0x2CD: OpCDM1(); break;
		case 0x2CE: OpCEM1(); break;
		case 0x2CF: OpCFM1(); break;
		case 0x2D0: OpD0E0(); break;
		case 0x2D1: OpD1E0M1X0(); break;
		case 0x2D2: OpD2E0M1(); break;
		case 0x2D3: OpD3M1(); break;
		case 0x2D4: OpD4E0(); break;
		case 0x2D5: OpD5E0M1(); break;
		case 0x2D6: OpD6E0M1(); break;
		case 0x2D7: OpD7M1(); break;
		case 0x2D8: OpD8(); break;
		case 0x2D9: OpD9M1X0(); break;
		case 0x2DA: OpDAE0X0(); break;
		case 0x2DB: OpDB(); break;
		case 0x2DC: OpDC(); break;
		case 0x2DD: OpDDM1X0(); break;
		case 0x2DE: OpDEM1X0(); break;
		case 0x2DF: OpDFM1(); break;
		case 0x2E0: OpE0X0(); break;
		case 0x2E1: OpE1E0M1(); break;
		case 0x2E2: OpE2(); break;
		case 0x2E3: OpE3M1(); break;
		case 0x2E4: OpE4X0(); break;
		case 0x2E5: OpE5M1(); break;
		case 0x2E6: OpE6M1(); break;
		case 0x2E7: OpE7M1(); break;
		case 0x2E8: OpE8X0(); break;
		case 0x2E9: OpE9M1(); break;
		case 0x2EA: OpEA(); break;
		case 0x2EB: OpEB(); break;
		case 0x2EC: OpECX0(); break;
		case 0x2ED: OpEDM1(); break;
		case 0x2EE: OpEEM1(); break;
		case 0x2EF: OpEFM1(); break;
		case 0x2F0: OpF0E0(); break;
		case 0x2F1: OpF1E0M1X0(); break;
		case 0x2F2: OpF2E0M1(); break;
		case 0x2F3: OpF3M1(); break;
		case 0x2F4: OpF4E0(); break;
		case 0x2F5: OpF5E0M1(); break;
		case 0x2F6: OpF6E0M1(); break;
		case 0x2F7: OpF7M1(); break;
		case 0x2F8: OpF8(); break;
		case 0x2F9: OpF9M1X0(); break;
		case 0x2FA: OpFAE0X0(); break;
		case 0x2FB: OpFB(); break;
		case 0x2FC: OpFCE0(); break;
		case 0x2FD: OpFDM1X0(); break;
		case 0x2FE: OpFEM1X0(); break;
		case 0x2FF: OpFFM1(); break;
		
		// OpcodesM0X0
		case 0x300: Op00(); break;
		case 0x301: Op01E0M0(); break;
		case 0x302: Op02(); break;
		case 0x303: Op03M0(); break;
		case 0x304: Op04M0(); break;
		case 0x305: Op05M0(); break;
		case 0x306: Op06M0(); break;
		case 0x307: Op07M0(); break;
		case 0x308: Op08E0(); break;
		case 0x309: Op09M0(); break;
		case 0x30A: Op0AM0(); break;
		case 0x30B: Op0BE0(); break;
		case 0x30C: Op0CM0(); break;
		case 0x30D: Op0DM0(); break;
		case 0x30E: Op0EM0(); break;
		case 0x30F: Op0FM0(); break;
		case 0x310: Op10E0(); break;
		case 0x311: Op11E0M0X0(); break;
		case 0x312: Op12E0M0(); break;
		case 0x313: Op13M0(); break;
		case 0x314: Op14M0(); break;
		case 0x315: Op15E0M0(); break;
		case 0x316: Op16E0M0(); break;
		case 0x317: Op17M0(); break;
		case 0x318: Op18(); break;
		case 0x319: Op19M0X0(); break;
		case 0x31A: Op1AM0(); break;
		case 0x31B: Op1B(); break;
		case 0x31C: Op1CM0(); break;
		case 0x31D: Op1DM0X0(); break;
		case 0x31E: Op1EM0X0(); break;
		case 0x31F: Op1FM0(); break;
		case 0x320: Op20E0(); break;
		case 0x321: Op21E0M0(); break;
		case 0x322: Op22E0(); break;
		case 0x323: Op23M0(); break;
		case 0x324: Op24M0(); break;
		case 0x325: Op25M0(); break;
		case 0x326: Op26M0(); break;
		case 0x327: Op27M0(); break;
		case 0x328: Op28E0(); break;
		case 0x329: Op29M0(); break;
		case 0x32A: Op2AM0(); break;
		case 0x32B: Op2BE0(); break;
		case 0x32C: Op2CM0(); break;
		case 0x32D: Op2DM0(); break;
		case 0x32E: Op2EM0(); break;
		case 0x32F: Op2FM0(); break;
		case 0x330: Op30E0(); break;
		case 0x331: Op31E0M0X0(); break;
		case 0x332: Op32E0M0(); break;
		case 0x333: Op33M0(); break;
		case 0x334: Op34E0M0(); break;
		case 0x335: Op35E0M0(); break;
		case 0x336: Op36E0M0(); break;
		case 0x337: Op37M0(); break;
		case 0x338: Op38(); break;
		case 0x339: Op39M0X0(); break;
		case 0x33A: Op3AM0(); break;
		case 0x33B: Op3B(); break;
		case 0x33C: Op3CM0X0(); break;
		case 0x33D: Op3DM0X0(); break;
		case 0x33E: Op3EM0X0(); break;
		case 0x33F: Op3FM0(); break;
		case 0x340: Op40Slow(); break;
		case 0x341: Op41E0M0(); break;
		case 0x342: Op42(); break;
		case 0x343: Op43M0(); break;
		case 0x344: Op44X0(); break;
		case 0x345: Op45M0(); break;
		case 0x346: Op46M0(); break;
		case 0x347: Op47M0(); break;
		case 0x348: Op48E0M0(); break;
		case 0x349: Op49M0(); break;
		case 0x34A: Op4AM0(); break;
		case 0x34B: Op4BE0(); break;
		case 0x34C: Op4C(); break;
		case 0x34D: Op4DM0(); break;
		case 0x34E: Op4EM0(); break;
		case 0x34F: Op4FM0(); break;
		case 0x350: Op50E0(); break;
		case 0x351: Op51E0M0X0(); break;
		case 0x352: Op52E0M0(); break;
		case 0x353: Op53M0(); break;
		case 0x354: Op54X0(); break;
		case 0x355: Op55E0M0(); break;
		case 0x356: Op56E0M0(); break;
		case 0x357: Op57M0(); break;
		case 0x358: Op58(); break;
		case 0x359: Op59M0X0(); break;
		case 0x35A: Op5AE0X0(); break;
		case 0x35B: Op5B(); break;
		case 0x35C: Op5C(); break;
		case 0x35D: Op5DM0X0(); break;
		case 0x35E: Op5EM0X0(); break;
		case 0x35F: Op5FM0(); break;
		case 0x360: Op60E0(); break;
		case 0x361: Op61E0M0(); break;
		case 0x362: Op62E0(); break;
		case 0x363: Op63M0(); break;
		case 0x364: Op64M0(); break;
		case 0x365: Op65M0(); break;
		case 0x366: Op66M0(); break;
		case 0x367: Op67M0(); break;
		case 0x368: Op68E0M0(); break;
		case 0x369: Op69M0(); break;
		case 0x36A: Op6AM0(); break;
		case 0x36B: Op6BE0(); break;
		case 0x36C: Op6C(); break;
		case 0x36D: Op6DM0(); break;
		case 0x36E: Op6EM0(); break;
		case 0x36F: Op6FM0(); break;
		case 0x370: Op70E0(); break;
		case 0x371: Op71E0M0X0(); break;
		case 0x372: Op72E0M0(); break;
		case 0x373: Op73M0(); break;
		case 0x374: Op74E0M0(); break;
		case 0x375: Op75E0M0(); break;
		case 0x376: Op76E0M0(); break;
		case 0x377: Op77M0(); break;
		case 0x378: Op78(); break;
		case 0x379: Op79M0X0(); break;
		case 0x37A: Op7AE0X0(); break;
		case 0x37B: Op7B(); break;
		case 0x37C: Op7C(); break;
		case 0x37D: Op7DM0X0(); break;
		case 0x37E: Op7EM0X0(); break;
		case 0x37F: Op7FM0(); break;
		case 0x380: Op80E0(); break;
		case 0x381: Op81E0M0(); break;
		case 0x382: Op82(); break;
		case 0x383: Op83M0(); break;
		case 0x384: Op84X0(); break;
		case 0x385: Op85M0(); break;
		case 0x386: Op86X0(); break;
		case 0x387: Op87M0(); break;
		case 0x388: Op88X0(); break;
		case 0x389: Op89M0(); break;
		case 0x38A: Op8AM0(); break;
		case 0x38B: Op8BE0(); break;
		case 0x38C: Op8CX0(); break;
		case 0x38D: Op8DM0(); break;
		case 0x38E: Op8EX0(); break;
		case 0x38F: Op8FM0(); break;
		case 0x390: Op90E0(); break;
		case 0x391: Op91E0M0X0(); break;
		case 0x392: Op92E0M0(); break;
		case 0x393: Op93M0(); break;
		case 0x394: Op94E0X0(); break;
		case 0x395: Op95E0M0(); break;
		case 0x396: Op96E0X0(); break;
		case 0x397: Op97M0(); break;
		case 0x398: Op98M0(); break;
		case 0x399: Op99M0X0(); break;
		case 0x39A: Op9A(); break;
		case 0x39B: Op9BX0(); break;
		case 0x39C: Op9CM0(); break;
		case 0x39D: Op9DM0X0(); break;
		case 0x39E: Op9EM0X0(); break;
		case 0x39F: Op9FM0(); break;
		case 0x3A0: OpA0X0(); break;
		case 0x3A1: OpA1E0M0(); break;
		case 0x3A2: OpA2X0(); break;
		case 0x3A3: OpA3M0(); break;
		case 0x3A4: OpA4X0(); break;
		case 0x3A5: OpA5M0(); break;
		case 0x3A6: OpA6X0(); break;
		case 0x3A7: OpA7M0(); break;
		case 0x3A8: OpA8X0(); break;
		case 0x3A9: OpA9M0(); break;
		case 0x3AA: OpAAX0(); break;
		case 0x3AB: OpABE0(); break;
		case 0x3AC: OpACX0(); break;
		case 0x3AD: OpADM0(); break;
		case 0x3AE: OpAEX0(); break;
		case 0x3AF: OpAFM0(); break;
		case 0x3B0: OpB0E0(); break;
		case 0x3B1: OpB1E0M0X0(); break;
		case 0x3B2: OpB2E0M0(); break;
		case 0x3B3: OpB3M0(); break;
		case 0x3B4: OpB4E0X0(); break;
		case 0x3B5: OpB5E0M0(); break;
		case 0x3B6: OpB6E0X0(); break;
		case 0x3B7: OpB7M0(); break;
		case 0x3B8: OpB8(); break;
		case 0x3B9: OpB9M0X0(); break;
		case 0x3BA: OpBAX0(); break;
		case 0x3BB: OpBBX0(); break;
		case 0x3BC: OpBCX0(); break;
		case 0x3BD: OpBDM0X0(); break;
		case 0x3BE: OpBEX0(); break;
		case 0x3BF: OpBFM0(); break;
		case 0x3C0: OpC0X0(); break;
		case 0x3C1: OpC1E0M0(); break;
		case 0x3C2: OpC2(); break;
		case 0x3C3: OpC3M0(); break;
		case 0x3C4: OpC4X0(); break;
		case 0x3C5: OpC5M0(); break;
		case 0x3C6: OpC6M0(); break;
		case 0x3C7: OpC7M0(); break;
		case 0x3C8: OpC8X0(); break;
		case 0x3C9: OpC9M0(); break;
		case 0x3CA: OpCAX0(); break;
		case 0x3CB: OpCB(); break;
		case 0x3CC: OpCCX0(); break;
		case 0x3CD: OpCDM0(); break;
		case 0x3CE: OpCEM0(); break;
		case 0x3CF: OpCFM0(); break;
		case 0x3D0: OpD0E0(); break;
		case 0x3D1: OpD1E0M0X0(); break;
		case 0x3D2: OpD2E0M0(); break;
		case 0x3D3: OpD3M0(); break;
		case 0x3D4: OpD4E0(); break;
		case 0x3D5: OpD5E0M0(); break;
		case 0x3D6: OpD6E0M0(); break;
		case 0x3D7: OpD7M0(); break;
		case 0x3D8: OpD8(); break;
		case 0x3D9: OpD9M0X0(); break;
		case 0x3DA: OpDAE0X0(); break;
		case 0x3DB: OpDB(); break;
		case 0x3DC: OpDC(); break;
		case 0x3DD: OpDDM0X0(); break;
		case 0x3DE: OpDEM0X0(); break;
		case 0x3DF: OpDFM0(); break;
		case 0x3E0: OpE0X0(); break;
		case 0x3E1: OpE1E0M0(); break;
		case 0x3E2: OpE2(); break;
		case 0x3E3: OpE3M0(); break;
		case 0x3E4: OpE4X0(); break;
		case 0x3E5: OpE5M0(); break;
		case 0x3E6: OpE6M0(); break;
		case 0x3E7: OpE7M0(); break;
		case 0x3E8: OpE8X0(); break;
		case 0x3E9: OpE9M0(); break;
		case 0x3EA: OpEA(); break;
		case 0x3EB: OpEB(); break;
		case 0x3EC: OpECX0(); break;
		case 0x3ED: OpEDM0(); break;
		case 0x3EE: OpEEM0(); break;
		case 0x3EF: OpEFM0(); break;
		case 0x3F0: OpF0E0(); break;
		case 0x3F1: OpF1E0M0X0(); break;
		case 0x3F2: OpF2E0M0(); break;
		case 0x3F3: OpF3M0(); break;
		case 0x3F4: OpF4E0(); break;
		case 0x3F5: OpF5E0M0(); break;
		case 0x3F6: OpF6E0M0(); break;
		case 0x3F7: OpF7M0(); break;
		case 0x3F8: OpF8(); break;
		case 0x3F9: OpF9M0X0(); break;
		case 0x3FA: OpFAE0X0(); break;
		case 0x3FB: OpFB(); break;
		case 0x3FC: OpFCE0(); break;
		case 0x3FD: OpFDM0X0(); break;
		case 0x3FE: OpFEM0X0(); break;
		case 0x3FF: OpFFM0(); break;
		
		// OpcodesM0X1
		case 0x400: Op00(); break;
		case 0x401: Op01E0M0(); break;
		case 0x402: Op02(); break;
		case 0x403: Op03M0(); break;
		case 0x404: Op04M0(); break;
		case 0x405: Op05M0(); break;
		case 0x406: Op06M0(); break;
		case 0x407: Op07M0(); break;
		case 0x408: Op08E0(); break;
		case 0x409: Op09M0(); break;
		case 0x40A: Op0AM0(); break;
		case 0x40B: Op0BE0(); break;
		case 0x40C: Op0CM0(); break;
		case 0x40D: Op0DM0(); break;
		case 0x40E: Op0EM0(); break;
		case 0x40F: Op0FM0(); break;
		case 0x410: Op10E0(); break;
		case 0x411: Op11E0M0X1(); break;
		case 0x412: Op12E0M0(); break;
		case 0x413: Op13M0(); break;
		case 0x414: Op14M0(); break;
		case 0x415: Op15E0M0(); break;
		case 0x416: Op16E0M0(); break;
		case 0x417: Op17M0(); break;
		case 0x418: Op18(); break;
		case 0x419: Op19M0X1(); break;
		case 0x41A: Op1AM0(); break;
		case 0x41B: Op1B(); break;
		case 0x41C: Op1CM0(); break;
		case 0x41D: Op1DM0X1(); break;
		case 0x41E: Op1EM0X1(); break;
		case 0x41F: Op1FM0(); break;
		case 0x420: Op20E0(); break;
		case 0x421: Op21E0M0(); break;
		case 0x422: Op22E0(); break;
		case 0x423: Op23M0(); break;
		case 0x424: Op24M0(); break;
		case 0x425: Op25M0(); break;
		case 0x426: Op26M0(); break;
		case 0x427: Op27M0(); break;
		case 0x428: Op28E0(); break;
		case 0x429: Op29M0(); break;
		case 0x42A: Op2AM0(); break;
		case 0x42B: Op2BE0(); break;
		case 0x42C: Op2CM0(); break;
		case 0x42D: Op2DM0(); break;
		case 0x42E: Op2EM0(); break;
		case 0x42F: Op2FM0(); break;
		case 0x430: Op30E0(); break;
		case 0x431: Op31E0M0X1(); break;
		case 0x432: Op32E0M0(); break;
		case 0x433: Op33M0(); break;
		case 0x434: Op34E0M0(); break;
		case 0x435: Op35E0M0(); break;
		case 0x436: Op36E0M0(); break;
		case 0x437: Op37M0(); break;
		case 0x438: Op38(); break;
		case 0x439: Op39M0X1(); break;
		case 0x43A: Op3AM0(); break;
		case 0x43B: Op3B(); break;
		case 0x43C: Op3CM0X1(); break;
		case 0x43D: Op3DM0X1(); break;
		case 0x43E: Op3EM0X1(); break;
		case 0x43F: Op3FM0(); break;
		case 0x440: Op40Slow(); break;
		case 0x441: Op41E0M0(); break;
		case 0x442: Op42(); break;
		case 0x443: Op43M0(); break;
		case 0x444: Op44X1(); break;
		case 0x445: Op45M0(); break;
		case 0x446: Op46M0(); break;
		case 0x447: Op47M0(); break;
		case 0x448: Op48E0M0(); break;
		case 0x449: Op49M0(); break;
		case 0x44A: Op4AM0(); break;
		case 0x44B: Op4BE0(); break;
		case 0x44C: Op4C(); break;
		case 0x44D: Op4DM0(); break;
		case 0x44E: Op4EM0(); break;
		case 0x44F: Op4FM0(); break;
		case 0x450: Op50E0(); break;
		case 0x451: Op51E0M0X1(); break;
		case 0x452: Op52E0M0(); break;
		case 0x453: Op53M0(); break;
		case 0x454: Op54X1(); break;
		case 0x455: Op55E0M0(); break;
		case 0x456: Op56E0M0(); break;
		case 0x457: Op57M0(); break;
		case 0x458: Op58(); break;
		case 0x459: Op59M0X1(); break;
		case 0x45A: Op5AE0X1(); break;
		case 0x45B: Op5B(); break;
		case 0x45C: Op5C(); break;
		case 0x45D: Op5DM0X1(); break;
		case 0x45E: Op5EM0X1(); break;
		case 0x45F: Op5FM0(); break;
		case 0x460: Op60E0(); break;
		case 0x461: Op61E0M0(); break;
		case 0x462: Op62E0(); break;
		case 0x463: Op63M0(); break;
		case 0x464: Op64M0(); break;
		case 0x465: Op65M0(); break;
		case 0x466: Op66M0(); break;
		case 0x467: Op67M0(); break;
		case 0x468: Op68E0M0(); break;
		case 0x469: Op69M0(); break;
		case 0x46A: Op6AM0(); break;
		case 0x46B: Op6BE0(); break;
		case 0x46C: Op6C(); break;
		case 0x46D: Op6DM0(); break;
		case 0x46E: Op6EM0(); break;
		case 0x46F: Op6FM0(); break;
		case 0x470: Op70E0(); break;
		case 0x471: Op71E0M0X1(); break;
		case 0x472: Op72E0M0(); break;
		case 0x473: Op73M0(); break;
		case 0x474: Op74E0M0(); break;
		case 0x475: Op75E0M0(); break;
		case 0x476: Op76E0M0(); break;
		case 0x477: Op77M0(); break;
		case 0x478: Op78(); break;
		case 0x479: Op79M0X1(); break;
		case 0x47A: Op7AE0X1(); break;
		case 0x47B: Op7B(); break;
		case 0x47C: Op7C(); break;
		case 0x47D: Op7DM0X1(); break;
		case 0x47E: Op7EM0X1(); break;
		case 0x47F: Op7FM0(); break;
		case 0x480: Op80E0(); break;
		case 0x481: Op81E0M0(); break;
		case 0x482: Op82(); break;
		case 0x483: Op83M0(); break;
		case 0x484: Op84X1(); break;
		case 0x485: Op85M0(); break;
		case 0x486: Op86X1(); break;
		case 0x487: Op87M0(); break;
		case 0x488: Op88X1(); break;
		case 0x489: Op89M0(); break;
		case 0x48A: Op8AM0(); break;
		case 0x48B: Op8BE0(); break;
		case 0x48C: Op8CX1(); break;
		case 0x48D: Op8DM0(); break;
		case 0x48E: Op8EX1(); break;
		case 0x48F: Op8FM0(); break;
		case 0x490: Op90E0(); break;
		case 0x491: Op91E0M0X1(); break;
		case 0x492: Op92E0M0(); break;
		case 0x493: Op93M0(); break;
		case 0x494: Op94E0X1(); break;
		case 0x495: Op95E0M0(); break;
		case 0x496: Op96E0X1(); break;
		case 0x497: Op97M0(); break;
		case 0x498: Op98M0(); break;
		case 0x499: Op99M0X1(); break;
		case 0x49A: Op9A(); break;
		case 0x49B: Op9BX1(); break;
		case 0x49C: Op9CM0(); break;
		case 0x49D: Op9DM0X1(); break;
		case 0x49E: Op9EM0X1(); break;
		case 0x49F: Op9FM0(); break;
		case 0x4A0: OpA0X1(); break;
		case 0x4A1: OpA1E0M0(); break;
		case 0x4A2: OpA2X1(); break;
		case 0x4A3: OpA3M0(); break;
		case 0x4A4: OpA4X1(); break;
		case 0x4A5: OpA5M0(); break;
		case 0x4A6: OpA6X1(); break;
		case 0x4A7: OpA7M0(); break;
		case 0x4A8: OpA8X1(); break;
		case 0x4A9: OpA9M0(); break;
		case 0x4AA: OpAAX1(); break;
		case 0x4AB: OpABE0(); break;
		case 0x4AC: OpACX1(); break;
		case 0x4AD: OpADM0(); break;
		case 0x4AE: OpAEX1(); break;
		case 0x4AF: OpAFM0(); break;
		case 0x4B0: OpB0E0(); break;
		case 0x4B1: OpB1E0M0X1(); break;
		case 0x4B2: OpB2E0M0(); break;
		case 0x4B3: OpB3M0(); break;
		case 0x4B4: OpB4E0X1(); break;
		case 0x4B5: OpB5E0M0(); break;
		case 0x4B6: OpB6E0X1(); break;
		case 0x4B7: OpB7M0(); break;
		case 0x4B8: OpB8(); break;
		case 0x4B9: OpB9M0X1(); break;
		case 0x4BA: OpBAX1(); break;
		case 0x4BB: OpBBX1(); break;
		case 0x4BC: OpBCX1(); break;
		case 0x4BD: OpBDM0X1(); break;
		case 0x4BE: OpBEX1(); break;
		case 0x4BF: OpBFM0(); break;
		case 0x4C0: OpC0X1(); break;
		case 0x4C1: OpC1E0M0(); break;
		case 0x4C2: OpC2(); break;
		case 0x4C3: OpC3M0(); break;
		case 0x4C4: OpC4X1(); break;
		case 0x4C5: OpC5M0(); break;
		case 0x4C6: OpC6M0(); break;
		case 0x4C7: OpC7M0(); break;
		case 0x4C8: OpC8X1(); break;
		case 0x4C9: OpC9M0(); break;
		case 0x4CA: OpCAX1(); break;
		case 0x4CB: OpCB(); break;
		case 0x4CC: OpCCX1(); break;
		case 0x4CD: OpCDM0(); break;
		case 0x4CE: OpCEM0(); break;
		case 0x4CF: OpCFM0(); break;
		case 0x4D0: OpD0E0(); break;
		case 0x4D1: OpD1E0M0X1(); break;
		case 0x4D2: OpD2E0M0(); break;
		case 0x4D3: OpD3M0(); break;
		case 0x4D4: OpD4E0(); break;
		case 0x4D5: OpD5E0M0(); break;
		case 0x4D6: OpD6E0M0(); break;
		case 0x4D7: OpD7M0(); break;
		case 0x4D8: OpD8(); break;
		case 0x4D9: OpD9M0X1(); break;
		case 0x4DA: OpDAE0X1(); break;
		case 0x4DB: OpDB(); break;
		case 0x4DC: OpDC(); break;
		case 0x4DD: OpDDM0X1(); break;
		case 0x4DE: OpDEM0X1(); break;
		case 0x4DF: OpDFM0(); break;
		case 0x4E0: OpE0X1(); break;
		case 0x4E1: OpE1E0M0(); break;
		case 0x4E2: OpE2(); break;
		case 0x4E3: OpE3M0(); break;
		case 0x4E4: OpE4X1(); break;
		case 0x4E5: OpE5M0(); break;
		case 0x4E6: OpE6M0(); break;
		case 0x4E7: OpE7M0(); break;
		case 0x4E8: OpE8X1(); break;
		case 0x4E9: OpE9M0(); break;
		case 0x4EA: OpEA(); break;
		case 0x4EB: OpEB(); break;
		case 0x4EC: OpECX1(); break;
		case 0x4ED: OpEDM0(); break;
		case 0x4EE: OpEEM0(); break;
		case 0x4EF: OpEFM0(); break;
		case 0x4F0: OpF0E0(); break;
		case 0x4F1: OpF1E0M0X1(); break;
		case 0x4F2: OpF2E0M0(); break;
		case 0x4F3: OpF3M0(); break;
		case 0x4F4: OpF4E0(); break;
		case 0x4F5: OpF5E0M0(); break;
		case 0x4F6: OpF6E0M0(); break;
		case 0x4F7: OpF7M0(); break;
		case 0x4F8: OpF8(); break;
		case 0x4F9: OpF9M0X1(); break;
		case 0x4FA: OpFAE0X1(); break;
		case 0x4FB: OpFB(); break;
		case 0x4FC: OpFCE0(); break;
		case 0x4FD: OpFDM0X1(); break;
		case 0x4FE: OpFEM0X1(); break;
		case 0x4FF: OpFFM0(); break;
		
		// OpcodesSlow
		case 0x500: Op00(); break;
		case 0x501: Op01Slow(); break;
		case 0x502: Op02(); break;
		case 0x503: Op03Slow(); break;
		case 0x504: Op04Slow(); break;
		case 0x505: Op05Slow(); break;
		case 0x506: Op06Slow(); break;
		case 0x507: Op07Slow(); break;
		case 0x508: Op08Slow(); break;
		case 0x509: Op09Slow(); break;
		case 0x50A: Op0ASlow(); break;
		case 0x50B: Op0BSlow(); break;
		case 0x50C: Op0CSlow(); break;
		case 0x50D: Op0DSlow(); break;
		case 0x50E: Op0ESlow(); break;
		case 0x50F: Op0FSlow(); break;
		case 0x510: Op10Slow(); break;
		case 0x511: Op11Slow(); break;
		case 0x512: Op12Slow(); break;
		case 0x513: Op13Slow(); break;
		case 0x514: Op14Slow(); break;
		case 0x515: Op15Slow(); break;
		case 0x516: Op16Slow(); break;
		case 0x517: Op17Slow(); break;
		case 0x518: Op18(); break;
		case 0x519: Op19Slow(); break;
		case 0x51A: Op1ASlow(); break;
		case 0x51B: Op1B(); break;
		case 0x51C: Op1CSlow(); break;
		case 0x51D: Op1DSlow(); break;
		case 0x51E: Op1ESlow(); break;
		case 0x51F: Op1FSlow(); break;
		case 0x520: Op20Slow(); break;
		case 0x521: Op21Slow(); break;
		case 0x522: Op22Slow(); break;
		case 0x523: Op23Slow(); break;
		case 0x524: Op24Slow(); break;
		case 0x525: Op25Slow(); break;
		case 0x526: Op26Slow(); break;
		case 0x527: Op27Slow(); break;
		case 0x528: Op28Slow(); break;
		case 0x529: Op29Slow(); break;
		case 0x52A: Op2ASlow(); break;
		case 0x52B: Op2BSlow(); break;
		case 0x52C: Op2CSlow(); break;
		case 0x52D: Op2DSlow(); break;
		case 0x52E: Op2ESlow(); break;
		case 0x52F: Op2FSlow(); break;
		case 0x530: Op30Slow(); break;
		case 0x531: Op31Slow(); break;
		case 0x532: Op32Slow(); break;
		case 0x533: Op33Slow(); break;
		case 0x534: Op34Slow(); break;
		case 0x535: Op35Slow(); break;
		case 0x536: Op36Slow(); break;
		case 0x537: Op37Slow(); break;
		case 0x538: Op38(); break;
		case 0x539: Op39Slow(); break;
		case 0x53A: Op3ASlow(); break;
		case 0x53B: Op3B(); break;
		case 0x53C: Op3CSlow(); break;
		case 0x53D: Op3DSlow(); break;
		case 0x53E: Op3ESlow(); break;
		case 0x53F: Op3FSlow(); break;
		case 0x540: Op40Slow(); break;
		case 0x541: Op41Slow(); break;
		case 0x542: Op42(); break;
		case 0x543: Op43Slow(); break;
		case 0x544: Op44Slow(); break;
		case 0x545: Op45Slow(); break;
		case 0x546: Op46Slow(); break;
		case 0x547: Op47Slow(); break;
		case 0x548: Op48Slow(); break;
		case 0x549: Op49Slow(); break;
		case 0x54A: Op4ASlow(); break;
		case 0x54B: Op4BSlow(); break;
		case 0x54C: Op4CSlow(); break;
		case 0x54D: Op4DSlow(); break;
		case 0x54E: Op4ESlow(); break;
		case 0x54F: Op4FSlow(); break;
		case 0x550: Op50Slow(); break;
		case 0x551: Op51Slow(); break;
		case 0x552: Op52Slow(); break;
		case 0x553: Op53Slow(); break;
		case 0x554: Op54Slow(); break;
		case 0x555: Op55Slow(); break;
		case 0x556: Op56Slow(); break;
		case 0x557: Op57Slow(); break;
		case 0x558: Op58(); break;
		case 0x559: Op59Slow(); break;
		case 0x55A: Op5ASlow(); break;
		case 0x55B: Op5B(); break;
		case 0x55C: Op5CSlow(); break;
		case 0x55D: Op5DSlow(); break;
		case 0x55E: Op5ESlow(); break;
		case 0x55F: Op5FSlow(); break;
		case 0x560: Op60Slow(); break;
		case 0x561: Op61Slow(); break;
		case 0x562: Op62Slow(); break;
		case 0x563: Op63Slow(); break;
		case 0x564: Op64Slow(); break;
		case 0x565: Op65Slow(); break;
		case 0x566: Op66Slow(); break;
		case 0x567: Op67Slow(); break;
		case 0x568: Op68Slow(); break;
		case 0x569: Op69Slow(); break;
		case 0x56A: Op6ASlow(); break;
		case 0x56B: Op6BSlow(); break;
		case 0x56C: Op6CSlow(); break;
		case 0x56D: Op6DSlow(); break;
		case 0x56E: Op6ESlow(); break;
		case 0x56F: Op6FSlow(); break;
		case 0x570: Op70Slow(); break;
		case 0x571: Op71Slow(); break;
		case 0x572: Op72Slow(); break;
		case 0x573: Op73Slow(); break;
		case 0x574: Op74Slow(); break;
		case 0x575: Op75Slow(); break;
		case 0x576: Op76Slow(); break;
		case 0x577: Op77Slow(); break;
		case 0x578: Op78(); break;
		case 0x579: Op79Slow(); break;
		case 0x57A: Op7ASlow(); break;
		case 0x57B: Op7B(); break;
		case 0x57C: Op7CSlow(); break;
		case 0x57D: Op7DSlow(); break;
		case 0x57E: Op7ESlow(); break;
		case 0x57F: Op7FSlow(); break;
		case 0x580: Op80Slow(); break;
		case 0x581: Op81Slow(); break;
		case 0x582: Op82Slow(); break;
		case 0x583: Op83Slow(); break;
		case 0x584: Op84Slow(); break;
		case 0x585: Op85Slow(); break;
		case 0x586: Op86Slow(); break;
		case 0x587: Op87Slow(); break;
		case 0x588: Op88Slow(); break;
		case 0x589: Op89Slow(); break;
		case 0x58A: Op8ASlow(); break;
		case 0x58B: Op8BSlow(); break;
		case 0x58C: Op8CSlow(); break;
		case 0x58D: Op8DSlow(); break;
		case 0x58E: Op8ESlow(); break;
		case 0x58F: Op8FSlow(); break;
		case 0x590: Op90Slow(); break;
		case 0x591: Op91Slow(); break;
		case 0x592: Op92Slow(); break;
		case 0x593: Op93Slow(); break;
		case 0x594: Op94Slow(); break;
		case 0x595: Op95Slow(); break;
		case 0x596: Op96Slow(); break;
		case 0x597: Op97Slow(); break;
		case 0x598: Op98Slow(); break;
		case 0x599: Op99Slow(); break;
		case 0x59A: Op9A(); break;
		case 0x59B: Op9BSlow(); break;
		case 0x59C: Op9CSlow(); break;
		case 0x59D: Op9DSlow(); break;
		case 0x59E: Op9ESlow(); break;
		case 0x59F: Op9FSlow(); break;
		case 0x5A0: OpA0Slow(); break;
		case 0x5A1: OpA1Slow(); break;
		case 0x5A2: OpA2Slow(); break;
		case 0x5A3: OpA3Slow(); break;
		case 0x5A4: OpA4Slow(); break;
		case 0x5A5: OpA5Slow(); break;
		case 0x5A6: OpA6Slow(); break;
		case 0x5A7: OpA7Slow(); break;
		case 0x5A8: OpA8Slow(); break;
		case 0x5A9: OpA9Slow(); break;
		case 0x5AA: OpAASlow(); break;
		case 0x5AB: OpABSlow(); break;
		case 0x5AC: OpACSlow(); break;
		case 0x5AD: OpADSlow(); break;
		case 0x5AE: OpAESlow(); break;
		case 0x5AF: OpAFSlow(); break;
		case 0x5B0: OpB0Slow(); break;
		case 0x5B1: OpB1Slow(); break;
		case 0x5B2: OpB2Slow(); break;
		case 0x5B3: OpB3Slow(); break;
		case 0x5B4: OpB4Slow(); break;
		case 0x5B5: OpB5Slow(); break;
		case 0x5B6: OpB6Slow(); break;
		case 0x5B7: OpB7Slow(); break;
		case 0x5B8: OpB8(); break;
		case 0x5B9: OpB9Slow(); break;
		case 0x5BA: OpBASlow(); break;
		case 0x5BB: OpBBSlow(); break;
		case 0x5BC: OpBCSlow(); break;
		case 0x5BD: OpBDSlow(); break;
		case 0x5BE: OpBESlow(); break;
		case 0x5BF: OpBFSlow(); break;
		case 0x5C0: OpC0Slow(); break;
		case 0x5C1: OpC1Slow(); break;
		case 0x5C2: OpC2Slow(); break;
		case 0x5C3: OpC3Slow(); break;
		case 0x5C4: OpC4Slow(); break;
		case 0x5C5: OpC5Slow(); break;
		case 0x5C6: OpC6Slow(); break;
		case 0x5C7: OpC7Slow(); break;
		case 0x5C8: OpC8Slow(); break;
		case 0x5C9: OpC9Slow(); break;
		case 0x5CA: OpCASlow(); break;
		case 0x5CB: OpCB(); break;
		case 0x5CC: OpCCSlow(); break;
		case 0x5CD: OpCDSlow(); break;
		case 0x5CE: OpCESlow(); break;
		case 0x5CF: OpCFSlow(); break;
		case 0x5D0: OpD0Slow(); break;
		case 0x5D1: OpD1Slow(); break;
		case 0x5D2: OpD2Slow(); break;
		case 0x5D3: OpD3Slow(); break;
		case 0x5D4: OpD4Slow(); break;
		case 0x5D5: OpD5Slow(); break;
		case 0x5D6: OpD6Slow(); break;
		case 0x5D7: OpD7Slow(); break;
		case 0x5D8: OpD8(); break;
		case 0x5D9: OpD9Slow(); break;
		case 0x5DA: OpDASlow(); break;
		case 0x5DB: OpDB(); break;
		case 0x5DC: OpDCSlow(); break;
		case 0x5DD: OpDDSlow(); break;
		case 0x5DE: OpDESlow(); break;
		case 0x5DF: OpDFSlow(); break;
		case 0x5E0: OpE0Slow(); break;
		case 0x5E1: OpE1Slow(); break;
		case 0x5E2: OpE2Slow(); break;
		case 0x5E3: OpE3Slow(); break;
		case 0x5E4: OpE4Slow(); break;
		case 0x5E5: OpE5Slow(); break;
		case 0x5E6: OpE6Slow(); break;
		case 0x5E7: OpE7Slow(); break;
		case 0x5E8: OpE8Slow(); break;
		case 0x5E9: OpE9Slow(); break;
		case 0x5EA: OpEA(); break;
		case 0x5EB: OpEB(); break;
		case 0x5EC: OpECSlow(); break;
		case 0x5ED: OpEDSlow(); break;
		case 0x5EE: OpEESlow(); break;
		case 0x5EF: OpEFSlow(); break;
		case 0x5F0: OpF0Slow(); break;
		case 0x5F1: OpF1Slow(); break;
		case 0x5F2: OpF2Slow(); break;
		case 0x5F3: OpF3Slow(); break;
		case 0x5F4: OpF4Slow(); break;
		case 0x5F5: OpF5Slow(); break;
		case 0x5F6: OpF6Slow(); break;
		case 0x5F7: OpF7Slow(); break;
		case 0x5F8: OpF8(); break;
		case 0x5F9: OpF9Slow(); break;
		case 0x5FA: OpFASlow(); break;
		case 0x5FB: OpFB(); break;
		case 0x5FC: OpFCSlow(); break;
		case 0x5FD: OpFDSlow(); break;
		case 0x5FE: OpFESlow(); break;
		case 0x5FF: OpFFSlow(); break;
		default:
			System.out.println("Unknown op code");
		}
	}

}